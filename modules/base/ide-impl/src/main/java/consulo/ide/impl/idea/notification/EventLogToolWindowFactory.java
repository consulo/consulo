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
package consulo.ide.impl.idea.notification;

import consulo.application.AllIcons;
import consulo.language.editor.PlatformDataKeys;
import consulo.ui.ex.action.ContextHelpAction;
import consulo.ide.impl.idea.notification.impl.NotificationsConfigurable;
import consulo.ide.impl.idea.notification.impl.NotificationsConfigurationImpl;
import consulo.codeEditor.Editor;
import consulo.ide.impl.idea.openapi.editor.actions.ScrollToTheEndToolbarAction;
import consulo.ide.impl.idea.openapi.editor.actions.ToggleUseSoftWrapsToolbarAction;
import consulo.codeEditor.impl.SoftWrapAppliancePlaces;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.application.dumb.DumbAware;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.project.Project;
import consulo.ide.impl.idea.openapi.ui.SimpleToolWindowPanel;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.util.dataholder.Key;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.project.ui.wm.ToolWindowFactory;
import consulo.ide.impl.idea.ui.AncestorListenerAdapter;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentFactory;
import consulo.ui.ex.content.ContentManager;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;

import javax.swing.event.AncestorEvent;

/**
 * @author peter
 */
public class EventLogToolWindowFactory implements ToolWindowFactory, DumbAware {
  @RequiredUIAccess
  @Override
  public void createToolWindowContent(@Nonnull final Project project, @Nonnull ToolWindow toolWindow) {
    EventLog.getProjectComponent(project).initDefaultContent();
  }

  static void createContent(Project project, ToolWindow toolWindow, EventLogConsole console, String title) {
    // update default Event Log tab title
    ContentManager contentManager = toolWindow.getContentManager();
    Content generalContent = contentManager.getContent(0);
    if (generalContent != null && contentManager.getContentCount() == 1) {
      generalContent.setDisplayName("General");
    }

    final Editor editor = console.getConsoleEditor();

    SimpleToolWindowPanel panel = new SimpleToolWindowPanel(false, true) {
      @Override
      public Object getData(@Nonnull @NonNls Key dataId) {
        return PlatformDataKeys.HELP_ID == dataId ? EventLog.HELP_ID : super.getData(dataId);
      }
    };
    panel.setContent(editor.getComponent());
    panel.addAncestorListener(new LogShownTracker(project));

    ActionToolbar toolbar = createToolbar(project, editor, console);
    toolbar.setTargetComponent(editor.getContentComponent());
    panel.setToolbar(toolbar.getComponent());

    Content content = ContentFactory.getInstance().createContent(panel, title, false);
    contentManager.addContent(content);
    contentManager.setSelectedContent(content);
  }

  private static ActionToolbar createToolbar(Project project, Editor editor, EventLogConsole console) {
    DefaultActionGroup group = new DefaultActionGroup();
    group.add(new EditNotificationSettings(project));
    group.add(new DisplayBalloons());
    group.add(new ToggleSoftWraps(editor));
    group.add(new ScrollToTheEndToolbarAction(editor));
    group.add(ActionManager.getInstance().getAction(IdeActions.ACTION_MARK_ALL_NOTIFICATIONS_AS_READ));
    group.add(new EventLogConsole.ClearLogAction(console));
    group.add(new ContextHelpAction(EventLog.HELP_ID));

    return ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, group, false);
  }

  private static class DisplayBalloons extends ToggleAction implements DumbAware {
    public DisplayBalloons() {
      super("Show balloons", "Enable or suppress notification balloons", AllIcons.General.Balloon);
    }

    @Override
    public boolean isSelected(AnActionEvent e) {
      return NotificationsConfigurationImpl.getInstanceImpl().SHOW_BALLOONS;
    }

    @Override
    public void setSelected(AnActionEvent e, boolean state) {
      NotificationsConfigurationImpl.getInstanceImpl().SHOW_BALLOONS = state;
    }
  }

  private static class EditNotificationSettings extends DumbAwareAction {
    private final Project myProject;

    public EditNotificationSettings(Project project) {
      super("Settings", "Edit notification settings", AllIcons.General.Settings);
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
