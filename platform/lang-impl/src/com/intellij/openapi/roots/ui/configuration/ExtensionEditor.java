/*
 * Copyright 2013 Consulo.org
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
package com.intellij.openapi.roots.ui.configuration;

import com.intellij.openapi.roots.ui.configuration.extension.ExtensionCheckedTreeNode;
import com.intellij.openapi.roots.ui.configuration.extension.ExtensionTreeCellRenderer;
import com.intellij.ui.CheckboxTree;
import com.intellij.ui.CheckboxTreeBase;
import com.intellij.ui.JBSplitter;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 10:33/19.05.13
 */
public class ExtensionEditor extends ModuleElementsEditor {
  private final ModuleConfigurationState myState;

  public ExtensionEditor(ModuleConfigurationState state) {
    super(state);
    myState = state;
  }

  @NotNull
  @Override
  protected JComponent createComponentImpl() {
    JPanel rootPane = new JPanel(new BorderLayout());

    JBSplitter splitter = new JBSplitter();
    splitter.setSplitterProportionKey(getClass().getName());

    CheckboxTree tree = new CheckboxTree(new ExtensionTreeCellRenderer(), new ExtensionCheckedTreeNode(null, myState),
                                         new CheckboxTreeBase.CheckPolicy(true, true, false, true));
    tree.setRootVisible(false);

    splitter.setFirstComponent(tree);

    JPanel configPanel = new JPanel(new BorderLayout());

    splitter.setSecondComponent(configPanel);

    rootPane.add(splitter, BorderLayout.CENTER);
    return rootPane;
  }

  @Override
  public void saveData() {

  }

  @Nls
  @Override
  public String getDisplayName() {
    return "Extensions";
  }

  @Nullable
  @Override
  public String getHelpTopic() {
    return null;
  }
}
