package org.contractlib.parser;

import java.io.Reader;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.ArrayList;

import org.contractlib.util.Pair;

import org.contractlib.sexpr.*;
import org.contractlib.factory.*;

/**
 * This is the old parser for CONTRACT-LIB that is not based on the ANTLR grammar, but uses a simple
 * jflex s-expression grammar. The commands are then matched and converted "by hand".
 */
@Deprecated
public class HandwrittenParser extends Scanner {
    public HandwrittenParser(Reader reader) {
        super(reader);
    }

    public List<SExpr> sexprs() throws IOException {
        Token token;

        Stack<Object> stack = new Stack<>();

        while ((token = next()) != EOF) {
            if (token == LPAREN) {
                stack.push(token);
            } else if (token == RPAREN) {
                List<SExpr> arguments = new ArrayList<>();

                Object expr;
                while ((expr = stack.pop()) != LPAREN) {
                    arguments.add((SExpr) expr);
                }

                Collections.reverse(arguments);
                expr = new SList(arguments);

                stack.push(expr);
            } else {
                stack.push(token);
            }
        }

        List<SExpr> result = new ArrayList<>();

        for (Object expr : stack) {
            result.add((SExpr) expr);
        }

        return result;
    }

    static interface Action<T> {
        T get() throws IOException;
    }

    static interface Function<T, R> {
        R get(T t) throws IOException;
    }

    public <T> T parens(Action<T> action) throws IOException {
        expect(LPAREN);
        T result = action.get();
        expect(RPAREN);
        return result;
    }

    public <T> List<T> repeat(Action<T> action) throws IOException {
        T elem;
        List<T> result = new ArrayList<>();

        while ((elem = action.get()) != null) {
            result.add(elem);
        }

        return result;
    }

    public <T> T maybeParametricWithImplicitLParen(Function<List<String>, T> action) throws IOException {
        if (check(LPAREN)) {
            Token token = peek();

            switch (token) {
            case Identifier identifier:
                if (identifier.name().equals("par")) {
                    next();

                    List<String> params = parens(() -> params());
                    expect(LPAREN);
                    T result = action.get(params);
                    expect(RPAREN);
                    return result;
                }

            default:
                List<String> params = List.of();
                return action.get(params);
            }
        } else {
            List<String> params = List.of();
            return action.get(params);
        }
    }

    public <Term, Type, Abstraction, Datatype, FunDecll, Command> List<Command> script(Commands<Term, Type, Abstraction, Datatype, FunDecll, Command> factory)
            throws IOException {
        return repeat(() -> command(factory));
    }

    public <Term, Type> List<Term> terms(Types<Type> context, Terms<Term, Type> scope) throws IOException {
        return repeat(() -> term(context, scope));
    }

    public <Term, Type> List<Pair<Term, Term>> contracts(Types<Type> context, Terms<Term, Type> scope)
            throws IOException {
        return repeat(() -> contract(context, scope));
    }

    public <Term, Type> List<Type> types(Types<Type> context) throws IOException {
        return repeat(() -> type(context));
    }

    public <Type> List<Pair<String, List<Pair<String, Type>>>> constructors(Types<Type> context)
            throws IOException {
        return repeat(() -> constructor(context));
    }

    public <Type> List<Pair<String, Type>> selectors(Types<Type> context) throws IOException {
        return repeat(() -> selector(context));
    }

    public <Type> List<Pair<String, Type>> formals(Types<Type> context) throws IOException {
        return repeat(() -> formal(context));
    }

    public <Type, Datatype> List<Datatype> datatypes(Datatypes<Type, Datatype> data) throws IOException {
        return repeat(() -> datatype(data));
    }

    public <Type, Abstraction> List<Abstraction> abstractions(Abstractions<Type, Abstraction> data) throws IOException {
        return repeat(() -> abstraction(data));
    }

    public <Type> List<Pair<String, Pair<Mode, Type>>> formalsWithMode(Types<Type> context) throws IOException {
        return repeat(() -> formalWithMode(context));
    }

    public List<String> identifiers() throws IOException {
        return repeat(() -> identifier());
    }

    public List<String> params() throws IOException {
        return repeat(() -> param());
    }

    public List<Pair<String, Integer>> arities() throws IOException {
        return repeat(() -> arity());
    }

    public <Type> Type type(Types<Type> context) throws IOException {
        Token token = peek();

        switch (token) {
        case Identifier id:
            next();
            return context.identifier(id.name());

        case LParen lp:
            expect(LPAREN);
            String name = identifier();
            List<Type> arguments = types(context);
            expect(RPAREN);

            return context.sort(name, arguments);

        default:
            return null;
        }
    }

    public <Type> Pair<String, Type> selector(Types<Type> context) throws IOException {
        if (check(LPAREN)) {
            String name = identifier();
            Type types = type(context);
            expect(RPAREN);

            return new Pair<>(name, types);
        } else {
            return null;
        }
    }

