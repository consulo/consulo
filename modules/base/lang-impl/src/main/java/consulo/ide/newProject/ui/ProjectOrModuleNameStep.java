/*
 * Copyright 2013-2019 consulo.io
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
package consulo.ide.newProject.ui;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.util.projectWizard.NamePathComponent;
import com.intellij.ide.util.projectWizard.ProjectWizardUtil;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ui.JBUI;
import consulo.annotation.DeprecationInfo;
import consulo.disposer.Disposable;
import consulo.ide.wizard.newModule.NewModuleWizardContext;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.wizard.WizardStep;
import consulo.ui.wizard.WizardStepValidationException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * @author VISTALL
 * @since 2019-08-20
 */
@Deprecated(forRemoval = true)
@DeprecationInfo("Desktop variant of UnifiedProjectOrModuleNameStep. Use UnifiedProjectOrModuleNameStep")
public class ProjectOrModuleNameStep<C extends NewModuleWizardContext> implements WizardStep<C> {
  private final JPanel myPanel;
  protected final JPanel myAdditionalContentPanel;
  protected NamePathComponent myNamePathComponent;

  public ProjectOrModuleNameStep(C context) {
    myNamePathComponent = new NamePathComponent(IdeBundle.message("label.project.name"), IdeBundle.message("label.project.files.location"),
                                                IdeBundle.message("title.select.project.file.directory", IdeBundle.message("project.new.wizard.project.identification")),
                                                IdeBundle.message("description.select.project.file.directory", StringUtil.capitalize(IdeBundle.message("project.new.wizard.project.identification"))),
                                                true, false);
    final String baseDir = context.getPath();
    final String projectName = context.getName();
    final String initialProjectName = projectName != null ? projectName : ProjectWizardUtil.findNonExistingFileName(baseDir, "untitled", "");
    myNamePathComponent.setPath(projectName == null ? (baseDir + File.separator + initialProjectName) : baseDir);
    myNamePathComponent.setNameValue(initialProjectName);
    myNamePathComponent.getNameComponent().select(0, initialProjectName.length());

    myPanel = new JPanel(new GridBagLayout());
    myPanel.add(myNamePathComponent,
                new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 1.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, JBUI.emptyInsets(), 0, 0));

    myAdditionalContentPanel = new JPanel(new BorderLayout());
    myPanel.add(myAdditionalContentPanel, new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 1.0, 1.0, GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, JBUI.emptyInsets(), 0, 0));
  }

  @Override
  public void validateStep(@Nonnull C c) throws WizardStepValidationException {
    validateNameAndPath(c);
  }

  private boolean validateNameAndPath(@Nonnull NewModuleWizardContext context) throws WizardStepValidationException {
    final String name = myNamePathComponent.getNameValue();
    if (name.length() == 0) {
      final ApplicationInfo info = ApplicationInfo.getInstance();
      throw new WizardStepValidationException(IdeBundle.message("prompt.new.project.file.name", info.getVersionName(), context.getTargetId()));
    }

    final String projectFileDirectory = myNamePathComponent.getPath();
    if (projectFileDirectory.length() == 0) {
      throw new WizardStepValidationException(IdeBundle.message("prompt.enter.project.file.location", context.getTargetId()));
    }

    final boolean shouldPromptCreation = myNamePathComponent.isPathChangedByUser();
    if (!ProjectWizardUtil.createDirectoryIfNotExists(IdeBundle.message("directory.project.file.directory", context.getTargetId()), projectFileDirectory, shouldPromptCreation)) {
      return false;
    }

    final File file = new File(projectFileDirectory);
    if (file.exists() && !file.canWrite()) {
      throw new WizardStepValidationException(String.format("Directory '%s' is not writable!\nPlease choose another project location.", projectFileDirectory));
    }

    boolean shouldContinue = true;
    final File projectDir = new File(myNamePathComponent.getPath(), Project.DIRECTORY_STORE_FOLDER);
    if (projectDir.exists()) {
      int answer = Messages
              .showYesNoDialog(IdeBundle.message("prompt.overwrite.project.folder", projectDir.getAbsolutePath(), context.getTargetId()), IdeBundle.message("title.file.already.exists"), Messages.getQuestionIcon());
      shouldContinue = (answer == Messages.YES);
    }

    return shouldContinue;
  }

  @Override
  public void onStepLeave(@Nonnull C c) {
    c.setName(getProjectName());
    final String projectFileDirectory = getProjectFileDirectory();
    c.setPath(projectFileDirectory);
  }

  public String getProjectFileDirectory() {
    return myNamePathComponent.getPath();
  }

  public String getProjectName() {
    return myNamePathComponent.getNameValue();
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public java.awt.Component getSwingComponent(@Nonnull C context, @Nonnull Disposable uiDisposable) {
    return myPanel;
  }

  @Nullable
  @Override
  public java.awt.Component getSwingPreferredFocusedComponent() {
    return myNamePathComponent.getNameComponent();
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public Component getComponent(@Nonnull C context, @Nonnull Disposable uiDisposable) {
    throw new UnsupportedOperationException();
  }

  @Nullable
  @Override
  public Component getPreferredFocusedComponent() {
    throw new UnsupportedOperationException();
  }
}
