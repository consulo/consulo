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
package consulo.desktop.qt.ui.impl.base;

import consulo.desktop.qt.ui.impl.image.DesktopQtImage;
import consulo.ui.image.Image;
import io.qt.core.QMargins;
import io.qt.core.QSize;
import io.qt.gui.QIcon;
import io.qt.gui.QPaintEvent;
import io.qt.gui.QPainter;
import io.qt.widgets.QSizePolicy;
import io.qt.widgets.QStyle;
import io.qt.widgets.QStyleOption;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

public class DesktopQtImageWidget extends QWidget {
    private @Nullable Image myImage;

    public DesktopQtImageWidget(@Nullable QWidget parent) {
        super(parent);

        setSizePolicy(QSizePolicy.Policy.Fixed, QSizePolicy.Policy.Fixed);
    }

    public @Nullable Image getImage() {
        return myImage;
    }

    public void setImage(@Nullable Image image) {
        boolean sameImage = myImage == image;
        myImage = image;

        if (!sameImage) {
            updateGeometry();
        }
        update();
    }

    @Override
    public QSize sizeHint() {
        Image image = myImage;
        QMargins margins = contentsMargins();

        int width = image == null ? 0 : Math.max(0, image.getWidth());
        int height = image == null ? 0 : Math.max(0, image.getHeight());

        return new QSize(width + margins.left() + margins.right(), height + margins.top() + margins.bottom());
    }

    @Override
    public QSize minimumSizeHint() {
        return sizeHint();
    }

    @Override
    protected void paintEvent(QPaintEvent event) {
        QPainter painter = new QPainter(this);
        try {
            QStyleOption option = new QStyleOption();
            option.initFrom(this);
            style().drawPrimitive(QStyle.PrimitiveElement.PE_Widget, option, painter, this);

            Image image = myImage;
            if (image != null) {
                DesktopQtImage.paint(painter, contentsRect(), isEnabled() ? QIcon.Mode.Normal : QIcon.Mode.Disabled, image, null);
            }
        }
        finally {
            painter.end();
        }
    }
}
