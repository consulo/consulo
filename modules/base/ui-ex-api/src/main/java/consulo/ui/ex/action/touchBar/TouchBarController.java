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
package consulo.ui.ex.action.touchBar;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.disposer.Disposable;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.internal.TouchBarControllerInternal;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;
import java.util.Objects;

/**
 * @author VISTALL
 * @since 2025-06-08
 */
@ServiceAPI(ComponentScope.APPLICATION)
public sealed interface TouchBarController permits TouchBarControllerInternal {
    static TouchBarController getInstance() {
        return Application.get().getInstance(TouchBarController.class);
    }

    boolean isEnabled();

    default void setButtonActions(@Nonnull JComponent component,
                                  Collection<? extends JButton> buttons,
                                  Collection<? extends JButton> principal,
                                  JButton defaultButton) {
        setButtonActions(component, buttons, principal, defaultButton, null);
    }

    void setButtonActions(@Nonnull JComponent component,
                          Collection<? extends JButton> buttons,
                          Collection<? extends JButton> principal,
                          JButton defaultButton,
                          @Nullable ActionGroup extraActions);

    @Nullable
    Disposable showWindowActions(@Nonnull Component contentPane);

    default void setActions(@Nonnull JComponent component, @Nonnull String actionId) {
        ActionGroup action = (ActionGroup) ActionManager.getInstance().getAction(actionId);
        setActions(component, Objects.requireNonNull(action, () -> "Can't find action group: " + actionId));
    }

    default void setActions(@Nonnull JComponent component, @Nonnull AnAction anAction) {
        setActions(component, ActionGroup.of(anAction));
    }

    void setActions(@Nonnull JComponent component, @Nullable ActionGroup group);
}
