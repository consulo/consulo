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
import consulo.ui.image.Image;
import org.intellij.lang.annotations.Language;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.List;

/**
 * @author VISTALL
 * @since 30-Jan-17
 */
public interface ModuleImportProvider<C extends ModuleImportContext> {
  ExtensionPointName<ModuleImportProvider<?>> EP_NAME = ExtensionPointName.create("com.intellij.moduleImportProvider");

  @SuppressWarnings("unchecked")
  @Nonnull
  default C createContext() {
    return (C)new ModuleImportContext();
  }

  /**
   * If return false - this provider will be avaliable from 'Import Module' action from project structore
   */
  default boolean isOnlyForNewImport() {
    return true;
  }

  @Nonnull
  abstract String getName();

  @Nonnull
  abstract Image getIcon();

  boolean canImport(@Nonnull File fileOrDirectory);

  default List<Module> commit(@Nonnull C context, @Nonnull Project project) {
    return commit(context, project, null, DefaultModulesProvider.createForProject(project), null);
  }

  @Nonnull
  List<Module> commit(@Nonnull C context,
                      @Nonnull Project project,
                      @javax.annotation.Nullable ModifiableModuleModel model,
                      @Nonnull ModulesProvider modulesProvider,
                      @Nullable ModifiableArtifactModel artifactModel);

  default String getPathToBeImported(@Nonnull VirtualFile file) {
    return getDefaultPath(file);
  }

  static String getDefaultPath(@Nonnull VirtualFile file) {
    return file.isDirectory() ? file.getPath() : file.getParent().getPath();
  }

  default void addSteps(StepSequence sequence, WizardContext context, @Nonnull C moduleImportContext, String id) {
    ModuleWizardStep[] steps = createSteps(context, moduleImportContext);
    for (ModuleWizardStep step : steps) {
      sequence.addSpecificStep(id, step);
    }
  }

  default ModuleWizardStep[] createSteps(@Nonnull WizardContext context, @Nonnull C moduleImportContext) {
    return ModuleWizardStep.EMPTY_ARRAY;
  }

  @Nonnull
  @Language("HTML")
  default String getFileSample() {
    return getName();
  }

  @Nullable
  @Deprecated
  default Image getIconForFile(VirtualFile file) {
    return null;
  }

  default boolean validate(Project current, Project dest) {
    return true;
  }
}
