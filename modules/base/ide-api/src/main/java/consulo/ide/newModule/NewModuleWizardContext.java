/*
 * Copyright 2013-2019 consulo.io
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
package consulo.ide.newModule;

import consulo.ide.localize.IdeLocalize;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2019-08-20
 *
 * Base implementation {@link NewModuleWizardContextBase}
 */
public interface NewModuleWizardContext {
    void setName(@Nonnull String name);

    void setPath(@Nonnull String path);

    @Nonnull
    String getName();

    @Nonnull
    String getPath();

    @Nonnull
    default String getTargetId() {
        return isNewProject()
            ? IdeLocalize.projectNewWizardProjectIdentification().get()
            : IdeLocalize.projectNewWizardModuleIdentification().get();
    }

    /**
     * @return true if its called from NewProject action
     */
    default boolean isNewProject() {
        return getProject() == null;
    }

    @Nullable
    default Project getProject() {
        return null;
    }
}
