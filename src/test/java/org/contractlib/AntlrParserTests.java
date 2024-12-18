package org.contractlib;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.contractlib.antlr4parser.ContractLIBLexer;
import org.contractlib.antlr4parser.ContractLIBParser;
import org.contractlib.ast.*;
import org.contractlib.parser.ContractLibANTLRParser;
import org.contractlib.parser.HandwrittenParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.nio.file.Paths;

class AntlrParserTests {

    @Test
    void testExamples() throws IOException {
        Path path = Paths.get("src/test/contractlib/examples/examples.smt2");
        System.out.println("Parsing:  " + path);

        // ANTLR parser
        CharStream charStream = CharStreams.fromPath(path);
        ContractLIBLexer lexer = new ContractLIBLexer(charStream);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ContractLIBParser parser = new ContractLIBParser(tokens);

        ContractLIBParser.ScriptContext ctx = parser.script();
        Factory factory = new Factory();
        ContractLibANTLRParser<Term, Type, Abstraction, Datatype, FunDecl, Command> converter = new ContractLibANTLRParser<>(factory);
        converter.visit(ctx);

        StringBuilder sb = new StringBuilder();
        for (var command : converter.getCommands()) {
            sb.append(command.toString());
            sb.append(System.lineSeparator());
            sb.append(System.lineSeparator());
        }
        System.out.println(sb);
    }

    // Test that handwritten parser and ANTLR parser produce the same results.
    // TODO: This test does not succeed at the moment, output differs at least for
    //  declare-abstractions!
    @Test
    void testAgainstHandwrittenParser() throws IOException {
        Path path = Paths.get("src/test/contractlib/examples/examples.smt2");
        System.out.println("Parsing:  " + path);

        // ANTLR parser
        CharStream charStream = CharStreams.fromPath(path);
        ContractLIBLexer lexer = new ContractLIBLexer(charStream);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ContractLIBParser parser = new ContractLIBParser(tokens);

        ContractLIBParser.ScriptContext ctx = parser.script();
        Factory factory = new Factory();
        ContractLibANTLRParser<Term, Type, Abstraction, Datatype, FunDecl, Command> converter = new ContractLibANTLRParser<>(factory);
        converter.visit(ctx);

        StringBuilder sb = new StringBuilder();
        for (var command : converter.getCommands()) {
            sb.append(command.toString());
            sb.append(System.lineSeparator());
            sb.append(System.lineSeparator());
        }
        System.out.println(sb);

        // Handwritten parser for comparison
        System.out.println("Parsing:  " + path);
        Reader reader = new FileReader(path.toFile());
        HandwrittenParser handwrittenParser = new HandwrittenParser(reader);
        Factory ast = new Factory();
        StringBuilder sb2 = new StringBuilder();
        for (var command : handwrittenParser.script(ast)) {
            sb2.append(command.toString());
            sb2.append(System.lineSeparator());
            sb2.append(System.lineSeparator());
        }
        System.out.println(sb2);
        Assertions.assertEquals(sb2, sb);
    }

    @Disabled("Not working at the moment!")
    @Test
    void builtin() throws IOException {
        canParseAll(new File("src/test/contractlib/builtin"));
    }

    @Disabled("Not working at the moment!")
    @Test
    void examples() throws IOException {
        canParseAll(new File("src/test/contractlib/examples"));
    }

    @Disabled("Not working at the moment!")
    @Test
    void regression() throws IOException {
        canParseAll(new File("src/test/contractlib/regression"));
    }

    void canParseAll(File path) throws IOException {
        File[] files = path.listFiles();

        for (File file : files) {
            if (file.getName().endsWith(".smt2")) {
                canParse(file);
            }
        }
    }

    void canParse(File file) throws IOException {
        System.out.println("Parsing:  " + file.getPath());

        CharStream charStream = CharStreams.fromPath(file.toPath());
        ContractLIBLexer lexer = new ContractLIBLexer(charStream);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ContractLIBParser parser = new ContractLIBParser(tokens);

        ContractLIBParser.ScriptContext s = parser.script();
    }
}
