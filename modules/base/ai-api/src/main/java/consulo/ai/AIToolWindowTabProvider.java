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
package consulo.ai;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;

/**
 * A tab of the AI tool window beside the chats - the calls the MCP server of the IDE answered, for one.
 *
 * @author VISTALL
 * @since 2026-11-14
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface AIToolWindowTabProvider {
    LocalizeValue getDisplayName();

    /**
     * Whether the tab is shown in the project - the tool window is there while the chats or a tab are.
     */
    default boolean isAvailable(Project project) {
        return true;
    }

    @RequiredUIAccess
    Component createComponent(Project project, Disposable parentDisposable);
}
