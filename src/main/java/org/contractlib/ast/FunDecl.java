package org.contractlib.ast;

import org.contractlib.util.Pair;

import java.util.List;

public record FunDecl(
    String name,
    List<String> params,
    List<Pair<String, Type>> arguments,
    Type result
) {
}
