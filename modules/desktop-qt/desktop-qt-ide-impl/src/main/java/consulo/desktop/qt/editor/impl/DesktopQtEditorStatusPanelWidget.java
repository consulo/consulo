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
package consulo.desktop.qt.editor.impl;

import consulo.colorScheme.EditorColorsScheme;
import consulo.desktop.qt.ui.impl.TargetQt;
import consulo.desktop.qt.ui.impl.image.DesktopQtImage;
import consulo.language.editor.impl.internal.markup.AnalyzerStatus;
import consulo.language.editor.impl.internal.markup.AnalyzingType;
import consulo.language.editor.impl.internal.markup.ErrorStripeRenderer;
import consulo.language.editor.impl.internal.markup.PassWrapper;
import consulo.language.editor.impl.internal.markup.StatusItem;
import consulo.ui.image.Image;
import io.qt.core.QPoint;
import io.qt.core.QRect;
import io.qt.gui.QIcon;
import io.qt.gui.QPaintEvent;
import io.qt.gui.QPainter;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

/**
 * The counters in the top right corner of the editor - how many errors and warnings the analyser has found, and
 * whether it is still looking.
 * <p>
 * {@link ErrorStripeRenderer} paints itself with {@code java.awt.Graphics}, which is no use here, but it also
 * answers {@link ErrorStripeRenderer#getStatus} with an {@link AnalyzerStatus} - plain data. That is the seam the
 * web frontend reads too, and it is the only one a frontend without awt can use.
 *
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtEditorStatusPanelWidget extends QWidget {
    private static final int ICON_SIZE = 12;
    private static final int ITEM_GAP = 8;
    private static final int ICON_TEXT_GAP = 2;
    private static final int PADDING = 6;

    private final DesktopQtEditorWidget mySurface;
    private final DesktopQtEditorImpl myEditor;

    private List<StatusItem> myItems = List.of();

    public DesktopQtEditorStatusPanelWidget(DesktopQtEditorWidget surface, DesktopQtEditorImpl editor) {
        super(surface);
        mySurface = surface;
        myEditor = editor;
    }

    /**
     * Re-reads the analyser and resizes to whatever it now has to say. Called whenever the markup changes, which
     * is what the daemon does as it works through the file.
     */
    public void refresh() {
        List<StatusItem> items = readItems();

        myItems = items;

        setVisible(!items.isEmpty());

        if (items.isEmpty()) {
            return;
        }

        setToolTip(buildTooltip());
        mySurface.updateSideAreas();
        update();
    }

    private List<StatusItem> readItems() {
        DesktopQtMarkupModelImpl markupModel = (DesktopQtMarkupModelImpl) myEditor.getMarkupModel();

        ErrorStripeRenderer renderer = markupModel.getErrorStripeRenderer();
        if (renderer == null || !markupModel.isErrorStripeVisible()) {
            return List.of();
        }

        AnalyzerStatus status = renderer.getStatus(myEditor);

        List<StatusItem> items = status.getExpandedStatus();
        if (!items.isEmpty()) {
            return items;
        }

        // a clean file produces no counters, and the icon alone then says what happened - the green check, the
        // eye while analysing, the crossed out light when highlighting is off
        Image icon = status.getIcon();
        return icon == null ? List.of() : List.of(new StatusItem("", icon));
    }

    /**
     * What the awt panel puts in the popup its widget opens: the daemon title, the progress of every running pass
     * and the severity summary. There is no popup here yet, so it is the tooltip - as it is in the browser.
     */
    private String buildTooltip() {
        DesktopQtMarkupModelImpl markupModel = (DesktopQtMarkupModelImpl) myEditor.getMarkupModel();

        ErrorStripeRenderer renderer = markupModel.getErrorStripeRenderer();
        if (renderer == null) {
            return "";
        }

        AnalyzerStatus status = renderer.getStatus(myEditor);

        List<String> lines = new ArrayList<>();

        if (status.getTitle() != null && !status.getTitle().isEmpty()) {
            lines.add(status.getTitle());
        }
        if (status.getDetails() != null && !status.getDetails().isEmpty()) {
            lines.add(status.getDetails());
        }

        for (PassWrapper pass : status.getPasses()) {
            lines.add(pass.getPresentableName() + ": " + pass.toPercent() + "%");
        }

        if (lines.isEmpty()) {
            StringBuilder summary = new StringBuilder();
            for (StatusItem item : myItems) {
                if (!summary.isEmpty()) {
                    summary.append(", ");
                }
                summary.append(item.getText());

                if (item.getType() != null) {
                    summary.append(' ').append(item.getType());
                }
            }

            if (status.getAnalyzingType() != AnalyzingType.COMPLETE) {
                summary.append(" so far");
            }

            lines.add(summary.toString());
        }

        return String.join("\n", lines);
    }

    /**
     * How wide the counters are, so the surface can place the panel clear of the error strip.
     */
    public int preferredWidth() {
        if (myItems.isEmpty()) {
            return 0;
        }

        DesktopQtEditorFontMetrics metrics = myEditor.getFontMetrics();

        int width = PADDING;
        for (StatusItem item : myItems) {
            if (item.getIcon() != null) {
                width += ICON_SIZE + ICON_TEXT_GAP;
            }

            width += (int) Math.ceil(metrics.getTextWidth(item.getText())) + ITEM_GAP;
        }

        return width + PADDING;
    }

    public int preferredHeight() {
        return Math.max(ICON_SIZE, myEditor.getLineHeight()) + 2 * 2;
    }

    @Override
    protected void paintEvent(QPaintEvent event) {
        QPainter painter = new QPainter(this);
        try {
            paint(painter);
        }
        finally {
            painter.end();
        }
    }

    private void paint(QPainter painter) {
        EditorColorsScheme scheme = myEditor.getColorsScheme();
        DesktopQtEditorFontMetrics metrics = myEditor.getFontMetrics();

        // the panel floats over the text, so it carries the editor background rather than letting the code show
        // through the counters
        painter.fillRect(0, 0, width(), height(), TargetQt.to(scheme.getDefaultBackground()));

        painter.setFont(metrics.getFont(Font.PLAIN));
        painter.setPen(TargetQt.to(scheme.getDefaultForeground()));

        int x = PADDING;
        int centerY = height() / 2;

        for (StatusItem item : myItems) {
            QIcon icon = toIcon(item.getIcon());
            if (icon != null) {
                icon.paint(painter, new QRect(x, centerY - ICON_SIZE / 2, ICON_SIZE, ICON_SIZE));
                x += ICON_SIZE + ICON_TEXT_GAP;
            }

            String text = item.getText();
            if (!text.isEmpty()) {
                painter.drawText(new QPoint(x, centerY + metrics.getAscent() / 2 - 1), text);
                x += (int) Math.ceil(metrics.getTextWidth(text));
            }

            x += ITEM_GAP;
        }
    }

    private static @Nullable QIcon toIcon(@Nullable Image image) {
        return image instanceof DesktopQtImage qtImage ? qtImage.toQIcon() : null;
    }
}
