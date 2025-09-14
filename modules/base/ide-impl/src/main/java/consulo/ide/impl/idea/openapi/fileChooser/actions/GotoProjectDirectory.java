/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.fileChooser.actions;

import consulo.annotation.component.ActionImpl;
import consulo.application.Application;
import consulo.fileChooser.FileSystemTree;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.ui.ex.action.AnActionEvent;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

@ActionImpl(id = "FileChooser.GotoProject")
public final class GotoProjectDirectory extends FileChooserAction {
    @Inject
    public GotoProjectDirectory(Application application) {
        super(
            ActionLocalize.actionFilechooserGotoprojectText(),
            ActionLocalize.actionFilechooserGotoprojectDescription(),
            application.getIcon()
        );
    }

    @Override
    protected void actionPerformed(@Nonnull FileSystemTree fileSystemTree, @Nonnull AnActionEvent e) {
        VirtualFile projectPath = getProjectDir(e);
        if (projectPath != null) {
            fileSystemTree.select(projectPath, () -> fileSystemTree.expand(projectPath, null));
        }
    }

    @Override
    protected void update(@Nonnull FileSystemTree fileSystemTree, @Nonnull AnActionEvent e) {
        VirtualFile projectPath = getProjectDir(e);
        e.getPresentation().setEnabled(projectPath != null && fileSystemTree.isUnderRoots(projectPath));
    }

    @Nullable
    private static VirtualFile getProjectDir(@Nonnull AnActionEvent e) {
        VirtualFile projectFileDir = e.getData(Project.PROJECT_FILE_DIRECTORY);
        return projectFileDir != null && projectFileDir.isValid() ? projectFileDir : null;
    }
}
