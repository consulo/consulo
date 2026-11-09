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

import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.continuousIntegration.CIBuildMonitor;
import consulo.continuousIntegration.CIManager;
import consulo.continuousIntegration.impl.icon.ContinuousIntegrationImplIconGroup;
import consulo.continuousIntegration.impl.internal.monitor.CIBuildMonitorImpl;
import consulo.continuousIntegration.impl.localize.ContinuousIntegrationImplLocalize;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowFactory;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentFactory;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.ex.toolWindow.ToolWindowAnchor;
import consulo.ui.image.Image;

/**
 * @since 2026-11-09
 */
@ExtensionImpl
public class CIToolWindowFactory implements ToolWindowFactory, DumbAware {
    public static final String ID = "ContinuousIntegration";

    @Override
    public String getId() {
        return ID;
    }

    @RequiredUIAccess
    @Override
    public void createToolWindowContent(Project project, ToolWindow toolWindow) {
        CIToolWindowPanel panel = new CIToolWindowPanel(
            project,
            CIManager.getInstance(project),
            (CIBuildMonitorImpl) CIBuildMonitor.getInstance(project)
        );
        Content content = ContentFactory.getInstance().createUIContent(panel.getComponent(), "", false);
        content.setDisposer(panel);
        toolWindow.getContentManager().addContent(content);
    }

    @Override
    public boolean shouldBeAvailable(Project project) {
        return CIToolWindowState.isAvailable(project);
    }

    @Override
    public ToolWindowAnchor getAnchor() {
        return ToolWindowAnchor.RIGHT;
    }

    @Override
    public Image getIcon() {
        return ContinuousIntegrationImplIconGroup.toolwindowci();
    }

    @Override
    public LocalizeValue getDisplayName() {
        return ContinuousIntegrationImplLocalize.toolwindowTitle();
    }
}
