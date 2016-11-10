/*
 * Copyright 2013-2016 consulo.io
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
package consulo.backgroundTaskByVfsChange;

import com.intellij.application.options.ReplacePathToMacroMap;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.*;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ActionCallback;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileAdapter;
import com.intellij.openapi.vfs.VirtualFileEvent;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.pointers.VirtualFilePointer;
import com.intellij.openapi.vfs.pointers.VirtualFilePointerManager;
import com.intellij.util.xmlb.XmlSerializer;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import consulo.backgroundTaskByVfsChange.ui.BackgroundTaskByVfsChangeManageDialog;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 22:50/06.10.13
 */
@State(
        name = "BackgroundTaskByVfsChangeManager",
        storages = {@Storage(file = StoragePathMacros.WORKSPACE_FILE)})
public class BackgroundTaskByVfsChangeManagerImpl extends BackgroundTaskByVfsChangeManager implements PersistentStateComponent<Element>, Disposable {
  private static final Key<Boolean> PROCESSING_BACKGROUND_TASK = Key.create("processing.background.task");

  @NotNull
  private final Project myProject;

  private List<BackgroundTaskByVfsChangeTaskImpl> myTasks = new ArrayList<>();

  private Listener myListener;

  private final VirtualFileAdapter myVirtualFileListener;

  public BackgroundTaskByVfsChangeManagerImpl(@NotNull Project project) {
    myProject = project;
    myListener = project.getMessageBus().syncPublisher(TOPIC);
    myVirtualFileListener = new VirtualFileAdapter() {
      @Override
      public void contentsChanged(@NotNull VirtualFileEvent event) {
        runTasks(event.getFile());
      }
    };
    VirtualFileManager.getInstance().addVirtualFileListener(myVirtualFileListener);
  }

  @Override
  public void openManageDialog(@NotNull VirtualFile virtualFile) {
    new BackgroundTaskByVfsChangeManageDialog(myProject, virtualFile).show();
  }

  @NotNull
  @Override
  public List<BackgroundTaskByVfsChangeTask> findTasks(@NotNull VirtualFile virtualFile) {
    List<BackgroundTaskByVfsChangeTask> list = new ArrayList<>();
    for (BackgroundTaskByVfsChangeTaskImpl task : myTasks) {
      VirtualFile file = task.getVirtualFilePointer().getFile();
      if (file == null) {
        continue;
      }
      if (file.equals(virtualFile)) {
        list.add(task);
      }
    }
    return list;
  }

  @NotNull
  @Override
  public List<BackgroundTaskByVfsChangeTask> findEnabledTasks(@NotNull VirtualFile virtualFile) {
    List<BackgroundTaskByVfsChangeTask> list = new ArrayList<>();
    for (BackgroundTaskByVfsChangeTaskImpl task : myTasks) {
      if(!task.isEnabled()) {
        continue;
      }
      VirtualFile file = task.getVirtualFilePointer().getFile();
      if (file == null) {
        continue;
      }
      if (file.equals(virtualFile)) {
        list.add(task);
      }
    }
    return list;
  }

  @NotNull
  @Override
  public BackgroundTaskByVfsChangeTask[] getTasks() {
    return myTasks.toArray(new BackgroundTaskByVfsChangeTask[myTasks.size()]);
  }

  @Override
  public boolean cancelTask(@NotNull BackgroundTaskByVfsChangeTask task) {
    assert task instanceof BackgroundTaskByVfsChangeTaskImpl;
    boolean remove = myTasks.remove(task);
    if (remove) {
      myListener.taskCanceled(task);
    }
    return remove;
  }

  @Override
  public void registerTask(@NotNull BackgroundTaskByVfsChangeTask task) {
    BackgroundTaskByVfsChangeTaskImpl implTask = (BackgroundTaskByVfsChangeTaskImpl)task;
    myTasks.add(implTask);
    myListener.taskAdded(task);
  }

  @Override
  public void runTasks(@NotNull final VirtualFile virtualFile) {
    Boolean processed = virtualFile.getUserData(PROCESSING_BACKGROUND_TASK);
    if(processed == Boolean.TRUE) {
      return;
    }

    final List<BackgroundTaskByVfsChangeTask> tasks = findEnabledTasks(virtualFile);
    if (tasks.isEmpty()) {
      return;
    }

    Task.Backgroundable backgroundTask = new Task.Backgroundable(myProject, "Processing: " + virtualFile.getName()) {
      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        virtualFile.putUserData(PROCESSING_BACKGROUND_TASK, Boolean.TRUE);
        call(indicator, virtualFile, tasks, 0);
      }
    };
    backgroundTask.queue();
  }

  public void call(final ProgressIndicator indicator, final VirtualFile file, final List<BackgroundTaskByVfsChangeTask> tasks, final int index) {
    if(index == tasks.size()) {
      file.putUserData(PROCESSING_BACKGROUND_TASK, null);
      return;
    }

    BackgroundTaskByVfsChangeTaskImpl task = (BackgroundTaskByVfsChangeTaskImpl)tasks.get(index);

    ActionCallback callback = new ActionCallback();
    callback.doWhenProcessed(() -> call(indicator, file, tasks, index + 1));
    indicator.setText2("Task: " + task.getName());
    task.run(callback);
  }

  @Nullable
  @Override
  public Element getState() {
    Element element = new Element("state");
    for (BackgroundTaskByVfsChangeTaskImpl task : myTasks) {
      Element taskElement = new Element("task");
      element.addContent(taskElement);

      taskElement.setAttribute("url", task.getVirtualFilePointer().getUrl());
      taskElement.setAttribute("provider-name", task.getProviderName());
      taskElement.setAttribute("name", task.getName());
      taskElement.setAttribute("enabled", String.valueOf(task.isEnabled()));

      Element serialize = XmlSerializer.serialize(task.getParameters());

      taskElement.addContent(serialize);

      ExpandMacroToPathMap expandMacroToPathMap = task.createExpandMacroToPathMap();

      expandMacroToPathMap.substitute(serialize, false, true);
    }
    return element;
  }

  @Override
  public void loadState(Element state) {
    for (Element element : state.getChildren("task")) {
      String url = element.getAttributeValue("url");
      String name = element.getAttributeValue("name");
      String providerName = element.getAttributeValue("provider-name");
      boolean enabled = Boolean.valueOf(element.getAttributeValue("enabled"));

      VirtualFilePointer virtualFilePointer = VirtualFilePointerManager.getInstance().create(url, this, null);

      BackgroundTaskByVfsParametersImpl parameters = new BackgroundTaskByVfsParametersImpl(myProject);

      BackgroundTaskByVfsChangeTaskImpl task = new BackgroundTaskByVfsChangeTaskImpl(myProject, virtualFilePointer, providerName, name, parameters, this);
      task.setEnabled(enabled);

      Element parametersElement = element.getChild("parameters");
      if (parametersElement != null) {
        ReplacePathToMacroMap replaceMacroToPathMap = task.createReplaceMacroToPathMap();

        replaceMacroToPathMap.substitute(parametersElement, false, true);

        XmlSerializer.deserializeInto(parameters, parametersElement);
      }

      registerTask(task);
    }
  }

  @Override
  public void dispose() {
    VirtualFileManager.getInstance().removeVirtualFileListener(myVirtualFileListener);
    myTasks.clear();
  }

  public void taskChanged(BackgroundTaskByVfsChangeTaskImpl backgroundTaskByVfsChangeTask) {
    myListener.taskChanged(backgroundTaskByVfsChangeTask);
  }
}
