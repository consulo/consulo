/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.openapi.options;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import consulo.annotations.RequiredDispatchThread;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.awt.*;

public abstract class ShowSettingsUtil {
  public static ShowSettingsUtil getInstance() {
    return ServiceManager.getService(ShowSettingsUtil.class);
  }

  public abstract void showSettingsDialog(@Nullable Project project);

  public abstract void showSettingsDialog(@Nullable Project project, Class toSelect);

  public abstract void showSettingsDialog(@Nullable Project project, @Nonnull String nameToSelect);

  public abstract void showSettingsDialog(@Nonnull final Project project, final Configurable toSelect);

  @RequiredDispatchThread
  public boolean editConfigurable(Project project, Configurable configurable) {
    return editConfigurable(null, project, configurable);
  }

  @RequiredDispatchThread
  public abstract boolean editConfigurable(@Nullable String title, Project project, Configurable configurable);

  @RequiredDispatchThread
  public boolean editConfigurable(Project project, Configurable configurable, Runnable advancedInitialization) {
    return editConfigurable(null, project, configurable, advancedInitialization);
  }

  @RequiredDispatchThread
  public abstract boolean editConfigurable(@Nullable String title, Project project, Configurable configurable, Runnable advancedInitialization);

  @RequiredDispatchThread
  public abstract boolean editConfigurable(Component parent, Configurable configurable);

  @RequiredDispatchThread
  public abstract boolean editConfigurable(Component parent, Configurable configurable, Runnable advancedInitialization);

  @RequiredDispatchThread
  public boolean editConfigurable(Project project, @NonNls String dimensionServiceKey, Configurable configurable) {
    return editConfigurable(null, project, dimensionServiceKey, configurable);
  }

  @RequiredDispatchThread
  public abstract boolean editConfigurable(@Nullable String title, Project project, @NonNls String dimensionServiceKey, Configurable configurable);

  @RequiredDispatchThread
  public abstract boolean editConfigurable(Component parent, String dimensionServiceKey, Configurable configurable);

  /**
   * @deprecated create a new instance of configurable instead
   */
  public abstract <T extends Configurable> T findProjectConfigurable(Project project, Class<T> confClass);

  /**
   * @deprecated create a new instance of configurable instead
   */
  public abstract <T extends Configurable> T findApplicationConfigurable(Class<T> confClass);

  @Nonnull
  public static String getSettingsMenuName() {
    return SystemInfo.isMac ? "Preferences" : "Settings";
  }
}