/*
 * Copyright 2013-2016 consulo.io
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
package consulo.desktop.awt.ui.impl;

import consulo.desktop.awt.facade.FromSwingComponentWrapper;
import consulo.desktop.awt.ui.impl.base.SwingComponentDelegate;
import consulo.desktop.awt.ui.impl.util.AWTFocusAdapterAsBlurListener;
import consulo.desktop.awt.ui.impl.util.AWTFocusAdapterAsFocusListener;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.*;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.event.BlurEvent;
import consulo.ui.event.ComponentEventListener;
import consulo.ui.event.FocusEvent;
import consulo.ui.ex.awt.JBLabel;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.internal.LocalizeValueWithMnemonic;
import consulo.ui.image.Image;
import consulo.ui.util.TextWithMnemonic;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 12-Jun-16
 */
class DesktopLabelImpl extends SwingComponentDelegate<DesktopLabelImpl.MyJLabel> implements Label {
    public class MyJLabel extends JBLabel implements FromSwingComponentWrapper {
        private LocalizeValue myTextValue;

        private HorizontalAlignment myHorizontalAlignment2 = HorizontalAlignment.LEFT;

        private ColorValue myForegroudColor;

        MyJLabel(@Nonnull LocalizeValue text, LabelOptions options) {
            super("");

            setHorizontalAlignment2(options.getHorizontalAlignment());

            myTextValue = text;

            updateText();
        }

        @Override
        public void updateUI() {
            super.updateUI();

            updateText();
        }

        @Nonnull
        @Override
        public Component toUIComponent() {
            return DesktopLabelImpl.this;
        }

        public void setForegroundColor(ColorValue foregroundColor) {
            myForegroudColor = foregroundColor;

            updateForegroundColor();
        }

        private void setHorizontalAlignment2(@Nonnull HorizontalAlignment horizontalAlignment) {
            myHorizontalAlignment2 = horizontalAlignment;
            switch (horizontalAlignment) {
                case LEFT:
                    setHorizontalAlignment(SwingConstants.LEFT);
                    break;
                case CENTER:
                    setHorizontalAlignment(SwingConstants.CENTER);
                    break;
                case RIGHT:
                    setHorizontalAlignment(SwingConstants.RIGHT);
                    break;
            }
        }

        public HorizontalAlignment getHorizontalAlignment2() {
            return myHorizontalAlignment2;
        }

        @Nonnull
        public LocalizeValue getTextValue() {
            return myTextValue;
        }

        public void setTextValue(@Nonnull LocalizeValue textValue) {
            myTextValue = textValue;

            updateText();
        }

        private void updateForegroundColor() {
            if (myForegroudColor == null) {
                setForeground(null);
            }
            else {
                setForeground(TargetAWT.to(myForegroudColor));
            }
        }

        private void updateText() {
            if (myTextValue == null) {
                return;
            }

            TextWithMnemonic mnemonicInfo = LocalizeValueWithMnemonic.get(myTextValue);
            if (!mnemonicInfo.hasMnemonic()) {
                setText(mnemonicInfo.getText());
                setDisplayedMnemonicIndex(-1);
                setDisplayedMnemonic(0);
            }
            else {
                setText(mnemonicInfo.getText());
                setDisplayedMnemonicIndex(mnemonicInfo.getMnemonicIndex());
                setDisplayedMnemonic(mnemonicInfo.getMnemonic());
            }
        }
    }

    public DesktopLabelImpl(LocalizeValue text, LabelOptions options) {
        initialize(new MyJLabel(text, options));
    }

    @Override
    public void setImage(@Nullable Image icon) {
        toAWTComponent().setIcon(TargetAWT.to(icon));
    }

    @Nullable
    @Override
    public Image getImage() {
        return TargetAWT.from(toAWTComponent().getIcon());
    }

    @RequiredUIAccess
    @Override
    public void setText(@Nonnull LocalizeValue text) {
        toAWTComponent().setTextValue(text);
    }

    @Nonnull
    @Override
    public LocalizeValue getText() {
        return toAWTComponent().getTextValue();
    }

    @Override
    public void setForegroundColor(ColorValue colorValue) {
        toAWTComponent().setForegroundColor(colorValue);
    }

    @Nonnull
    @Override
    public Disposable addFocusListener(@Nonnull ComponentEventListener<HasFocus, FocusEvent> listener) {
        AWTFocusAdapterAsFocusListener adapter = new AWTFocusAdapterAsFocusListener(this, listener);
        toAWTComponent().addFocusListener(adapter);
        return () -> toAWTComponent().removeFocusListener(adapter);
    }

    @Nonnull
    @Override
    public Disposable addBlurListener(@Nonnull ComponentEventListener<HasFocus, BlurEvent> listener) {
        AWTFocusAdapterAsBlurListener adapter = new AWTFocusAdapterAsBlurListener(this, listener);
        toAWTComponent().addFocusListener(adapter);
        return () -> toAWTComponent().removeFocusListener(adapter);
    }

    @Nullable
    @Override
    public ColorValue getForegroundColor() {
        return toAWTComponent().myForegroudColor;
    }

    @Nullable
    @Override
    public Component getTarget() {
        return TargetAWT.from(toAWTComponent().getLabelFor());
    }

    @Override
    public void setTarget(@Nullable Component component) {
        toAWTComponent().setLabelFor(TargetAWT.to(component));
    }
}
