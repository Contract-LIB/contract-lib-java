package org.contractlib.factory;

import org.contractlib.util.Pair;

import java.util.List;

public interface Abstractions<TYPE, ABSTRACTION> {
    Types<TYPE> types(List<String> params);

    ABSTRACTION abstraction(List<String> params, List<Pair<String, List<Pair<String, TYPE>>>> constructors);
}