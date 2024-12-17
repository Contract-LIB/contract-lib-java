package org.contractlib.factory;

import org.contractlib.util.Pair;

import java.util.List;

/**
 * An abstract factory for creating command data structures during parsing.
 *
 * @param <TERM>
 * @param <TYPE>
 * @param <ABSTRACTION>
 * @param <DATATYPE>
 * @param <FUNDECL>
 * @param <COMMAND>
 */
public interface Commands<TERM, TYPE, ABSTRACTION, DATATYPE, FUNDECL, COMMAND> {
    Types<TYPE> types(List<String> params);

    COMMAND declareSort(String name, Integer arity);

    COMMAND defineSort(String name, List<String> params, TYPE body);

    COMMAND declareAbstractions(List<Pair<String, Integer>> arities, List<ABSTRACTION> abstractions);

    Datatypes<TYPE, DATATYPE> datatypes(List<Pair<String, Integer>> arities);

    Abstractions<TYPE, ABSTRACTION> abstractions(List<Pair<String, Integer>> arities);

    Functions<TYPE, FUNDECL> functions();

    COMMAND declareDatatypes(List<Pair<String, Integer>> arities, List<DATATYPE> datatypes);

    Terms<TERM, TYPE> terms(List<Pair<String, TYPE>> variables);

    COMMAND declareFun(String name, List<String> params, List<TYPE> arguments, TYPE result);

    COMMAND defineFun(String name, List<String> params, List<Pair<String, TYPE>> arguments, TYPE result, TERM body);

    COMMAND defineFunRec(String name, List<String> params, List<Pair<String, TYPE>> arguments, TYPE result, TERM body);

    COMMAND defineContract(String name, List<Pair<String, Pair<Mode, TYPE>>> arguments,
                           List<Pair<TERM, TERM>> contracts);

    COMMAND assertion(TERM term);

    COMMAND declareConst(String name, TYPE result);

    COMMAND defineFunsRec(List<FUNDECL> functionDecls,
                          List<TERM> bodies);
}