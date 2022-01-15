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
package consulo.compiler.options;

import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.compiler.options.ExcludedEntriesConfigurable;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import jakarta.inject.Inject;
import org.jetbrains.annotations.Nls;
import javax.annotation.Nullable;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 17:53/11.07.13
 */
public class CompilerExcludedConfigurable implements Configurable {
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
    return null;
  }
}
