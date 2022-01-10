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
package com.intellij.compiler.options;

import com.intellij.compiler.CompilerWorkspaceConfiguration;
import com.intellij.openapi.compiler.CompilerBundle;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.NotNullComputable;
import consulo.compiler.CompilationType;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.options.SimpleConfigurable;
import consulo.ui.CheckBox;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.util.LabeledBuilder;
import jakarta.inject.Inject;

import javax.annotation.Nonnull;

public class CompilerConfigurable extends SimpleConfigurable<CompilerConfigurable.Root> implements Configurable {
  protected static class Root implements NotNullComputable<Component> {
    private ComboBox<CompilationType> myCompilerOptions;
    private CheckBox myCbClearOutputDirectory;
    private CheckBox myCbAutoShowFirstError;

    private VerticalLayout myLayout;

    @RequiredUIAccess
    public Root() {
      myLayout = VerticalLayout.create();

      myCompilerOptions = ComboBox.<CompilationType>builder().fillByEnum(CompilationType.class, Enum::name).build();
      Component compilerOptions = LabeledBuilder.sided(LocalizeValue.localizeTODO("Compilation type:"), myCompilerOptions);
      myLayout.add(compilerOptions);
      compilerOptions.setVisible(false);

      myCbClearOutputDirectory = CheckBox.create(CompilerBundle.message("label.option.clear.output.directory.on.rebuild"));
      myLayout.add(myCbClearOutputDirectory);

      myCbAutoShowFirstError = CheckBox.create(CompilerBundle.message("label.option.autoshow.first.error"));
      myLayout.add(myCbAutoShowFirstError);
    }

    @Nonnull
    @Override
    public Component compute() {
      return myLayout;
    }
  }

  private final CompilerWorkspaceConfiguration myCompilerWorkspaceConfiguration;

  @Inject
  public CompilerConfigurable(Project project) {
    myCompilerWorkspaceConfiguration = CompilerWorkspaceConfiguration.getInstance(project);
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  protected Root createPanel(Disposable uiDisposable) {
    return new Root();
  }

  @RequiredUIAccess
  @Override
  protected boolean isModified(@Nonnull Root component) {
    boolean isModified = !Comparing.equal(component.myCompilerOptions.getValue(), myCompilerWorkspaceConfiguration.COMPILATION_TYPE);
    isModified |= component.myCbClearOutputDirectory.getValue() != myCompilerWorkspaceConfiguration.CLEAR_OUTPUT_DIRECTORY;
    isModified |= component.myCbAutoShowFirstError.getValue() != myCompilerWorkspaceConfiguration.AUTO_SHOW_ERRORS_IN_EDITOR;
    return isModified;
  }

  @RequiredUIAccess
  @Override
  protected void reset(@Nonnull Root component) {
    component.myCompilerOptions.setValue(myCompilerWorkspaceConfiguration.COMPILATION_TYPE);
    component.myCbAutoShowFirstError.setValue(myCompilerWorkspaceConfiguration.AUTO_SHOW_ERRORS_IN_EDITOR);
    component.myCbClearOutputDirectory.setValue(myCompilerWorkspaceConfiguration.CLEAR_OUTPUT_DIRECTORY);
  }

  @RequiredUIAccess
  @Override
  protected void apply(@Nonnull Root component) throws ConfigurationException {
    myCompilerWorkspaceConfiguration.COMPILATION_TYPE = component.myCompilerOptions.getValue();
    myCompilerWorkspaceConfiguration.AUTO_SHOW_ERRORS_IN_EDITOR = component.myCbAutoShowFirstError.getValue();
    myCompilerWorkspaceConfiguration.CLEAR_OUTPUT_DIRECTORY = component.myCbClearOutputDirectory.getValue();
  }

  @Override
  public String getDisplayName() {
    return CompilerBundle.message("compiler.configurable.display.name");
  }
}
