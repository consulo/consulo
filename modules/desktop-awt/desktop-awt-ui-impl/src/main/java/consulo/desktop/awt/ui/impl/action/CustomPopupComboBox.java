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
package consulo.desktop.awt.ui.impl.action;

import consulo.ui.ex.awt.accessibility.ScreenReader;

import javax.swing.JComboBox;

/**
 * @author VISTALL
 * @since 2026-10-11
 */
public abstract class CustomPopupComboBox<E> extends JComboBox<E> {
    private static final String uiClassID = "ComboBoxActionButtonUI";

    protected CustomPopupComboBox() {
        setFocusable(ScreenReader.isActive());
    }

    public abstract void showPopupImpl();

    public abstract void hidePopupImpl();

    public abstract boolean isCustomPopupVisible();

    public boolean isArrowVisible() {
        return true;
    }

    @Override
    public String getUIClassID() {
        return uiClassID;
    }
}
