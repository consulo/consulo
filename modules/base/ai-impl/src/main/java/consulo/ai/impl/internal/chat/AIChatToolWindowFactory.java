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
import consulo.ai.localize.AILocalize;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowFactory;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentFactory;
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

    /**
     * Without a provider plugin there is nothing to talk to, so the tool window is not registered at
     * all rather than shown empty. Installing one makes it appear on the next start.
     */
    @Override
    public boolean validate(Project project) {
        return !AIProviderTable.getInstance().getTypes().isEmpty();
    }

    @RequiredUIAccess
    @Override
    public void createToolWindowContent(Project project, ToolWindow toolWindow) {
        AIChatPanel panel = new AIChatPanel(project, UIAccess.current());

        Content content = ContentFactory.getInstance().createUIContent(panel.getComponent(), "", false);
        content.setCloseable(false);
        toolWindow.getContentManager().addContent(content);
    }

    @Override
    public ToolWindowAnchor getAnchor() {
        return ToolWindowAnchor.RIGHT;
    }

    @Override
    public Image getIcon() {
        return PlatformIconGroup.actionsLightning();
    }

    @Override
    public LocalizeValue getDisplayName() {
        return AILocalize.toolwindowDisplayName();
    }
}
