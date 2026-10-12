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
package consulo.desktop.qt.ui.impl.layout;

import consulo.desktop.qt.ui.impl.DesktopQtLength;
import consulo.ui.Length;
import io.qt.core.QMargins;
import io.qt.core.QRect;
import io.qt.core.QSize;
import io.qt.core.Qt;
import io.qt.widgets.QLayout;
import io.qt.widgets.QLayoutItem;
import io.qt.widgets.QWidget;
import io.qt.widgets.QWidgetItem;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-11
 */
public class DesktopQtTileLayout extends QLayout {
    private final List<QLayoutItem> myItems = new ArrayList<>();
    private final Length myMinTileWidth;
    private final int myGap;

    public DesktopQtTileLayout(Length minTileWidth, int gap) {
        myMinTileWidth = minTileWidth;
        myGap = gap;
    }

    @Override
    public void addItem(QLayoutItem item) {
        QWidget widget = item.widget();
        if (widget == null) {
            return;
        }

        item.dispose();
        myItems.add(new QWidgetItem(widget));
        invalidate();
    }

    @Override
    public int count() {
        return myItems.size();
    }

    @Override
    public @Nullable QLayoutItem itemAt(int index) {
        return index >= 0 && index < myItems.size() ? myItems.get(index) : null;
    }

    @Override
    public @Nullable QLayoutItem takeAt(int index) {
        if (index < 0 || index >= myItems.size()) {
            return null;
        }
        QLayoutItem item = myItems.remove(index);
        invalidate();
        return item;
    }

    @Override
    public Qt.Orientations expandingDirections() {
        return new Qt.Orientations(Qt.Orientation.Horizontal);
    }

    @Override
    public boolean hasHeightForWidth() {
        return true;
    }

    @Override
    public int heightForWidth(int width) {
        QMargins margins = contentsMargins();
        return arrange(width - margins.left() - margins.right(), 0, 0, false) + margins.top() + margins.bottom();
    }

    @Override
    public QSize sizeHint() {
        QMargins margins = contentsMargins();
        int width = minTileWidth();
        for (QLayoutItem item : visibleItems()) {
            width = Math.max(width, item.sizeHint().width());
        }
        int height = arrange(width, 0, 0, false);
        return new QSize(width + margins.left() + margins.right(), height + margins.top() + margins.bottom());
    }

    @Override
    public QSize minimumSize() {
        QMargins margins = contentsMargins();
        int width = 0;
        int height = 0;
        for (QLayoutItem item : visibleItems()) {
            QSize size = item.minimumSize();
            width = Math.max(width, size.width());
            height = Math.max(height, size.height());
        }
        return new QSize(width + margins.left() + margins.right(), height + margins.top() + margins.bottom());
    }

    @Override
    public void setGeometry(QRect rect) {
        super.setGeometry(rect);

        QMargins margins = contentsMargins();
        int width = rect.width() - margins.left() - margins.right();
        arrange(width, rect.x() + margins.left(), rect.y() + margins.top(), true);
    }

    private int arrange(int width, int left, int top, boolean apply) {
        List<QLayoutItem> items = visibleItems();
        if (items.isEmpty() || width <= 0) {
            return 0;
        }

        int fit = (width + myGap) / Math.max(1, minTileWidth() + myGap);
        int columns = Math.max(1, Math.min(fit, items.size()));

        int[] lefts = new int[columns];
        int[] widths = new int[columns];
        int tracks = Math.max(0, width - myGap * (columns - 1));
        for (int column = 0; column < columns; column++) {
            int start = column * tracks / columns + column * myGap;
            int end = (column + 1) * tracks / columns + column * myGap;
            lefts[column] = start;
            widths[column] = end - start;
        }

        int y = 0;
        for (int rowStart = 0; rowStart < items.size(); rowStart += columns) {
            int rowEnd = Math.min(items.size(), rowStart + columns);

            int rowHeight = 0;
            for (int i = rowStart; i < rowEnd; i++) {
                rowHeight = Math.max(rowHeight, heightFor(items.get(i), widths[i - rowStart]));
            }

            if (apply) {
                for (int i = rowStart; i < rowEnd; i++) {
                    int column = i - rowStart;
                    items.get(i).setGeometry(new QRect(left + lefts[column], top + y, widths[column], rowHeight));
                }
            }

            y += rowHeight;
            if (rowEnd < items.size()) {
                y += myGap;
            }
        }
        return y;
    }

    private static int heightFor(QLayoutItem item, int width) {
        int height = item.hasHeightForWidth() ? item.heightForWidth(width) : item.sizeHint().height();
        return Math.max(height, item.minimumSize().height());
    }

    private int minTileWidth() {
        QWidget widget = parentWidget();
        return widget == null ? myMinTileWidth.toPixels(16) : DesktopQtLength.toPixels(widget, myMinTileWidth);
    }

    private List<QLayoutItem> visibleItems() {
        List<QLayoutItem> items = new ArrayList<>(myItems.size());
        for (QLayoutItem item : myItems) {
            if (!item.isEmpty()) {
                items.add(item);
            }
        }
        return items;
    }
}
