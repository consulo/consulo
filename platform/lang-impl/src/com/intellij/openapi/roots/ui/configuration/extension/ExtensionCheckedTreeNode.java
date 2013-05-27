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
package com.intellij.openapi.roots.ui.configuration.extension;

import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleExtensionWithSdkOrderEntry;
import com.intellij.openapi.roots.ui.configuration.ExtensionEditor;
import com.intellij.openapi.roots.ui.configuration.ModuleConfigurationState;
import com.intellij.openapi.util.Comparing;
import com.intellij.ui.CheckedTreeNode;
import org.consulo.module.extension.ModuleExtension;
import org.consulo.module.extension.ModuleExtensionProviderEP;
import org.consulo.module.extension.ModuleExtensionWithSdk;
import org.consulo.module.extension.MutableModuleExtension;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Vector;

/**
 * @author VISTALL
 * @since 11:42/19.05.13
 */
public class ExtensionCheckedTreeNode extends CheckedTreeNode {
  private final ModuleExtensionProviderEP myProviderEP;
  @NotNull private final ModuleConfigurationState myState;
  private final ExtensionEditor myExtensionEditor;
  private MutableModuleExtension<?> myExtension;

  public ExtensionCheckedTreeNode(@Nullable ModuleExtensionProviderEP providerEP,
                                  @NotNull ModuleConfigurationState state,
                                  ExtensionEditor extensionEditor) {
    super(null);
    myProviderEP = providerEP;
    myState = state;
    myExtensionEditor = extensionEditor;

    String parentKey = null;
    if (providerEP != null) {
      parentKey = providerEP.key;

      final ModifiableRootModel model = state.getRootModel();
      if (model != null) {
        myExtension = (MutableModuleExtension)model.getExtensionWithoutCheck(providerEP.getInstance().getImmutableClass());
      }
    }

    setAllowsChildren(true);
    Vector<ExtensionCheckedTreeNode> child = new Vector<ExtensionCheckedTreeNode>();
    for (ModuleExtensionProviderEP ep : ModuleExtensionProviderEP.EP_NAME.getExtensions()) {
      if (Comparing.equal(ep.parentKey, parentKey)) {
        final ExtensionCheckedTreeNode e = new ExtensionCheckedTreeNode(ep, state, myExtensionEditor);
        e.setParent(this);

        child.add(e);
      }
    }

    setUserObject(myExtension);
    children = child.isEmpty() ? null : child;
  }

  @Override
  public void setChecked(boolean enabled) {
    if (myExtension == null) {
      return;
    }
    myExtension.setEnabled(enabled);
    if (myExtension instanceof ModuleExtensionWithSdk) {
      final ModifiableRootModel rootModel = myState.getRootModel();
      if (rootModel == null) {
        return;
      }

      final ModuleExtensionWithSdkOrderEntry sdkOrderEntry = rootModel.findModuleExtensionSdkEntry(myExtension);
      if (sdkOrderEntry != null) {
        rootModel.removeOrderEntry(sdkOrderEntry);
      }
      if (enabled) {
        rootModel.addModuleExtensionSdkEntry((ModuleExtensionWithSdk)myExtension);
      }
      myExtensionEditor.extensionChanged(myExtension);
    }
  }

  @Override
  public boolean isChecked() {
    return myExtension == null || myExtension.isEnabled();
  }

  @Override
  public boolean isEnabled() {
    if (myExtension == null) {
      return true;
    }

    final ModifiableRootModel rootModel = myState.getRootModel();
    if (rootModel == null) {
      return false;
    }

    final ModuleExtensionProviderEP absoluteParent = findParentWithoutParent(myExtension.getId());

    final ModuleExtension extension = rootModel.getExtension(absoluteParent.getInstance().getImmutableClass());
    if (extension != null) {
      return true;
    }

    // if no nodes checked - it enabled
    for (ModuleExtensionProviderEP ep : ModuleExtensionProviderEP.EP_NAME.getExtensions()) {
      if (ep.parentKey != null) {
        continue;
      }
      final ModuleExtension tempExtension = rootModel.getExtension(ep.getInstance().getImmutableClass());
      if (tempExtension != null) {
        return false;
      }
    } return true;
  }

  @NotNull
  private static ModuleExtensionProviderEP findParentWithoutParent(String id) {
    for (ModuleExtensionProviderEP ep : ModuleExtensionProviderEP.EP_NAME.getExtensions()) {
      if (ep.key.equals(id)) {
        if (ep.parentKey == null) {
          return ep;
        }
        else {
          return findParentWithoutParent(ep.parentKey);
        }
      }
    }
    throw new IllegalArgumentException("Cant find for id: " + id);
  }

  public ModuleExtensionProviderEP getProviderEP() {
    return myProviderEP;
  }

  public ModuleExtension getExtension() {
    return myExtension;
  }
}
