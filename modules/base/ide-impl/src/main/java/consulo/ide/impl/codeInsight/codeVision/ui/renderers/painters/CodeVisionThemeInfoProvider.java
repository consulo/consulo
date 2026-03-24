// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.codeInsight.codeVision.ui.renderers.painters;

import consulo.codeEditor.DefaultLanguageHighlighterColors;
import consulo.codeEditor.Editor;
import consulo.codeEditor.impl.EditorSettingsExternalizable;
import consulo.ide.impl.idea.openapi.editor.ex.util.EditorUtil;
import consulo.language.editor.impl.internal.inlay.param.HintUtils;
import consulo.ui.ex.awt.JBCurrentTheme;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;

import java.awt.Color;
import java.awt.Font;

public class CodeVisionThemeInfoProvider {
    private static final CodeVisionThemeInfoProvider INSTANCE = new CodeVisionThemeInfoProvider();

    public static CodeVisionThemeInfoProvider getInstance() {
        return INSTANCE;
    }

    public Color foregroundColor(Editor editor, boolean hovered) {
        if (hovered) {
            return JBCurrentTheme.Link.Foreground.ENABLED;
        }
        else {
            return TargetAWT.to(editor.getColorsScheme().getAttributes(DefaultLanguageHighlighterColors.INLAY_TEXT_WITHOUT_BACKGROUND).getForegroundColor());
        }
    }

    public Font font(Editor editor) {
        return font(editor, Font.PLAIN);
    }

    public Font font(Editor editor, int style) {
        float size = lensFontSize(editor);
        if (EditorSettingsExternalizable.getInstance().isUseEditorFontInInlays()) {
            Font editorFont = EditorUtil.getEditorFont();
            return editorFont.deriveFont(style, size);
        }
        else {
            return UIUtil.getLabelFont().deriveFont(style, size);
        }
    }

    public float lensFontSize(Editor editor) {
        return HintUtils.getSize(editor);
    }
}
