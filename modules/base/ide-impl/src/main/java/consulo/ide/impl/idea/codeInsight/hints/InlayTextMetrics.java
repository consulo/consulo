/*
 * Copyright 2013-2025 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.ide.impl.idea.codeInsight.hints;

import consulo.application.ui.UISettings;
import consulo.application.util.registry.Registry;
import consulo.codeEditor.Editor;
import consulo.codeEditor.RealEditor;
import consulo.codeEditor.impl.EditorSettingsExternalizable;
import consulo.codeEditor.impl.FontInfo;
import consulo.ide.impl.idea.openapi.editor.ex.util.EditorUtil;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.util.DesktopAntialiasingTypeUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.font.FontRenderContext;

public final class InlayTextMetrics {
    private final Editor editor;
    private final int fontHeight;
    private final int fontBaseline;
    private final FontMetrics fontMetrics;
    private final int fontType;
    private final float ideScale;

    private InlayTextMetrics(Editor editor,
                             int fontHeight,
                             int fontBaseline,
                             FontMetrics fontMetrics,
                             int fontType,
                             float ideScale) {
        this.editor = editor;
        this.fontHeight = fontHeight;
        this.fontBaseline = fontBaseline;
        this.fontMetrics = fontMetrics;
        this.fontType = fontType;
        this.ideScale = ideScale;
    }

    public static InlayTextMetrics create(Editor editor,
                                          float size,
                                          int fontType,
                                          FontRenderContext context) {
        Font font;
        if (EditorSettingsExternalizable.getInstance().isUseEditorFontInInlays()) {
            font = EditorUtil.getEditorFont().deriveFont(fontType, size);
        }
        else {
            font = UIUtil.getFontWithFallback(UIUtil.getLabelFont().getFamily(), fontType, (int) size);
        }
        FontMetrics metrics = FontInfo.getFontMetrics(font, context);
        int fh = (int) Math.ceil(font.createGlyphVector(context, "Albpq@").getVisualBounds().getHeight());
        int fb = (int) Math.ceil(font.createGlyphVector(context, "Alb").getVisualBounds().getHeight());
        return new InlayTextMetrics(editor, fh, fb, metrics, fontType, UISettings.getInstance().getIdeScale());
    }

    public Font getFont() {
        return fontMetrics.getFont();
    }

    public int getFontHeight() {
        return fontHeight;
    }

    public int getFontBaseline() {
        return fontBaseline;
    }

    public int getAscent() {
        return editor.getAscent();
    }

    public int getDescent() {
        return (editor instanceof RealEditor ? ((RealEditor) editor).getDescent() : 0);
    }

    public int getLineHeight() {
        return editor.getLineHeight();
    }

    public int offsetFromTop() {
        return (getLineHeight() - fontHeight) / 2;
    }

    public int getStringWidth(String text) {
        return fontMetrics.stringWidth(text);
    }

    private static FontRenderContext getFontRenderContext(JComponent component) {
        FontRenderContext frc = FontInfo.getFontRenderContext(component);
        return new FontRenderContext(
            frc.getTransform(),
            DesktopAntialiasingTypeUtil.getKeyForCurrentScope(false),
            getEditorFractionalMetricsHint()
        );
    }

    public static Object getEditorFractionalMetricsHint() {
        return Registry.is("editor.text.fractional.metrics")
            ? RenderingHints.VALUE_FRACTIONALMETRICS_ON
            : RenderingHints.VALUE_FRACTIONALMETRICS_OFF;
    }
}