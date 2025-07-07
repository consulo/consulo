/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

import consulo.application.dumb.DumbAware;
import consulo.fileEditor.FileEditorWindow;
import consulo.fileEditor.internal.FileEditorManagerEx;
import consulo.util.lang.Comparing;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;

public class CloseAllEditorsButActiveAction extends AnAction implements DumbAware {
    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        FileEditorManagerEx fileEditorManager = FileEditorManagerEx.getInstanceEx(project);
        VirtualFile selectedFile;
        FileEditorWindow window = e.getData(FileEditorWindow.DATA_KEY);
        if (window != null) {
            window.closeAllExcept(e.getData(VirtualFile.KEY));
            return;
        }
        selectedFile = fileEditorManager.getSelectedFiles()[0];
        VirtualFile[] siblings = fileEditorManager.getSiblings(selectedFile);
        for (VirtualFile sibling : siblings) {
            if (!Comparing.equal(selectedFile, sibling)) {
                fileEditorManager.closeFile(sibling);
            }
        }
    }

    @Override
    public void update(@Nonnull AnActionEvent event) {
        Presentation presentation = event.getPresentation();
        Project project = event.getData(Project.KEY);
        if (project == null) {
            presentation.setEnabled(false);
            return;
        }
        FileEditorManagerEx fileEditorManager = FileEditorManagerEx.getInstanceEx(project);
        VirtualFile selectedFile;
        FileEditorWindow window = event.getData(FileEditorWindow.DATA_KEY);
        if (window != null) {
            presentation.setEnabled(window.getFiles().length > 1);
            return;
        }
        else {
            if (fileEditorManager.getSelectedFiles().length == 0) {
                presentation.setEnabled(false);
                return;
            }
            selectedFile = fileEditorManager.getSelectedFiles()[0];
        }
        VirtualFile[] siblings = fileEditorManager.getSiblings(selectedFile);
        presentation.setEnabled(siblings.length > 1);
    }
}
