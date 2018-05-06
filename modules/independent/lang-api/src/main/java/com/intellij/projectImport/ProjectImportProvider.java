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

/*
 * User: anna
 * Date: 10-Jul-2007
 */
package com.intellij.projectImport;

import com.intellij.ide.util.newProjectWizard.StepSequence;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.annotations.DeprecationInfo;
import consulo.ui.image.Image;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Deprecated
public abstract class ProjectImportProvider {
  public static final ExtensionPointName<ProjectImportProvider> EP_NAME = ExtensionPointName.create("com.intellij.projectImportProvider");

  protected ProjectImportBuilder myBuilder;

  protected ProjectImportProvider(final ProjectImportBuilder builder) {
    myBuilder = builder;
    myBuilder.setProvider(this);
  }

  public ProjectImportBuilder getBuilder() {
    return myBuilder;
  }

  @NonNls @Nonnull
  public String getId(){
    return getBuilder().getName();
  }

  @Nonnull
  public String getName(){
    return getBuilder().getName();
  }

  @Nonnull
  public Image getIcon() {
    return getBuilder().getIcon();
  }

  public boolean canImport(VirtualFile fileOrDirectory, @javax.annotation.Nullable Project project) {
    return !fileOrDirectory.isDirectory() && canImportFromFile(fileOrDirectory);
  }

  @Deprecated
  @DeprecationInfo(value = "Use #canImport(VirtualFile fileOrDirectory, @Nullable Project project)", until = "2.0")
  protected boolean canImportFromFile(VirtualFile file) {
    return false;
  }

  public String getPathToBeImported(VirtualFile file) {
    return getDefaultPath(file);
  }

  public static String getDefaultPath(VirtualFile file) {
    return file.isDirectory() ? file.getPath() : file.getParent().getPath();
  }

  public boolean canCreateNewProject() {
    return true;
  }

  public void addSteps(StepSequence sequence, WizardContext context, String id) {
    ModuleWizardStep[] steps = createSteps(context);
    for (ModuleWizardStep step : steps) {
      sequence.addSpecificStep(id, step);
    }
  }

  public ModuleWizardStep[] createSteps(WizardContext context) {
    return ModuleWizardStep.EMPTY_ARRAY;
  }

  @Nonnull
  @Language("HTML")
  public String getFileSample() {
    return getName();
  }

  @Nullable
  public Image getIconForFile(VirtualFile file) {
    return null;
  }
}
