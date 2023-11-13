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
package consulo.compiler.impl.internal;

import consulo.annotation.component.ExtensionImpl;
import consulo.compiler.CompilerBundle;
import consulo.configurable.ConfigurationException;
import consulo.configurable.ProjectConfigurable;
import consulo.configurable.SimpleConfigurable;
import consulo.configurable.StandardConfigurableIds;
import consulo.disposer.Disposable;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.VerticalLayout;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import jakarta.annotation.Nonnull;
import java.util.function.Supplier;

@ExtensionImpl
public class CompilerConfigurable extends SimpleConfigurable<CompilerConfigurable.Root> implements ProjectConfigurable {
  protected static class Root implements Supplier<Component> {
    private CheckBox myCbClearOutputDirectory;
    private CheckBox myCbAutoShowFirstError;

    private VerticalLayout myLayout;

    @RequiredUIAccess
    public Root() {
      myLayout = VerticalLayout.create();

      myCbClearOutputDirectory = CheckBox.create(CompilerBundle.message("label.option.clear.output.directory.on.rebuild"));
      myLayout.add(myCbClearOutputDirectory);

      myCbAutoShowFirstError = CheckBox.create(CompilerBundle.message("label.option.autoshow.first.error"));
      myLayout.add(myCbAutoShowFirstError);
    }

    @Nonnull
    @Override
    public Component get() {
      return myLayout;
    }
  }

  private final Provider<CompilerWorkspaceConfiguration> myCompilerWorkspaceConfiguration;

  @Inject
  public CompilerConfigurable(Provider<CompilerWorkspaceConfiguration> compilerWorkspaceConfiguration) {
    myCompilerWorkspaceConfiguration = compilerWorkspaceConfiguration;
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  protected Root createPanel(@Nonnull Disposable uiDisposable) {
    return new Root();
  }

  @RequiredUIAccess
  @Override
  protected boolean isModified(@Nonnull Root component) {
    CompilerWorkspaceConfiguration compilerWorkspaceConfiguration = myCompilerWorkspaceConfiguration.get();

    boolean isModified = component.myCbClearOutputDirectory.getValue() != compilerWorkspaceConfiguration.CLEAR_OUTPUT_DIRECTORY;
    isModified |= component.myCbAutoShowFirstError.getValue() != compilerWorkspaceConfiguration.AUTO_SHOW_ERRORS_IN_EDITOR;
    return isModified;
  }

  @RequiredUIAccess
  @Override
  protected void reset(@Nonnull Root component) {
    CompilerWorkspaceConfiguration compilerWorkspaceConfiguration = myCompilerWorkspaceConfiguration.get();

    component.myCbAutoShowFirstError.setValue(compilerWorkspaceConfiguration.AUTO_SHOW_ERRORS_IN_EDITOR);
    component.myCbClearOutputDirectory.setValue(compilerWorkspaceConfiguration.CLEAR_OUTPUT_DIRECTORY);
  }

  @RequiredUIAccess
  @Override
  protected void apply(@Nonnull Root component) throws ConfigurationException {
    CompilerWorkspaceConfiguration compilerWorkspaceConfiguration = myCompilerWorkspaceConfiguration.get();

    compilerWorkspaceConfiguration.AUTO_SHOW_ERRORS_IN_EDITOR = component.myCbAutoShowFirstError.getValue();
    compilerWorkspaceConfiguration.CLEAR_OUTPUT_DIRECTORY = component.myCbClearOutputDirectory.getValue();
  }

  @Nonnull
  @Override
  public String getDisplayName() {
    return CompilerBundle.message("compiler.configurable.display.name");
  }

  @Nonnull
  @Override
  public String getId() {
    return StandardConfigurableIds.COMPILER_GROUP;
  }
}
