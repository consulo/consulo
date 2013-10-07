/*
 * Copyright 2013 Consulo.org
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
package org.consulo.vfs.backgroundTask;

import com.intellij.application.options.ReplacePathToMacroMap;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.pointers.VirtualFilePointer;
import com.intellij.openapi.vfs.pointers.VirtualFilePointerManager;
import com.intellij.util.xmlb.XmlSerializer;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 22:50/06.10.13
 */
@State(
  name = "BackgroundTaskByVfsChangeManager",
  storages = {@Storage(file = StoragePathMacros.WORKSPACE_FILE)})
public class BackgroundTaskByVfsChangeManagerImpl extends BackgroundTaskByVfsChangeManager
  implements PersistentStateComponent<Element>, Disposable {
  @NotNull
  private final Project myProject;

  private List<BackgroundTaskByVfsChangeTaskImpl> myTasks = new ArrayList<BackgroundTaskByVfsChangeTaskImpl>();

  public BackgroundTaskByVfsChangeManagerImpl(@NotNull Project project) {
    myProject = project;
  }

  @Override
  public BackgroundTaskByVfsChangeTask getTask(@NotNull VirtualFile virtualFile) {
    for (BackgroundTaskByVfsChangeTaskImpl task : myTasks) {
      VirtualFile file = task.getVirtualFilePointer().getFile();
      if (file == null) {
        continue;
      }
      if (file.equals(virtualFile)) {
        return task;
      }
    }
    return null;
  }

  @Override
  public boolean cancelTask(@NotNull BackgroundTaskByVfsChangeTask task) {
    boolean remove = myTasks.remove(task);
    if (remove) {
      task.dispose();
    }
    return remove;
  }

  @NotNull
  @Override
  public BackgroundTaskByVfsChangeTask registerTask(@NotNull VirtualFile virtualFile,
                                                    @NotNull BackgroundTaskByVfsChangeProvider changeProvider,
                                                    @NotNull BackgroundTaskByVfsParameters parameters) {
    BackgroundTaskByVfsChangeTaskImpl task =
      new BackgroundTaskByVfsChangeTaskImpl(myProject, virtualFile, this, changeProvider.getName(), parameters);

    myTasks.add(task);
    return task;
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
      String providerName = element.getAttributeValue("provider-name");

      VirtualFilePointer virtualFilePointer = VirtualFilePointerManager.getInstance().create(url, this, null);

      BackgroundTaskByVfsParametersImpl parameters = new BackgroundTaskByVfsParametersImpl(myProject);

      BackgroundTaskByVfsChangeTaskImpl task =
        new BackgroundTaskByVfsChangeTaskImpl(myProject, virtualFilePointer, providerName, parameters);

      Element parametersElement = element.getChild("parameters");
      if(parametersElement != null) {
        ReplacePathToMacroMap replaceMacroToPathMap = task.createReplaceMacroToPathMap();

        replaceMacroToPathMap.substitute(parametersElement, false, true);

        XmlSerializer.deserializeInto(parameters, parametersElement);
      }

      myTasks.add(task);
    }
  }

  @Override
  public void dispose() {
    myTasks.clear();
  }
}
