// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.codeInsight.hints;

import consulo.application.ui.UISettings;
import consulo.codeEditor.DefaultLanguageHighlighterColors;
import consulo.codeEditor.Editor;
import consulo.codeEditor.impl.EditorSettingsExternalizable;
import consulo.codeEditor.impl.FontInfo;
import consulo.colorScheme.EditorColorsManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.util.DesktopAntialiasingTypeUtil;

import javax.swing.*;
import java.awt.font.FontRenderContext;
import java.util.Objects;

public class InlayTextMetricsStorage {
    private final Editor editor;
    private InlayTextMetrics smallTextMetrics;
    private InlayTextMetrics normalTextMetrics;
    private InlayTextMetricsStamp lastStamp;

    public InlayTextMetricsStorage(Editor editor) {
        this.editor = editor;
    }

    @RequiredUIAccess
    private float getNormalTextSize() {
        return editor.getColorsScheme().getEditorFontSize();
    }

    @RequiredUIAccess
    private float getSmallTextSize() {
        return Math.max(1f, getNormalTextSize() - 1f);
    }

    @RequiredUIAccess
    public InlayTextMetricsStamp getCurrentStamp() {
        return new InlayTextMetricsStamp(
            getNormalTextSize(),
            getFontFamilyName(),
            UISettings.getInstance().getIdeScale(),
            getFontRenderContext(editor.getComponent())
        );
    }

    @RequiredUIAccess
    public InlayTextMetrics getFontMetrics(boolean small) {
        InlayTextMetricsStamp current = getCurrentStamp();
        if (!Objects.equals(lastStamp, current)) {
            lastStamp = current;
            smallTextMetrics = null;
            normalTextMetrics = null;
        }
        InlayTextMetrics metrics = small ? smallTextMetrics : normalTextMetrics;
        if (metrics == null) {
            float size = small ? getSmallTextSize() : getNormalTextSize();
            metrics = InlayTextMetrics.create(editor, size, getFontType(), current.getFontRenderContext());
            if (small) smallTextMetrics = metrics;
            else normalTextMetrics = metrics;
        }
        return metrics;
    }

    private String getFontFamilyName() {
        if (EditorSettingsExternalizable.getInstance().isUseEditorFontInInlays()) {
            return EditorColorsManager.getInstance().getGlobalScheme().getEditorFontName();
        }
        else {
            return UIUtil.getLabelFont().getFamily();
        }
    }

    public Editor getEditor() {
        return editor;
    }

    private int getFontType() {
        return editor.getColorsScheme()
            .getAttributes(DefaultLanguageHighlighterColors.INLAY_DEFAULT)
            .getFontType();
    }

    private static FontRenderContext getFontRenderContext(JComponent component) {
        FontRenderContext frc = FontInfo.getFontRenderContext(component);
        return new FontRenderContext(
            frc.getTransform(),
            DesktopAntialiasingTypeUtil.getKeyForCurrentScope(false),
            InlayTextMetrics.getEditorFractionalMetricsHint()
        );
    }
}

