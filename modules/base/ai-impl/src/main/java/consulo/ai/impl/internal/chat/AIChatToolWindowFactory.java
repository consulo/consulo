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

import consulo.ai.AIProviderTable;
import consulo.ai.icon.AIIconGroup;
import consulo.ai.localize.AILocalize;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.project.ui.wm.ToolWindowManagerListener;
import consulo.project.ui.wm.ToolWindowFactory;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.ex.toolWindow.ToolWindowAnchor;
import consulo.ui.image.Image;

/**
 * @author VISTALL
 * @since 2026-08-04
 */
@ExtensionImpl
public class AIChatToolWindowFactory implements ToolWindowFactory, DumbAware {
    public static final String ID = "AI Chat";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public boolean validate(Project project) {
        return !AIProviderTable.getInstance().getProviders().isEmpty();
    }

    @Override
    public boolean canCloseContents() {
        return true;
    }

    @RequiredUIAccess
    @Override
    public void createToolWindowContent(Project project, ToolWindow toolWindow) {
        AIChatView chatView = project.getInstance(AIChatView.class);
        chatView.init(toolWindow);

        // like the terminal: showing the window again after its last chat was closed starts a new one
        project.getMessageBus().connect(project).subscribe(ToolWindowManagerListener.class, new ToolWindowManagerListener() {
            @Override
            public void stateChanged(ToolWindowManager manager) {
                if (!toolWindow.isVisible()) {
                    return;
                }
                toolWindow.setIcon(getIcon());
                if (toolWindow.getContentManager().getContentCount() == 0) {
                    chatView.addDefaultChat(toolWindow);
                }
            }
        });
    }

    @Override
    public ToolWindowAnchor getAnchor() {
        return ToolWindowAnchor.RIGHT;
    }

    @Override
    public Image getIcon() {
        return AIIconGroup.toolwindowAiassistant();
    }

    @Override
    public LocalizeValue getDisplayName() {
        return AILocalize.toolwindowDisplayName();
    }
}
