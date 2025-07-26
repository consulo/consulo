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
import consulo.localize.LocalizeValue;
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

    protected enum Direction {
        PREVIOUS(-1), NEXT(+1);

        public final int dir;

        private Direction(int dir) {
            this.dir = dir;
        }
    }
    private final Direction myDir;

    TabNavigationActionBase(@Nonnull LocalizeValue text, @Nonnull LocalizeValue description, @Nonnull Direction dir) {
        super(text, description);
        myDir = dir;
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getRequiredData(Project.KEY);

        ToolWindowManager windowManager = ToolWindowManager.getInstance(project);

        if (windowManager.isEditorComponentActive()) {
            doNavigate(e.getDataContext(), project);
            return;
        }

        ContentManager contentManager = e.getRequiredData(PlatformDataKeys.NONEMPTY_CONTENT_MANAGER);
        doNavigate(contentManager);
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        Project project = e.getData(Project.KEY);
        presentation.setEnabled(false);
        if (project == null) {
            return;
        }
        ToolWindowManager windowManager = ToolWindowManager.getInstance(project);
        if (windowManager.isEditorComponentActive()) {
            FileEditorManagerEx editorManager = FileEditorManagerEx.getInstanceEx(project);
            FileEditorWindow currentWindow = e.getData(FileEditorWindow.DATA_KEY);
            if (currentWindow == null) {
                editorManager.getCurrentWindow();
            }
            if (currentWindow != null) {
                VirtualFile[] files = currentWindow.getFiles();
                presentation.setEnabled(files.length > 1);
            }
            return;
        }

        ContentManager contentManager = e.getData(PlatformDataKeys.NONEMPTY_CONTENT_MANAGER);
        presentation.setEnabled(contentManager != null && contentManager.getContentCount() > 1 && contentManager.isSingleSelection());
    }

    private void doNavigate(ContentManager contentManager) {
        if (myDir == Direction.PREVIOUS) {
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

    private static void navigateImpl(DataContext dataContext, Project project, VirtualFile selectedFile, @Nonnull Direction dir) {
        FileEditorManagerEx editorManager = FileEditorManagerEx.getInstanceEx(project);
        FileEditorWindow currentWindow = dataContext.getData(FileEditorWindow.DATA_KEY);
        if (currentWindow == null) {
            currentWindow = editorManager.getCurrentWindow();
        }
        VirtualFile[] files = currentWindow.getFiles();
        int index = ArrayUtil.find(files, selectedFile);
        LOG.assertTrue(index != -1);
        editorManager.openFile(files[(index + files.length + dir.dir) % files.length], true);
    }
}
