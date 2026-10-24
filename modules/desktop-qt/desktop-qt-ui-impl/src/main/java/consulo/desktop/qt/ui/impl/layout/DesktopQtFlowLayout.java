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
 * Items at their size hint width, wrapped into rows at the width of the layout.
 *
 * @author VISTALL
 * @since 2026-10-24
 */
public class DesktopQtFlowLayout extends QLayout {
    private final List<QLayoutItem> myItems = new ArrayList<>();
    private final int myGap;

    public DesktopQtFlowLayout(int gap) {
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
        int width = 0;
        int height = 0;
        List<QLayoutItem> items = visibleItems();
        for (int i = 0; i < items.size(); i++) {
            QSize size = items.get(i).sizeHint();
            width += size.width() + (i > 0 ? myGap : 0);
            height = Math.max(height, size.height());
        }
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

        int y = 0;
        int rowStart = 0;
        while (rowStart < items.size()) {
            int rowEnd = rowStart;
            int rowWidth = 0;
            int rowHeight = 0;
            while (rowEnd < items.size()) {
                QSize size = items.get(rowEnd).sizeHint();
                int itemWidth = Math.min(size.width(), width);
                int next = rowWidth + (rowEnd > rowStart ? myGap : 0) + itemWidth;
                if (rowEnd > rowStart && next > width) {
                    break;
                }
                rowWidth = next;
                rowHeight = Math.max(rowHeight, size.height());
                rowEnd++;
            }

            if (apply) {
                int x = 0;
                for (int i = rowStart; i < rowEnd; i++) {
                    QLayoutItem item = items.get(i);
                    int itemWidth = Math.min(item.sizeHint().width(), width);
                    item.setGeometry(new QRect(left + x, top + y, itemWidth, rowHeight));
                    x += itemWidth + myGap;
                }
            }

            y += rowHeight;
            if (rowEnd < items.size()) {
                y += myGap;
            }
            rowStart = rowEnd;
        }
        return y;
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
