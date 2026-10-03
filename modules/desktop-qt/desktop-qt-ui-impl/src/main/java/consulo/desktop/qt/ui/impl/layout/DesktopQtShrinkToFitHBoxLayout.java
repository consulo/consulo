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

import consulo.ui.ex.internal.ShrinkToFit;
import io.qt.core.QRect;
import io.qt.widgets.QHBoxLayout;
import io.qt.widgets.QLayoutItem;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public class DesktopQtShrinkToFitHBoxLayout extends QHBoxLayout {
    @Override
    public void setGeometry(QRect rect) {
        super.setGeometry(rect);

        List<QLayoutItem> items = new ArrayList<>();
        List<QRect> geometries = new ArrayList<>();
        for (int i = 0; i < count(); i++) {
            QLayoutItem item = itemAt(i);
            if (item != null && !item.isEmpty()) {
                items.add(item);
                geometries.add(item.geometry());
            }
        }

        if (!isLeftToRight(geometries)) {
            return;
        }

        int[] preferred = new int[items.size()];
        int[] minimum = new int[items.size()];
        long deficit = 0;
        for (int i = 0; i < items.size(); i++) {
            QLayoutItem item = items.get(i);
            int width = geometries.get(i).width();
            if (item.hasHeightForWidth()) {
                preferred[i] = width;
                minimum[i] = width;
                continue;
            }

            preferred[i] = Math.max(width, item.sizeHint().width());
            minimum[i] = Math.min(width, item.minimumSize().width());
            deficit += preferred[i] - width;
        }

        if (deficit <= 0) {
            return;
        }

        int[] widths = ShrinkToFit.widths(preferred, minimum, deficit);

        int oldRight = geometries.get(0).x();
        int newRight = oldRight;
        for (int i = 0; i < items.size(); i++) {
            QRect old = geometries.get(i);
            int x = newRight + old.x() - oldRight;
            oldRight = old.x() + old.width();
            newRight = x + widths[i];
            if (x != old.x() || widths[i] != old.width()) {
                items.get(i).setGeometry(new QRect(x, old.y(), widths[i], old.height()));
            }
        }
    }

    private static boolean isLeftToRight(List<QRect> geometries) {
        if (geometries.isEmpty()) {
            return false;
        }

        for (int i = 1; i < geometries.size(); i++) {
            if (geometries.get(i).x() < geometries.get(i - 1).x()) {
                return false;
            }
        }
        return true;
    }
}
