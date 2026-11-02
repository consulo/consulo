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
package consulo.mcpServer.impl.internal.activity;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.application.dumb.DumbAware;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.mcpServer.impl.internal.setting.McpServerSettings;
import consulo.mcpServer.localize.McpServerLocalize;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowFactory;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentFactory;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.ex.toolWindow.ToolWindowAnchor;
import consulo.ui.image.Image;
import jakarta.inject.Inject;

/**
 * @author VISTALL
 * @since 2026-11-02
 */
@ExtensionImpl
public class McpToolWindowFactory implements ToolWindowFactory, DumbAware {
    public static final String ID = "MCP";

    private final Application myApplication;
    private final McpServerSettings mySettings;
    private final McpToolCallLog myCallLog;

    @Inject
    public McpToolWindowFactory(Application application, McpServerSettings settings, McpToolCallLog callLog) {
        myApplication = application;
        mySettings = settings;
        myCallLog = callLog;
    }

    @Override
    public String getId() {
        return ID;
    }

    @RequiredUIAccess
    @Override
    public void createToolWindowContent(Project project, ToolWindow toolWindow) {
        Disposable disposable = Disposable.newDisposable("McpCallLogPanel");
        McpCallLogPanel panel = new McpCallLogPanel(myApplication, myCallLog, disposable);
        Content content = ContentFactory.getInstance().createUIContent(panel.getComponent(), "", false);
        content.setDisposer(disposable);
        toolWindow.getContentManager().addContent(content);
    }

    @Override
    public boolean shouldBeAvailable(Project project) {
        return mySettings.isEnabled();
    }

    @Override
    public ToolWindowAnchor getAnchor() {
        return ToolWindowAnchor.BOTTOM;
    }

    @Override
    public Image getIcon() {
        return PlatformIconGroup.toolwindowsToolwindowmessages();
    }

    @Override
    public LocalizeValue getDisplayName() {
        return McpServerLocalize.toolwindowDisplayName();
    }
}
