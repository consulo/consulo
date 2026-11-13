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
package consulo.module.creation.ui;

import consulo.application.Application;
import consulo.application.platform.PlatformTarget;
import consulo.application.platform.PlatformTargetManager;
import consulo.disposer.Disposable;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.module.creation.NewModuleWizardContext;
import consulo.module.creation.localize.ModuleCreationLocalize;
import consulo.platform.Platform;
import consulo.ui.Component;
import consulo.ui.TextBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.fileChooser.FileChooserTextBoxBuilder;
import consulo.ui.ex.TextComponentAccessor;
import consulo.ui.ex.wizard.WizardStep;
import consulo.ui.util.FormBuilder;
import org.jspecify.annotations.Nullable;

import java.nio.file.FileSystems;
import java.nio.file.Path;

/**
 * @author VISTALL
 * @since 08/01/2021
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
  @Override
  public Component getComponent(C context, Disposable uiDisposable) {
    if (myRootPanel == null) {
      myRootPanel = buildComponent(uiDisposable);
    }
    return myRootPanel;
  }

  @RequiredUIAccess
  private Component buildComponent(Disposable uiDisposable) {
    myUserNameEntered = false;
    myUserPathEntered = false;

    FormBuilder formBuilder = FormBuilder.create();

    myNameTextBox = TextBox.create();

    formBuilder.addLabeled(myContext.isNewProject() ? ModuleCreationLocalize.labelProjectName() : ModuleCreationLocalize.labelModuleName(), myNameTextBox);

    FileChooserTextBoxBuilder builder = FileChooserTextBoxBuilder.create(myContext.getProject());
    builder.uiDisposable(uiDisposable);
    builder.textBoxAccessor(new TextComponentAccessor<>() {
      @RequiredUIAccess
      @Override
      public String getValue(TextBox component) {
        return component.getValueOrError();
      }

      @RequiredUIAccess
      @Override
      public void setValue(TextBox component, String path, boolean fireListeners) {
        component.setValue(path, fireListeners);

        if (fireListeners) {
          myUserPathEntered = true;

          if (!myUserNameEntered) {
            int lastSeparatorIndex = lastSeparatorIndex(path);
            if (lastSeparatorIndex >= 0 && (lastSeparatorIndex + 1) < path.length()) {
              myNameTextBox.setValue(path.substring(lastSeparatorIndex + 1), false);
            }
          }
        }
      }
    });

    // a new project may be created on any machine - a module is of the machine of its project
    FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
    builder.fileChooserDescriptor(myContext.isNewProject() ? descriptor.withAnyPlatform() : descriptor.withPlatform(myContext.getPlatform()));

    myFileChooserController = builder.build();

    formBuilder.addLabeled(myContext.isNewProject() ? ModuleCreationLocalize.labelProjectFilesLocation() : ModuleCreationLocalize.labelModuleContentRoot(), myFileChooserController.getComponent());

    String projectOrModuleName = myContext.getName();
    String projectOrModulePath = myContext.getPath();

    myNameTextBox.setValue(projectOrModuleName);
    myFileChooserController.setValue(projectOrModulePath, false);

    myNameTextBox.addValueListener(event -> {
      if (myUserPathEntered) {
        return;
      }

      String name = myNameTextBox.getValue();
      String path = myFileChooserController.getValue().trim();
      int lastSeparatorIndex = lastSeparatorIndex(path);

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

  private static int lastSeparatorIndex(String path) {
    // a path of any machine
    return Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
  }

  @RequiredUIAccess
  protected void extend(FormBuilder builder, Disposable uiDisposable) {
    // for plugins
  }

  @Override
  public void onStepLeave(C c) {
    c.setName(getName());
    c.setPath(getPath());
    c.setPlatform(getPlatform());
  }

  @RequiredUIAccess
  public String getPath() {
    return myFileChooserController.getValue();
  }

  /**
   * The machine of the chosen location - the location may be chosen on any machine for a new project.
   */
  @RequiredUIAccess
  public Platform getPlatform() {
    Path path = myFileChooserController.getPathValue();
    if (path == null || path.getFileSystem() == FileSystems.getDefault()) {
      return myContext.isNewProject() ? Platform.current() : myContext.getPlatform();
    }
    PlatformTarget target = Application.get().getInstance(PlatformTargetManager.class).findTargetOf(path);
    Platform platform = target != null ? target.getPlatform() : null;
    return platform != null ? platform : myContext.getPlatform();
  }

  public String getName() {
    return myNameTextBox.getValueOrError();
  }

  @Override
  public @Nullable Component getPreferredFocusedComponent() {
    return myNameTextBox;
  }
}
