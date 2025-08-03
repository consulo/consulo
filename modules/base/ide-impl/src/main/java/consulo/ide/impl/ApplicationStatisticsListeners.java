/*
 * Copyright 2013-2020 consulo.io
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
package consulo.ide.impl;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.component.messagebus.MessageBus;
import consulo.component.messagebus.MessageBusConnection;
import consulo.application.internal.AppLifecycleListener;
import consulo.externalService.statistic.AbstractApplicationUsagesCollector;
import consulo.externalService.statistic.UsagesCollector;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.event.ProjectManagerListener;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2020-06-14
 */
@Singleton
@ServiceAPI(value = ComponentScope.APPLICATION, lazy = false)
@ServiceImpl
public class ApplicationStatisticsListeners {
  private boolean persistOnClosing = !ApplicationManager.getApplication().isUnitTestMode();

  @Inject
  public ApplicationStatisticsListeners(@Nonnull Application application) {
    final MessageBus messageBus = application.getMessageBus();

    MessageBusConnection connection = messageBus.connect();
    connection.subscribe(AppLifecycleListener.class, new AppLifecycleListener() {
      @Override
      public void appClosing() {
        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
          if (project.isInitialized()) {
            doPersistProjectUsages(project);
          }
        }
        persistOnClosing = false;
      }
    });

    connection.subscribe(ProjectManagerListener.class, new ProjectManagerListener() {
      @Override
      public void projectClosing(@Nonnull Project project) {
        if (project.isInitialized()) {
          if (persistOnClosing) {
            doPersistProjectUsages(project);
          }
        }
      }
    });
  }

  private static void doPersistProjectUsages(@Nonnull Project project) {
    if (DumbService.isDumb(project)) return;
    
    project.getApplication().getExtensionPoint(UsagesCollector.class).forEachExtensionSafe(usagesCollector -> {
      if (usagesCollector instanceof AbstractApplicationUsagesCollector) {
        ((AbstractApplicationUsagesCollector)usagesCollector).persistProjectUsages(project);
      }
    });
  }
}
