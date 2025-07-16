
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
import consulo.fileEditor.internal.FileEditorManagerEx;
import consulo.ide.localize.IdeLocalize;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.undoRedo.CommandProcessor;
import jakarta.annotation.Nonnull;

public class PrevSplitAction extends AnAction implements DumbAware {
    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getRequiredData(Project.KEY);
        CommandProcessor.getInstance().newCommand()
            .project(project)
            .name(IdeLocalize.commandGoToPrevSplit())
            .run(() -> {
                FileEditorManagerEx manager = FileEditorManagerEx.getInstanceEx(project);
                manager.setCurrentWindow(manager.getPrevWindow(manager.getCurrentWindow()));
            });
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        if (project == null) {
            e.getPresentation().setEnabled(false);
            return;
        }
        FileEditorManagerEx manager = FileEditorManagerEx.getInstanceEx(project);
        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        e.getPresentation().setEnabled(toolWindowManager.isEditorComponentActive() && manager.isInSplitter() && manager.getCurrentWindow() != null);
    }
}
