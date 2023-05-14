/*
 * Copyright 2013-2016 consulo.io
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
package consulo.ide.impl.compiler.setting;

import consulo.annotation.component.ExtensionImpl;
import consulo.compiler.CompilerBundle;
import consulo.compiler.CompilerManager;
import consulo.configurable.ConfigurationException;
import consulo.configurable.ProjectConfigurable;
import consulo.configurable.StandardConfigurableIds;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.project.Project;
import jakarta.inject.Inject;
import org.jetbrains.annotations.Nls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;

/**
 * @author VISTALL
 * @since 17:53/11.07.13
 */
@ExtensionImpl
public class CompilerExcludedConfigurable implements ProjectConfigurable {
  private ExcludedEntriesConfigurable myConfigurable;

  @Inject
  public CompilerExcludedConfigurable(final Project project) {
    CompilerManager compilerManager = CompilerManager.getInstance(project);

    myConfigurable = new ExcludedEntriesConfigurable(project, new FileChooserDescriptor(true, true, false, false, false, true),
                                                     compilerManager.getExcludedEntriesConfiguration());
  }

  @Nullable
  @Override
  public JComponent createComponent() {
    return myConfigurable.createComponent();
  }

  @Override
  public boolean isModified() {
    return myConfigurable.isModified();
  }

  @Override
  public void apply() throws ConfigurationException {
    myConfigurable.apply();
  }

  @Override
  public void reset() {
    myConfigurable.reset();
  }

  @Override
  public void disposeUIResources() {
    myConfigurable.disposeUIResources();
  }

  @Nls
  @Override
  public String getDisplayName() {
    return CompilerBundle.message("actions.exclude.from.compile.text");
  }

  @Nonnull
  @Override
  public String getId() {
    return "project.propCompiler.excluded";
  }

  @Nullable
  @Override
  public String getParentId() {
    return StandardConfigurableIds.COMPILER_GROUP;
  }
}
