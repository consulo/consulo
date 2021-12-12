/*
 * Copyright 2013-2016 consulo.io
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
package consulo.roots.ui.configuration;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModuleExtensionWithSdkOrderEntry;
import com.intellij.openapi.roots.ui.configuration.*;
import com.intellij.openapi.ui.Splitter;
import com.intellij.openapi.util.Comparing;
import com.intellij.ui.CheckboxTreeNoPolicy;
import com.intellij.ui.CheckedTreeNode;
import com.intellij.ui.OnePixelSplitter;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.tree.TreeUtil;
import consulo.awt.TargetAWT;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.logging.Logger;
import consulo.module.extension.ModuleExtension;
import consulo.module.extension.ModuleExtensionWithSdk;
import consulo.module.extension.MutableModuleExtension;
import consulo.module.extension.swing.SwingMutableModuleExtension;
import consulo.psi.PsiPackageManager;
import consulo.psi.PsiPackageSupportProvider;
import consulo.roots.ModifiableModuleRootLayer;
import consulo.roots.ui.configuration.extension.ExtensionCheckedTreeNode;
import consulo.roots.ui.configuration.extension.ExtensionTreeCellRenderer;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.border.BorderStyle;
import consulo.ui.layout.Layout;
import org.jetbrains.annotations.Nls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * @author VISTALL
 * @since 10:33/19.05.13
 */
public class ExtensionEditor extends ModuleElementsEditor {
  private static final Logger LOG = Logger.getInstance(ExtensionEditor.class);

  private final ModuleConfigurationState myState;
  private final CompilerOutputsEditor myCompilerOutputEditor;
  private final ClasspathEditor myClasspathEditor;
  private final ContentEntriesEditor myContentEntriesEditor;
  private CheckboxTreeNoPolicy myTree;
  private Splitter mySplitter;

  private ModuleExtension<?> myConfigurablePanelExtension;

  private final Map<JComponent, Disposable> myExtensionDisposables = new HashMap<>();

  public ExtensionEditor(ModuleConfigurationState state, CompilerOutputsEditor compilerOutputEditor, ClasspathEditor classpathEditor, ContentEntriesEditor contentEntriesEditor) {
    super(state);
    myState = state;
    myCompilerOutputEditor = compilerOutputEditor;
    myClasspathEditor = classpathEditor;
    myContentEntriesEditor = contentEntriesEditor;

    registerDisposable(() -> {
      for (Disposable disposable : myExtensionDisposables.values()) {
        Disposer.dispose(disposable);
      }

      myExtensionDisposables.clear();
    });
  }

