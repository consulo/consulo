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

import com.intellij.compiler.CompilationType;
import com.intellij.compiler.CompilerConfiguration;
import com.intellij.openapi.compiler.CompilerBundle;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.ui.components.JBLabel;

import javax.swing.*;
import java.awt.*;

public class CompilerConfigurable implements Configurable {
  private JComboBox myComboBox;
  private CompilerConfiguration myCompilerConfiguration;

  public CompilerConfigurable(Project project) {
    myCompilerConfiguration = CompilerConfiguration.getInstance(project);
  }

  @Override
  public String getDisplayName() {
    return CompilerBundle.message("compiler.configurable.display.name");
  }

  @Override
  public String getHelpTopic() {
    return "project.propCompiler";
  }

  @Override
  public JComponent createComponent() {
    myComboBox = new JComboBox(CompilationType.VALUES);
    myComboBox.setSelectedItem(myCompilerConfiguration.getCompilationType());

    JPanel mainPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

    JPanel temp = new JPanel(new BorderLayout());
    temp.add(new JBLabel("Compilation type: "), BorderLayout.WEST);
    temp.add(myComboBox, BorderLayout.CENTER);

    mainPanel.add(temp);
    return mainPanel;
  }

  @Override
  public boolean isModified() {
    if(!Comparing.equal(myComboBox.getSelectedItem(), myCompilerConfiguration.getCompilationType())) {
      return true;
    }
    return false;
  }

  @Override
  public void apply() throws ConfigurationException {
    myCompilerConfiguration.setCompilationType((CompilationType)myComboBox.getSelectedItem());
  }

  @Override
  public void reset() {
    myComboBox.setSelectedItem(myCompilerConfiguration.getCompilationType());
  }

  @Override
  public void disposeUIResources() {

  }
}
