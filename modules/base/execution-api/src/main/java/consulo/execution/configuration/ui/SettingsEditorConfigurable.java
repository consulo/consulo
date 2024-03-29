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
package consulo.execution.configuration.ui;

import consulo.configurable.BaseConfigurable;
import consulo.configurable.ConfigurationException;
import consulo.disposer.Disposer;
import consulo.execution.configuration.ui.event.SettingsEditorListener;
import consulo.ui.annotation.RequiredUIAccess;

import javax.swing.*;

public abstract class SettingsEditorConfigurable<Settings> extends BaseConfigurable {
  private SettingsEditor<Settings> myEditor;
  private final Settings mySettings;
  private final SettingsEditorListener<Settings> myListener;
  private final JComponent myComponent;

  public SettingsEditorConfigurable(SettingsEditor<Settings> editor, Settings settings) {
    myEditor = editor;
    mySettings = settings;
    myListener = settingsEditor -> setModified(true);
    myEditor.addSettingsEditorListener(myListener);
    myComponent = myEditor.getComponent();
  }

  @RequiredUIAccess
  @Override
  public JComponent createComponent() {
    return myComponent;
  }

  @RequiredUIAccess
  @Override
  public void apply() throws ConfigurationException {
    myEditor.applyTo(mySettings);
    setModified(false);
  }

  @RequiredUIAccess
  @Override
  public void reset() {
    myEditor.resetFrom(mySettings);
    setModified(false);
  }

  @RequiredUIAccess
  @Override
  public void disposeUIResources() {
    if (myEditor != null) {
      myEditor.removeSettingsEditorListener(myListener);
      Disposer.dispose(myEditor);
    }
    myEditor = null;
  }

  public SettingsEditor<Settings> getEditor() {
    return myEditor;
  }

  public Settings getSettings() {
    return mySettings;
  }
}
