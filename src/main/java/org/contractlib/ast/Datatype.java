package org.contractlib.ast;

import org.contractlib.util.Pair;

import java.util.List;

public record Datatype(
    List<String> params,
    List<Pair<String, List<Pair<String, Type>>>> constrs
) {
}
