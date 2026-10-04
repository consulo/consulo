// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ui.grid;

import consulo.util.lang.Pair;

import java.util.function.IntUnaryOperator;

/**
 * @author gregsh
 */
public interface RawIndexConverter {

    boolean isValidViewRowIdx(int viewRowIdx);

    boolean isValidViewColumnIdx(int viewColumnIdx);

    IntUnaryOperator row2View();

    IntUnaryOperator column2View();

    PairPairFunction<Integer> rowAndColumn2Model();

    PairPairFunction<Integer> rowAndColumn2View();

    IntUnaryOperator row2Model();

    IntUnaryOperator column2Model();

    @FunctionalInterface
    interface PairPairFunction<T> {
        Pair<T, T> fun(T v1, T v2);
    }
}
