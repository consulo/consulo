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
package consulo.ide.setting;

import consulo.annotation.DeprecationInfo;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.configurable.Configurable;
import consulo.configurable.UnnamedConfigurable;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.concurrent.AsyncResult;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.*;
import java.util.function.Consumer;

@ServiceAPI(ComponentScope.APPLICATION)
public abstract class ShowSettingsUtil implements ProjectStructureSettingsUtil {
  public static final String DIMENSION_KEY = "OptionsEditor";

  public static ShowSettingsUtil getInstance() {
    return Application.get().getInstance(ShowSettingsUtil.class);
  }

  @RequiredUIAccess
  @Nonnull
  public AsyncResult<Void> showSettingsDialog(@Nullable Project project) {
    return showSettingsDialog(project, (Configurable)null);
  }

  @RequiredUIAccess
  @Nonnull
  public AsyncResult<Void> showSettingsDialog(@Nullable Project project, Class toSelect) {
    return showAndSelect(project, toSelect, o -> {
    });
  }

  @RequiredUIAccess
  @Nonnull
  public <T extends UnnamedConfigurable> AsyncResult<Void> showAndSelect(@Nullable Project project, @Nonnull Class<T> toSelect) {
    return showAndSelect(project, toSelect, o -> {
    });
  }

  @RequiredUIAccess
  @Nonnull
  public abstract <T extends UnnamedConfigurable> AsyncResult<Void> showAndSelect(@Nullable Project project, @Nonnull Class<T> toSelect, @Nonnull Consumer<T> afterSelect);

  @RequiredUIAccess
  @Nonnull
  public abstract AsyncResult<Void> showSettingsDialog(@Nullable Project project, @Nonnull String nameToSelect);

  @RequiredUIAccess
  @Nonnull
  public abstract AsyncResult<Void> showSettingsDialog(@Nullable Project project, final String id2Select, final String filter);

  @RequiredUIAccess
  @Nonnull
  public abstract AsyncResult<Void> showSettingsDialog(@Nullable Project project, @Nullable Configurable toSelect);

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

  public abstract boolean isAlreadyShown(Project project);
}