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
package consulo.ai.impl.internal.chat;

import consulo.ai.icon.AIIconGroup;
import consulo.ai.localize.AILocalize;
import consulo.annotation.component.ActionImpl;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.toolWindow.ToolWindow;

/**
 * Puts the cursor into the message box of the chat the user works with - or shows the chats when none is open.
 *
 * @author VISTALL
 * @since 2026-11-14
 */
@ActionImpl(id = "AI.FocusChat")
public class FocusAIChatAction extends DumbAwareAction implements AnActionWithSyncUpdate {
    public FocusAIChatAction() {
        super(AILocalize.actionFocusChatText(), AILocalize.actionFocusChatDescription(), AIIconGroup.toolwindowAiassistant());
    }

    @Override
    public void update(AnActionEvent e) {
        e.getPresentation().setEnabled(e.getData(Project.KEY) != null);
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        if (project == null) {
            return;
        }
        AIChatPanel panel = project.getInstance(AIChatView.class).showActiveChat();
        if (panel != null) {
            panel.focusInput();
            return;
        }
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(AIChatToolWindowFactory.ID);
        if (toolWindow != null) {
            toolWindow.activate(null);
        }
    }
}
