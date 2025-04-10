/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.externalSystem.ui.awt;

import consulo.dataContext.DataProvider;
import consulo.execution.action.Location;
import consulo.execution.executor.DefaultRunExecutor;
import consulo.externalSystem.ExternalSystemBundle;
import consulo.externalSystem.ExternalSystemManager;
import consulo.externalSystem.internal.ui.ExternalSystemRecentTaskListModel;
import consulo.externalSystem.internal.ui.ExternalSystemRecentTasksList;
import consulo.externalSystem.internal.ui.ExternalSystemTasksTree;
import consulo.externalSystem.model.ExternalSystemDataKeys;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.model.execution.ExternalTaskExecutionInfo;
import consulo.externalSystem.model.project.ExternalProjectPojo;
import consulo.externalSystem.setting.AbstractExternalSystemLocalSettings;
import consulo.externalSystem.task.ExternalSystemTaskLocation;
import consulo.externalSystem.util.ExternalSystemApiUtil;
import consulo.language.plain.PlainTextFileType;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiFileFactory;
import consulo.project.Project;
import consulo.project.ui.notification.NotificationGroup;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.awt.*;
import consulo.util.dataholder.Key;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Supplier;

import static consulo.externalSystem.util.ExternalSystemConstants.*;

/**
 * @author Denis Zhdanov
 * @since 5/12/13 10:18 PM
 */
public class ExternalSystemTasksPanel extends SimpleToolWindowPanel implements DataProvider {

  @Nonnull
  private final ExternalSystemRecentTasksList myRecentTasksList;
  @Nonnull
  private final ExternalSystemTasksTreeModel myAllTasksModel;
  @Nonnull
  private final ExternalSystemTasksTree myAllTasksTree;
  @Nonnull
  private final ProjectSystemId               myExternalSystemId;
  @Nonnull
  private final NotificationGroup             myNotificationGroup;
  @Nonnull
  private final Project                       myProject;

  @Nullable
  private Supplier<ExternalTaskExecutionInfo> mySelectedTaskProvider;

