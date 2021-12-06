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
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBusConnection;
import consulo.application.AccessRule;
import consulo.container.util.StatCollector;
import consulo.logging.Logger;
import consulo.util.concurrent.AsyncResult;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author yole
 */
@State(name = "ModuleManager", storages = @Storage("modules.xml"))
@Singleton
public class ModuleManagerComponent extends ModuleManagerImpl {
  public static final Logger LOG = Logger.getInstance(ModuleManagerComponent.class);

  private final ProgressManager myProgressManager;
  private final MessageBusConnection myConnection;

  @Inject
  public ModuleManagerComponent(Project project, ProgressManager progressManager) {
    super(project);
    myConnection = myMessageBus.connect(project);
    myProgressManager = progressManager;
    myConnection.setDefaultHandler((event, params) -> cleanCachedStuff());

    myConnection.subscribe(ProjectTopics.PROJECT_ROOTS);

    if(project.isDefault()) {
      myReady = true;
    }
  }

  @Nonnull
  @Override
  protected ModuleEx createModule(@Nonnull String name, @Nullable String dirUrl, ProgressIndicator progressIndicator) {
    return new ModuleImpl(name, dirUrl, myProject);
  }

  public void loadModules(@Nonnull ProgressIndicator indicator, AsyncResult<Void> result) {
    StatCollector stat = new StatCollector();

    stat.markWith("load modules", () -> loadModules(myModuleModel, indicator, true));

    indicator.setIndeterminate(true);

    AccessRule.writeAsync(() -> {
      stat.markWith("fire modules add", () -> {
        for (Module module : myModuleModel.myModules) {
          fireModuleAdded(module);
        }
      });

      myReady = true;
      stat.dump("ModulesManager", LOG::info);
    }).doWhenDone((Runnable)result::setDone);
  }

  @Override
  protected void fireModulesAdded() {
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
  protected void doFirstModulesLoad() {
  }

  @Override
  protected void deliverPendingEvents() {
    myConnection.deliverImmediately();
  }
}
