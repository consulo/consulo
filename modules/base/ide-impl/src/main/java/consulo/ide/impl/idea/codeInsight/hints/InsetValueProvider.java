// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.hints;

public interface InsetValueProvider {
    /**
     * Horizontal inset on the left.
     */
    default int getLeft() {
        return 0;
    }

    /**
     * Horizontal inset on the right.
     */
    default int getRight() {
        return 0;
    }

    /**
     * Vertical inset at the top.
     */
    default int getTop() {
        return 0;
    }

    /**
     * Vertical inset at the bottom.
     */
    default int getDown() {
        return 0;
    }
}