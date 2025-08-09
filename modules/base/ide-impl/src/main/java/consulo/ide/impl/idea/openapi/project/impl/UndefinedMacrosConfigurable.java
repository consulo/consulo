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
package consulo.ide.impl.idea.openapi.project.impl;

import consulo.configurable.Configurable;
import consulo.configurable.ConfigurationException;
import consulo.ide.impl.idea.application.options.pathMacros.PathMacroListEditor;
import consulo.localize.LocalizeValue;
import consulo.project.localize.ProjectLocalize;
import consulo.ui.ex.awt.IdeBorderFactory;
import consulo.ui.ex.awt.internal.laf.MultiLineLabelUI;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;

/**
 * @author Eugene Zhuravlev
 * @since 2004-12-04
 */
public class UndefinedMacrosConfigurable implements Configurable{
  private PathMacroListEditor myEditor;
  private final String myText;
  private final Collection<String> myUndefinedMacroNames;

  public UndefinedMacrosConfigurable(String text, Collection<String> undefinedMacroNames) {
    myText = text;
    myUndefinedMacroNames = undefinedMacroNames;
  }

  @Override
  public LocalizeValue getDisplayName() {
    return ProjectLocalize.projectConfigurePathVariablesTitle();
  }

  @Override
  public JComponent createComponent() {
    JPanel mainPanel = new JPanel(new BorderLayout());
    // important: do not allow to remove or change macro name for already defined macros befor project is loaded
    myEditor = new PathMacroListEditor(myUndefinedMacroNames);
    JComponent editorPanel = myEditor.getPanel();

    mainPanel.add(editorPanel, BorderLayout.CENTER);

    JLabel textLabel = new JLabel(myText);
    textLabel.setUI(new MultiLineLabelUI());
    textLabel.setBorder(IdeBorderFactory.createEmptyBorder(6, 6, 6, 6));
    mainPanel.add(textLabel, BorderLayout.NORTH);

    return mainPanel;
  }

  @Override
  public boolean isModified() {
    return myEditor.isModified();
  }

  @Override
  public void apply() throws ConfigurationException {
    myEditor.commit();
  }

  @Override
  public void reset() {
    myEditor.reset();
  }

  @Override
  public void disposeUIResources() {
    myEditor = null;
  }
}
