/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ide.actions;

import consulo.language.content.ProjectRootsUtil;
import consulo.module.Module;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author Konstantin Bulenkov
 */
public class OpenModuleSettingsAction extends EditSourceAction {
    @RequiredUIAccess
    @Override
    public void update(@Nonnull AnActionEvent event) {
        super.update(event);

        if (!isModuleInProjectViewPopup(event)) {
            event.getPresentation().setEnabledAndVisible(false);
        }
    }

    @Nullable
    @Override
    protected Image getTemplateIcon() {
        return null;
    }

    protected static boolean isModuleInProjectViewPopup(@Nonnull AnActionEvent e) {
        return ActionPlaces.PROJECT_VIEW_POPUP.equals(e.getPlace()) && isModuleInContext(e);
    }

    public static boolean isModuleInContext(@Nonnull AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        Module module = e.getData(Module.KEY);
        if (project != null && module != null) {
            VirtualFile moduleFolder = e.getData(VirtualFile.KEY);
            if (moduleFolder == null) {
                return false;
            }
            if (ProjectRootsUtil.isModuleContentRoot(moduleFolder, project) || ProjectRootsUtil.isModuleSourceRoot(moduleFolder, project)) {
                return true;
            }
        }
        return false;
    }
}
