/*
 * Copyright 2013-2026 consulo.io
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
package consulo.desktop.qt.editor.impl.internal;

import consulo.codeEditor.DefaultLanguageHighlighterColors;
import consulo.codeEditor.Inlay;
import consulo.codeEditor.InlayContent;
import consulo.codeEditor.InlayContentSegment;
import consulo.codeEditor.impl.EditorSettingsExternalizable;
import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.TextAttributes;
import consulo.desktop.qt.ui.impl.TargetQt;
import consulo.desktop.qt.ui.impl.image.DesktopQtImage;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.image.Image;
import io.qt.core.QPointF;
import io.qt.core.Qt;
import io.qt.core.QRectF;
import io.qt.gui.QBrush;
import io.qt.gui.QFont;
import io.qt.gui.QFontMetricsF;
import io.qt.gui.QPen;
import io.qt.widgets.QApplication;
import io.qt.gui.QPainter;
import org.jspecify.annotations.Nullable;

import java.awt.Font;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * The hints standing between the characters of a line - a parameter name, an inferred type.
 * <p>
 * A renderer paints itself with {@code Graphics2D}, which is no use here, but it also answers
 * {@link InlayContent} - the text and the colour scheme keys of what it would have drawn. That is the seam a
 * frontend without awt has, and a renderer which does not answer it is simply not drawn, the same tolerance a
 * line marker presentation with no painter registered gets.
 * <p>
 * Only the placements which leave the row its usual height are handled: an inline hint and one past the end of
 * the line. A block hint stands on rows of its own and every row here is one line tall, so it is left out until
 * that holds no more.
 *
 * @author VISTALL
 * @since 2026-08-17
 */
public class DesktopQtEditorInlays {
    private static final double SMALLER_FONT_SCALE = 0.8;

    /** gap left either side of the hint, so it does not touch the code it sits between */
    private static final int MARGIN = 2;

    /** room inside the rounded background, around what the hint says */
    private static final int PADDING = 4;

    private static final int ARC = 8;

    /** the chip is inset from the row so consecutive lines of hints do not run together */
    private static final int VERTICAL_INSET = 1;

    /**
     * An inlay together with what it says and how wide that is, so the painter and the coordinate mapper cannot
     * measure it differently.
     */
    public record Rendered(Inlay<?> inlay, InlayContent content, double width) {
    }

    private final DesktopQtEditorImpl myEditor;

    private @Nullable QFont myHintFont;
    private @Nullable QFontMetricsF myHintMetrics;
    private @Nullable String myHintFontKey;

    public DesktopQtEditorInlays(DesktopQtEditorImpl editor) {
        myEditor = editor;
    }

    /**
     * The font a hint is set in. It is the interface font rather than the editor one unless the settings ask
     * otherwise - a hint is not code, and the awt editor sets it the same way.
     * <p>
     * The editor font is given in pixels, and a {@code QFont} sized that way answers -1 to {@code pointSizeF} -
     * scaling that produced a one point font, which is a hint drawn as an empty box of the right width. So the
     * pixel size is what gets scaled when there is one, and the hint is measured with metrics of its own rather
     * than by scaling the width of the editor font.
     */
    private synchronized QFont hintFont(boolean smaller) {
        QFont base = EditorSettingsExternalizable.getInstance().isUseEditorFontInInlays()
            ? myEditor.getFontMetrics().getFont(Font.PLAIN)
            : QApplication.font();

        String key = base.key() + '|' + smaller;

        if (myHintFont == null || !key.equals(myHintFontKey)) {
            QFont font = new QFont(base);

            if (smaller) {
                int pixelSize = base.pixelSize();

                if (pixelSize > 0) {
                    font.setPixelSize(Math.max(1, (int) Math.round(pixelSize * SMALLER_FONT_SCALE)));
                }
                else {
                    font.setPointSizeF(Math.max(1, base.pointSizeF() * SMALLER_FONT_SCALE));
                }
            }

            myHintFont = font;
            myHintMetrics = new QFontMetricsF(font);
            myHintFontKey = key;
        }

        return myHintFont;
    }

    private double textWidth(String text, boolean smaller) {
        if (text.isEmpty()) {
            return 0;
        }

        hintFont(smaller);

        return Objects.requireNonNull(myHintMetrics).horizontalAdvance(text);
    }

    /**
     * The hints drawn inside the row, in the order they are met walking it.
     */
    public List<Rendered> inlineIn(int startOffset, int endOffset) {
        List<Rendered> rendered = new ArrayList<>();

        for (Inlay<?> inlay : myEditor.getInlayModel().getInlineElementsInRange(startOffset, endOffset)) {
            addRendered(rendered, inlay);
        }

        rendered.sort(Comparator.comparingInt(entry -> entry.inlay().getOffset()));

        return rendered;
    }

    /**
     * The hints drawn past the end of the row, after everything the line itself holds.
     */
    public List<Rendered> afterLineEndIn(int startOffset, int endOffset) {
        List<Rendered> rendered = new ArrayList<>();

        for (Inlay<?> inlay : myEditor.getInlayModel().getAfterLineEndElementsInRange(startOffset, endOffset)) {
            addRendered(rendered, inlay);
        }

        rendered.sort(Comparator.comparingInt(entry -> entry.inlay().getOffset()));

        return rendered;
    }

