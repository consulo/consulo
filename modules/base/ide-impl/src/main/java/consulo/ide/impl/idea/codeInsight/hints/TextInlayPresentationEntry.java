// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.codeInsight.hints;

import consulo.application.ApplicationManager;
import consulo.codeEditor.Editor;
import consulo.codeEditor.event.EditorMouseEvent;
import consulo.colorScheme.TextAttributes;
import consulo.ide.impl.idea.ui.paint.EffectPainter;
import consulo.language.editor.inlay.InlayActionData;
import consulo.project.Project;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.awt.util.DesktopAntialiasingTypeUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;

import java.awt.*;

public class TextInlayPresentationEntry extends InlayPresentationEntry {
    private final String text;
    private final byte parentIndexToSwitch;

    public TextInlayPresentationEntry(String text, byte parentIndexToSwitch, InlayMouseArea clickArea) {
        super(clickArea);
        this.text = text;
        this.parentIndexToSwitch = parentIndexToSwitch;
    }

    @Override
    public void handleClick(EditorMouseEvent e,
                            InlayPresentationList list,
                            boolean controlDown) {
        Editor editor = e.getEditor();
        Project project = editor.getProject();
        if (clickArea != null && project != null) {
            InlayActionData actionData = clickArea.getActionData();
            if (controlDown) {
                ApplicationManager.getApplication()
                    .getService(DeclarativeInlayActionService.class)
                    .invokeActionHandler(actionData, e);
            }
        }
        if (parentIndexToSwitch != (byte) -1) {
            list.toggleTreeState(parentIndexToSwitch);
        }
    }

    @Override
    public void render(Graphics2D graphics,
                       InlayTextMetrics metrics,
                       TextAttributes attributes,
                       boolean isDisabled,
                       int yOffset,
                       int rectHeight,
                       Editor editor) {
        Object savedHint = graphics.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);
        Color savedColor = graphics.getColor();
        try {
            ColorValue foreground = attributes.getForegroundColor();
            if (foreground != null) {
                int width = computeWidth(metrics);
                int height = computeHeight(metrics);
                Font font = metrics.getFont();
                graphics.setFont(font);
                graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    DesktopAntialiasingTypeUtil.getKeyForCurrentScope(false));
                graphics.setColor(TargetAWT.to(foreground));
                int baseline = Math.max(editor.getAscent(),
                    (rectHeight + metrics.getAscent() - metrics.getDescent()) / 2) - 1;
                graphics.drawString(text, 0, baseline);
                ColorValue effectColor = attributes.getEffectColor();
                if (effectColor == null) {
                    effectColor = foreground;
                }
                if (isDisabled) {
                    graphics.setColor(TargetAWT.to(effectColor));
                    EffectPainter.STRIKE_THROUGH.paint(graphics, 0,
                        baseline + yOffset, width, height, font);
                }
            }
        }
        finally {
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, savedHint);
            graphics.setColor(savedColor);
        }
    }

    @Override
    public int computeWidth(InlayTextMetrics metrics) {
        return metrics.getStringWidth(text);
    }

    @Override
    public int computeHeight(InlayTextMetrics metrics) {
        return metrics.getFontHeight();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TextInlayPresentationEntry)) {
            return false;
        }
        TextInlayPresentationEntry that = (TextInlayPresentationEntry) other;
        return text.equals(that.text);
    }

    @Override
    public int hashCode() {
        return text.hashCode();
    }

    @Override
    public String toString() {
        return "TextInlayPresentationEntry(text='" + text +
            "', parentIndexToSwitch=" + parentIndexToSwitch + ")";
    }
}