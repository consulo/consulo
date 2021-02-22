package com.intellij.model.search.impl;

import java.util.Collection;
import java.util.function.Function;

// typealis
public interface XTransformation<B, R> extends Function<B, Collection<? extends XResult<? extends R>>> {
}
