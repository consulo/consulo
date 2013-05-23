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

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ui.configuration.extension.ExtensionCheckedTreeNode;
import com.intellij.openapi.roots.ui.configuration.extension.ExtensionTreeCellRenderer;
import com.intellij.ui.CheckboxTree;
import com.intellij.ui.CheckboxTreeBase;
import com.intellij.ui.JBSplitter;
import com.intellij.util.ui.tree.TreeUtil;
import org.consulo.module.extension.ModuleExtensionWithSdk;
import org.consulo.module.extension.MutableModuleExtension;
import org.consulo.psi.PsiPackageManager;
import org.consulo.psi.PsiPackageSupportProvider;
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
  private JBSplitter mySplitter;

  public ExtensionEditor(ModuleConfigurationState state, ClasspathEditor e) {
    super(state);
    myState = state;
    myClasspathEditor = e;
  }

  @NotNull
  @Override
  protected JComponent createComponentImpl() {
    myRootPane = new JPanel(new BorderLayout());

    mySplitter = new JBSplitter();

    myTree = new CheckboxTree(new ExtensionTreeCellRenderer(), new ExtensionCheckedTreeNode(null, myState, this),
                              new CheckboxTreeBase.CheckPolicy(false, true, true, false));
    myTree.setRootVisible(false);
    myTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    myTree.addTreeSelectionListener(new TreeSelectionListener() {
      @Override
      public void valueChanged(final TreeSelectionEvent e) {
        final List<MutableModuleExtension> selected = TreeUtil.collectSelectedObjectsOfType(myTree, MutableModuleExtension.class);
        mySplitter.setSecondComponent(null);

        if (!selected.isEmpty()) {
          final MutableModuleExtension extension = selected.get(0);
          if (!extension.isEnabled()) {
            return;
          }

          mySplitter.setSecondComponent(createConfigurationPanel(extension));
        }
      }
    });

    mySplitter.setFirstComponent(myTree);

    myRootPane.add(mySplitter, BorderLayout.CENTER);

    return myRootPane;
  }

  @Nullable
  private JComponent createConfigurationPanel(final @NotNull MutableModuleExtension<?> extension) {
    return extension.createConfigurablePanel(new Runnable() {
      @Override
      public void run() {
        extensionChanged(extension);
      }
    });
  }

  public void extensionChanged(MutableModuleExtension<?> extension) {
    if (!extension.isEnabled()) {
      mySplitter.setSecondComponent(null);
    }
    else {
      mySplitter.setSecondComponent(createConfigurationPanel(extension));
    }

    if (extension instanceof ModuleExtensionWithSdk) {
      myClasspathEditor.moduleStateChanged();
    }

    for (PsiPackageSupportProvider supportProvider : PsiPackageSupportProvider.EP_NAME.getExtensions()) {
      final Module module = extension.getModule();
      if (supportProvider.getSupportedModuleExtensionClass() == extension.getClass()) {
        PsiPackageManager.getInstance(module.getProject()).dropCache(extension.getClass());
      }
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
