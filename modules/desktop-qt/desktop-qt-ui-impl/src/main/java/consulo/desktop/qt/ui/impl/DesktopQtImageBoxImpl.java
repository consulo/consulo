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

import consulo.desktop.qt.ui.impl.base.DesktopQtImageWidget;
import consulo.desktop.qt.ui.impl.image.DesktopQtIconOwner;
import consulo.ui.ImageBox;
import consulo.ui.image.Image;
import io.qt.widgets.QWidget;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtImageBoxImpl extends QtComponentDelegate<DesktopQtImageWidget> implements ImageBox, DesktopQtIconOwner {
    private final Image myImage;

    public DesktopQtImageBoxImpl(Image image) {
        myImage = image;
    }

    @Override
    protected DesktopQtImageWidget createQt(QWidget parent) {
        return new DesktopQtImageWidget(parent);
    }

    @Override
    protected void initialize(DesktopQtImageWidget component) {
        component.setImage(myImage);
    }

    @Override
    public void refreshIcons() {
        DesktopQtImageWidget component = myComponent;
        if (component != null && !component.isDisposed()) {
            component.setImage(myImage);
        }
    }

    @Override
    public Image getImage() {
        return myImage;
    }
}
