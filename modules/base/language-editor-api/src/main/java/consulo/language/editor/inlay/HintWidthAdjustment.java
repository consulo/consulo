// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.inlay;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Defines the adjustment to inlay hint's width. This supports the scenario when existing hint is replaced with some text (and possibly
 * other hint), and we want to avoid shifting the following text. This is implemented by introducing an additional spacing at a given
 * offset in hint's text ({@link InlayInfo#getText()}) to make hint's width equal to width of given editor text plus optionally
 * a width of a hint with another text. If matching widths would require decreasing hint's width, no adjustment is performed.
 *
 * @see InlayParameterHintsProvider#getParameterHints(PsiElement)
 * @see InlayInfo
 */
public class HintWidthAdjustment {
    private final String editorTextToMatch;
    private final String hintTextToMatch;
    private final int adjustmentOffset;

    public HintWidthAdjustment(@Nonnull String editorTextToMatch, @Nullable String hintTextToMatch, int adjustmentOffset) {
        this.editorTextToMatch = editorTextToMatch;
        this.hintTextToMatch = hintTextToMatch;
        this.adjustmentOffset = adjustmentOffset;
    }

    /**
     * Editor text, which width should be matched
     */
    public @Nonnull String getEditorTextToMatch() {
        return editorTextToMatch;
    }

    /**
     * Text of hint, which width should be added to editor text width for matching
     */
    public @Nullable String getHintTextToMatch() {
        return hintTextToMatch;
    }

    /**
     * Position in hint's text where the additional spacing should be inserted
     */
    public int getAdjustmentPosition() {
        return adjustmentOffset;
    }
}