    public <Type> Pair<String, List<Pair<String, Type>>> constructor(Types<Type> context) throws IOException {
        if (check(LPAREN)) {
            String name = identifier();
            List<Pair<String, Type>> selectors = selectors(context);
            expect(RPAREN);

            return new Pair<>(name, selectors);
        } else {
            return null;
        }
    }

    public <Term, Type> Pair<Term, Term> contract(Types<Type> context, Terms<Term, Type> scope) throws IOException {
        if (check(LPAREN)) {
            Term precondition = term(context, scope);
            Term postcondition = term(context, scope);
            expect(RPAREN);

            return new Pair<>(precondition, postcondition);

        } else {
            return null;
        }
    }

    public <Type> Pair<String, Type> formal(Types<Type> context) throws IOException {
        if (check(LPAREN)) {
            String name = identifier();
            Type type = type(context);
            Pair<String, Type> result = new Pair(name, type);
            expect(RPAREN);

            return result;

        } else {
            return null;
        }
    }

    public String param() throws IOException {
        Token token = peek();

        switch (token) {
        case Identifier identifier:
            next();

            return identifier.name();

        default:
            return null;
        }
    }

    public Pair<String, Integer> arity() throws IOException {
        if (check(LPAREN)) {
            String id = identifier();
            Integer args = integer();
            expect(RPAREN);

            return new Pair(id, args);
        } else {
            return null;
        }
    }

    public <Type, Datatype> Datatype datatype(Datatypes<Type, Datatype> data) throws IOException {
        if (peek(LPAREN)) {
            return maybeParametricWithImplicitLParen((params) -> {
                Types<Type> context = data.types(params);
                List<Pair<String, List<Pair<String, Type>>>> constructors = constructors(context);
                expect(RPAREN);

                return data.datatype(params, constructors);
            });
        } else {
            return null;
        }
    }

    public <Type, Abstraction> Abstraction abstraction(Abstractions<Type, Abstraction> data) throws IOException {
        if (peek(LPAREN)) {
            return maybeParametricWithImplicitLParen((params) -> {
                Types<Type> context = data.types(params);
                List<Pair<String, List<Pair<String, Type>>>> constructors = constructors(context);
                expect(RPAREN);

                return data.abstraction(params, constructors);
            });
        } else {
            return null;
        }
    }

    public Mode mode() throws IOException {
        String id = identifier();

        switch (id) {
        case "in":
            return Mode.IN;
        case "out":
            return Mode.OUT;
        case "inout":
            return Mode.INOUT;
        default:
            unexpected(new Identifier(id));
            return null;
        }
    }

    public <Type> Pair<String, Pair<Mode, Type>> formalWithMode(Types<Type> context) throws IOException {
        if (check(LPAREN)) {
            String name = identifier();

            expect(LPAREN);
            Mode mode = mode();
            Type type = type(context);
            expect(RPAREN);

            Pair<String, Pair<Mode, Type>> result = new Pair(name, new Pair(mode, type));
            expect(RPAREN);

            return result;

        } else {
            return null;
        }
    }

    public <Term, Type, Abstraction, Datatype, FunDecll, Command> Command command(Commands<Term, Type, Abstraction, Datatype, FunDecll, Command> factory)
            throws IOException {
        if (check(LPAREN)) {
            String command = identifier();

            switch (command) {
            case "assert": {
                List<String> params = List.of();
                List<Pair<String, Type>> variables = List.of();
                Types<Type> context = factory.types(params);
                Terms<Term, Type> scope = factory.terms(variables);
                Term formula = term(context, scope);
                expect(RPAREN);

                return factory.assertion(formula);
            }

            case "declare-sort": {
                String sort = identifier();
                int arity = integer();
                expect(RPAREN);

                return factory.declareSort(sort, arity);
            }

            case "define-sort": {
                String sort = identifier();
                List<String> params = parens(() -> params());
                Types<Type> context = factory.types(params);
                Type body = type(context);
                expect(RPAREN);

                return factory.defineSort(sort, params, body);
            }

            case "declare-fun": {
                String function = identifier();

                return maybeParametricWithImplicitLParen((params) -> {
                    Types<Type> context = factory.types(params);

                    // Note: LPAREN is already consumed by the (par check
                    List<Type> arguments = types(context);
                    expect(RPAREN);

                    Type result = type(context);
                    expect(RPAREN);

                    return factory.declareFun(function, params, arguments, result);
                });
            }

            case "define-fun": {
                String function = identifier();
                return maybeParametricWithImplicitLParen((params) -> {
                    Types<Type> context = factory.types(params);

                    // Note: LPAREN is already consumed by the (par check
                    List<Pair<String, Type>> arguments = formals(context);
                    expect(RPAREN);

                    Type result = type(context);
                    Terms<Term, Type> scope = factory.terms(arguments);
                    Term body = term(context, scope);
                    expect(RPAREN);

                    // TODO: support recursion?
                    return factory.defineFun(function, params, arguments, result, body);
                });
            }

            case "declare-datatypes": {
                List<Pair<String, Integer>> arities = parens(() -> arities());
                Datatypes<Type, Datatype> data = factory.datatypes(arities);

                List<Datatype> datatypes = parens(() -> datatypes(data));

                return factory.declareDatatypes(arities, datatypes);
            }

            case "declare-abstractions": {
                List<Pair<String, Integer>> arities = parens(() -> arities());
                Abstractions<Type, Abstraction> data = factory.abstractions(arities);

                List<Abstraction> abstractions = parens(() -> abstractions(data));

                return factory.declareAbstractions(arities, abstractions);
            }

            case "define-contract": {
                String procedure = identifier();
                List<String> params = List.of();
                Types<Type> context = factory.types(params);
                List<Pair<String, Pair<Mode, Type>>> arguments = parens(() -> formalsWithMode(context));

                List<Pair<String, Type>> arguments_ = new ArrayList<>();
                for (Pair<String, Pair<Mode, Type>> arg : arguments) {
                    arguments_.add(new Pair(arg.first(), arg.second().second()));
                }

                Terms<Term, Type> scope = factory.terms(arguments_);
                List<Pair<Term, Term>> contracts = parens(() -> contracts(context, scope));
                expect(RPAREN);

                return factory.defineContract(procedure, arguments, contracts);

            }

            default:
                return null;
            }
        } else {
            return null;
        }
    }

