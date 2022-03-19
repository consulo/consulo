/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.diagnostic.logging;

import consulo.execution.configuration.RunConfigurationBase;
import com.intellij.ui.content.ContentManagerAdapter;
import com.intellij.util.ArrayUtil;
import consulo.content.scope.SearchScope;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.execution.configuration.RunProfile;
import consulo.execution.configuration.log.AdditionalTabComponent;
import consulo.execution.ui.layout.RunnerLayoutUi;
import consulo.process.ProcessHandler;
import consulo.project.Project;
import consulo.ui.ex.ComponentWithActions;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.event.ContentManagerEvent;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

public abstract class LogConsoleManagerBase implements LogConsoleManager, Disposable {
  private final Project myProject;
  private final Map<AdditionalTabComponent, Content> myAdditionalContent = new HashMap<AdditionalTabComponent, Content>();
  private final SearchScope mySearchScope;

  protected LogConsoleManagerBase(@Nonnull Project project, @Nonnull SearchScope searchScope) {
    myProject = project;
    mySearchScope = searchScope;
  }

  @Override
  public void addLogConsole(@Nonnull String name, @Nonnull String path, @Nonnull Charset charset, long skippedContent, @Nonnull RunConfigurationBase runConfiguration) {
    addLogConsole(name, path, charset, skippedContent, getDefaultIcon(), runConfiguration);
  }

  public void addLogConsole(final String name, final String path, @Nonnull Charset charset, final long skippedContent, Image icon, @Nullable RunProfile runProfile) {
    doAddLogConsole(new LogConsoleImpl(myProject, new File(path), charset, skippedContent, name, false, mySearchScope) {
      @Override
      public boolean isActive() {
        return isConsoleActive(path);
      }
    }, path, icon, runProfile);
  }

  private void doAddLogConsole(@Nonnull final LogConsoleBase log, String id, Image icon, @Nullable RunProfile runProfile) {
    if (runProfile instanceof RunConfigurationBase) {
      ((RunConfigurationBase)runProfile).customizeLogConsole(log);
    }
    log.attachStopLogConsoleTrackingListener(getProcessHandler());
    addAdditionalTabComponent(log, id, icon);

    getUi().addListener(new ContentManagerAdapter() {
      @Override
      public void selectionChanged(final ContentManagerEvent event) {
        log.activate();
      }
    }, log);
  }

  private boolean isConsoleActive(String id) {
    final Content content = getUi().findContent(id);
    return content != null && content.isSelected();
  }

  @Override
  public void removeLogConsole(@Nonnull String path) {
    Content content = getUi().findContent(path);
    if (content != null) {
      removeAdditionalTabComponent((LogConsoleBase)content.getComponent());
    }
  }

  @Override
  public void addAdditionalTabComponent(@Nonnull AdditionalTabComponent tabComponent, @Nonnull String id) {
    addAdditionalTabComponent(tabComponent, id, getDefaultIcon());
  }

  public Content addAdditionalTabComponent(@Nonnull AdditionalTabComponent tabComponent, @Nonnull String id, @Nullable Image icon) {
    Content logContent = getUi().createContent(id, (ComponentWithActions)tabComponent, tabComponent.getTabTitle(), icon,
                                               tabComponent.getPreferredFocusableComponent());
    myAdditionalContent.put(tabComponent, logContent);
    getUi().addContent(logContent);
    return logContent;
  }

  @Override
  public void removeAdditionalTabComponent(@Nonnull AdditionalTabComponent component) {
    Disposer.dispose(component);
    final Content content = myAdditionalContent.remove(component);
    if (!getUi().isDisposed()) {
      getUi().removeContent(content, true);
    }
  }

  @Override
  public void dispose() {
    for (AdditionalTabComponent component : ArrayUtil.toObjectArray(myAdditionalContent.keySet(), AdditionalTabComponent.class)) {
      removeAdditionalTabComponent(component);
    }
  }

  protected abstract Image getDefaultIcon();

  protected abstract RunnerLayoutUi getUi();

  public abstract ProcessHandler getProcessHandler();
}
