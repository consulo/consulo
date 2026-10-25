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
package consulo.desktop.qt.ui.impl.avatar;

import consulo.desktop.qt.ui.impl.QtComponentDelegate;
import consulo.desktop.qt.ui.impl.image.DesktopQtIconOwner;
import consulo.localize.LocalizeValue;
import consulo.ui.Avatar;
import consulo.ui.AvatarStyle;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.ui.impl.avatar.AvatarPresentation;
import io.qt.widgets.QLabel;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-18
 */
public class DesktopQtAvatarImpl extends QtComponentDelegate<QLabel> implements Avatar, DesktopQtIconOwner {
    private LocalizeValue myName;
    private @Nullable Image myImage;
    private @Nullable AvatarStyle myStyle;
    private @Nullable AvatarStyle myOutline;

    public DesktopQtAvatarImpl(LocalizeValue name, @Nullable Image image) {
        myName = name;
        myImage = image;
    }

    @Override
    protected QLabel createQt(QWidget parent) {
        return new QLabel(parent);
    }

    @Override
    protected void initialize(QLabel component) {
        update(component);
    }

    @Override
    public void refreshIcons() {
        QLabel component = myComponent;
        if (component != null && !component.isDisposed()) {
            update(component);
        }
    }

    @Override
    public void addStyle(AvatarStyle style) {
        if (AvatarPresentation.isOutline(style)) {
            myOutline = style;
        }
        else {
            myStyle = style;
        }
        refreshIcons();
    }

    @Override
    public LocalizeValue getName() {
        return myName;
    }

    @RequiredUIAccess
    @Override
    public void setName(LocalizeValue name) {
        myName = name;
        refreshIcons();
    }

    @Override
    public @Nullable Image getImage() {
        return myImage;
    }

    @RequiredUIAccess
    @Override
    public void setImage(@Nullable Image image) {
        myImage = image;
        refreshIcons();
    }

    private void update(QLabel component) {
        int size = AvatarPresentation.sizeOf(myStyle);
        String name = myName.get();
        component.setFixedSize(size, size);
        component.setPixmap(DesktopQtAvatarPainter.paint(name, myImage, size, myOutline, component.devicePixelRatioF(), component.palette()));
        component.setToolTip(name);
        DesktopQtAvatarPainter.repaintWhileLoading(component, myImage, this::refreshIcons);
    }
}
