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
package consulo.continuousIntegration.impl.internal.toolWindow;

import consulo.continuousIntegration.CIManager;
import consulo.continuousIntegration.CIServer;
import consulo.continuousIntegration.CIServerType;
import consulo.continuousIntegration.impl.localize.ContinuousIntegrationImplLocalize;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.toolWindow.ToolWindow;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Title and icon of the tool window: with a single provider (Jenkins only) the tool window takes its name and icon.
 *
 * @since 2026-11-09
 */
public final class CIToolWindowState {
    private CIToolWindowState() {
    }

    @RequiredUIAccess
    public static void update(Project project) {
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(CIToolWindowFactory.ID);
        if (toolWindow == null) {
            return;
        }

        Set<CIServerType<?>> types = new LinkedHashSet<>();
        for (CIServer server : CIManager.getInstance(project).getServers()) {
            types.add(server.getType());
        }
        if (types.isEmpty()) {
            project.getApplication().getExtensionPoint(CIServerType.class).forEach(types::add);
        }

        if (types.size() == 1) {
            CIServerType<?> type = types.iterator().next();
            toolWindow.setIcon(type.getIcon());
            toolWindow.setDisplayName(type.getPresentableName());
        }
        else {
            toolWindow.setIcon(PlatformIconGroup.webreferencesServer());
            toolWindow.setDisplayName(ContinuousIntegrationImplLocalize.toolwindowTitle());
        }
    }
}
