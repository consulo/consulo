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
package com.intellij.ide.util.newProjectWizard.modes;

import com.intellij.ide.util.newProjectWizard.StepSequence;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.projectImport.ImportChooserStep;
import consulo.moduleImport.ModuleImportContext;
import consulo.moduleImport.ModuleImportProvider;
import consulo.moduleImport.ModuleImportProviders;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ImportMode implements Disposable {
  private StepSequence myStepSequence;
  private final ModuleImportProvider<?>[] myProviders;

  public ImportMode(@NotNull ModuleImportProvider<?>[] providers) {
    myProviders = providers;
  }

  @NotNull
  public String getDisplayName(final WizardContext context) {
    return ProjectBundle.message("project.new.wizard.import.title", context.getPresentationName());
  }

  @NotNull
  public String getDescription(final WizardContext context) {
    final String productName = ApplicationNamesInfo.getInstance().getFullProductName();
    return ProjectBundle.message("project.new.wizard.import.description", productName, context.getPresentationName(),
                                 StringUtil.join(ModuleImportProviders.getExtensions(false), ModuleImportProvider::getName, ", "));
  }

  @Nullable
  public StepSequence getSteps() {
    assert myStepSequence != null;
    return myStepSequence;
  }

  @Override
  public void dispose() {
    myStepSequence = null;
  }

  @NotNull
  @SuppressWarnings("unchecked")
  public StepSequence createSteps(final WizardContext context) {
    final StepSequence stepSequence = new StepSequence();
    if (myProviders.length > 1) {
      stepSequence.addCommonStep(new ImportChooserStep(myProviders, stepSequence, context));
    }

    for (ModuleImportProvider provider : myProviders) {
      ModuleImportContext moduleImportContext = context.getModuleImportContext(provider);

      provider.addSteps(stepSequence, context, moduleImportContext, provider.getClass().getName());
    }

    if (myProviders.length == 1) {
      stepSequence.setType(myProviders[0].getClass().getName());
    }

    return myStepSequence = stepSequence;
  }

  public boolean isAvailable(WizardContext context) {
    return myProviders.length > 0;
  }

  public void onChosen(final boolean enabled) {
  }

  public String getShortName() {
    return "Import from External Model";
  }
}