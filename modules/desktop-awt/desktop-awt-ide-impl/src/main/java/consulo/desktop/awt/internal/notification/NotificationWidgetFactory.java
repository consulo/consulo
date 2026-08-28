// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.internal.notification;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.ui.UISettings;
import consulo.project.Project;
import consulo.project.ui.wm.StatusBar;
import consulo.project.ui.wm.StatusBarWidget;
import consulo.project.ui.wm.StatusBarWidgetFactory;
import consulo.ui.ex.localize.UILocalize;
import consulo.ui.ex.toolWindow.ToolWindowSettings;

@ExtensionImpl(id = "notificationsWidget", order = "after readOnlyWidget")
public class NotificationWidgetFactory implements StatusBarWidgetFactory {
    @Override

    public String getDisplayName() {
        return UILocalize.statusBarNotificationsWidgetName().get();
    }

    @Override
    public boolean isAvailable(Project project) {
        return ToolWindowSettings.getInstance(project).isHideToolStripes() || UISettings.getInstance().getPresentationMode();
    }

    @Override

    public StatusBarWidget createWidget(Project project) {
        return new IdeNotificationArea(this);
    }

    @Override
    public boolean canBeEnabledOn(StatusBar statusBar) {
        Project project = statusBar.getProject();
        return project != null && isAvailable(project);
    }
}
