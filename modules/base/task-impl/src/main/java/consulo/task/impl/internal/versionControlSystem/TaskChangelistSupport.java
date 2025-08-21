/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package consulo.task.impl.internal.versionControlSystem;

import consulo.annotation.component.ExtensionImpl;
import consulo.document.Document;
import consulo.task.impl.internal.action.TaskAutoCompletionListProvider;
import consulo.language.editor.ui.awt.EditorTextField;
import consulo.language.editor.ui.awt.TextFieldWithAutoCompletionContributor;
import consulo.project.Project;
import consulo.task.ChangeListInfo;
import consulo.task.LocalTask;
import consulo.task.TaskManager;
import consulo.versionControlSystem.change.EditChangelistSupport;
import consulo.versionControlSystem.change.LocalChangeList;
import jakarta.inject.Inject;

import javax.swing.*;
import java.util.function.Consumer;

/**
 * @author Dmitry Avdeev
 */
@ExtensionImpl
public class TaskChangelistSupport implements EditChangelistSupport {
  private final Project myProject;
  private final TaskManager myTaskManager;

  @Inject
  public TaskChangelistSupport(Project project, TaskManager taskManager) {
    myProject = project;
    myTaskManager = taskManager;
  }

  @Override
  public void installSearch(EditorTextField name, EditorTextField comment) {
    Document document = name.getDocument();
    TaskAutoCompletionListProvider completionProvider = new TaskAutoCompletionListProvider(myProject);

    TextFieldWithAutoCompletionContributor.installCompletion(document, myProject, completionProvider, false);
  }

  @Override
  public Consumer<LocalChangeList> addControls(JPanel bottomPanel, LocalChangeList initial) {
    JCheckBox checkBox = new JCheckBox("Track context");
    checkBox.setMnemonic('t');
    checkBox.setToolTipText("Reload context (e.g. open editors) when changelist is set active");
    checkBox.setSelected(initial == null ? myTaskManager.isTrackContextForNewChangelist() : myTaskManager.getAssociatedTask(initial) != null);
    bottomPanel.add(checkBox);
    return changeList -> {
      if (initial == null) {
        myTaskManager.setTrackContextForNewChangelist(checkBox.isSelected());
        if (checkBox.isSelected()) {
          myTaskManager.trackContext(changeList);
        }
        else {
          myTaskManager.getActiveTask().addChangelist(new ChangeListInfo(changeList));
        }
      }
      else {
        LocalTask associatedTask = myTaskManager.getAssociatedTask(changeList);
        if (checkBox.isSelected()) {
          if (associatedTask == null) {
            myTaskManager.trackContext(changeList);
          }
        }
        else {
          if (associatedTask != null) {
            myTaskManager.removeTask(associatedTask);
          }
        }
      }
    };
  }

  @Override
  public void changelistCreated(LocalChangeList changeList) {
  }
}
