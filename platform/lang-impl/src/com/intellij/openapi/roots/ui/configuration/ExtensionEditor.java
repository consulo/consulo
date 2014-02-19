/*
 * Copyright 2013 must-be.org
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
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleExtensionWithSdkOrderEntry;
import com.intellij.openapi.roots.ui.configuration.extension.ExtensionCheckedTreeNode;
import com.intellij.openapi.roots.ui.configuration.extension.ExtensionTreeCellRenderer;
import com.intellij.openapi.util.Comparing;
import com.intellij.ui.CheckboxTree;
import com.intellij.ui.CheckboxTreeBase;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.components.JBScrollPane;
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
  private final ContentEntriesEditor myContentEntriesEditor;
  private JPanel myRootPane;
  private CheckboxTree myTree;
  private JBSplitter mySplitter;

  public ExtensionEditor(ModuleConfigurationState state, ClasspathEditor classpathEditor, ContentEntriesEditor contentEntriesEditor) {
    super(state);
    myState = state;
    myClasspathEditor = classpathEditor;
    myContentEntriesEditor = contentEntriesEditor;
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
    TreeUtil.expandAll(myTree);

    mySplitter.setFirstComponent(myTree);

    myRootPane.add(new JBScrollPane(mySplitter), BorderLayout.CENTER);

    return myRootPane;
  }

  @Nullable
  private JComponent createConfigurationPanel(final @NotNull MutableModuleExtension<?> extension) {
    return extension.createConfigurablePanel(myState.getRootModel(), new Runnable() {
      @Override
      public void run() {
        extensionChanged(extension);
      }
    });
  }

  public void extensionChanged(MutableModuleExtension<?> extension) {
    final JComponent secondComponent = mySplitter.getSecondComponent();
    if (secondComponent == null && extension.isEnabled() || secondComponent != null && !extension.isEnabled()) {
      if (!extension.isEnabled()) {
        mySplitter.setSecondComponent(null);
      }
      else {
        mySplitter.setSecondComponent(createConfigurationPanel(extension));
      }
    }

    final ModifiableRootModel rootModel = myState.getRootModel();
    assert rootModel != null;

    if (extension instanceof ModuleExtensionWithSdk) {
      final ModuleExtensionWithSdkOrderEntry sdkOrderEntry = rootModel.findModuleExtensionSdkEntry(extension);
      if (!extension.isEnabled() && sdkOrderEntry != null) {
        rootModel.removeOrderEntry(sdkOrderEntry);
      }

      if (extension.isEnabled()) {
        final ModuleExtensionWithSdk sdkExtension = (ModuleExtensionWithSdk)extension;
        if (!sdkExtension.getInheritableSdk().isNull()) {
          if (sdkOrderEntry == null) {
            rootModel.addModuleExtensionSdkEntry(sdkExtension);
          }
          else {
            final ModuleExtensionWithSdk<?> moduleExtension = sdkOrderEntry.getModuleExtension();
            if (moduleExtension != null && !Comparing.equal(sdkExtension.getInheritableSdk(), moduleExtension.getInheritableSdk())) {
              rootModel.addModuleExtensionSdkEntry(sdkExtension);
            }
          }
        }
      }
    }

    for (PsiPackageSupportProvider supportProvider : PsiPackageSupportProvider.EP_NAME.getExtensions()) {
      final Module module = extension.getModule();
      if (supportProvider.getSupportedModuleExtensionClass().isAssignableFrom(extension.getClass())) {
        PsiPackageManager.getInstance(module.getProject()).dropCache(extension.getClass());
      }
    }

    myClasspathEditor.moduleStateChanged();
    myContentEntriesEditor.moduleStateChanged();
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