    public <Term, Type> Term term(Types<Type> context, Terms<Term, Type> scope) throws IOException {
        Token token = peek(); // do not shift this token, as it could be a RPAREN that indicates end of the argument
                              // list

        switch (token) {
        case Literal lit:
            next();
            return scope.literal(lit.value());

        case Identifier id:
            next();
            return scope.identifier(id.name());

        case LParen lp:
            expect(LPAREN);

            String function = identifier(); // TODO: extend this to indexed functions

            switch (function) {
            case "old":
                Term argument = term(context, scope);
                expect(RPAREN);

                return scope.old(argument);

            case "exists":
            case "forall":
            case "lambda":
                List<Pair<String, Type>> formals = parens(() -> formals(context));
                Terms<Term, Type> scope_ = scope.extended(formals);
                Term body = term(context, scope_);
                expect(RPAREN);

                return scope.binder(function, formals, body);

            default:
                List<Term> arguments = terms(context, scope);
                expect(RPAREN);

                return scope.application(function, arguments);
            }

        default:
            return null;
        }
    }

    static final Token EOF = null;

    static class LParen implements Token {
        public String toString() {
            return "LPAREN";
        }
    }

    static class RParen implements Token {
        public String toString() {
            return "RPAREN";
        }
    }

    static final Token LPAREN = new LParen();
    static final Token RPAREN = new RParen();

    Token eof() {
        return EOF;
    }

    Token lparen() {
        return LPAREN;
    }

    Token rparen() {
        return RPAREN;
    }

    Token identifier(String text) {
        return new Identifier(text);
    }

    Token keyword(String text) {
        return new Keyword(text);
    }

    Token string(String text) {
        return new Literal(text);
    }

    Token real(String text) {
        return new Literal(Double.valueOf(text));
    }

    Token integer(String text) {
        return new Literal(Integer.valueOf(text));
    }

    Token hexadecimal(String text) {
        throw new UnsupportedOperationException();
    }

    Token binary(String text) {
        throw new UnsupportedOperationException();
    }

    String identifier() throws IOException {
        Token token = next();
        Identifier identifier = (Identifier) token;
        return identifier.name();
    }

    Integer integer() throws IOException {
        Token token = next();
        Literal literal = (Literal) token;
        return (Integer) literal.value();
    }

    Atom atom() throws IOException {
        Token token = next();
        Atom atom = (Atom) token;
        return atom;
    }

    boolean check(Token token) throws IOException {
        if (peek() == token) {
            next();
            return true;
        } else {
            return false;
        }
    }

    boolean peek(Token token) throws IOException {
        if (peek() == token) {
            return true;
        } else {
            return false;
        }
    }

    Token pending = null;

    Token peek() throws IOException {
        if (pending == null) {
            pending = shift();
            // System.out.println("shifted: " + pending);
        }

        return pending;
    }

    Token next() throws IOException {
        if (pending == null) {
            pending = shift();
            // System.out.println("shifted: " + pending);
        }

        Token result = pending;
        pending = null;

        return result;
    }

    void expect(Token token) throws IOException {
        Token next = next();
        if (next != token)
            unexpected(next, token);
    }

    Token unexpected(Token token) {
        throw new RuntimeException("unexpected token " + token);
    }

    Token unexpected(Token token, Token expected) {
        throw new RuntimeException("expected token " + expected + " but found " + token);
    }

    Token unexpected(String text) {
        throw new RuntimeException("unexpected token " + text + " at " + line() + ":" + column());
    }
}