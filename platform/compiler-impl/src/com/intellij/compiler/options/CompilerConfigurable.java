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
import com.intellij.compiler.CompilerWorkspaceConfiguration;
import com.intellij.openapi.compiler.CompilerBundle;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;

import javax.swing.*;

public class CompilerConfigurable implements Configurable {
  private final CompilerConfiguration myCompilerConfiguration;
  private final CompilerWorkspaceConfiguration myCompilerWorkspaceConfiguration;

  private JComboBox myCompilerOptions;
  private JPanel myRootPanel;
  private JCheckBox myCbClearOutputDirectory;
  private JCheckBox myCbAutoShowFirstError;

  public CompilerConfigurable(Project project) {
    myCompilerConfiguration = CompilerConfiguration.getInstance(project);
    myCompilerWorkspaceConfiguration = CompilerWorkspaceConfiguration.getInstance(project);
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
    return myRootPanel;
  }

  @Override
  public boolean isModified() {
    boolean isModified = !Comparing.equal(myCompilerOptions.getSelectedItem(), myCompilerConfiguration.getCompilationType());
    isModified |= ComparingUtils.isModified(myCbClearOutputDirectory, myCompilerWorkspaceConfiguration.CLEAR_OUTPUT_DIRECTORY);
    isModified |= ComparingUtils.isModified(myCbAutoShowFirstError, myCompilerWorkspaceConfiguration.AUTO_SHOW_ERRORS_IN_EDITOR);
    return isModified;
  }

  @Override
  public void apply() throws ConfigurationException {
    myCompilerConfiguration.setCompilationType((CompilationType)myCompilerOptions.getSelectedItem());
    myCompilerWorkspaceConfiguration.AUTO_SHOW_ERRORS_IN_EDITOR = myCbAutoShowFirstError.isSelected();
    myCompilerWorkspaceConfiguration.CLEAR_OUTPUT_DIRECTORY = myCbClearOutputDirectory.isSelected();
  }

  @Override
  public void reset() {
    myCompilerOptions.setSelectedItem(myCompilerConfiguration.getCompilationType());
    myCbAutoShowFirstError.setSelected(myCompilerWorkspaceConfiguration.AUTO_SHOW_ERRORS_IN_EDITOR);
    myCbClearOutputDirectory.setSelected(myCompilerWorkspaceConfiguration.CLEAR_OUTPUT_DIRECTORY);
  }

  @Override
  public void disposeUIResources() {

  }

  private void createUIComponents() {
    myCompilerOptions = new JComboBox(CompilationType.VALUES);
  }
}
