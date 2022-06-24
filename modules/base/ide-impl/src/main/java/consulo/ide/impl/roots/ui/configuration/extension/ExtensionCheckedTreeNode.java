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
package consulo.ide.impl.roots.ui.configuration.extension;

import consulo.application.Application;
import consulo.ide.impl.idea.openapi.util.Comparing;
import consulo.ide.impl.idea.ui.CheckedTreeNode;
import consulo.ide.impl.roots.ui.configuration.ExtensionEditor;
import consulo.ide.setting.module.ModuleConfigurationState;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.module.content.layer.ModuleExtensionProvider;
import consulo.module.extension.ModuleExtension;
import consulo.module.extension.MutableModuleExtension;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.tree.TreeNode;
import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

/**
 * @author VISTALL
 * @since 11:42/19.05.13
 */
public class ExtensionCheckedTreeNode extends CheckedTreeNode {
  private static class ExtensionProviderEPComparator implements Comparator<TreeNode> {
    private static final Comparator<TreeNode> INSTANCE = new ExtensionProviderEPComparator();

    @Override
    public int compare(TreeNode o1, TreeNode o2) {
      final ModuleExtensionProvider i1 = ((ExtensionCheckedTreeNode)o1).myProvider;
      final ModuleExtensionProvider i2 = ((ExtensionCheckedTreeNode)o2).myProvider;
      return i1.getName().compareIgnoreCase(i2.getName());
    }
  }

  private final ModuleExtensionProvider myProvider;
  @Nonnull
  private final ModuleConfigurationState myState;
  private final ExtensionEditor myExtensionEditor;
  private MutableModuleExtension<?> myExtension;

  public ExtensionCheckedTreeNode(@Nullable ModuleExtensionProvider moduleExtensionProvider, @Nonnull ModuleConfigurationState state, ExtensionEditor extensionEditor) {
    super(null);
    myProvider = moduleExtensionProvider;
    myState = state;
    myExtensionEditor = extensionEditor;

    String parentId = null;
    if (moduleExtensionProvider != null) {
      parentId = moduleExtensionProvider.getId();

      final ModifiableRootModel model = state.getRootModel();
      if (model != null) {
        myExtension = model.getExtensionWithoutCheck(moduleExtensionProvider.getId());
      }
    }

    setAllowsChildren(true);
    Vector<TreeNode> child = new Vector<>();
    for (ModuleExtensionProvider ep : Application.get().getExtensionPoint(ModuleExtensionProvider.class).getExtensionList()) {
      if (Comparing.equal(ep.getParentId(), parentId)) {
        final ExtensionCheckedTreeNode e = new ExtensionCheckedTreeNode(ep, state, myExtensionEditor);
        e.setParent(this);

        child.add(e);
      }
    }
    Collections.sort(child, ExtensionProviderEPComparator.INSTANCE);
    setUserObject(myExtension);
    // at java 9 children is Vector<TreeNode>()
    //noinspection Convert2Diamond
    children = child.isEmpty() ? null : child;
  }

  @Override
  @RequiredUIAccess
  public void setChecked(boolean enabled) {
    if (myExtension == null) {
      return;
    }
    myExtension.setEnabled(enabled);
    myExtensionEditor.extensionChanged(myExtension);
  }

  @Override
  public boolean isChecked() {
    return myExtension != null && myExtension.isEnabled();
  }

  @Override
  public boolean isEnabled() {
    // if extension not found dont allow manage it
    if (myExtension == null) {
      return false;
    }
    if (myProvider != null && myProvider.isAllowMixin()) {
      return true;
    }

    if (myProvider != null && myProvider.isSystemOnly()) {
      return false;
    }

    final ModifiableRootModel rootModel = myState.getRootModel();
    if (rootModel == null) {
      return false;
    }

    final ModuleExtensionProvider absoluteParent = findParentWithoutParent(myExtension.getId());

    final ModuleExtension extension = rootModel.getExtension(absoluteParent.getId());
    if (extension != null) {
      return true;
    }

    // if no nodes checked - it enabled
    for (ModuleExtensionProvider ep : Application.get().getExtensionPoint(ModuleExtensionProvider.class).getExtensionList()) {
      if (ep.getParentId() != null) {
        continue;
      }
      final ModuleExtension tempExtension = rootModel.getExtension(ep.getId());
      if (tempExtension != null) {
        return false;
      }
    }
    return true;
  }

  @Nonnull
  private static ModuleExtensionProvider findParentWithoutParent(String id) {
    for (ModuleExtensionProvider ep : Application.get().getExtensionPoint(ModuleExtensionProvider.class).getExtensionList()) {
      if (ep.getId().equals(id)) {
        if (ep.getParentId() == null) {
          return ep;
        }
        else {
          return findParentWithoutParent(ep.getParentId());
        }
      }
    }
    throw new IllegalArgumentException("Cant find for id: " + id);
  }

  @Nullable
  public ModuleExtensionProvider getProvider() {
    return myProvider;
  }

  @Nullable
  public ModuleExtension getExtension() {
    return myExtension;
  }
}
