// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.hints.presentation;

import consulo.colorScheme.TextAttributes;
import consulo.ide.impl.idea.codeInsight.hints.InlayTextMetrics;
import consulo.ide.impl.idea.codeInsight.hints.InlayTextMetricsStorage;
import consulo.ide.impl.idea.openapi.editor.ex.util.EditorUtil;
import consulo.language.editor.inlay.BasePresentation;
import consulo.ui.annotation.RequiredUIAccess;

import java.awt.*;

public class TextPlaceholderPresentation extends BasePresentation {
    private final int length;
    private final InlayTextMetricsStorage textMetricsStorage;
    private final boolean small;

    public TextPlaceholderPresentation(int length,
                                       InlayTextMetricsStorage textMetricsStorage,
                                       boolean small) {
        this.length = length;
        this.textMetricsStorage = textMetricsStorage;
        this.small = small;
    }

    @Override
    public int getWidth() {
        return EditorUtil.getPlainSpaceWidth(textMetricsStorage.getEditor()) * length;
    }

    @Override
    public int getHeight() {
        return getMetrics().getFontHeight();
    }

    @RequiredUIAccess
    private InlayTextMetrics getMetrics() {
        return textMetricsStorage.getFontMetrics(small);
    }

    @Override
    public void paint(Graphics2D g, TextAttributes attributes) {
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(' ');
        }
        return sb.toString();
    }
}
