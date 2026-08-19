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

import consulo.localize.LocalizeValue;
import consulo.ui.ToggleButton;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ValueComponentEvent;
import consulo.ui.event.details.InputDetails;
import consulo.ui.event.details.ProgrammaticInputDetails;
import io.qt.widgets.QPushButton;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class DesktopQtToggleButtonImpl extends DesktopQtButtonImpl implements ToggleButton {
    private boolean mySelected;

    private boolean myFireListeners = true;

    public DesktopQtToggleButtonImpl(LocalizeValue text) {
        super(text);
    }

    @Override
    protected void initialize(QPushButton component) {
        component.setCheckable(true);
        component.setChecked(mySelected);

        super.initialize(component);

        component.toggled.connect(checked -> {
            mySelected = checked;

            if (myFireListeners) {
                getListenerDispatcher(ValueComponentEvent.class)
                    .onEvent(new ValueComponentEvent(this, mySelected, DesktopQtCurrentInput.current(component)));
            }
        });
    }

    /**
     * A click is what flips a toggle - the value listeners see the new state, the click listeners still run so
     * an action bound to the button performs with it.
     */
    @RequiredUIAccess
    @Override
    public void invoke(InputDetails inputDetails) {
        setValue(!mySelected);

        super.invoke(inputDetails);
    }

    @Override
    public Boolean getValue() {
        return mySelected;
    }

    @RequiredUIAccess
    @Override
    public void setValue(@Nullable Boolean value, boolean fireListeners) {
        boolean selected = value != null && value;

        if (mySelected == selected) {
            return;
        }

        mySelected = selected;

        if (myComponent == null) {
            if (fireListeners) {
                getListenerDispatcher(ValueComponentEvent.class)
                    .onEvent(new ValueComponentEvent(this, mySelected, ProgrammaticInputDetails.INSTANCE));
            }
            return;
        }

        myFireListeners = fireListeners;
        try {
            myComponent.setChecked(mySelected);
        }
        finally {
            myFireListeners = true;
        }
    }
}
