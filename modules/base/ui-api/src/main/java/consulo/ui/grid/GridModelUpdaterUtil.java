// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ui.grid;

public final class GridModelUpdaterUtil {
    private GridModelUpdaterUtil() {
    }

    public static int[] getColumnsIndicesRange(int first, int length) {
        int[] range = new int[length];
        for (int i = 0; i < length; i++) {
            range[i] = first + i;
        }
        return range;
    }
}
