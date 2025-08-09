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
package consulo.ide.impl.idea.ide.actionMacro;

import consulo.configurable.Configurable;
import consulo.configurable.ConfigurationException;
import consulo.disposer.Disposer;
import consulo.ide.localize.IdeLocalize;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;

import javax.swing.*;

/**
 * @author max
 * @since 2003-07-21
 */
public class ActionMacroConfigurable implements Configurable {
  private ActionMacroConfigurationPanel myPanel;

  @Override
  public LocalizeValue getDisplayName() {
    return IdeLocalize.titleEditMacros();
  }

  @RequiredUIAccess
  @Override
  public JComponent createComponent() {
    myPanel = new ActionMacroConfigurationPanel();
    return myPanel.getPanel();
  }

  @RequiredUIAccess
  @Override
  public void apply() throws ConfigurationException {
    myPanel.apply();
  }

  @RequiredUIAccess
  @Override
  public void reset() {
    myPanel.reset();
  }

  @RequiredUIAccess
  @Override
  public boolean isModified() {
    return myPanel.isModified();
  }

  @RequiredUIAccess
  @Override
  public void disposeUIResources() {
    Disposer.dispose(myPanel);
    myPanel = null;
  }
}
