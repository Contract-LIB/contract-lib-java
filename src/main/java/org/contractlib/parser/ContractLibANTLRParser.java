package org.contractlib.parser;

import org.contractlib.antlr4parser.ContractLIBBaseVisitor;
import org.contractlib.antlr4parser.ContractLIBParser;
import org.contractlib.factory.Mode;
import org.contractlib.factory.Types;
import org.contractlib.util.Pair;

import org.contractlib.factory.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Parser for CONTRACT-LIB based on the ANTLR4 grammar to be found in this repository. To use it,
 * instantiate it with concrete types that implement the interfaces in the package
 * {@link org.contractlib.factory}. In addition, an implementation of the interface {@link Commands}
 * needs to be provided, which then acts as a factory for the concrete elements.
 * The results of the parsing are stored in the list of commands, which can be obtained via
 * {@link #getCommands()}.
 *
 * @param <TERM>
 * @param <TYPE>
 * @param <ABS>
 * @param <DT>
 * @param <FUNDECL>
 * @param <COMMAND>
 */
public class ContractLibANTLRParser<TERM, TYPE, ABS, DT, FUNDECL, COMMAND> extends ContractLIBBaseVisitor<Void> {

    private final Commands<TERM, TYPE, ABS, DT, FUNDECL, COMMAND> factory;

    private final List<COMMAND> commands = new ArrayList<>();

    public ContractLibANTLRParser(Commands<TERM, TYPE, ABS, DT, FUNDECL, COMMAND> factory) {
        this.factory = factory;
    }

    public List<COMMAND> getCommands() {
        return commands;
    }

    @Override
    public Void visitCmd_assert(ContractLIBParser.Cmd_assertContext ctx) {
        List<String> params = new ArrayList<>();
        Types<TYPE> context = factory.types(params);

        // TODO: this needs to be filled
        List<Pair<String, TYPE>> vars = List.of();

        TERM t = convertTerm(ctx.term(), factory.terms(vars), context);
        commands.add(factory.assertion(t));
        return null;
    }

    @Override
    public Void visitCmd_declareAbstractions(ContractLIBParser.Cmd_declareAbstractionsContext ctx) {

        List<Pair<String, Integer>> arities = new ArrayList<>();
        for (var a : ctx.sort_dec()) {
            String name = convertSymbol(a.symbol());
            Integer arity = Integer.parseInt(a.numeral().Numeral().getText());
            arities.add(new Pair<>(name, arity));
        }

        List<ABS> abstractions = new ArrayList<>();
        for (var d : ctx.datatype_dec()) {
            List<String> params = new ArrayList<>();
            for(var par : d.symbol()) {
                params.add(convertSymbol(par));
            }
            Types<TYPE> context = factory.types(params);
            ABS abstr = convertAbstraction(d, params, context, arities);
            abstractions.add(abstr);
            //System.out.println("Found declaration of abstraction " + abstr);
        }
        commands.add(factory.declareAbstractions(arities, abstractions));

        return null;
    }

    private ABS convertAbstraction(ContractLIBParser.Datatype_decContext ctx,
                                            List<String> params,
                                            Types<TYPE> context,
                                            List<Pair<String, Integer>> arities) {
        List<Pair<String, List<Pair<String, TYPE>>>> constrs = new ArrayList<>();
        for (var ctr : ctx.constructor_dec()) {
            constrs.add(convertConstructor(ctr, context));
        }
        Abstractions<TYPE, ABS> abs = factory.abstractions(arities);
        return abs.abstraction(params, constrs);
    }

    private Pair<String, List<Pair<String, TYPE>>> convertConstructor(ContractLIBParser.Constructor_decContext constr,
                                                                            Types<TYPE> params) {
        String ctrName = convertSymbol(constr.symbol());
        List<Pair<String, TYPE>> selectors = new ArrayList<>();
        for (var selector : constr.selector_dec()) {
            String name = convertSymbol(selector.symbol());
            TYPE t = convertSort(selector.sort(), params);

            Pair<String, TYPE> sel = new Pair<>(name, t);
            selectors.add(sel);
        }
        return new Pair<>(ctrName, selectors);
    }

    private TYPE convertSort(ContractLIBParser.SortContext sort, Types<TYPE> types) {
        String identifier = sort.identifier().getText();

        // TODO: repair parametric sorts
        List<TYPE> args = new ArrayList<>();
        if (sort.sort() != null) {
            // parametric sort
            for (var arg : sort.sort()) {
                args.add(convertSort(arg, types));
            }
        }
        if(args.isEmpty()) {
            return types.identifier(identifier);
        } else {
            return types.sort(identifier, args);
        }
    }


    private String convertSymbol(ContractLIBParser.SymbolContext ctx) {
        if (ctx.quotedSymbol() != null) {
            return ctx.quotedSymbol().QuotedSymbol().getSymbol().getText();
        } else {
            return ctx.simpleSymbol().getText();
        }
    }

    @Override
    public Void visitCmd_declareFun(ContractLIBParser.Cmd_declareFunContext ctx) {

        // TODO: where do the parameters come from? Function declarations are non-parametric in
        //  SMT-LIB ...
        List<String> params = new ArrayList<>();
        Types<TYPE> context = factory.types(params);

        String name = convertSymbol(ctx.symbol());
        List<TYPE> args = new ArrayList<>();
        int arity = ctx.sort().size() - 1;
        for (int i = 0; i < arity; i++) {

            args.add(convertSort(ctx.sort(i), context));
        }
        TYPE result = convertSort(ctx.sort(arity), context);

        commands.add(factory.declareFun(name, params, args, result));
        return null;
    }

    @Override
    public Void visitCmd_declareConst(ContractLIBParser.Cmd_declareConstContext ctx) {
        // TODO: where do the parameters come from? Function declarations are non-parametric in
        //  SMT-LIB ...
        List<String> params = new ArrayList<>();
        Types<TYPE> context = factory.types(params);

        String name = convertSymbol(ctx.symbol());
        TYPE result = convertSort(ctx.sort(), context);

        commands.add(factory.declareConst(name, result));
        return null;
    }

    @Override
    public Void visitCmd_defineFun(ContractLIBParser.Cmd_defineFunContext ctx) {
        // TODO: where do the parameters come from? Function definitions are non-parametric in
        //  SMT-LIB ...
        List<String> params = new ArrayList<>();
        FunctionDef<TYPE,TERM> fun = convertFunctionDef(ctx.function_def());
        commands.add(factory.defineFun(fun.name, params, fun.args, fun.result, fun.body));
        return null;
    }

    @Override
    public Void visitCmd_defineFunRec(ContractLIBParser.Cmd_defineFunRecContext ctx) {
        // TODO: where do the parameters come from? Function definitions are non-parametric in
        //  SMT-LIB ...
        List<String> params = new ArrayList<>();
        FunctionDef<TYPE,TERM> fun = convertFunctionDef(ctx.function_def());
        commands.add(factory.defineFunRec(fun.name, params, fun.args, fun.result, fun.body));
        return null;
    }

    private FunctionDef<TYPE, TERM> convertFunctionDef(ContractLIBParser.Function_defContext ctx) {

        // TODO: where do the parameters come from? Function definitions are non-parametric in
        //  SMT-LIB ...
        List<String> params = new ArrayList<>();
        Types<TYPE> context = factory.types(params);

        String name = convertSymbol(ctx.symbol());
        List<Pair<String, TYPE>> args = convertSortedVars(ctx.sorted_var(), context);
        TYPE result = convertSort(ctx.sort(), context);
        TERM body = convertTerm(ctx.term(), factory.terms(args), context);

        return new FunctionDef<>(name, args, result, body);
    }

    private FunctionDec<TYPE> convertFunctionDec(ContractLIBParser.Function_decContext ctx) {

        // TODO: where do the parameters come from? Function definitions are non-parametric in
        //  SMT-LIB ...
        List<String> params = new ArrayList<>();
        Types<TYPE> context = factory.types(params);

        String name = convertSymbol(ctx.symbol());
        List<Pair<String, TYPE>> arguments = convertSortedVars(ctx.sorted_var(), context);
        TYPE result = convertSort(ctx.sort(), context);

        return new FunctionDec<>(name, arguments, result);
    }

    private record FunctionDef<TYPE, TERM>(String name, List<Pair<String, TYPE>> args, TYPE result, TERM body) {
    }

    private record FunctionDec<TYPE>(String name, List<Pair<String, TYPE>> args, TYPE result) {
    }

    @Override
    public Void visitCmd_defineFunsRec(ContractLIBParser.Cmd_defineFunsRecContext ctx) {
        // TODO: check sizes

        // TODO: where do the parameters come from? Function definitions are non-parametric in
        //  SMT-LIB ...
        List<String> params = new ArrayList<>();
        Types<TYPE> context = factory.types(params);

        List<FUNDECL> functionDecls = new ArrayList<>();
        List<Pair<String, TYPE>> scope = new ArrayList<>();
        for (ContractLIBParser.Function_decContext fun : ctx.function_dec()) {
            FunctionDec<TYPE> f = convertFunctionDec(fun);
            FUNDECL fd = factory.functions().funDec(f.name, params, f.args, f.result);
            scope.add(new Pair<>(f.name, f.result));     // TODO: I think this is wrong.
            functionDecls.add(fd);
        }

        List<TERM> bodies = new ArrayList<>();
        for (var t : ctx.term()) {
            bodies.add(convertTerm(t, factory.terms(scope), context));
        }
        commands.add(factory.defineFunsRec(functionDecls, bodies));
        return null;
    }

    @Override
    public Void visitCmd_declareSort(ContractLIBParser.Cmd_declareSortContext ctx) {
        String name = convertSymbol(ctx.symbol());
        Integer arity = convertNumeral(ctx.numeral());
        commands.add(factory.declareSort(name, arity));
        return null;
    }

    @Override
    public Void visitCmd_defineSort(ContractLIBParser.Cmd_defineSortContext ctx) {
        List<String> ps = new ArrayList<>();
        Types<TYPE> context = factory.types(ps);

        // TODO: check ctx.symbol() size
        String name = convertSymbol(ctx.symbol(0));

        List<String> params = new ArrayList<>();
        for (int i = 1; i < ctx.symbol().size(); i++) {
            params.add(convertSymbol(ctx.symbol(i)));
        }

        TYPE body = convertSort(ctx.sort(), context);
        commands.add(factory.defineSort(name, params, body));
        return null;
    }

    private Integer convertNumeral(ContractLIBParser.NumeralContext numeral) {
        return Integer.parseInt(numeral.Numeral().getText());
    }

    @Override
    public Void visitCmd_declareDatatypes(ContractLIBParser.Cmd_declareDatatypesContext ctx) {
        List<Pair<String, Integer>> arities = new ArrayList<>();
        for (var a : ctx.sort_dec()) {
            String name = convertSymbol(a.symbol());
            Integer arity = convertNumeral(a.numeral());
            arities.add(new Pair<>(name, arity));
        }

        List<DT> datatypes = new ArrayList<>();
        for (var d : ctx.datatype_dec()) {
            List<String> params = new ArrayList<>();
            for(var par : d.symbol()) {
                params.add(convertSymbol(par));
            }
            Types<TYPE> context = factory.types(params);
            DT dt = convertDatatype(d, params, context, arities);
            datatypes.add(dt);
        }
        commands.add(factory.declareDatatypes(arities, datatypes));

        return null;
    }

    private DT convertDatatype(ContractLIBParser.Datatype_decContext ctx,
                                        List<String> params,
                                        Types<TYPE> context,
                                        List<Pair<String, Integer>> arities) {
        List<Pair<String, List<Pair<String, TYPE>>>> constrs = new ArrayList<>();
        for (var ctr : ctx.constructor_dec()) {
            constrs.add(convertConstructor(ctr, context));
        }

        Datatypes<TYPE, DT> dts = factory.datatypes(arities);
        return dts.datatype(params, constrs);
    }

    @Override
    public Void visitCmd_defineContract(ContractLIBParser.Cmd_defineContractContext ctx) {

        String name = convertSymbol(ctx.symbol());
        //System.out.println("Found contract for " + name);

        List<Pair<String, Pair<Mode, TYPE>>> formalWithMode = new ArrayList<>();

        Types<TYPE> context = factory.types(List.of());
        for (var f : ctx.formal()) {
            formalWithMode.add(convertFormal(f, context));
        }
        List<Pair<String, TYPE>> formal = formalWithMode.stream()
            .map(x -> new Pair<>(x.first(), x.second().second())).toList();

        List<Pair<TERM, TERM>> contracts = new ArrayList<>();
        for (var c : ctx.contract()) {
            contracts.add(convertContract(c, context, formal));
        }

        commands.add(factory.defineContract(name, formalWithMode, contracts));
        return null;
    }

    private Pair<TERM, TERM> convertContract(ContractLIBParser.ContractContext ctx,
                                             Types<TYPE> context,
                                             List<Pair<String, TYPE>> vars) {
        Terms<TERM, TYPE> scope = factory.terms(vars);
        TERM pre = convertTerm(ctx.term(0), scope, context);
        TERM post = convertTerm(ctx.term(1), scope, context);

        return new Pair<>(pre, post);
    }

    private List<Pair<String, TYPE>> convertSortedVars(List<ContractLIBParser.Sorted_varContext> ctxs,
                                                       Types<TYPE> context) {
        List<Pair<String, TYPE>> result = new ArrayList<>();
        for (ContractLIBParser.Sorted_varContext bv : ctxs) {
            result.add(convertSortedVar(bv, context));
        }
        return result;
    }

    private TERM convertTerm(ContractLIBParser.TermContext ctx,
                             Terms<TERM, TYPE> scope,
                             Types<TYPE> context) {
        if (ctx.GRW_Exists() != null) {
            List<Pair<String, TYPE>> boundVars = convertSortedVars(ctx.sorted_var(), context);
            Terms<TERM, TYPE> ext = scope.extended(boundVars);
            return scope.binder("exists", boundVars, convertTerm(ctx.term(0), ext, context));

        } else if (ctx.GRW_Forall() != null) {
            List<Pair<String, TYPE>> boundVars = convertSortedVars(ctx.sorted_var(), context);
            Terms<TERM, TYPE> ext = scope.extended(boundVars);
            return scope.binder("forall", boundVars, convertTerm(ctx.term(0), ext, context));

        } else if (ctx.GRW_Old() != null) {
            return scope.old(convertTerm(ctx.term(0), scope, context));

        } else if (ctx.spec_constant() != null) {
            return scope.literal(ctx.spec_constant().getText());

        } else if (ctx.qual_identifer() != null && ctx.term() != null) {
            String ident = ctx.qual_identifer().getText();
            List<TERM> args = new ArrayList<>();
            for (var t : ctx.term()) {
                args.add(convertTerm(t, scope, context));
            }
            if (args.isEmpty()) {
                return scope.identifier(ident);
            }
            return scope.application(ident, args);

        } else if (ctx.qual_identifer() != null) {
            String ident = ctx.qual_identifer().getText();
            return scope.identifier(ident);

        } else {
            // TODO: support other types of terms
        }
        return null;
    }

    private Pair<String, TYPE> convertSortedVar(ContractLIBParser.Sorted_varContext ctx,
                                                Types<TYPE> context) {
        String name = convertSymbol(ctx.symbol());
        TYPE type = convertSort(ctx.sort(), context);
        return new Pair<>(name, type);
    }

    private Pair<String, Pair<Mode, TYPE>> convertFormal(ContractLIBParser.FormalContext ctx,
                                                         Types<TYPE> context) {
        String name = convertSymbol(ctx.symbol());
        Mode mode = convertMode(ctx.argument_mode());
        TYPE type = convertSort(ctx.sort(), context);
        return new Pair<>(name, new Pair<>(mode, type));
    }

    private Mode convertMode(ContractLIBParser.Argument_modeContext ctx) {
        return switch (ctx.getText()) {
            case "in" -> Mode.IN;
            case "out" -> Mode.OUT;
            default -> Mode.INOUT;
        };
    }
}
