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
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.tree.TreeUtil;
import org.consulo.module.extension.MutableModuleExtension;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.util.List;

/**
 * @author VISTALL
 * @since 10:33/19.05.13
 */
public class ExtensionEditor extends ModuleElementsEditor {
  private final ModuleConfigurationState myState;
  private final ClasspathEditor myClasspathEditor;
  private JPanel myRootPane;
  private CheckboxTree myTree;

  public ExtensionEditor(ModuleConfigurationState state, ClasspathEditor e) {
    super(state);
    myState = state;
    myClasspathEditor = e;
  }

  @NotNull
  @Override
  protected JComponent createComponentImpl() {
    myRootPane = new JPanel(new BorderLayout());

    myTree = new CheckboxTree(new ExtensionTreeCellRenderer(), new ExtensionCheckedTreeNode(null, myState, myClasspathEditor),
                                               new CheckboxTreeBase.CheckPolicy(true, true, false, true));
    myTree.setRootVisible(false);
    myTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    myTree.addTreeSelectionListener(new TreeSelectionListener() {
      @Override
      public void valueChanged(final TreeSelectionEvent e) {
        final List<MutableModuleExtension> selected = TreeUtil.collectSelectedObjectsOfType(myTree, MutableModuleExtension.class);
        if (selected.isEmpty()) {
          return;
        }

        UIUtil.invokeLaterIfNeeded(new Runnable() {
          @Override
          public void run() {
            myRootPane.removeAll();
            myRootPane.add(createPanel(selected.get(0)));
          }
        });
      }
    });


    myRootPane.add(createPanel(null), BorderLayout.CENTER);

    return myRootPane;
  }

  private JComponent createPanel(MutableModuleExtension<?> extension) {
    final JComponent configurablePanel = extension == null ? null : extension.createConfigurablePanel(new Runnable() {
      @Override
      public void run() {
        myClasspathEditor.moduleStateChanged();
      }
    });
    if (configurablePanel == null) {
      return myTree;
    }
    else {
      final JBSplitter splitter = new JBSplitter();
      splitter.setSplitterProportionKey(getClass().getName());
      splitter.setFirstComponent(myTree);
      splitter.setSecondComponent(configurablePanel);
      return splitter;
    }
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
