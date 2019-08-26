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
 * Date: 09-Jul-2007
 */
package com.intellij.ide.util.newProjectWizard;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.util.newProjectWizard.modes.ImportMode;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.ProjectBuilder;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.ide.wizard.AbstractWizard;
import com.intellij.ide.wizard.CommitStepException;
import com.intellij.ide.wizard.Step;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Function;
import com.intellij.util.ui.JBUI;
import consulo.moduleImport.ModuleImportContext;
import consulo.moduleImport.ModuleImportProvider;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.TestOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * TODO [VISTALL] we need review this class
 */
public class AddModuleWizard extends AbstractWizard<ModuleWizardStep> {
  private final Project myCurrentProject;
  private ModuleImportProvider[] myImportProviders;
  private WizardContext myWizardContext;
  private ImportMode myWizardMode;

  /**
   * @param project if null, the wizard will start creating new project, otherwise will add a new module to the existing project.
   */
  public AddModuleWizard(@Nullable final Project project, final @Nonnull ModulesProvider modulesProvider, @Nullable String defaultPath) {
    super(project == null ? IdeBundle.message("title.new.project") : IdeBundle.message("title.add.module"), project);
    myCurrentProject = project;
    initModuleWizard(project, defaultPath);
  }

  /**
   * @param project if null, the wizard will start creating new project, otherwise will add a new module to the existing proj.
   */
  public AddModuleWizard(Component parent, final Project project, @Nonnull ModulesProvider modulesProvider) {
    super(project == null ? IdeBundle.message("title.new.project") : IdeBundle.message("title.add.module"), parent);
    myCurrentProject = project;
    initModuleWizard(project, null);
  }

  /**
   * Import mode
   */
  public AddModuleWizard(Project project, String filePath, ModuleImportProvider... importProviders) {
    super(getImportWizardTitle(project, importProviders), project);
    myCurrentProject = project;
    myImportProviders = importProviders;
    initModuleWizard(project, filePath);
  }

  /**
   * Import mode
   */
  public AddModuleWizard(Project project, Component dialogParent, String filePath, ModuleImportProvider... importProviders) {
    super(getImportWizardTitle(project, importProviders), dialogParent);
    myCurrentProject = project;
    myImportProviders = importProviders;
    initModuleWizard(project, filePath);
  }


  private static String getImportWizardTitle(Project project, ModuleImportProvider... providers) {
    StringBuilder builder = new StringBuilder("Import ");
    builder.append(project == null ? "Project" : "Module");
    if (providers.length == 1) {
      builder.append(" from ").append(providers[0].getName());
    }
    return builder.toString();
  }

  private void initModuleWizard(@Nullable final Project project, @Nullable final String defaultPath) {
    myWizardContext = new WizardContext(project);
    if (defaultPath != null) {
      myWizardContext.setProjectFileDirectory(defaultPath);
      myWizardContext.setProjectName(defaultPath.substring(FileUtil.toSystemIndependentName(defaultPath).lastIndexOf("/") + 1));
    }
    myWizardContext.addContextListener(new WizardContext.Listener() {
      @Override
      public void buttonsUpdateRequested() {
        updateButtons();
      }

      @Override
      public void nextStepRequested() {
        doNextAction();
      }
    });

    if (myImportProviders == null) {
      throw new IllegalArgumentException();
    }
    else {

      myWizardMode = new ImportMode(myImportProviders);

      for (ModuleImportProvider<?> provider : myImportProviders) {
        ModuleImportContext context = myWizardContext.initModuleImportContext(provider);
        context.setFileToImport(defaultPath);
      }

      StepSequence sequence = myWizardMode.createSteps(myWizardContext);
      appendSteps(sequence);

      if (myImportProviders.length == 1) {
        myWizardContext.setImportProvider(myImportProviders[0]);

        myWizardContext.getModuleImportContext(myImportProviders[0]).setUpdate(getWizardContext().getProject() != null);
      }
    }
    init();
  }

  private void appendSteps(@Nullable final StepSequence sequence) {
    if (sequence != null) {
      for (ModuleWizardStep step : sequence.getAllSteps()) {
        addStep(step);
      }
    }
  }

  @Override
  protected String addStepComponent(Component component) {
    if (component instanceof JComponent) {
      ((JComponent)component).setBorder(JBUI.Borders.empty());
    }
    return super.addStepComponent(component);
  }

  @Override
  protected void updateStep() {
    if (!mySteps.isEmpty()) {
      getCurrentStepObject().updateStep(myWizardContext);
    }
    super.updateStep();
    myIcon.setIcon(null);
  }

  @Override
  protected void dispose() {
    for (ModuleWizardStep step : mySteps) {
      step.disposeUIResources();
    }
    super.dispose();
  }

