// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ui.ex.grid.csv;

import consulo.document.util.TextRange;

public class ValueRange extends TextRange {
    public ValueRange(int startOffset, int endOffset) {
        super(startOffset, endOffset);
    }

    public ValueRange(int startOffset, int endOffset, boolean checkForProperTextRange) {
        super(startOffset, endOffset, checkForProperTextRange);
    }

    public CharSequence value(CharSequence s) {
        return subSequence(s);
    }
}
