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

import org.consulo.module.extension.ModuleExtension;
import org.consulo.module.extension.ModuleExtensionProviderEP;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ui.configuration.ModuleConfigurationState;
import com.intellij.openapi.util.Comparing;
import com.intellij.ui.CheckedTreeNode;
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
  private MutableModuleExtension<?> myExtension;

  public ExtensionCheckedTreeNode(@Nullable ModuleExtensionProviderEP providerEP, @NotNull ModuleConfigurationState state) {
    super(null);
    myProviderEP = providerEP;

    String parentKey = null;
    if(providerEP != null) {
      parentKey = providerEP.key;

      final ModifiableRootModel model = state.getRootModel();
      if(model != null) {
        myExtension = (MutableModuleExtension) model.getExtensionWithoutCheck(providerEP.getInstance().getImmutableClass());
      }
    }

    setAllowsChildren(true);
    Vector<ExtensionCheckedTreeNode> child = new Vector<ExtensionCheckedTreeNode>();
    for (ModuleExtensionProviderEP ep : ModuleExtensionProviderEP.EP_NAME.getExtensions()) {
      if (Comparing.equal(ep.parentKey, parentKey)) {
        final ExtensionCheckedTreeNode e = new ExtensionCheckedTreeNode(ep, state);
        e.setParent(this);

        child.add(e);
      }
    }

    children = child.isEmpty() ? null : child;
  }

  @Override
  public void setChecked(boolean enabled) {
    if(myExtension == null) {
      return;
    }
    myExtension.setEnabled(enabled);
  }

  @Override
  public boolean isChecked() {
    return myExtension == null || myExtension.isEnabled();
  }

  public ModuleExtensionProviderEP getProviderEP() {
    return myProviderEP;
  }

  public ModuleExtension getExtension() {
    return myExtension;
  }
}
