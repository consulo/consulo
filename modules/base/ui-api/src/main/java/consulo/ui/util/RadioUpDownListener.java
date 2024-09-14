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
package consulo.ui.util;

import consulo.annotation.access.RequiredReadAction;
import consulo.ui.Component;
import consulo.ui.RadioButton;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ComponentEventListener;
import consulo.ui.event.KeyPressedEvent;
import consulo.ui.event.details.KeyCode;

/**
 * @author VISTALL
 * @since 2024-09-14
 */
public final class RadioUpDownListener {
    public static void registerListener(RadioButton... radioButtons) {
        ComponentEventListener<Component, KeyPressedEvent> listener = e -> {
            KeyCode code = e.getInputDetails().getKeyCode();

            final int selected = getSelected(radioButtons);
            if (selected != -1) {
                if (code == KeyCode.UP) {
                    up(radioButtons, selected, selected);
                }
                else if (code == KeyCode.DOWN) {
                    down(radioButtons, selected, selected);
                }
            }
        };

        for (RadioButton radioButton : radioButtons) {
            radioButton.addKeyPressedListener(listener);
        }
    }

    @RequiredUIAccess
    private static void down(RadioButton[] radioButtons, int selected, int stop) {
        int newIdx = selected + 1;
        if (newIdx > radioButtons.length - 1) {
            newIdx = 0;
        }
        if (!click(radioButtons[newIdx]) && stop != newIdx) {
            down(radioButtons, newIdx, selected);
        }
    }

    @RequiredUIAccess
    private static void up(RadioButton[] radioButtons, int selected, int stop) {
        int newIdx = selected - 1;
        if (newIdx < 0) {
            newIdx = radioButtons.length - 1;
        }
        if (!click(radioButtons[newIdx]) && stop != newIdx) {
            up(radioButtons, newIdx, selected);
        }
    }

    @RequiredReadAction
    private static int getSelected(RadioButton[] radioButtons) {
        for (int i = 0; i < radioButtons.length; i++) {
            if (radioButtons[i].getValueOrError()) {
                return i;
            }
        }
        return -1;
    }

    @RequiredUIAccess
    private static boolean click(final RadioButton button) {
        if (button.isEnabled() && button.isVisible()) {
            button.focus();
            button.setValue(true);
            return true;
        }
        return false;
    }
}