package org.contractlib.factory;

import org.contractlib.util.Pair;

import java.util.List;

public interface Datatypes<TYPE, DATATYPE> {
    Types<TYPE> types(List<String> params);

    DATATYPE datatype(List<String> params, List<Pair<String, List<Pair<String, TYPE>>>> constructors);
}