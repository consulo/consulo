/*
 * Copyright 2013-2017 consulo.io
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
package consulo.moduleImport;

import com.intellij.ide.util.newProjectWizard.StepSequence;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.configuration.DefaultModulesProvider;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.ModifiableArtifactModel;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;
import java.util.List;

/**
 * @author VISTALL
 * @since 30-Jan-17
 */
public interface ModuleImportProvider<C extends ModuleImportContext> {
  ExtensionPointName<ModuleImportProvider<?>> EP_NAME = ExtensionPointName.create("com.intellij.moduleImportProvider");

  @SuppressWarnings("unchecked")
  @NotNull
  default C createContext() {
    return (C)new ModuleImportContext();
  }

  /**
   * If return false - this provider will be avaliable from 'Import Module' action from project structore
   */
  default boolean isOnlyForNewImport() {
    return true;
  }

  @NotNull
  abstract String getName();

  @Nullable
  abstract Icon getIcon();

  boolean canImport(@NotNull File fileOrDirectory) ;

  default List<Module> commit(@NotNull C context, @NotNull Project project) {
    return commit(context, project, null, DefaultModulesProvider.createForProject(project), null);
  }

  @NotNull
  List<Module> commit(@NotNull C context,
                      @NotNull Project project,
                      @Nullable ModifiableModuleModel model,
                      @NotNull ModulesProvider modulesProvider,
                      @Nullable ModifiableArtifactModel artifactModel);

  default String getPathToBeImported(@NotNull VirtualFile file) {
    return getDefaultPath(file);
  }

  static String getDefaultPath(@NotNull VirtualFile file) {
    return file.isDirectory() ? file.getPath() : file.getParent().getPath();
  }

  default void addSteps(StepSequence sequence, WizardContext context, @NotNull C moduleImportContext, String id) {
    ModuleWizardStep[] steps = createSteps(context, moduleImportContext);
    for (ModuleWizardStep step : steps) {
      sequence.addSpecificStep(id, step);
    }
  }

  default ModuleWizardStep[] createSteps(@NotNull WizardContext context, @NotNull C moduleImportContext) {
    return ModuleWizardStep.EMPTY_ARRAY;
  }

  @NotNull
  @Language("HTML")
  default String getFileSample() {
    return getName();
  }

  @Nullable
  @Deprecated
  default Icon getIconForFile(VirtualFile file) {
    return null;
  }

  default boolean validate(Project current, Project dest) {
    return true;
  }
}
