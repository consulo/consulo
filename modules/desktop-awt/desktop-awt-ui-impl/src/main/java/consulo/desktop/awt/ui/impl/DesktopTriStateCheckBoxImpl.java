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
package consulo.desktop.awt.ui.impl;

import consulo.desktop.awt.ui.impl.event.DesktopAWTInputDetails;
import consulo.desktop.awt.ui.impl.facade.FromSwingComponentWrapper;
import consulo.desktop.awt.ui.impl.base.SwingComponentDelegate;
import consulo.localize.LocalizeValue;
import consulo.ui.CheckBoxStyle;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ValueComponentEvent;
import consulo.util.lang.ThreeState;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-08-23
 */
public class DesktopTriStateCheckBoxImpl extends SwingComponentDelegate<DesktopTriStateCheckBoxImpl.MyTriStateCheckBox>
    implements consulo.ui.TriStateCheckBox {

    class MyTriStateCheckBox extends consulo.ui.ex.awt.ThreeStateCheckBox implements FromSwingComponentWrapper {
        private LocalizeValue myLabelText = LocalizeValue.empty();

        @Override
        public void updateUI() {
            super.updateUI();

            // null if called from parent object before field initialize
            if (myLabelText != null) {
                updateLabelText();
            }
        }

        @Override
        public Component toUIComponent() {
            return DesktopTriStateCheckBoxImpl.this;
        }

        public void setLabelText(LocalizeValue labelText) {
            myLabelText = labelText;
        }

        public LocalizeValue getLabelText() {
            return myLabelText;
        }

        private void updateLabelText() {
            updateTextForButton(this, myLabelText);
        }
    }

    @RequiredUIAccess
    @Override
    protected MyTriStateCheckBox createComponent() {
        MyTriStateCheckBox component = new MyTriStateCheckBox();
        component.addActionListener(e -> fireListeners());
        return component;
    }

    @Override
    public void addStyle(CheckBoxStyle style) {
        switch (style) {
            case TRANSPARENT_BACKGROUND:
                toAWTComponent().setOpaque(false);
                break;
        }
    }

    @Override
    public ThreeState getValue() {
        return fromState(toAWTComponent().getState());
    }

    @RequiredUIAccess
    @Override
    public void setValue(@Nullable ThreeState value, boolean fireListeners) {
        toAWTComponent().setState(toState(value));

        if (fireListeners) {
            fireListeners();
        }
    }

    @SuppressWarnings("unchecked")
    @RequiredUIAccess
    private void fireListeners() {
        getListenerDispatcher(ValueComponentEvent.class)
            .onEvent(new ValueComponentEvent(this, getValue(), DesktopAWTInputDetails.currentEvent(toAWTComponent())));
    }

    @Override
    public boolean isUnsureEnabled() {
        return toAWTComponent().isThirdStateEnabled();
    }

    @RequiredUIAccess
    @Override
    public void setUnsureEnabled(boolean unsureEnabled) {
        toAWTComponent().setThirdStateEnabled(unsureEnabled);
    }

    @Override
    public LocalizeValue getLabelText() {
        return toAWTComponent().getLabelText();
    }

    @RequiredUIAccess
    @Override
    public void setLabelText(LocalizeValue labelText) {
        toAWTComponent().setLabelText(labelText);
        toAWTComponent().updateLabelText();
    }

    private static consulo.ui.ex.awt.ThreeStateCheckBox.State toState(@Nullable ThreeState value) {
        if (value == null) {
            return consulo.ui.ex.awt.ThreeStateCheckBox.State.DONT_CARE;
        }
        return switch (value) {
            case YES -> consulo.ui.ex.awt.ThreeStateCheckBox.State.SELECTED;
            case NO -> consulo.ui.ex.awt.ThreeStateCheckBox.State.NOT_SELECTED;
            case UNSURE -> consulo.ui.ex.awt.ThreeStateCheckBox.State.DONT_CARE;
        };
    }

    private static ThreeState fromState(consulo.ui.ex.awt.ThreeStateCheckBox.State state) {
        return switch (state) {
            case SELECTED -> ThreeState.YES;
            case NOT_SELECTED -> ThreeState.NO;
            case DONT_CARE -> ThreeState.UNSURE;
        };
    }
}
