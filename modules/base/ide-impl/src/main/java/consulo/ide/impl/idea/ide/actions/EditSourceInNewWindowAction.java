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

import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.impl.internal.FileEditorManagerImpl;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

/**
 * @author Konstantin Bulenkov
 */
public class EditSourceInNewWindowAction extends DumbAwareAction {
    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        final FileEditorManager manager = FileEditorManager.getInstance(e == null ? null : e.getData(Project.KEY));
        ((FileEditorManagerImpl)manager).openFileInNewWindow(getVirtualFiles(e)[0]);
    }

    protected VirtualFile[] getVirtualFiles(AnActionEvent e) {
        final VirtualFile[] files = e.getData(VirtualFile.KEY_OF_ARRAY);
        if (files != null) {
            return files;
        }

        final VirtualFile file = e.getData(VirtualFile.KEY);
        return file == null ? VirtualFile.EMPTY_ARRAY : new VirtualFile[]{file};
    }

    @Override
    @RequiredUIAccess
    public void update(AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible((e == null ? null : e.getData(Project.KEY)) != null && getVirtualFiles(e).length == 1);
    }
}
