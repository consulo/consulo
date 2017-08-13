/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.openapi.module.impl;

import com.intellij.ProjectTopics;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.impl.ProjectLifecycleListener;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author yole
 */
@State(name = ModuleManagerImpl.COMPONENT_NAME, storages = @Storage(file = StoragePathMacros.PROJECT_CONFIG_DIR + "/modules.xml"))
public class ModuleManagerComponent extends ModuleManagerImpl {
  public static final Logger LOGGER = Logger.getInstance(ModuleManagerComponent.class);

  private final ProgressManager myProgressManager;
  private final MessageBusConnection myConnection;

  public ModuleManagerComponent(Project project, ProgressManager progressManager, MessageBus bus) {
    super(project, bus);
    myConnection = bus.connect(project);
    myProgressManager = progressManager;
    myConnection.setDefaultHandler((event, params) -> cleanCachedStuff());

    myConnection.subscribe(ProjectTopics.PROJECT_ROOTS);
    myConnection.subscribe(ProjectLifecycleListener.TOPIC, new ProjectLifecycleListener() {
      @Override
      public void projectComponentsInitialized(@NotNull final Project project) {
        long t = System.currentTimeMillis();
        loadModules(myModuleModel, true);
        t = System.currentTimeMillis() - t;
        LOGGER.info(myModuleModel.getModules().length + " module(s) loaded in " + t + " ms");
      }
    });

  }

  @NotNull
  @Override
  protected ModuleEx createModule(@NotNull String name, @Nullable String dirUrl, ProgressIndicator progressIndicator) {
    return new ModuleImpl(name, dirUrl, myProject);
  }

  @Override
  protected void fireModulesAdded() {
    if (ApplicationManager.getApplication().isCompilerServerMode()) {
      for (final Module module : myModuleModel.getModules()) {
        fireModuleAdded(module);
      }
      return;
    }

    Runnable runnableWithProgress = () -> {
      for (final Module module : myModuleModel.getModules()) {
        final Application app = ApplicationManager.getApplication();
        final Runnable swingRunnable = () -> fireModuleAddedInWriteAction(module);
        if (app.isDispatchThread() || app.isHeadlessEnvironment()) {
          swingRunnable.run();
        }
        else {
          ProgressIndicator pi = ProgressManager.getInstance().getProgressIndicator();
          app.invokeAndWait(swingRunnable, pi.getModalityState());
        }
      }
    };

    ProgressIndicator progressIndicator = myProgressManager.getProgressIndicator();
    if (progressIndicator == null) {
      myProgressManager.runProcessWithProgressSynchronously(runnableWithProgress, "Loading modules", false, myProject);
    }
    else {
      runnableWithProgress.run();
    }
  }

  @Override
  protected void deliverPendingEvents() {
    myConnection.deliverImmediately();
  }
}
