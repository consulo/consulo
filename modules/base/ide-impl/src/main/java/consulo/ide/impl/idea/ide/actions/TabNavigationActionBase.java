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
import consulo.dataContext.DataContext;
import consulo.fileEditor.FileEditorWindow;
import consulo.fileEditor.internal.FileEditorManagerEx;
import consulo.language.editor.PlatformDataKeys;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.content.ContentManager;
import consulo.util.collection.ArrayUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

abstract class TabNavigationActionBase extends AnAction implements DumbAware {
    private static final Logger LOG = Logger.getInstance(TabNavigationActionBase.class);

    private final int myDir;

    TabNavigationActionBase(int dir) {
        LOG.assertTrue(dir == 1 || dir == -1);
        myDir = dir;
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        DataContext dataContext = e.getDataContext();
        Project project = e.getRequiredData(Project.KEY);

        ToolWindowManager windowManager = ToolWindowManager.getInstance(project);

        if (windowManager.isEditorComponentActive()) {
            doNavigate(dataContext, project);
            return;
        }

        ContentManager contentManager = e.getRequiredData(PlatformDataKeys.NONEMPTY_CONTENT_MANAGER);
        doNavigate(contentManager);
    }

    @Override
    public void update(@Nonnull AnActionEvent event) {
        Presentation presentation = event.getPresentation();
        Project project = event.getData(Project.KEY);
        presentation.setEnabled(false);
        if (project == null) {
            return;
        }
        ToolWindowManager windowManager = ToolWindowManager.getInstance(project);
        if (windowManager.isEditorComponentActive()) {
            FileEditorManagerEx editorManager = FileEditorManagerEx.getInstanceEx(project);
            FileEditorWindow currentWindow = event.getData(FileEditorWindow.DATA_KEY);
            if (currentWindow == null) {
                editorManager.getCurrentWindow();
            }
            if (currentWindow != null) {
                VirtualFile[] files = currentWindow.getFiles();
                presentation.setEnabled(files.length > 1);
            }
            return;
        }

        ContentManager contentManager = event.getData(PlatformDataKeys.NONEMPTY_CONTENT_MANAGER);
        presentation.setEnabled(contentManager != null && contentManager.getContentCount() > 1 && contentManager.isSingleSelection());
    }

    private void doNavigate(ContentManager contentManager) {
        if (myDir == -1) {
            contentManager.selectPreviousContent();
        }
        else {
            contentManager.selectNextContent();
        }
    }

    private void doNavigate(DataContext dataContext, Project project) {
        VirtualFile selectedFile = dataContext.getData(VirtualFile.KEY);
        navigateImpl(dataContext, project, selectedFile, myDir);
    }

    public static void navigateImpl(DataContext dataContext, Project project, VirtualFile selectedFile, int dir) {
        LOG.assertTrue(dir == 1 || dir == -1);
        FileEditorManagerEx editorManager = FileEditorManagerEx.getInstanceEx(project);
        FileEditorWindow currentWindow = dataContext.getData(FileEditorWindow.DATA_KEY);
        if (currentWindow == null) {
            currentWindow = editorManager.getCurrentWindow();
        }
        VirtualFile[] files = currentWindow.getFiles();
        int index = ArrayUtil.find(files, selectedFile);
        LOG.assertTrue(index != -1);
        editorManager.openFile(files[(index + files.length + dir) % files.length], true);
    }
}