  @Override
  public void moduleStateChanged() {
    mySplitter.setSecondComponent(null);
    myTree.setModel(new DefaultTreeModel(new ExtensionCheckedTreeNode(null, myState, this)));
    TreeUtil.expandAll(myTree);
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  protected JComponent createComponentImpl(@Nonnull Disposable parentUIDisposable) {
    JPanel rootPane = new JPanel(new BorderLayout());

    mySplitter = new OnePixelSplitter();

    myTree = new CheckboxTreeNoPolicy(new ExtensionTreeCellRenderer(), new ExtensionCheckedTreeNode(null, myState, this)) {
      @Override
      protected void adjustParentsAndChildren(CheckedTreeNode node, boolean checked) {
        if (!checked) {
          changeNodeState(node, false);
          checkOrUncheckChildren(node, false);
        }
        else {
          // we need collect parents, and enable it in right order
          // A
          // - B
          // -- C
          // when we enable C, ill be calls like A -> B -> C
          List<CheckedTreeNode> parents = new ArrayList<>();
          TreeNode parent = node.getParent();
          while (parent != null) {
            if (parent instanceof CheckedTreeNode) {
              parents.add((CheckedTreeNode)parent);
            }
            parent = parent.getParent();
          }

          Collections.reverse(parents);
          for (CheckedTreeNode checkedTreeNode : parents) {
            checkNode(checkedTreeNode, true);
          }
          changeNodeState(node, true);
        }
        repaint();
      }
    };

    myTree.setRootVisible(false);
    myTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    myTree.addTreeSelectionListener(new TreeSelectionListener() {
      @Override
      @RequiredUIAccess
      public void valueChanged(final TreeSelectionEvent e) {
        final List<MutableModuleExtension> selected = TreeUtil.collectSelectedObjectsOfType(myTree, MutableModuleExtension.class);
        updateSecondComponent(ContainerUtil.<MutableModuleExtension>getFirstItem(selected));
      }
    });
    TreeUtil.expandAll(myTree);

    mySplitter.setFirstComponent(myTree);

    rootPane.add(ScrollPaneFactory.createScrollPane(mySplitter), BorderLayout.CENTER);

    return rootPane;
  }

  @Nullable
  @RequiredUIAccess
  @SuppressWarnings("deprecation")
  private JComponent createConfigurationPanel(final @Nonnull MutableModuleExtension extension) {
    myConfigurablePanelExtension = extension;
    @RequiredUIAccess Runnable updateOnCheck = () -> extensionChanged(extension);

    Disposable uiDisposable = Disposable.newDisposable("module extension: " + extension.getId());

    JComponent result = null;

    if (extension instanceof SwingMutableModuleExtension) {
      result = ((SwingMutableModuleExtension)extension).createConfigurablePanel(uiDisposable, updateOnCheck);
    }
    else {
      Component component = extension.createConfigurationComponent(uiDisposable, updateOnCheck);

      if (component != null) {
        if (component instanceof Layout) {
          component.removeBorders();

          component.addBorders(BorderStyle.EMPTY, null, 5);
        }

        result = (JComponent)TargetAWT.to(component);
      }
    }

    if (result != null) {
      myExtensionDisposables.put(result, uiDisposable);
    }

    return result;
  }

  @RequiredUIAccess
  private void updateSecondComponent(@Nullable MutableModuleExtension extension) {
    JComponent oldComponent;
    if (extension == null || !extension.isEnabled()) {
      oldComponent = mySplitter.replaceSecondComponent(null);
    }
    else {
      oldComponent = mySplitter.replaceSecondComponent(createConfigurationPanel(extension));
    }

    if (oldComponent != null) {
      Disposable disposable = myExtensionDisposables.remove(oldComponent);
      if (disposable != null) {
        Disposer.dispose(disposable);
      }
    }
  }

  @RequiredUIAccess
  public void extensionChanged(MutableModuleExtension extension) {
    final JComponent secondComponent = myConfigurablePanelExtension != extension ? null : mySplitter.getSecondComponent();
    if (secondComponent == null && extension.isEnabled() || secondComponent != null && !extension.isEnabled()) {
      updateSecondComponent(!extension.isEnabled() ? null : extension);
    }

    if (extension instanceof ModuleExtensionWithSdk) {
      // we using module layer, dont use modifiable model - it ill proxy, and methods 'addModuleExtensionSdkEntry' && 'removeOrderEntry'
      // ill call this method again
      ModifiableModuleRootLayer moduleRootLayer = extension.getModuleRootLayer();

      final ModuleExtensionWithSdkOrderEntry sdkOrderEntry = moduleRootLayer.findModuleExtensionSdkEntry(extension);
      if (!extension.isEnabled() && sdkOrderEntry != null) {
        moduleRootLayer.removeOrderEntry(sdkOrderEntry);
      }

      if (extension.isEnabled()) {
        final ModuleExtensionWithSdk sdkExtension = (ModuleExtensionWithSdk)extension;
        if (!sdkExtension.getInheritableSdk().isNull()) {
          if (sdkOrderEntry == null) {
            moduleRootLayer.addModuleExtensionSdkEntry(sdkExtension);
          }
          else {
            final ModuleExtensionWithSdk<?> moduleExtension = sdkOrderEntry.getModuleExtension();
            if (moduleExtension != null && !Comparing.equal(sdkExtension.getInheritableSdk(), moduleExtension.getInheritableSdk())) {
              moduleRootLayer.addModuleExtensionSdkEntry(sdkExtension);
            }
          }
        }
        else if (sdkOrderEntry != null) {
          moduleRootLayer.removeOrderEntry(sdkOrderEntry);
        }
      }
    }

    for (PsiPackageSupportProvider supportProvider : PsiPackageSupportProvider.EP_NAME.getExtensionList()) {
      final Module module = extension.getModule();
      if (supportProvider.isSupported(extension)) {
        PsiPackageManager.getInstance(module.getProject()).dropCache(extension.getClass());
      }
    }

    myClasspathEditor.moduleStateChanged();
    myContentEntriesEditor.moduleStateChanged();
    myCompilerOutputEditor.moduleStateChanged();
  }

  @Override
  public void saveData() {

  }

  @Nls
  @Override
  public String getDisplayName() {
    return "Extensions";
  }
}