  @Override
  protected final void doOKAction() {
    int idx = getCurrentStep();
    try {
      do {
        final ModuleWizardStep step = mySteps.get(idx);
        if (step != getCurrentStepObject()) {
          step.updateStep(myWizardContext);
        }
        if (!commitStepData(step)) {
          return;
        }
        step.onStepLeaving(getWizardContext());
        try {
          step._commit(true);
        }
        catch (CommitStepException e) {
          String message = e.getMessage();
          if (message != null) {
            Messages.showErrorDialog(getCurrentStepComponent(), message);
          }
          return;
        }
        if (!isLastStep(idx)) {
          idx = getNextStep(idx);
        }
        else {
          break;
        }
      }
      while (true);
    }
    finally {
      myCurrentStep = idx;
      updateStep();
    }
    super.doOKAction();
  }

  protected boolean commitStepData(final ModuleWizardStep step) {
    try {
      if (!step.validate(myWizardContext)) {
        return false;
      }
    }
    catch (ConfigurationException e) {
      Messages.showErrorDialog(myCurrentProject, e.getMessage(), e.getTitle());
      return false;
    }
    step.updateDataModel();
    return true;
  }

  @Override
  public void doNextAction() {
    final ModuleWizardStep step = getCurrentStepObject();
    if (!commitStepData(step)) {
      return;
    }
    step.onStepLeaving(getWizardContext());
    super.doNextAction();
  }

  @Override
  protected void doPreviousAction() {
    final ModuleWizardStep step = getCurrentStepObject();
    step.onStepLeaving(getWizardContext());
    super.doPreviousAction();
  }

  @Override
  public void doCancelAction() {
    final ModuleWizardStep step = getCurrentStepObject();
    step.onStepLeaving(getWizardContext());
    super.doCancelAction();
  }

  private boolean isLastStep(int step) {
    return getNextStep(step) == step;
  }


  @Override
  protected String getHelpID() {
    ModuleWizardStep step = getCurrentStepObject();
    if (step != null) {
      return step.getHelpId();
    }
    return null;
  }

  @Override
  protected final int getNextStep(final int step) {
    ModuleWizardStep nextStep = null;
    final StepSequence stepSequence = getSequence();
    if (stepSequence != null) {
      ModuleWizardStep current = mySteps.get(step);
      nextStep = stepSequence.getNextStep(current);
      while (nextStep != null && !nextStep.isStepVisible()) {
        nextStep = stepSequence.getNextStep(nextStep);
      }
    }
    return nextStep == null ? step : mySteps.indexOf(nextStep);
  }

  public StepSequence getSequence() {
    return getMode().getSteps();
  }

  @Override
  protected final int getPreviousStep(final int step) {
    ModuleWizardStep previousStep = null;
    final StepSequence stepSequence = getSequence();
    if (stepSequence != null) {
      previousStep = stepSequence.getPreviousStep(mySteps.get(step));
      while (previousStep != null && !previousStep.isStepVisible()) {
        previousStep = stepSequence.getPreviousStep(previousStep);
      }
    }
    return previousStep == null ? 0 : mySteps.indexOf(previousStep);
  }

  private ImportMode getMode() {
    return myWizardMode;
  }

  @Nonnull
  public String getNewProjectFilePath() {
    return myWizardContext.getProjectFileDirectory();
  }

  @Nonnull
  public WizardContext getWizardContext() {
    return myWizardContext;
  }

  @Nonnull
  public String getNewCompileOutput() {
    final String projectFilePath = myWizardContext.getProjectFileDirectory();
    @NonNls String path = myWizardContext.getCompilerOutputDirectory();
    if (path == null) {
      path = StringUtil.endsWithChar(projectFilePath, '/') ? projectFilePath + "out" : projectFilePath + "/out";
    }
    return path;
  }

  @NonNls
  public String getModuleDirPath() {
    return myWizardContext.getProjectFileDirectory() + File.separator + myWizardContext.getProjectName();
  }

  @Deprecated
  @Nullable
  public ProjectBuilder getProjectBuilder() {
    return myWizardContext.getProjectBuilder();
  }

  @Nullable
  public ModuleImportProvider<?> getImportProvider() {
    return myWizardContext.getImportProvider();
  }

  public String getProjectName() {
    return myWizardContext.getProjectName();
  }

  @Override
  protected String getDimensionServiceKey() {
    return "NewModule_or_Project.wizard";
  }

  /**
   * Allows to ask current wizard to move to the desired step.
   *
   * @param filter closure that allows to indicate target step - is called with each of registered steps and is expected
   *               to return <code>true</code> for the step to go to
   * @return <code>true</code> if current wizard is navigated to the target step; <code>false</code> otherwise
   */
  public boolean navigateToStep(@Nonnull Function<Step, Boolean> filter) {
    for (int i = 0, myStepsSize = mySteps.size(); i < myStepsSize; i++) {
      ModuleWizardStep step = mySteps.get(i);
      if (filter.fun(step) != Boolean.TRUE) {
        continue;
      }

      // Update current step.
      myCurrentStep = i;
      updateStep();
      return true;
    }
    return false;
  }

  @TestOnly
  public void doOk() {
    doOKAction();
  }

  @TestOnly
  public boolean isLast() {
    return isLastStep();
  }

  @TestOnly
  public void commit() {
    commitStepData(getCurrentStepObject());
  }
}
