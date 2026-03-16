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
package consulo.desktop.awt.os.mac.internal.touchBar;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.internal.TouchBarFacade;
import consulo.ui.ex.popup.JBPopup;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;

/**
 * @author VISTALL
 * @since 21.06.2025.
 */
@ExtensionImpl
public class MacTouchBarFacade implements TouchBarFacade {
    
    @Override
    public String getId() {
        return "macTouchBar";
    }

    
    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.localizeTODO("Mac Touch Bar");
    }

    @Override
    public boolean isAvailable() {
        return Platform.current().os().isMac();
    }

    @Override
    public boolean isEnabled() {
        return TouchbarSupport.isEnabled();
    }

    @Override
    public void initialize() {
        TouchbarSupport.onApplicationLoaded();
    }

    @Override
    public void setButtonActions(JComponent component,
                                 Collection<? extends JButton> buttons,
                                 Collection<? extends JButton> principal,
                                 JButton defaultButton,
                                 @Nullable ActionGroup extraActions) {
        Touchbar.setButtonActions(component, buttons, principal, defaultButton,  extraActions);
    }

    @Nullable
    @Override
    public Disposable showWindowActions(Component contentPane) {
        return TouchbarSupport.showWindowActions(contentPane);
    }

    @Override
    public void onUpdateEditorHeader(Object editor, JComponent header) {
        TouchbarSupport.onUpdateEditorHeader((Editor) editor);
    }

    @Override
    public void showPopupItems(JBPopup popup, JComponent popupComponent) {
        TouchbarSupport.showPopupItems(popup, popupComponent);
    }

    @Override
    public void setActions(JComponent component, @Nullable ActionGroup group) {
        Touchbar.setActions(component, group);
    }
}
