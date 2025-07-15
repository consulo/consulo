/*
 * Copyright 2013-2023 consulo.io
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
package consulo.versionControlSystem.action;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.localize.LocalizeValue;
import consulo.platform.base.localize.ActionLocalize;
import consulo.ui.ex.action.AnActionEvent;
import jakarta.annotation.Nonnull;

@ExtensionAPI(ComponentScope.APPLICATION)
public interface AnnotateToggleActionProvider {
    boolean isEnabled(AnActionEvent e);

    boolean isSuspended(AnActionEvent e);

    boolean isAnnotated(AnActionEvent e);

    void perform(AnActionEvent e, boolean selected);

    @Nonnull
    default LocalizeValue getActionName(@Nonnull AnActionEvent e) {
        return ActionLocalize.actionAnnotateText();
    }
}
