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
package consulo.ui.ex.internal;

import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ClickEvent;
import consulo.ui.event.ComponentEventListener;
import consulo.ui.ex.ComboBoxWithCustomPopup;
import consulo.ui.ex.popup.JBPopup;
import org.jspecify.annotations.Nullable;

import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2026-10-11
 */
public final class ComboBoxCustomPopupHandler implements ComponentEventListener<Component, ClickEvent> {
    private static final int REOPEN_THRESHOLD_MS = 200;

    private final ComboBoxWithCustomPopup<?> myComboBox;
    private final Function<Runnable, @Nullable JBPopup> myPopupBuilder;

    private @Nullable JBPopup myPopup;
    private long myHiddenAt;

    public ComboBoxCustomPopupHandler(ComboBoxWithCustomPopup<?> comboBox, Function<Runnable, @Nullable JBPopup> popupBuilder) {
        myComboBox = comboBox;
        myPopupBuilder = popupBuilder;
    }

    @RequiredUIAccess
    @Override
    public void onEvent(ClickEvent event) {
        if (!myComboBox.isEnabled()) {
            return;
        }

        JBPopup current = myPopup;
        if (current != null) {
            myPopup = null;
            current.cancel();
            return;
        }

        if (System.currentTimeMillis() - myHiddenAt < REOPEN_THRESHOLD_MS) {
            return;
        }

        JBPopup popup = myPopupBuilder.apply(this::popupClosed);
        if (popup == null) {
            return;
        }

        myPopup = popup;
        popup.showBy(myComboBox, null);
    }

    private void popupClosed() {
        myPopup = null;
        myHiddenAt = System.currentTimeMillis();
    }
}
