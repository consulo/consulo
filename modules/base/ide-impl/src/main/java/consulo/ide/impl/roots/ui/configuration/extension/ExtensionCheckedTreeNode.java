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
import consulo.ide.impl.roots.ui.configuration.ExtensionEditor;
import consulo.ide.setting.module.ModuleConfigurationState;
import consulo.localize.LocalizeValue;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.module.content.layer.ModuleExtensionProvider;
import consulo.module.extension.ModuleExtension;
import consulo.module.extension.MutableModuleExtension;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.tree.CheckedTreeNode;
import consulo.util.lang.Comparing;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.tree.TreeNode;
import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

/**
 * @author VISTALL
 * @since 2013-05-19
 */
public class ExtensionCheckedTreeNode extends CheckedTreeNode {
    private static final Comparator<TreeNode> CASE_INSENSITIVE_EXTENSION_PROVIDER_NAME_COMPARATOR =
        Comparator.comparing(tn -> ((ExtensionCheckedTreeNode) tn).myProvider.getName(), LocalizeValue.CASE_INSENSITIVE_ORDER);

    private final ModuleExtensionProvider myProvider;
    @Nonnull
    private final ModuleConfigurationState myState;
    private final ExtensionEditor myExtensionEditor;
    private MutableModuleExtension<?> myExtension;

    public ExtensionCheckedTreeNode(
        @Nullable ModuleExtensionProvider moduleExtensionProvider,
        @Nonnull ModuleConfigurationState state,
        ExtensionEditor extensionEditor
    ) {
        super(null);
        myProvider = moduleExtensionProvider;
        myState = state;
        myExtensionEditor = extensionEditor;

        String parentId = null;
        if (moduleExtensionProvider != null) {
            parentId = moduleExtensionProvider.getId();

            ModifiableRootModel model = state.getRootModel();
            if (model != null) {
                myExtension = model.getExtensionWithoutCheck(moduleExtensionProvider.getId());
            }
        }

        setAllowsChildren(true);
        Vector<TreeNode> child = new Vector<>();
        for (ModuleExtensionProvider ep : Application.get().getExtensionPoint(ModuleExtensionProvider.class).getExtensionList()) {
            if (Comparing.equal(ep.getParentId(), parentId)) {
                ExtensionCheckedTreeNode e = new ExtensionCheckedTreeNode(ep, state, myExtensionEditor);
                e.setParent(this);

                child.add(e);
            }
        }
        Collections.sort(child, CASE_INSENSITIVE_EXTENSION_PROVIDER_NAME_COMPARATOR);
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
        // If extension is not found, don't allow to manage it.
        if (myExtension == null) {
            return false;
        }
        if (myProvider != null && myProvider.isAllowMixin()) {
            return true;
        }

        if (myProvider != null && myProvider.isSystemOnly()) {
            return false;
        }

        ModifiableRootModel rootModel = myState.getRootModel();
        if (rootModel == null) {
            return false;
        }

        ModuleExtensionProvider absoluteParent = findParentWithoutParent(myExtension.getId());

        ModuleExtension extension = rootModel.getExtension(absoluteParent.getId());
        if (extension != null) {
            return true;
        }

        // If no nodes checked - it's enabled
        return Application.get().getExtensionPoint(ModuleExtensionProvider.class).allMatchSafe(ep -> {
            if (ep.getParentId() != null) {
                return true;
            }
            ModuleExtension tempExtension = rootModel.getExtension(ep.getId());
            return tempExtension == null;
        });
    }

    @Nonnull
    private static ModuleExtensionProvider findParentWithoutParent(String id) {
        ModuleExtensionProvider provider = Application.get().getExtensionPoint(ModuleExtensionProvider.class).computeSafeIfAny(ep -> {
            if (!ep.getId().equals(id)) {
                return null;
            }
            if (ep.getParentId() == null) {
                return ep;
            }
            else {
                return findParentWithoutParent(ep.getParentId());
            }
        });
        if (provider == null) {
            throw new IllegalArgumentException("Cant find for id: " + id);
        }
        return provider;
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
