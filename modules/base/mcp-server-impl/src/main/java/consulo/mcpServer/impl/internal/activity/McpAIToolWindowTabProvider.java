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

import consulo.ai.AIToolWindowTabProvider;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.mcpServer.impl.internal.setting.McpServerSettings;
import consulo.mcpServer.localize.McpServerLocalize;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.inject.Inject;

/**
 * The calls the MCP server of the IDE answered, as a tab of the AI tool window.
 *
 * @author VISTALL
 * @since 2026-11-14
 */
@ExtensionImpl
public class McpAIToolWindowTabProvider implements AIToolWindowTabProvider {
    private final Application myApplication;
    private final McpServerSettings mySettings;
    private final McpToolCallLog myCallLog;

    @Inject
    public McpAIToolWindowTabProvider(Application application, McpServerSettings settings, McpToolCallLog callLog) {
        myApplication = application;
        mySettings = settings;
        myCallLog = callLog;
    }

    @Override
    public LocalizeValue getDisplayName() {
        return McpServerLocalize.toolwindowDisplayName();
    }

    @Override
    public boolean isAvailable(Project project) {
        return mySettings.isEnabled();
    }

    @RequiredUIAccess
    @Override
    public Component createComponent(Project project, Disposable parentDisposable) {
        return new McpCallLogPanel(myApplication, myCallLog, parentDisposable).getComponent();
    }
}
