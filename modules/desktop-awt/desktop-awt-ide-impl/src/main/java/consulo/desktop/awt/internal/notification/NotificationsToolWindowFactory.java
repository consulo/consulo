/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.desktop.awt.internal.notification;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.HelpManager;
import consulo.application.dumb.DumbAware;
import consulo.codeEditor.Editor;
import consulo.codeEditor.SoftWrapAppliancePlaces;
import consulo.ide.impl.idea.notification.impl.NotificationsConfigurable;
import consulo.ide.impl.idea.notification.impl.NotificationsConfigurationImpl;
import consulo.ide.impl.idea.openapi.editor.actions.ScrollToTheEndToolbarAction;
import consulo.ide.impl.idea.openapi.editor.actions.ToggleUseSoftWrapsToolbarAction;
import consulo.ide.impl.idea.ui.AncestorListenerAdapter;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.ui.view.localize.ProjectUIViewLocalize;
import consulo.project.ui.wm.ToolWindowFactory;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.SimpleToolWindowPanel;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentFactory;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.ex.toolWindow.ToolWindowAnchor;
import consulo.ui.image.Image;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;

import javax.swing.event.AncestorEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * @author peter
 */
@ExtensionImpl
public class NotificationsToolWindowFactory implements ToolWindowFactory, DumbAware {
    @Nonnull
    @Override
    public String getId() {
        return EventLog.NOTIFICATIONS_TOOLWINDOW_ID;
    }

    @RequiredUIAccess
    @Override
    public void createToolWindowContent(@Nonnull Project project, @Nonnull ToolWindow toolWindow) {
        NotificationProjectTracker tracker = NotificationProjectTracker.getInstance(project);
        createContent(project, toolWindow, tracker.getEventLogConsole(), "");
        tracker.initDefaultContent();
    }

    @Nonnull
    @Override
    public ToolWindowAnchor getAnchor() {
        return ToolWindowAnchor.RIGHT;
    }

    @Override
    public boolean isSecondary() {
        return true;
    }

    @Nonnull
    @Override
    public Image getIcon() {
        return PlatformIconGroup.toolwindowsNotifications();
    }

    @Nonnull
    @Override
    public LocalizeValue getDisplayName() {
        return ProjectUIViewLocalize.toolwindowNotificationsDisplayName();
    }

    private static void createContent(Project project, ToolWindow toolWindow, EventLogConsole console, String title) {
        ContentManager contentManager = toolWindow.getContentManager();
        Editor editor = console.getConsoleEditor();

        SimpleToolWindowPanel panel = new SimpleToolWindowPanel(false, true) {
            @Override
            public Object getData(@Nonnull Key dataId) {
                return (HelpManager.HELP_ID == dataId) ? EventLog.HELP_ID : super.getData(dataId);
            }
        };
        panel.setContent(editor.getComponent());
        panel.addAncestorListener(new LogShownTracker(project));

        Content content = ContentFactory.getInstance().createContent(panel, title, false);
        contentManager.addContent(content);
        contentManager.setSelectedContent(content);

        List<AnAction> group = new ArrayList<>();
        group.add(new DisplayBalloons());
        group.add(new ToggleSoftWraps(editor));
        group.add(new ScrollToTheEndToolbarAction(editor));
        group.add(new MarkAllNotificationsAsReadAction());
        group.add(new EventLogConsole.ClearLogAction(console));
        group.add(new EditNotificationSettings(project));

        toolWindow.setTabActions(group.toArray(AnAction.EMPTY_ARRAY));
    }

    private static class DisplayBalloons extends ToggleAction implements DumbAware {
        public DisplayBalloons() {
            super(
                LocalizeValue.localizeTODO("Show balloons"),
                LocalizeValue.localizeTODO("Enable or suppress notification balloons"),
                PlatformIconGroup.generalBalloon()
            );
        }

        @Override
        public boolean isSelected(@Nonnull AnActionEvent e) {
            return NotificationsConfigurationImpl.getInstanceImpl().SHOW_BALLOONS;
        }

        @Override
        public void setSelected(@Nonnull AnActionEvent e, boolean state) {
            NotificationsConfigurationImpl.getInstanceImpl().SHOW_BALLOONS = state;
        }
    }

    private static class EditNotificationSettings extends DumbAwareAction {
        private final Project myProject;

        public EditNotificationSettings(Project project) {
            super(
                LocalizeValue.localizeTODO("Settings"),
                LocalizeValue.localizeTODO("Edit notification settings"),
                PlatformIconGroup.generalSettings()
            );
            myProject = project;
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(@Nonnull AnActionEvent e) {
            ShowSettingsUtil.getInstance().showAndSelect(myProject, NotificationsConfigurable.class);
        }
    }

    private static class ToggleSoftWraps extends ToggleUseSoftWrapsToolbarAction {
        private final Editor myEditor;

        public ToggleSoftWraps(Editor editor) {
            super(SoftWrapAppliancePlaces.CONSOLE);
            myEditor = editor;
        }

        @Override
        protected Editor getEditor(AnActionEvent e) {
            return myEditor;
        }
    }

    private static class LogShownTracker extends AncestorListenerAdapter {
        private final Project myProject;

        public LogShownTracker(Project project) {
            myProject = project;
        }

        @Override
        public void ancestorAdded(AncestorEvent event) {
            ToolWindow log = EventLog.getEventLog(myProject);
            if (log != null && log.isVisible()) {
                EventLog.getLogModel(myProject).logShown();
            }
        }
    }
}
