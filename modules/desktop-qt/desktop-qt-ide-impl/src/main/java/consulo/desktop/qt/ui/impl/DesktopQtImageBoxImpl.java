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
package consulo.desktop.qt.ui.impl;

import consulo.desktop.qt.ui.impl.image.DesktopQtIconOwner;
import consulo.desktop.qt.ui.impl.image.DesktopQtImage;
import consulo.ui.ImageBox;
import consulo.ui.image.Image;
import io.qt.gui.QPixmap;
import io.qt.widgets.QLabel;
import io.qt.widgets.QSizePolicy;
import io.qt.widgets.QWidget;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtImageBoxImpl extends QtComponentDelegate<QLabel> implements ImageBox, DesktopQtIconOwner {
    private final Image myImage;

    public DesktopQtImageBoxImpl(Image image) {
        myImage = image;
    }

    @Override
    protected QLabel createQt(QWidget parent) {
        return new QLabel(parent);
    }

    @Override
    protected void initialize(QLabel component) {
        applyImage(component);
    }

    @Override
    public void refreshIcons() {
        if (myComponent != null) {
            applyImage(myComponent);
        }
    }

    private void applyImage(QLabel component) {
        QPixmap pixmap = myImage instanceof DesktopQtImage qtImage ? qtImage.toQPixmap() : null;

        if (pixmap != null && !pixmap.isNull()) {
            component.setPixmap(pixmap);

            // a label already asks for the size of its pixmap and whatever border was put around it, while a fixed
            // size would swallow that border instead of leaving room for it
            component.setSizePolicy(QSizePolicy.Policy.Fixed, QSizePolicy.Policy.Fixed);
        }
        else {
            component.setFixedSize(myImage.getWidth(), myImage.getHeight());
        }
    }

    @Override
    public Image getImage() {
        return myImage;
    }
}
