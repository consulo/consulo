/*
 * Copyright 2000-2024 JetBrains s.r.o.
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
package consulo.versionControlSystem.distributed.push;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.project.Project;

import java.util.List;

/**
 * Provides additional actions to the "Push" combo-button in the Push Dialog.
 * <p>
 * If there are several extensions implementing this interface, the order of groups of actions is defined by the order of extensions.
 *
 * @see PushDialogCustomizer
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface PushDialogActionsProvider {
    /**
     * Returns the list of actions which will be located above the default "Push" action.
     * The action which is first in the list will be the default action of the combo-button.
     */
    List<PushActionBase> getCustomActionsAboveDefault(Project project);
}