  public ExternalSystemTasksPanel(@Nonnull Project project,
                                  @Nonnull ProjectSystemId externalSystemId,
                                  @Nonnull NotificationGroup notificationGroup)
  {
    super(true);
    myExternalSystemId = externalSystemId;
    myNotificationGroup = notificationGroup;
    myProject = project;

    ExternalSystemManager<?, ?, ?, ?, ?> manager = ExternalSystemApiUtil.getManager(externalSystemId);
    assert manager != null;
    AbstractExternalSystemLocalSettings settings = manager.getLocalSettingsProvider().apply(project);

    ExternalSystemRecentTaskListModel recentTasksModel = new ExternalSystemRecentTaskListModel(externalSystemId, project);
    recentTasksModel.setTasks(settings.getRecentTasks());
    myRecentTasksList = new ExternalSystemRecentTasksList(recentTasksModel, externalSystemId, project) {
      @Override
      protected void processMouseEvent(MouseEvent e) {
        if (e.getClickCount() > 0) {
          mySelectedTaskProvider = myRecentTasksList;
          myAllTasksTree.getSelectionModel().clearSelection();
        }
        super.processMouseEvent(e);
      }
    };

    myAllTasksModel = new ExternalSystemTasksTreeModel(externalSystemId);
    myAllTasksTree = new ExternalSystemTasksTree(myAllTasksModel, settings.getExpandStates(), project, externalSystemId) {
      @Override
      protected void processMouseEvent(MouseEvent e) {
        if (e.getClickCount() > 0) {
          mySelectedTaskProvider = myAllTasksTree;
          myRecentTasksList.getSelectionModel().clearSelection();
        }
        super.processMouseEvent(e);
      }
    };
    final String actionIdToUseForDoubleClick = DefaultRunExecutor.getRunExecutorInstance().getContextActionId();
    myAllTasksTree.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() >= 2 && !e.isPopupTrigger()) {
          ExternalSystemUiUtil.executeAction(actionIdToUseForDoubleClick, e);
        }
      }
    });
    ExternalSystemUiUtil.apply(settings, myAllTasksModel);
    PopupHandler.installPopupHandlerFromCustomActions(myAllTasksTree, TREE_ACTIONS_GROUP_ID, TREE_CONTEXT_MENU_PLACE);

    ActionManager actionManager = ActionManager.getInstance();
    ActionGroup group = (ActionGroup)actionManager.getAction(TOOL_WINDOW_TOOLBAR_ACTIONS_GROUP_ID);
    ActionToolbar toolbar = actionManager.createActionToolbar(TOOL_WINDOW_PLACE, group, true);
    toolbar.setTargetComponent(this);
    setToolbar(toolbar.getComponent());

    JPanel content = new JPanel(new GridBagLayout());
    content.setOpaque(true);
    content.setBackground(UIUtil.getListBackground());
    JComponent recentTasksWithTitle = wrap(myRecentTasksList, ExternalSystemBundle.message("tasks.recent.title"));
    content.add(recentTasksWithTitle, ExternalSystemUiUtil.getFillLineConstraints(0));
    JBScrollPane scrollPane = new JBScrollPane(myAllTasksTree);
    scrollPane.setBorder(null);
    JComponent allTasksWithTitle = wrap(scrollPane, ExternalSystemBundle.message("tasks.all.title"));
    content.add(allTasksWithTitle, ExternalSystemUiUtil.getFillLineConstraints(0).weighty(1).fillCell());
    setContent(content);
  }

  private static JComponent wrap(@Nonnull JComponent content, @Nonnull String title) {
    JPanel result = new JPanel(new BorderLayout());
    result.setOpaque(false);
    result.setBorder(IdeBorderFactory.createTitledBorder(title, false));
    result.add(content, BorderLayout.CENTER);
    return result;
  }

  @Nullable
  @Override
  public Object getData(@Nonnull @NonNls Key<?> dataId) {
    if (ExternalSystemDataKeys.RECENT_TASKS_LIST == dataId) {
      return myRecentTasksList;
    }
    else if (ExternalSystemDataKeys.ALL_TASKS_MODEL == dataId) {
      return myAllTasksModel;
    }
    else if (ExternalSystemDataKeys.EXTERNAL_SYSTEM_ID == dataId) {
      return myExternalSystemId;
    }
    else if (ExternalSystemDataKeys.NOTIFICATION_GROUP == dataId) {
      return myNotificationGroup;
    }
    else if (ExternalSystemDataKeys.SELECTED_TASK == dataId) {
      return mySelectedTaskProvider == null ? null : mySelectedTaskProvider.get();
    }
    else if (ExternalSystemDataKeys.SELECTED_PROJECT == dataId) {
      if (mySelectedTaskProvider != myAllTasksTree) {
        return null;
      }
      else {
        Object component = myAllTasksTree.getLastSelectedPathComponent();
        if (component instanceof ExternalSystemNode) {
          Object element = ((ExternalSystemNode)component).getDescriptor().getElement();
          return element instanceof ExternalProjectPojo ? element : null;
        }
      }
    }
    else if (Location.DATA_KEY == dataId) {
      Location location = buildLocation();
      return location == null ? super.getData(dataId) : location;
    }
    return null;
  }

  @Nullable
  private Location buildLocation() {
    if (mySelectedTaskProvider == null) {
      return null;
    }
    ExternalTaskExecutionInfo task = mySelectedTaskProvider.get();
    if (task == null) {
      return null;
    }

    String projectPath = task.getSettings().getExternalProjectPath();
    String name = myExternalSystemId.getReadableName() + projectPath + StringUtil.join(task.getSettings().getTaskNames(), " ");
    // We create a dummy text file instead of re-using external system file in order to avoid clashing with other configuration producers.
    // For example gradle files are enhanced groovy scripts but we don't want to run them via regular IJ groovy script runners.
    // Gradle tooling api should be used for running gradle tasks instead. IJ execution sub-system operates on Location objects
    // which encapsulate PsiElement and groovy runners are automatically applied if that PsiElement IS-A GroovyFile.
    PsiFile file = PsiFileFactory.getInstance(myProject).createFileFromText(name, PlainTextFileType.INSTANCE, "nichts");

    return new ExternalSystemTaskLocation(myProject, file, task);
  }
}
