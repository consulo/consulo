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
package consulo.roots.ui.configuration.extension;

import com.intellij.openapi.roots.ModifiableRootModel;
import consulo.roots.ui.configuration.ExtensionEditor;
import com.intellij.openapi.roots.ui.configuration.ModuleConfigurationState;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.CheckedTreeNode;
import consulo.module.extension.ModuleExtension;
import consulo.module.extension.ModuleExtensionProviderEP;
import consulo.module.extension.MutableModuleExtension;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author VISTALL
 * @since 11:42/19.05.13
 */
public class ExtensionCheckedTreeNode extends CheckedTreeNode {
  private static class ExtensionProviderEPComparator implements Comparator<ExtensionCheckedTreeNode> {
    private static final Comparator<ExtensionCheckedTreeNode> INSTANCE = new ExtensionProviderEPComparator();

    @Override
    public int compare(ExtensionCheckedTreeNode o1, ExtensionCheckedTreeNode o2) {
      final ModuleExtensionProviderEP i1 = o1.myProviderEP;
      final ModuleExtensionProviderEP i2 = o2.myProviderEP;
      return StringUtil.compare(i1.getName(), i2.getName(), true);
    }
  }

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
        myExtension = model.getExtensionWithoutCheck(providerEP.getKey());
      }
    }

    setAllowsChildren(true);
    List<ExtensionCheckedTreeNode> child = new ArrayList<ExtensionCheckedTreeNode>();
    for (ModuleExtensionProviderEP ep : ModuleExtensionProviderEP.EP_NAME.getExtensions()) {
      if (Comparing.equal(ep.parentKey, parentKey)) {
        final ExtensionCheckedTreeNode e = new ExtensionCheckedTreeNode(ep, state, myExtensionEditor);
        e.setParent(this);

        child.add(e);
      }
    }
    Collections.sort(child, ExtensionProviderEPComparator.INSTANCE);
    setUserObject(myExtension);
    children = child.isEmpty() ? null : new Vector<ExtensionCheckedTreeNode>(child);
  }

  @Override
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
    if(myExtension == null) {
      return false;
    }
    if (myProviderEP != null && myProviderEP.allowMixin) {
      return true;
    }

    if(myProviderEP != null && myProviderEP.systemOnly) {
      return false;
    }

    final ModifiableRootModel rootModel = myState.getRootModel();
    if (rootModel == null) {
      return false;
    }

    final ModuleExtensionProviderEP absoluteParent = findParentWithoutParent(myExtension.getId());

    final ModuleExtension extension = rootModel.getExtension(absoluteParent.getKey());
    if (extension != null) {
      return true;
    }

    // if no nodes checked - it enabled
    for (ModuleExtensionProviderEP ep : ModuleExtensionProviderEP.EP_NAME.getExtensions()) {
      if (ep.parentKey != null) {
        continue;
      }
      final ModuleExtension tempExtension = rootModel.getExtension(ep.getKey());
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

  @Nullable
  public ModuleExtensionProviderEP getProviderEP() {
    return myProviderEP;
  }

  @Nullable
  public ModuleExtension getExtension() {
    return myExtension;
  }
}
