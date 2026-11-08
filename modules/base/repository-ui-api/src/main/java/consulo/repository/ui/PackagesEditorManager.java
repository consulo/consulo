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
package consulo.repository.ui;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;

/**
 * Opens the packages editor of a target - the installed packages and the repository of a module or an interpreter, in an editor tab.
 * The same target of the same provider is opened in the same tab.
 */
@ServiceAPI(ComponentScope.PROJECT)
public interface PackagesEditorManager {
    static PackagesEditorManager getInstance(Project project) {
        return project.getInstance(PackagesEditorManager.class);
    }

    @RequiredUIAccess
    void open(Class<? extends PackageManagementServiceProvider> providerClass, String target);

    @RequiredUIAccess
    void open(PackageManagementServiceProvider provider, String target);
}
