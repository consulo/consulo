// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.codeInsight.codeVision.ui.renderers.painters;

import consulo.codeEditor.Editor;
import consulo.colorScheme.TextAttributes;
import consulo.ide.impl.codeInsight.codeVision.ui.model.RangeCodeVisionModel;
import consulo.ide.impl.idea.ui.paint.EffectPainter2D;
import consulo.language.editor.codeVision.CodeVisionEntry;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import org.jspecify.annotations.Nullable;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.function.Function;

public class CodeVisionVisionTextPainter<T> implements ICodeVisionEntryBasePainter<T> {
    private final Function<T, String> printer;
    private final CodeVisionTheme theme;

    public CodeVisionVisionTextPainter() {
        this(Object::toString, null);
    }

    public CodeVisionVisionTextPainter(Function<T, String> printer) {
        this(printer, null);
    }

    public CodeVisionVisionTextPainter(Function<T, String> printer, @Nullable CodeVisionTheme theme) {
        this.printer = printer;
        this.theme = theme != null ? theme : new CodeVisionTheme();
    }

    @Override
    public void paint(
        Editor editor,
        TextAttributes textAttributes,
        Graphics g,
        T value,
        Point point,
        RangeCodeVisionModel.InlayState state,
        boolean hovered,
        @Nullable CodeVisionEntry hoveredEntry
    ) {
        Graphics2D g2d = (Graphics2D) g;
        CodeVisionThemeInfoProvider themeInfoProvider = CodeVisionThemeInfoProvider.getInstance();

        boolean inSelectedBlock = textAttributes.getBackgroundColor() != null &&
            textAttributes.getBackgroundColor().equals(editor.getSelectionModel().getTextAttributes().getBackgroundColor());
        if (inSelectedBlock && editor.getSelectionModel().getTextAttributes().getForegroundColor() != null) {
            g2d.setColor(TargetAWT.to(editor.getSelectionModel().getTextAttributes().getForegroundColor()));
        }
        else {
            g2d.setColor(themeInfoProvider.foregroundColor(editor, hovered));
        }

        g2d.setFont(themeInfoProvider.font(editor));
        int x = point.x + theme.left;
        int y = point.y + theme.top;
        g2d.drawString(printer.apply(value), x, y);
        if (hovered) {
            Dimension size = size(editor, state, value);
            y += JBUI.scale(1);
            EffectPainter2D.LINE_UNDERSCORE.paint(g2d, x, y, size.width, 5.0, g2d.getFont());
        }
    }

    @Override
    public Dimension size(Editor editor, RangeCodeVisionModel.InlayState state, T value) {
        java.awt.FontMetrics fontMetrics =
            editor.getComponent().getFontMetrics(CodeVisionThemeInfoProvider.getInstance().font(editor));
        return new Dimension(
            fontMetrics.stringWidth(printer.apply(value)) + theme.left + theme.right,
            fontMetrics.getHeight() + theme.top + theme.bottom
        );
    }
}
