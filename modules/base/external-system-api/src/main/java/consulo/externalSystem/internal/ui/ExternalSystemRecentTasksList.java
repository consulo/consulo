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
package consulo.externalSystem.internal.ui;

import consulo.execution.RunManager;
import consulo.execution.configuration.ConfigurationType;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.executor.Executor;
import consulo.execution.executor.ExecutorRegistry;
import consulo.externalSystem.ExternalSystemManager;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.model.execution.ExternalTaskExecutionInfo;
import consulo.externalSystem.service.execution.AbstractExternalSystemTaskConfigurationType;
import consulo.externalSystem.service.execution.ExternalSystemRunConfiguration;
import consulo.externalSystem.internal.DefaultExternalSystemUiAware;
import consulo.externalSystem.ui.ExternalSystemUiAware;
import consulo.externalSystem.util.ExternalSystemApiUtil;
import consulo.externalSystem.util.ExternalSystemConstants;
import consulo.project.Project;
import consulo.ui.ex.awt.JBList;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import consulo.util.lang.StringUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.function.Supplier;

/**
 * @author Denis Zhdanov
 * @since 6/7/13 2:40 PM
 */
public class ExternalSystemRecentTasksList extends JBList implements Supplier<ExternalTaskExecutionInfo> {

  @Nonnull
  private static final JLabel EMPTY_RENDERER = new JLabel(" ");
  
  public ExternalSystemRecentTasksList(@Nonnull ExternalSystemRecentTaskListModel model,
                                       @Nonnull final ProjectSystemId externalSystemId,
                                       @Nonnull final Project project)
  {
    super(model);
    setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    ExternalSystemManager<?, ?, ?, ?, ?> manager = ExternalSystemApiUtil.getManager(externalSystemId);
    consulo.ui.image.Image icon = null;
    if (manager instanceof ExternalSystemUiAware externalSystemUiAware) {
      icon = externalSystemUiAware.getTaskIcon();
    }
    if (icon == null) {
      icon = DefaultExternalSystemUiAware.INSTANCE.getTaskIcon();
    }
    setCellRenderer(new MyRenderer(project, icon, ExternalSystemApiUtil.findConfigurationType(externalSystemId)));
    setVisibleRowCount(ExternalSystemConstants.RECENT_TASKS_NUMBER);

    registerKeyboardAction(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        ExternalTaskExecutionInfo task = get();
        if (task == null) {
          return;
        }
        ExternalSystemApiUtil.runTask(task.getSettings(), task.getExecutorId(), project, externalSystemId);
      }
    }, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

    addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() < 2) {
          return;
        }

        ExternalTaskExecutionInfo task = get();
        if (task == null) {
          return;
        }

        ExternalSystemApiUtil.runTask(task.getSettings(), task.getExecutorId(), project, externalSystemId);
      }
    });
  }

  @Override
  public ExternalSystemRecentTaskListModel getModel() {
    return (ExternalSystemRecentTaskListModel)super.getModel();
  }

  public void setFirst(@Nonnull ExternalTaskExecutionInfo task) {
    ExternalTaskExecutionInfo selected = get();
    ExternalSystemRecentTaskListModel model = getModel();
    model.setFirst(task);
    clearSelection();
    if (selected == null) {
      return;
    }
    for (int i = 0; i < model.size(); i++) {
      //noinspection SuspiciousMethodCalls
      if (selected.equals(model.getElementAt(i))) {
        addSelectionInterval(i, i);
        return;
      }
    }
  }

  @Nullable
  @Override
  public ExternalTaskExecutionInfo get() {
    int[] indices = getSelectedIndices();
    if (indices == null || indices.length != 1) {
      return null;
    }
    Object e = getModel().getElementAt(indices[0]);
    return e instanceof ExternalTaskExecutionInfo ? (ExternalTaskExecutionInfo)e : null;
  }

  private static class MyRenderer extends DefaultListCellRenderer {

    @Nonnull
    private final consulo.ui.image.Image myGenericTaskIcon;
    @Nonnull
    private final Project myProject;
    @Nullable
    private ConfigurationType myConfigurationType;

    MyRenderer(@Nonnull Project project, @Nonnull consulo.ui.image.Image genericTaskIcon, @Nullable ConfigurationType configurationType) {
      myProject = project;
      myGenericTaskIcon = genericTaskIcon;
      myConfigurationType = configurationType;
    }

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
      Component renderer = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      if (value instanceof ExternalSystemRecentTaskListModel.MyEmptyDescriptor) {
        return EMPTY_RENDERER;
      }
      else if (value instanceof ExternalTaskExecutionInfo taskInfo) {
        String text = null;
        if (myConfigurationType != null) {
          List<RunConfiguration> configurations = RunManager.getInstance(myProject).getConfigurationsList(myConfigurationType);
          for (RunConfiguration configuration : configurations) {
            if (!(configuration instanceof ExternalSystemRunConfiguration)) {
              continue;
            }
            ExternalSystemRunConfiguration c = (ExternalSystemRunConfiguration)configuration;
            if (c.getSettings().equals(taskInfo.getSettings())) {
              text = c.getName();
            }
          }
        }
        if (StringUtil.isEmpty(text)) {
          text = AbstractExternalSystemTaskConfigurationType.generateName(myProject, taskInfo.getSettings());
        }
        
        setText(text);
        Image icon = null;
        String executorId = taskInfo.getExecutorId();
        if (!StringUtil.isEmpty(executorId)) {
          Executor executor = ExecutorRegistry.getInstance().getExecutorById(executorId);
          if (executor != null) {
            icon = executor.getIcon();
          }
        }

        if (icon == null) {
          icon = myGenericTaskIcon;
        }
        setIcon(TargetAWT.to(icon));
      }

      return renderer;
    }

    @Override
    public void setIcon(Icon icon) {
      if (icon != null) {
        // Don't allow to reset icon.
        super.setIcon(icon);
      }
    }
  }
}
