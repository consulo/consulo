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

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.popup.JBPopup;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;

/**
 * @author VISTALL
 * @since 2025-06-08
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface TouchBarFacade {
    @Nonnull
    String getId();

    @Nonnull
    LocalizeValue getDisplayName();

    boolean isAvailable();

    boolean isEnabled();

    void initialize();

    void setButtonActions(@Nonnull JComponent component,
                          Collection<? extends JButton> buttons,
                          Collection<? extends JButton> principal,
                          JButton defaultButton,
                          @Nullable ActionGroup extraActions);

    @Nullable
    Disposable showWindowActions(@Nonnull Component contentPane);

    void onUpdateEditorHeader(@Nonnull Object editor, JComponent header);

    void showPopupItems(@Nonnull JBPopup popup, @Nonnull JComponent popupComponent);

    void setActions(@Nonnull JComponent component, @Nullable ActionGroup group);
}
