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
import consulo.annotation.DeprecationInfo;
import consulo.options.ProjectStructureSelector;
import consulo.platform.Platform;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.concurrent.AsyncResult;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.util.function.Consumer;

public abstract class ShowSettingsUtil {
  public static final String DIMENSION_KEY = "OptionsEditor";

  public static ShowSettingsUtil getInstance() {
    return ServiceManager.getService(ShowSettingsUtil.class);
  }

  @RequiredUIAccess
  public void showSettingsDialog(@Nullable Project project) {
    showSettingsDialog(project, (Configurable)null);
  }

  @RequiredUIAccess
  public void showSettingsDialog(@Nullable Project project, Class toSelect) {
    showAndSelect(project, toSelect, o -> {
    });
  }

  @RequiredUIAccess
  public <T extends UnnamedConfigurable> void showAndSelect(@Nullable Project project, @Nonnull Class<T> toSelect) {
    showAndSelect(project, toSelect, o -> {
    });
  }

  @RequiredUIAccess
  public abstract <T extends UnnamedConfigurable> void showAndSelect(@Nullable Project project, @Nonnull Class<T> toSelect, @Nonnull Consumer<T> afterSelect);

  @RequiredUIAccess
  public abstract void showSettingsDialog(@Nullable Project project, @Nonnull String nameToSelect);

  @RequiredUIAccess
  public abstract void showSettingsDialog(@Nullable Project project, final String id2Select, final String filter);

  @RequiredUIAccess
  public abstract void showSettingsDialog(@Nullable Project project, @Nullable Configurable toSelect);

  @RequiredUIAccess
  public void showProjectStructureDialog(@Nonnull Project project) {
    showProjectStructureDialog(project, projectStructureSelector -> projectStructureSelector.select(null, null, true));
  }

  @RequiredUIAccess
  public abstract AsyncResult<Void> showProjectStructureDialog(@Nonnull Project project, @RequiredUIAccess @Nonnull Consumer<ProjectStructureSelector> consumer);

  @RequiredUIAccess
  @Deprecated
  @DeprecationInfo("Show #showAndSelect()")
  public AsyncResult<Void> editConfigurable(Project project, Configurable configurable) {
    return editConfigurable(null, project, configurable);
  }

  @RequiredUIAccess
  @Deprecated
  @DeprecationInfo("Show #showAndSelect()")
  public abstract AsyncResult<Void> editConfigurable(@Nullable String title, Project project, Configurable configurable);

  @RequiredUIAccess
  @Deprecated
  @DeprecationInfo("Show #showAndSelect()")
  public AsyncResult<Void> editConfigurable(Project project, Configurable configurable, Runnable advancedInitialization) {
    return editConfigurable(null, project, configurable, advancedInitialization);
  }

  @RequiredUIAccess
  @Deprecated
  @DeprecationInfo("Show #showAndSelect()")
  public abstract AsyncResult<Void> editConfigurable(@Nullable String title, Project project, Configurable configurable, Runnable advancedInitialization);

  @RequiredUIAccess
  @Deprecated
  @DeprecationInfo("Show #showAndSelect()")
  public abstract AsyncResult<Void> editConfigurable(Component parent, Configurable configurable);

  @RequiredUIAccess
  @Deprecated
  @DeprecationInfo("Show #showAndSelect()")
  public abstract AsyncResult<Void> editConfigurable(Component parent, Configurable configurable, Runnable advancedInitialization);

  @RequiredUIAccess
  @Deprecated
  @DeprecationInfo("Show #showAndSelect()")
  public AsyncResult<Void> editConfigurable(Project project, String dimensionServiceKey, Configurable configurable) {
    return editConfigurable(null, project, dimensionServiceKey, configurable);
  }

  @RequiredUIAccess
  @Deprecated
  @DeprecationInfo("Show #showAndSelect()")
  public abstract AsyncResult<Void> editConfigurable(@Nullable String title, Project project, String dimensionServiceKey, Configurable configurable);

  @RequiredUIAccess
  @Deprecated
  @DeprecationInfo("Show #showAndSelect()")
  public abstract AsyncResult<Void> editConfigurable(Component parent, String dimensionServiceKey, Configurable configurable);

  /**
   * @deprecated create a new instance of configurable instead
   */
  public abstract <T extends Configurable> T findProjectConfigurable(Project project, Class<T> confClass);

  /**
   * @deprecated create a new instance of configurable instead
   */
  public abstract <T extends Configurable> T findApplicationConfigurable(Class<T> confClass);

  public abstract boolean isAlreadyShown();

  @Nonnull
  public static String getSettingsMenuName() {
    return Platform.current().os().isMac() ? "Preferences" : "Settings";
  }
}