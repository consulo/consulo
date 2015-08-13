/*
 * Copyright 2013-2014 must-be.org
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
package org.mustbe.consulo.vfs.backgroundTask.ui;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.ui.popup.ListPopupStep;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.*;
import com.intellij.util.Function;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mustbe.consulo.vfs.backgroundTask.*;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 30.04.14
 */
public class BackgroundTaskByVfsChangeManageDialog extends DialogWrapper {
  private final CheckBoxList<BackgroundTaskByVfsChangeTask> myBoxlist;
  private final Project myProject;
  private final VirtualFile myVirtualFile;
  private JPanel myPanel = new JPanel(new BorderLayout());
  private BackgroundTaskByVfsChangePanel myVfsChangePanel;

  private BackgroundTaskByVfsChangeTask myPrevTask;

  public BackgroundTaskByVfsChangeManageDialog(@NotNull final Project project, final VirtualFile virtualFile) {
    super(project);
    myProject = project;
    myVirtualFile = virtualFile;


    myVfsChangePanel = new BackgroundTaskByVfsChangePanel(project);
    myVfsChangePanel.reset(BackgroundTaskByVfsParametersImpl.EMPTY);

    myBoxlist = new CheckBoxList<BackgroundTaskByVfsChangeTask>();
    myBoxlist.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    myBoxlist.setCheckBoxListListener(new CheckBoxListListener() {
      @Override
      public void checkBoxSelectionChanged(int index, boolean value) {
        BackgroundTaskByVfsChangeTask task = (BackgroundTaskByVfsChangeTask)myBoxlist.getItemAt(index);
        if (task == null) {
          return;
        }
        task.setEnabled(value);
      }
    });
    myBoxlist.addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(ListSelectionEvent e) {
        if (myPrevTask != null) {
          myVfsChangePanel.applyTo(myPrevTask.getParameters());
          myPrevTask = null;
        }

        if (myBoxlist.getItemsCount() == 0 || myBoxlist.getSelectedIndex() == -1) {
          myVfsChangePanel.reset(BackgroundTaskByVfsParametersImpl.EMPTY);
          return;
        }
        BackgroundTaskByVfsChangeTask task = (BackgroundTaskByVfsChangeTask)myBoxlist.getItemAt(myBoxlist.getSelectedIndex());
        if (task == null) {
          myVfsChangePanel.reset(BackgroundTaskByVfsParametersImpl.EMPTY);
          return;
        }
        myVfsChangePanel.reset(task.getParameters());
        myPrevTask = task;
      }
    });

    final List<BackgroundTaskByVfsChangeProvider> providers = BackgroundTaskByVfsChangeProviders
            .getProviders(project, virtualFile);

    ToolbarDecorator decorator = ToolbarDecorator.createDecorator(myBoxlist);
    decorator = decorator.setAddActionUpdater(new AnActionButtonUpdater() {
      @Override
      public boolean isEnabled(AnActionEvent e) {
        return !providers.isEmpty();
      }
    });
    decorator = decorator.setAddAction(new AnActionButtonRunnable() {
      @Override
      public void run(AnActionButton anActionButton) {

        if (providers.size() > 1) {
          ListPopupStep<BackgroundTaskByVfsChangeProvider> listPopupStep = new BaseListPopupStep<BackgroundTaskByVfsChangeProvider>("Add", providers) {
            @NotNull
            @Override
            public String getTextFor(BackgroundTaskByVfsChangeProvider value) {
              return value.getTemplateName();
            }

            @Override
            public PopupStep onChosen(final BackgroundTaskByVfsChangeProvider val, boolean finalChoice) {
              add(val);
              return FINAL_CHOICE;
            }
          };
          ListPopup listPopup = JBPopupFactory.getInstance().createListPopup(listPopupStep);
          listPopup.show(anActionButton.getPreferredPopupPoint());
        }
        else {
          add(providers.get(0));
        }
      }
    });

    List<BackgroundTaskByVfsChangeTask> originalTasks = BackgroundTaskByVfsChangeManager.getInstance(project).findTasks(virtualFile);

    List<BackgroundTaskByVfsChangeTask> cloneTasks = new ArrayList<BackgroundTaskByVfsChangeTask>(originalTasks.size());
    for (BackgroundTaskByVfsChangeTask task : originalTasks) {
      cloneTasks.add(task.clone());
    }

    set(cloneTasks);

    myPanel.add(decorator.createPanel(), BorderLayout.NORTH);
    myPanel.add(myVfsChangePanel, BorderLayout.SOUTH);
    setTitle("Manage Background Tasks");
    init();
    pack();
  }

  private void set(List<BackgroundTaskByVfsChangeTask> cloneTasks) {
    myBoxlist.setItems(cloneTasks, new Function<BackgroundTaskByVfsChangeTask, String>() {
                         @Override
                         public String fun(BackgroundTaskByVfsChangeTask backgroundTaskByVfsChangeTask) {
                           return backgroundTaskByVfsChangeTask.getName();
                         }
                       }, new Function<BackgroundTaskByVfsChangeTask, Boolean>() {
                         @Override
                         public Boolean fun(BackgroundTaskByVfsChangeTask backgroundTaskByVfsChangeTask) {
                           return backgroundTaskByVfsChangeTask.isEnabled();
                         }
                       }
    );
  }

  private void add(@NotNull BackgroundTaskByVfsChangeProvider provider) {
    String name = Messages.showInputDialog(myProject, "Name", "Enter Name", UIUtil.getInformationIcon(), provider.getTemplateName(), null);
    if (name == null) {
      return;
    }

    List<BackgroundTaskByVfsChangeTask> tasks = getTasks();

    BackgroundTaskByVfsParametersImpl parameters = new BackgroundTaskByVfsParametersImpl(myProject);
    provider.setDefaultParameters(myProject, myVirtualFile, parameters);

    BackgroundTaskByVfsChangeManagerImpl manager = (BackgroundTaskByVfsChangeManagerImpl)BackgroundTaskByVfsChangeManagerImpl.getInstance(myProject);
    BackgroundTaskByVfsChangeTaskImpl e = new BackgroundTaskByVfsChangeTaskImpl(myProject, myVirtualFile, manager, provider, name, parameters);
    e.setEnabled(true);
    tasks.add(e);

    set(tasks);
  }

  @NotNull
  private List<BackgroundTaskByVfsChangeTask> getTasks() {
    List<BackgroundTaskByVfsChangeTask> list = new ArrayList<BackgroundTaskByVfsChangeTask>();
    for (int i = 0; i < myBoxlist.getItemsCount(); i++) {
      BackgroundTaskByVfsChangeTask task = (BackgroundTaskByVfsChangeTask)myBoxlist.getItemAt(i);
      list.add(task);
    }
    return list;
  }

  @Override
  protected void doOKAction() {
    if (myPrevTask != null) {
      myVfsChangePanel.applyTo(myPrevTask.getParameters());
    }

    BackgroundTaskByVfsChangeManager vfsChangeManager = BackgroundTaskByVfsChangeManager.getInstance(myProject);

    List<BackgroundTaskByVfsChangeTask> originalTasks = vfsChangeManager.findTasks(myVirtualFile);
    for (BackgroundTaskByVfsChangeTask originalTask : originalTasks) {
      vfsChangeManager.cancelTask(originalTask);
    }

    List<BackgroundTaskByVfsChangeTask> tasks = getTasks();
    for (BackgroundTaskByVfsChangeTask task : tasks) {
      vfsChangeManager.registerTask(task);
    }
    super.doOKAction();
  }

  @Nullable
  @Override
  protected String getDimensionServiceKey() {
    setSize(600, 200);
    return "#BackgroundTaskByVfsChangeManageDialog";
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    return myPanel;
  }
}
