/*
 * Copyright 2013-2025 consulo.io
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

import consulo.annotation.component.ExtensionImpl;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.popup.JBPopup;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;

/**
 * @author VISTALL
 * @since 2025-06-23
 */
@ExtensionImpl(order = "last")
public class NoneTouchBarFacade implements TouchBarFacade {
    
    @Override
    public String getId() {
        return "none";
    }

    
    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.localizeTODO("<none>");
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void initialize() {

    }

    @Override
    public void setButtonActions(JComponent component, Collection<? extends JButton> buttons, Collection<? extends JButton> principal, JButton defaultButton, @Nullable ActionGroup extraActions) {

    }

    @Override
    public @Nullable Disposable showWindowActions(Component contentPane) {
        return null;
    }

    @Override
    public void onUpdateEditorHeader(Object editor, JComponent header) {

    }

    @Override
    public void showPopupItems(JBPopup popup, JComponent popupComponent) {

    }

    @Override
    public void setActions(JComponent component, @Nullable ActionGroup group) {

    }
}
