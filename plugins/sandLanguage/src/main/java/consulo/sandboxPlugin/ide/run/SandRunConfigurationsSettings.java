/*
 * Copyright 2013-2020 consulo.io
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
package consulo.sandboxPlugin.ide.run;

import com.intellij.execution.configurations.RunConfigurationsSettings;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.UnnamedConfigurable;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;

/**
 * @author VISTALL
 * @since 2020-05-28
 *
 * just example for usage {@link RunConfigurationsSettings}
 */
public class SandRunConfigurationsSettings implements RunConfigurationsSettings {
  private static class TestConfiguration implements UnnamedConfigurable {

    @RequiredUIAccess
    @Nullable
    @Override
    public JComponent createComponent() {
      JPanel panel = new JPanel();
      panel.add(new JLabel("test label"));
      return panel;
    }

    @RequiredUIAccess
    @Override
    public boolean isModified() {
      return false;
    }

    @RequiredUIAccess
    @Override
    public void apply() throws ConfigurationException {

    }
  }

  @Nonnull
  @Override
  public UnnamedConfigurable createConfigurable() {
    return new TestConfiguration();
  }
}
