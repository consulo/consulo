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
import consulo.annotation.component.ExtensionAPI;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

/**
 * Provides the packages of a target - a module, an interpreter - to the packages editor.
 * <p>
 * The target is a string the provider resolves again, after a restart too, since the editor of it is restored with the project.
 *
 * @see PackagesEditorManager
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface PackageManagementServiceProvider {
    /**
     * The identifier of the provider, a part of the path of the editor file, which must not change.
     */
    String getId();

    LocalizeValue getName();

    Image getIcon();

    /**
     * The title of the editor of the target.
     */
    default LocalizeValue getTitle(String target) {
        return LocalizeValue.join(": ", getName(), LocalizeValue.of(target));
    }

    /**
     * Creates the service managing the packages of the target, or returns null if the target is gone - the module was removed.
     */
    @Nullable
    PackageManagementService create(Project project, String target);
}
