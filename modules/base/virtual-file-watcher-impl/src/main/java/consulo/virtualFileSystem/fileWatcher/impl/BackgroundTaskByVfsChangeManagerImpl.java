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
package consulo.virtualFileSystem.fileWatcher.impl;

import consulo.annotation.component.ServiceImpl;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.Task;
import consulo.build.ui.BuildViewManager;
import consulo.build.ui.DefaultBuildDescriptor;
import consulo.build.ui.progress.BuildProgress;
import consulo.build.ui.progress.BuildProgressDescriptor;
import consulo.component.macro.ExpandMacroToPathMap;
import consulo.component.macro.ReplacePathToMacroMap;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.disposer.Disposable;
import consulo.execution.ui.console.UrlFilter;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.util.concurrent.AsyncResult;
import consulo.util.dataholder.Key;
import consulo.util.xml.serializer.XmlSerializer;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileWatcher.BackgroundTaskByVfsChangeManager;
import consulo.virtualFileSystem.fileWatcher.BackgroundTaskByVfsChangeProvider;
import consulo.virtualFileSystem.fileWatcher.BackgroundTaskByVfsChangeTask;
import consulo.virtualFileSystem.fileWatcher.impl.ui.BackgroundTaskByVfsChangeManageDialog;
import consulo.virtualFileSystem.pointer.VirtualFilePointer;
import consulo.virtualFileSystem.pointer.VirtualFilePointerManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 22:50/06.10.13
 */
@Singleton
@State(name = "BackgroundTaskByVfsChangeManager", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
@ServiceImpl
public class BackgroundTaskByVfsChangeManagerImpl extends BackgroundTaskByVfsChangeManager implements PersistentStateComponent<Element>, Disposable {
  private static final Key<Boolean> PROCESSING_BACKGROUND_TASK = Key.create("processing.background.task");

  @Nonnull
  private final Project myProject;

  private final List<BackgroundTaskByVfsChangeTaskImpl> myTasks = new ArrayList<>();

  @Inject
  public BackgroundTaskByVfsChangeManagerImpl(@Nonnull Project project) {
    myProject = project;
  }

  @Nonnull
  @Override
  public BackgroundTaskByVfsChangeTask createTask(@Nonnull BackgroundTaskByVfsChangeProvider provider, @Nonnull VirtualFile virtualFile, @Nonnull String name) {
    BackgroundTaskByVfsParametersImpl parameters = new BackgroundTaskByVfsParametersImpl(myProject);
    parameters.setPassParentEnvs(true);

    provider.setDefaultParameters(myProject, virtualFile, parameters);
    BackgroundTaskByVfsChangeTaskImpl task = new BackgroundTaskByVfsChangeTaskImpl(myProject, virtualFile, this, provider, name, parameters);
    task.setEnabled(true);
    return task;
  }

  @RequiredUIAccess
  @Override
  public void openManageDialog(@Nonnull VirtualFile virtualFile) {
    new BackgroundTaskByVfsChangeManageDialog(myProject, virtualFile).showAsync();
  }

  @Nonnull
  @Override
  public List<BackgroundTaskByVfsChangeTask> findTasks(@Nonnull VirtualFile virtualFile) {
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

  @Nonnull
  @Override
  public List<BackgroundTaskByVfsChangeTask> findEnabledTasks(@Nonnull VirtualFile virtualFile) {
    if (myTasks.isEmpty()) {
      return List.of();
    }

    List<BackgroundTaskByVfsChangeTask> list = new ArrayList<>();
    for (BackgroundTaskByVfsChangeTaskImpl task : myTasks) {
      if (!task.isEnabled()) {
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

  @Nonnull
  @Override
  public BackgroundTaskByVfsChangeTask[] getTasks() {
    return myTasks.toArray(new BackgroundTaskByVfsChangeTask[myTasks.size()]);
  }

  @Nonnull
  public List<BackgroundTaskByVfsChangeTaskImpl> getTasksImpl() {
    return myTasks;
  }

  @Override
  public boolean removeTask(@Nonnull BackgroundTaskByVfsChangeTask task) {
    assert task instanceof BackgroundTaskByVfsChangeTaskImpl;
    return myTasks.remove(task);
  }

  @Override
  public void registerTask(@Nonnull BackgroundTaskByVfsChangeTask task) {
    BackgroundTaskByVfsChangeTaskImpl implTask = (BackgroundTaskByVfsChangeTaskImpl)task;
    myTasks.add(implTask);
  }

  @Override
  public void runTasks(@Nonnull final VirtualFile virtualFile) {
    Boolean processed = virtualFile.getUserData(PROCESSING_BACKGROUND_TASK);
    if (processed == Boolean.TRUE) {
      return;
    }

    final List<BackgroundTaskByVfsChangeTask> tasks = findEnabledTasks(virtualFile);
    if (tasks.isEmpty()) {
      return;
    }

    BuildProgress<BuildProgressDescriptor> buildProgress = BuildViewManager.getInstance(myProject).createBuildProgress();

    UUID uuid = UUID.randomUUID();
    DefaultBuildDescriptor buildDescriptor = new DefaultBuildDescriptor(uuid, virtualFile.getName(), myProject.getBasePath(), System.currentTimeMillis());
    buildDescriptor.withExecutionFilter(new UrlFilter(myProject));
    buildDescriptor.withRestartAction(new DumbAwareAction("Restart", null, PlatformIconGroup.actionsExecute()) {
      @RequiredUIAccess
      @Override
      public void actionPerformed(@Nonnull AnActionEvent e) {
        BackgroundTaskByVfsChangeManager.getInstance(myProject).runTasks(virtualFile);
      }
    });
    buildDescriptor.setActivateToolWindowWhenFailed(true);

    for (BackgroundTaskByVfsChangeTask task : tasks) {
      if (task.getParameters().isShowConsole()) {
        buildDescriptor.setActivateToolWindowWhenAdded(true);
        break;
      }
    }

    buildProgress.start(BuildProgressDescriptor.of(buildDescriptor));

    Task.Backgroundable backgroundTask = new Task.Backgroundable(myProject, "Processing: " + virtualFile.getName()) {
      @Override
      public void run(@Nonnull ProgressIndicator indicator) {
        virtualFile.putUserData(PROCESSING_BACKGROUND_TASK, Boolean.TRUE);
        call(indicator, tasks, buildProgress, 0, (result) -> {
          virtualFile.putUserData(PROCESSING_BACKGROUND_TASK, null);

          if (result) {
            buildProgress.finish(System.currentTimeMillis());
          }
          else {
            buildProgress.fail();
          }
        });
      }
    };
    backgroundTask.queue();
  }

  public void call(ProgressIndicator indicator, List<BackgroundTaskByVfsChangeTask> tasks, BuildProgress<BuildProgressDescriptor> buildProgress, int index, Consumer<Boolean> onFinish) {
    if (index == tasks.size()) {
      onFinish.accept(Boolean.TRUE);
      return;
    }

    BackgroundTaskByVfsChangeTaskImpl task = (BackgroundTaskByVfsChangeTaskImpl)tasks.get(index);

    AsyncResult<Void> callback = AsyncResult.undefined();
    // if ok - go next
    callback.doWhenDone(() -> call(indicator, tasks, buildProgress, index + 1, onFinish));
    // if failed - stop
    callback.doWhenRejected(() -> onFinish.accept(Boolean.FALSE));

    indicator.setText2("Task: " + task.getName());
    task.run(callback, buildProgress);
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
    myTasks.clear();
  }
}
