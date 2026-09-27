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

import consulo.desktop.qt.ui.impl.base.DesktopQtIconLabel;
import consulo.desktop.qt.ui.impl.image.DesktopQtIconOwner;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.LabelStyle;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtLabelImpl extends QtComponentDelegate<DesktopQtIconLabel> implements Label, DesktopQtIconOwner {
    private LocalizeValue myText = LocalizeValue.empty();
    private @Nullable Component myLabeledComponent;
    private @Nullable Image myImage;

    private final Set<LabelStyle> myStyles = EnumSet.noneOf(LabelStyle.class);

    public DesktopQtLabelImpl(LocalizeValue text) {
        myText = text;
    }

    @Override
    protected DesktopQtIconLabel createQt(QWidget parent) {
        return new DesktopQtIconLabel(parent);
    }

    @Override
    protected void initialize(DesktopQtIconLabel component) {
        component.setText(currentText());

        updateImage();

        updateBuddy();

        for (LabelStyle style : myStyles) {
            applyStyle(style);
        }
    }

    /**
     * A qt label only turns a {@code &} into an underline once it has a buddy to hand the key press to, so a
     * label standing on its own is given the text with the marker taken back out.
     */
    private String currentText() {
        return myComponent != null && myComponent.buddy() != null ? QtMnemonic.withMnemonic(myText) : QtMnemonic.plain(myText);
    }

    @Override
    public LocalizeValue getText() {
        return myText;
    }

    @RequiredUIAccess
    @Override
    public void setText(LocalizeValue text) {
        myText = text;

        if (myComponent != null) {
            myComponent.setText(currentText());
        }
    }

    @Override
    public void setImage(@Nullable Image icon) {
        myImage = icon;

        updateImage();
    }

    private void updateImage() {
        if (myComponent != null) {
            myComponent.setImage(myImage);
        }
    }

    @Override
    public void refreshIcons() {
        if (myImage != null) {
            updateImage();
        }
    }

    @Override
    public @Nullable Image getImage() {
        return myImage;
    }

    @Override
    public void setTarget(@Nullable Component component) {
        myLabeledComponent = component;

        updateBuddy();
    }

    private void updateBuddy() {
        if (myComponent == null || !(myLabeledComponent instanceof QtComponentDelegate<?> delegate)) {
            return;
        }

        delegate.whenBound(widget -> {
            if (myComponent != null) {
                myComponent.setBuddy(widget);
                myComponent.setText(currentText());
            }
        });
    }

    @Override
    public @Nullable Component getTarget() {
        return myLabeledComponent;
    }

    @Override
    public void addStyle(LabelStyle style) {
        myStyles.add(style);

        applyStyle(style);
    }

    private void applyStyle(LabelStyle style) {
        if (myComponent == null) {
            return;
        }

        switch (style) {
            case TRANSPARENT_BACKGROUND -> myComponent.setAutoFillBackground(false);
        }
    }
}
