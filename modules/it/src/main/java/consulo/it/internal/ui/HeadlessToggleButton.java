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
package consulo.it.internal.ui;

import consulo.localize.LocalizeValue;
import consulo.ui.ToggleButton;
import consulo.ui.event.ValueComponentEvent;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 */
public class HeadlessToggleButton extends HeadlessButton implements ToggleButton {
    private boolean mySelected;

    public HeadlessToggleButton(LocalizeValue text) {
        super(text);
    }

    @Override
    public Boolean getValue() {
        return mySelected;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setValue(@Nullable Boolean value, boolean fireListeners) {
        boolean selected = value != null && value;
        if (mySelected == selected) {
            return;
        }
        mySelected = selected;
        if (fireListeners) {
            getListenerDispatcher(ValueComponentEvent.class).onEvent(new ValueComponentEvent<>(this, selected));
        }
    }
}