    private void addRendered(List<Rendered> into, Inlay<?> inlay) {
        if (!inlay.isValid()) {
            return;
        }

        InlayContent content = inlay.getRenderer().getContent(inlay);
        if (content == null || content.segments().isEmpty()) {
            return;
        }

        into.add(new Rendered(inlay, content, widthOf(content)));
    }

    public double widthOf(InlayContent content) {
        return 2 * MARGIN + 2 * PADDING + contentWidth(content);
    }

    private double contentWidth(InlayContent content) {
        double width = 0;

        for (InlayContentSegment segment : content.segments()) {
            width += imageWidth(segment.image());
            width += textWidth(segment.text(), content.smallerFont());
        }

        return width;
    }

    private double imageWidth(@Nullable Image image) {
        return image == null ? 0 : Math.min(image.getWidth(), myEditor.getFontMetrics().getAscent());
    }

    /**
     * Draws the hint with its left edge at {@code x}, on the row whose top is {@code y}.
     */
    public void paint(QPainter painter, Rendered rendered, double x, int y, int lineHeight) {
        EditorColorsScheme scheme = myEditor.getColorsScheme();
        DesktopQtEditorFontMetrics metrics = myEditor.getFontMetrics();

        InlayContent content = rendered.content();

        boolean smaller = content.smallerFont();
        QFont font = hintFont(smaller);

        double chipX = x + MARGIN;
        double chipWidth = Math.max(0, rendered.width() - 2 * MARGIN);
        double chipHeight = Math.max(1, lineHeight - 2 * VERTICAL_INSET);

        ColorValue background = backgroundOf(scheme, content);
        if (background != null) {
            painter.save();
            painter.setRenderHint(QPainter.RenderHint.Antialiasing, true);
            painter.setPen(new QPen(Qt.PenStyle.NoPen));
            painter.setBrush(new QBrush(TargetQt.to(background)));
            painter.drawRoundedRect(new QRectF(chipX, y + VERTICAL_INSET, chipWidth, chipHeight), ARC, ARC);
            painter.restore();
        }

        int baseline = y + metrics.getAscent();
        double cursor = chipX + PADDING;

        painter.setFont(font);

        for (InlayContentSegment segment : content.segments()) {
            TextAttributes attributes = attributesOf(scheme, segment);

            Image image = segment.image();
            if (image instanceof DesktopQtImage qtImage) {
                int size = (int) Math.round(imageWidth(image));

                qtImage.toQIcon().paint(painter, (int) Math.round(cursor), y + (lineHeight - size) / 2, size, size);

                cursor += size;
            }

            String text = segment.text();
            if (!text.isEmpty()) {
                ColorValue foreground = attributes == null ? null : attributes.getForegroundColor();

                painter.setPen(TargetQt.to(foreground == null ? scheme.getDefaultForeground() : foreground));
                painter.drawText(new QPointF(cursor, baseline), text);

                cursor += textWidth(text, smaller);
            }
        }
    }

    /**
     * The hint is drawn as one rounded chip rather than a background per run, so the colour of the whole is taken
     * from the first run which names one.
     */
    private @Nullable ColorValue backgroundOf(EditorColorsScheme scheme, InlayContent content) {
        for (InlayContentSegment segment : content.segments()) {
            TextAttributes attributes = attributesOf(scheme, segment);

            if (attributes != null && attributes.getBackgroundColor() != null) {
                return attributes.getBackgroundColor();
            }
        }

        return null;
    }

    /**
     * Which run of the hint is under the pointer, or -1 when the pointer is in its margin or padding. A renderer
     * answers a click by the index of the run, so a click landing on the chip but on none of its runs is not one.
     */
    public int segmentAt(Rendered rendered, double xInInlay) {
        InlayContent content = rendered.content();

        double cursor = MARGIN + PADDING;
        int index = 0;

        for (InlayContentSegment segment : content.segments()) {
            double width = imageWidth(segment.image()) + textWidth(segment.text(), content.smallerFont());

            if (xInInlay >= cursor && xInInlay < cursor + width) {
                return index;
            }

            cursor += width;
            index++;
        }

        return -1;
    }

    /**
     * Runs whatever the hint does when it is clicked. Only the runs which say they answer a click are offered
     * one - the rest are text, and a click on them belongs to the editor.
     */
    @RequiredUIAccess
    public boolean click(Rendered rendered, double xInInlay, boolean controlDown) {
        int segment = segmentAt(rendered, xInInlay);
        if (segment < 0) {
            return false;
        }

        Inlay<?> inlay = rendered.inlay();

        if (!inlay.getRenderer().hasClickAction(inlay, segment)) {
            return false;
        }

        inlay.getRenderer().handleClick(inlay, segment, controlDown);

        return true;
    }

    private @Nullable TextAttributes attributesOf(EditorColorsScheme scheme, InlayContentSegment segment) {
        TextAttributes attributes =
            segment.attributesKey() == null ? null : scheme.getAttributes(segment.attributesKey());

        return attributes != null ? attributes : scheme.getAttributes(DefaultLanguageHighlighterColors.INLINE_PARAMETER_HINT);
    }
}
