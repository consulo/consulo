
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
import consulo.ui.ex.action.Presentation;
import consulo.undoRedo.CommandProcessor;

public class NextSplitAction extends AnAction implements DumbAware {
    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        CommandProcessor.getInstance().newCommand()
            .project(project)
            .name(IdeLocalize.commandGoToNextSplit())
            .run(() -> {
                FileEditorManagerEx manager = FileEditorManagerEx.getInstanceEx(project);
                manager.setCurrentWindow(manager.getNextWindow(manager.getCurrentWindow()));
            });
    }

    @Override
    @RequiredUIAccess
    public void update(AnActionEvent event) {
        Project project = event.getData(Project.KEY);
        Presentation presentation = event.getPresentation();
        if (project == null) {
            presentation.setEnabled(false);
            return;
        }
        FileEditorManagerEx manager = FileEditorManagerEx.getInstanceEx(project);
        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        presentation.setEnabled(
            toolWindowManager.isEditorComponentActive() && manager.isInSplitter() && manager.getCurrentWindow() != null
        );
    }
}
