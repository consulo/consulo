/*
 * Copyright 2013-2024 consulo.io
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
package consulo.ui.ex;

import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.ValueComponent;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ComponentEventListener;
import consulo.ui.event.ValueComponentEvent;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * When enabled the checkbox behaves like the ordinary checkbox
 * If to use special methods to enable/disable it,
 * it will manage different selected/unselected states for each mode - enabled or disabled
 *
 * @author VISTALL
 * @since 2024-07-06
 */
public final class StateRestoringCheckBoxWrapper {
    private final CheckBox myCheckBox;
    private boolean myIsSelectedWhenSelectable;

    @RequiredUIAccess
    public StateRestoringCheckBoxWrapper(LocalizeValue text) {
        myCheckBox = CheckBox.create(text);
    }

    public boolean isEnabled() {
        return myCheckBox.isEnabled();
    }

    public Boolean getValue() {
        return myCheckBox.getValue();
    }

    @RequiredUIAccess
    public void setValue(Boolean value) {
        myCheckBox.setValue(value);
    }

    @Nonnull
    public Component getComponent() {
        return myCheckBox;
    }

    @Nullable
    public Component getParentComponent() {
        return myCheckBox;
    }

    public Disposable addValueListener(
        @Nonnull ComponentEventListener<ValueComponent<Boolean>, ValueComponentEvent<Boolean>> valueListener
    ) {
        return myCheckBox.addValueListener(valueListener);
    }

    /**
     * The method should be used instead of setEnabled(false) or disable() in order to support selected state saving/recovering
     * Remembers the selected state of the checkbox when the checkbox is enabled, disables it
     * and sets the selected state according to tha parameter pased
     *
     * @param isSelected the parameter telling whetheer the checkbox is selected when disabled
     */
    @RequiredUIAccess
    public void makeUnselectable(boolean isSelected) {
        if (myCheckBox.isEnabled()) {
            myIsSelectedWhenSelectable = myCheckBox.getValue();
            myCheckBox.setEnabled(false);
        }
        myCheckBox.setValue(isSelected);
    }

    /**
     * The method should be used instead of setEnabled(true) or enable() in order to support selected state saving/recovering
     * Enables the checkbox and restores the selected state of the checkbox to the one, that it had before the makeUnselectable() method was called
     * that was before the checkbox was disabled
     */
    @RequiredUIAccess
    public void makeSelectable() {
        if (!myCheckBox.isEnabled()) {
            myCheckBox.setEnabled(true);
            myCheckBox.setValue(myIsSelectedWhenSelectable);
        }
    }

    public boolean isSelectedWhenSelectable() {
        if (myCheckBox.isEnabled()) {
            return myCheckBox.getValue();
        }
        return myIsSelectedWhenSelectable;
    }
}
