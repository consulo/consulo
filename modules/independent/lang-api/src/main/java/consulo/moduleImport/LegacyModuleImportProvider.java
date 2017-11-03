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

import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.ModifiableArtifactModel;
import com.intellij.projectImport.ProjectImportBuilder;
import com.intellij.projectImport.ProjectImportProvider;
import consulo.annotations.DeprecationInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 * @author VISTALL
 * @since 30-Jan-17
 */
@Deprecated
@DeprecationInfo("Bridge from ProjectImportProvider to ModuleImportProvider, until all usage will be replaced")
public class LegacyModuleImportProvider implements ModuleImportProvider<ModuleImportContext> {
  private ProjectImportProvider myProvider;

  public LegacyModuleImportProvider(ProjectImportProvider provider) {
    myProvider = provider;
  }

  public ProjectImportProvider getProvider() {
    return myProvider;
  }

  @NotNull
  @Override
  public String getName() {
    return myProvider.getName();
  }

  @Nullable
  @Override
  public Icon getIcon() {
    return myProvider.getIcon();
  }

  @Override
  public boolean canImport(@NotNull File fileOrDirectory) {
    return myProvider.canImport(LocalFileSystem.getInstance().findFileByIoFile(fileOrDirectory), null);
  }

  @NotNull
  @Override
  public List<Module> commit(@NotNull ModuleImportContext context,
                             @NotNull Project project,
                             ModifiableModuleModel model,
                             @NotNull ModulesProvider modulesProvider,
                             ModifiableArtifactModel artifactModel) {
    ProjectImportBuilder<?> builder = myProvider.getBuilder();
    List<Module> list = builder.commit(project, model, modulesProvider);
    return list == null ? Collections.<Module>emptyList() : list;
  }

  @Override
  public ModuleWizardStep[] createSteps(@NotNull WizardContext context, @NotNull ModuleImportContext moduleImportContext) {
    return myProvider.createSteps(context);
  }

  @NotNull
  @Override
  public String getFileSample() {
    return myProvider.getFileSample();
  }

  @Nullable
  @Override
  public Icon getIconForFile(VirtualFile file) {
    return myProvider.getIconForFile(file);
  }

  @Override
  public boolean validate(Project current, Project dest) {
    return myProvider.getBuilder().validate(current, dest);
  }
}
