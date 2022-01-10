/*
 * Copyright 2013-2021 consulo.io
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

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import consulo.disposer.Disposable;
import consulo.ide.ui.FileChooserTextBoxBuilder;
import consulo.ide.ui.TextComponentAccessor;
import consulo.ide.wizard.newModule.NewModuleWizardContext;
import consulo.platform.base.localize.IdeLocalize;
import consulo.ui.Component;
import consulo.ui.TextBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.util.FormBuilder;
import consulo.ui.wizard.WizardStep;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;

/**
 * @author VISTALL
 * @since 08/01/2021
 * <p>
 * Unified variant of {@link ProjectOrModuleNameStep}
 */
public class UnifiedProjectOrModuleNameStep<C extends NewModuleWizardContext> implements WizardStep<C> {
  private final C myContext;

  private Component myRootPanel;
  private TextBox myNameTextBox;
  private FileChooserTextBoxBuilder.Controller myFileChooserController;
  private boolean myUserPathEntered;
  private boolean myUserNameEntered;

  public UnifiedProjectOrModuleNameStep(C context) {
    myContext = context;
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public Component getComponent(@Nonnull C context, @Nonnull Disposable uiDisposable) {
    if (myRootPanel == null) {
      myRootPanel = buildComponent(uiDisposable);
    }
    return myRootPanel;
  }

  @Nonnull
  @RequiredUIAccess
  private Component buildComponent(Disposable uiDisposable) {
    myUserNameEntered = false;
    myUserPathEntered = false;

    FormBuilder formBuilder = FormBuilder.create();

    myNameTextBox = TextBox.create();

    formBuilder.addLabeled(myContext.isNewProject() ? IdeLocalize.labelProjectName() : IdeLocalize.labelModuleName(), myNameTextBox);

    FileChooserTextBoxBuilder builder = FileChooserTextBoxBuilder.create(myContext.getProject());
    builder.uiDisposable(uiDisposable);
    builder.textBoxAccessor(new TextComponentAccessor<TextBox>() {
      @RequiredUIAccess
      @Override
      public String getValue(TextBox component) {
        return component.getValueOrError();
      }

      @RequiredUIAccess
      @Override
      public void setValue(TextBox component, String path, boolean fireListeners) {
        component.setValue(path, fireListeners);

        if(fireListeners) {
          myUserPathEntered = true;

          if(!myUserNameEntered) {
            final int lastSeparatorIndex = path.lastIndexOf(File.separator);
            if (lastSeparatorIndex >= 0 && (lastSeparatorIndex + 1) < path.length()) {
              myNameTextBox.setValue(path.substring(lastSeparatorIndex + 1), false);
            }
          }
        }
      }
    });

    builder.fileChooserDescriptor(FileChooserDescriptorFactory.createSingleFolderDescriptor());

    myFileChooserController = builder.build();

    formBuilder.addLabeled(myContext.isNewProject() ? IdeLocalize.labelProjectFilesLocation() : IdeLocalize.labelModuleContentRoot(), myFileChooserController.getComponent());

    final String projectOrModuleName = myContext.getName();
    final String projectOrModulePath = myContext.getPath();

    myNameTextBox.setValue(projectOrModuleName);
    myFileChooserController.setValue(projectOrModulePath, false);

    myNameTextBox.addValueListener(event -> {
      if(myUserPathEntered) {
        return;
      }
      
      final String name = myNameTextBox.getValue();
      final String path = myFileChooserController.getValue().trim();
      final int lastSeparatorIndex = path.lastIndexOf(File.separator);

      if (lastSeparatorIndex >= 0) {
        String newPath = path.substring(0, lastSeparatorIndex + 1) + name;

        // do not fire events
        myFileChooserController.setValue(newPath, false);

        myUserNameEntered = true;
      }
    });


    extend(formBuilder, uiDisposable);

    return formBuilder.build();
  }

  @RequiredUIAccess
  protected void extend(@Nonnull FormBuilder builder, Disposable uiDisposable) {
    // for plugins
  }

  @Override
  public void onStepLeave(@Nonnull C c) {
    c.setName(getName());
    c.setPath(getPath());
  }

  @Nonnull
  @RequiredUIAccess
  public String getPath() {
    return myFileChooserController.getValue();
  }

  @Nonnull
  public String getName() {
    return myNameTextBox.getValueOrError();
  }

  @Nullable
  @Override
  public Component getPreferredFocusedComponent() {
    return myNameTextBox;
  }
}
