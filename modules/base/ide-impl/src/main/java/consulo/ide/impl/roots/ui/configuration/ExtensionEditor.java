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
package consulo.ide.impl.roots.ui.configuration;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.ClasspathEditor;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.CompilerOutputsEditor;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.ContentEntriesEditor;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.ModuleElementsEditor;
import consulo.ide.setting.module.ModuleConfigurationState;
import consulo.language.psi.PsiPackageManager;
import consulo.language.psi.PsiPackageSupportProvider;
import consulo.localize.LocalizeValue;
import consulo.module.Module;
import consulo.module.content.layer.ModifiableModuleRootLayer;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.module.content.layer.ModuleExtensionProvider;
import consulo.module.content.layer.extension.ModuleExtensionBase;
import consulo.module.content.layer.orderEntry.ModuleExtensionWithSdkOrderEntry;
import consulo.module.extension.ModuleExtension;
import consulo.module.extension.ModuleExtensionWithSdk;
import consulo.module.extension.MutableModuleExtension;
import consulo.project.localize.ProjectLocalize;
import consulo.ui.CheckBoxTree;
import consulo.ui.CheckBoxTreePolicy;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.Space;
import consulo.ui.TextAttribute;
import consulo.ui.TextItemPresentation;
import consulo.ui.TreeModel;
import consulo.ui.TreeNode;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.Layout;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.layout.SplitLayoutPosition;
import consulo.ui.layout.TwoComponentSplitLayout;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2013-05-19
 */
public class ExtensionEditor extends ModuleElementsEditor {
    private static final Comparator<TreeNode<ModuleExtensionProvider>> NAME_COMPARATOR = (o1, o2) -> {
        ModuleExtensionProvider p1 = o1.getValue();
        ModuleExtensionProvider p2 = o2.getValue();
        if (p1 == null || p2 == null) {
            return 0;
        }
        return LocalizeValue.comparator().compare(p1.getName(), p2.getName());
    };

    private final ModuleConfigurationState myState;
    private final CompilerOutputsEditor myCompilerOutputEditor;
    private final ClasspathEditor myClasspathEditor;
    private final ContentEntriesEditor myContentEntriesEditor;

    private @Nullable CheckBoxTree<ModuleExtensionProvider> myTree;
    private @Nullable DockLayout myDetailsPanel;
    private @Nullable Disposable myDetailsDisposable;
    private @Nullable MutableModuleExtension<?> myShownExtension;
    private @Nullable ModuleExtensionProvider mySelectedProvider;
    private boolean myUpdatingChecks;

    public ExtensionEditor(
        ModuleConfigurationState state,
        CompilerOutputsEditor compilerOutputEditor,
        ClasspathEditor classpathEditor,
        ContentEntriesEditor contentEntriesEditor
    ) {
        super(state);
        myState = state;
        myCompilerOutputEditor = compilerOutputEditor;
        myClasspathEditor = classpathEditor;
        myContentEntriesEditor = contentEntriesEditor;

        registerDisposable(this::disposeDetails);
    }

    @RequiredUIAccess
    @Override
    public void moduleStateChanged() {
        CheckBoxTree<ModuleExtensionProvider> tree = myTree;
        if (tree == null) {
            return;
        }

        syncChecks(tree);
        onSelected(mySelectedProvider);

        UIAccess uiAccess = UIAccess.current();
        tree.refreshAll().whenCompleteAsync((ignored, error) -> {
            if (myTree == tree) {
                tree.expandAll().whenCompleteAsync((expanded, expandError) -> selectFirstEnabled(tree), uiAccess);
            }
        }, uiAccess);
    }

    @RequiredUIAccess
    @Override
    protected Component createUIComponentImpl(Disposable parentUIDisposable) {
        CheckBoxTree<ModuleExtensionProvider> tree = CheckBoxTree.create(new ExtensionTreeModel());
        tree.setCheckPolicy(CheckBoxTreePolicy.INDEPENDENT);
        tree.setCheckEnabledFilter(this::isCheckEnabled);
        tree.addCheckListener(event -> onChecked(event.getValue(), event.isChecked()));
        tree.addSelectListener(event -> {
            TreeNode<ModuleExtensionProvider> node = event.getValue();
            onSelected(node == null ? null : node.getValue());
        });
        Disposer.register(parentUIDisposable, tree.destroyHook());
        myTree = tree;

        syncChecks(tree);
        UIAccess uiAccess = UIAccess.current();
        tree.expandAll().whenCompleteAsync((ignored, error) -> selectFirstEnabled(tree), uiAccess);

        DockLayout detailsPanel = DockLayout.create(Space.NONE);
        myDetailsPanel = detailsPanel;

        TwoComponentSplitLayout splitLayout = TwoComponentSplitLayout.create(SplitLayoutPosition.HORIZONTAL);
        splitLayout.setFirstComponent(ScrollableLayout.create(tree));
        splitLayout.setSecondComponent(detailsPanel);
        splitLayout.setProportion(30);
        return splitLayout;
    }

    @RequiredUIAccess
    public void extensionChanged(MutableModuleExtension extension) {
        if (myShownExtension == extension) {
            if (!extension.isEnabled()) {
                showDetails(null);
            }
        }
        else if (extension.isEnabled() && mySelectedProvider != null && getExtension(mySelectedProvider) == extension) {
            showDetails(extension);
        }

        if (extension instanceof ModuleExtensionWithSdk sdkExtension) {
            ModifiableModuleRootLayer moduleRootLayer = (ModifiableModuleRootLayer) ((ModuleExtensionBase) sdkExtension).getModuleRootLayer();

            ModuleExtensionWithSdkOrderEntry sdkOrderEntry = moduleRootLayer.findModuleExtensionSdkEntry(sdkExtension);
            if (!sdkExtension.isEnabled() && sdkOrderEntry != null) {
                moduleRootLayer.removeOrderEntry(sdkOrderEntry);
            }

            if (sdkExtension.isEnabled()) {
                if (!sdkExtension.getInheritableSdk().isNull()) {
                    if (sdkOrderEntry == null) {
                        moduleRootLayer.addModuleExtensionSdkEntry(sdkExtension);
                    }
                    else {
                        ModuleExtensionWithSdk<?> moduleExtension = sdkOrderEntry.getModuleExtension();
                        if (moduleExtension != null
                            && !Objects.equals(sdkExtension.getInheritableSdk(), moduleExtension.getInheritableSdk())) {
                            moduleRootLayer.addModuleExtensionSdkEntry(sdkExtension);
                        }
                    }
                }
                else if (sdkOrderEntry != null) {
                    moduleRootLayer.removeOrderEntry(sdkOrderEntry);
                }
            }
        }

        myProject.getApplication().getExtensionPoint(PsiPackageSupportProvider.class).forEachExtensionSafe(supportProvider -> {
            Module module = extension.getModule();
            if (supportProvider.isSupported(extension)) {
                PsiPackageManager.getInstance(module.getProject()).dropCache(extension.getClass());
            }
        });

        myClasspathEditor.moduleStateChanged();
        myContentEntriesEditor.moduleStateChanged();
        myCompilerOutputEditor.moduleStateChanged();
    }

    @Override
    public void saveData() {
    }

    @Override
    public LocalizeValue getDisplayName() {
        return ProjectLocalize.moduleExtensionsTitle();
    }

    @RequiredUIAccess
    private void onSelected(@Nullable ModuleExtensionProvider provider) {
        mySelectedProvider = provider;

        MutableModuleExtension<?> extension = provider == null ? null : getExtension(provider);
        if (extension == null || !extension.isEnabled()) {
            showDetails(null);
        }
        else if (myShownExtension != extension) {
            showDetails(extension);
        }
    }

    @RequiredUIAccess
    private void onChecked(ModuleExtensionProvider provider, boolean checked) {
        CheckBoxTree<ModuleExtensionProvider> tree = myTree;
        if (myUpdatingChecks || tree == null) {
            return;
        }

        List<ModuleExtensionProvider> chain = checked ? collectParentChain(provider) : collectSubtree(provider);
        for (ModuleExtensionProvider target : chain) {
            MutableModuleExtension<?> extension = getExtension(target);
            if (extension != null && extension.isEnabled() != checked) {
                extension.setEnabled(checked);
                extensionChanged(extension);
            }
        }

        syncChecks(tree);
        refreshTree(tree);

        if (checked) {
            selectProvider(tree, provider);
        }
    }

    @RequiredUIAccess
    private void selectProvider(CheckBoxTree<ModuleExtensionProvider> tree, ModuleExtensionProvider provider) {
        TreeNode<ModuleExtensionProvider> rootNode = tree.getRootNode();
        if (rootNode == null) {
            return;
        }

        UIAccess uiAccess = UIAccess.current();
        rootNode.findChildDeep(candidate -> candidate == provider).whenCompleteAsync((node, error) -> {
            if (node != null && myTree == tree) {
                tree.select(node);
            }
        }, uiAccess);
    }

    @RequiredUIAccess
    private void selectFirstEnabled(CheckBoxTree<ModuleExtensionProvider> tree) {
        TreeNode<ModuleExtensionProvider> rootNode = tree.getRootNode();
        if (rootNode == null || myTree != tree || mySelectedProvider != null) {
            return;
        }

        UIAccess uiAccess = UIAccess.current();
        rootNode.findChildDeep(this::isEnabled).whenCompleteAsync((node, error) -> {
            if (node != null && myTree == tree && mySelectedProvider == null) {
                tree.select(node);
            }
        }, uiAccess);
    }

    private boolean isEnabled(@Nullable ModuleExtensionProvider provider) {
        MutableModuleExtension<?> extension = provider == null ? null : getExtension(provider);
        return extension != null && extension.isEnabled();
    }

    @RequiredUIAccess
    private void refreshTree(CheckBoxTree<ModuleExtensionProvider> tree) {
        UIAccess uiAccess = UIAccess.current();
        tree.refreshAll().whenCompleteAsync((ignored, error) -> {
            if (myTree == tree) {
                tree.expandAll();
            }
        }, uiAccess);
    }

    @RequiredUIAccess
    private void syncChecks(CheckBoxTree<ModuleExtensionProvider> tree) {
        myUpdatingChecks = true;
        try {
            for (ModuleExtensionProvider provider : getProviders().values()) {
                MutableModuleExtension<?> extension = getExtension(provider);
                tree.setChecked(provider, extension != null && extension.isEnabled());
            }
        }
        finally {
            myUpdatingChecks = false;
        }
    }

    @RequiredUIAccess
    private void showDetails(@Nullable MutableModuleExtension<?> extension) {
        DockLayout detailsPanel = myDetailsPanel;
        disposeDetails();
        myShownExtension = null;
        if (detailsPanel == null) {
            return;
        }

        Component component = null;
        if (extension != null) {
            Disposable uiDisposable = Disposable.newDisposable("module extension: " + extension.getId());
            myDetailsDisposable = uiDisposable;
            myShownExtension = extension;

            @RequiredUIAccess Runnable updateOnCheck = () -> extensionChanged(extension);
            component = extension.createConfigurationComponent(uiDisposable, updateOnCheck);
            if (component instanceof Layout) {
                component.borderBuilder().allReset().apply();
                component.paddingBuilder().allSet(Space.MEDIUM).apply();
            }
        }

        detailsPanel.center(component == null ? Label.create(LocalizeValue.empty()) : component);
    }

    private void disposeDetails() {
        Disposable detailsDisposable = myDetailsDisposable;
        myDetailsDisposable = null;
        if (detailsDisposable != null) {
            detailsDisposable.disposeWithTree();
        }
    }

    private @Nullable MutableModuleExtension<?> getExtension(ModuleExtensionProvider provider) {
        ModifiableRootModel model = myState.getRootModel();
        return model == null ? null : model.getExtensionWithoutCheck(provider.getId());
    }

    @RequiredUIAccess
    private boolean isCheckEnabled(ModuleExtensionProvider provider) {
        MutableModuleExtension<?> extension = getExtension(provider);
        if (extension == null) {
            return false;
        }
        if (provider.isAllowMixin()) {
            return true;
        }
        if (provider.isSystemOnly()) {
            return false;
        }

        ModifiableRootModel rootModel = myState.getRootModel();
        if (rootModel == null) {
            return false;
        }

        ModuleExtensionProvider absoluteParent = findRootProvider(provider);
        if (rootModel.getExtension(absoluteParent.getId()) != null) {
            return true;
        }

        for (ModuleExtensionProvider candidate : getProviders().values()) {
            if (candidate.getParentId() == null && !candidate.isAllowMixin() && rootModel.getExtension(candidate.getId()) != null) {
                return false;
            }
        }
        return true;
    }

    private List<ModuleExtensionProvider> collectParentChain(ModuleExtensionProvider provider) {
        Map<String, ModuleExtensionProvider> providers = getProviders();
        List<ModuleExtensionProvider> chain = new ArrayList<>();
        for (ModuleExtensionProvider current = provider; current != null; ) {
            chain.add(current);
            String parentId = current.getParentId();
            current = parentId == null ? null : providers.get(parentId);
        }
        Collections.reverse(chain);
        return chain;
    }

    private List<ModuleExtensionProvider> collectSubtree(ModuleExtensionProvider provider) {
        List<ModuleExtensionProvider> result = new ArrayList<>();
        result.add(provider);
        for (int i = 0; i < result.size(); i++) {
            String id = result.get(i).getId();
            for (ModuleExtensionProvider candidate : getProviders().values()) {
                if (id.equals(candidate.getParentId())) {
                    result.add(candidate);
                }
            }
        }
        return result;
    }

    private ModuleExtensionProvider findRootProvider(ModuleExtensionProvider provider) {
        List<ModuleExtensionProvider> chain = collectParentChain(provider);
        return chain.get(0);
    }

    private Map<String, ModuleExtensionProvider> getProviders() {
        Map<String, ModuleExtensionProvider> providers = new LinkedHashMap<>();
        for (ModuleExtensionProvider provider : myProject.getApplication().getExtensionPoint(ModuleExtensionProvider.class).getExtensionList()) {
            providers.put(provider.getId(), provider);
        }
        return providers;
    }

    @RequiredUIAccess
    private void render(ModuleExtensionProvider provider, TextItemPresentation presentation) {
        boolean enabled = isCheckEnabled(provider);
        Image icon = provider.getIcon();
        presentation.withIcon(enabled ? icon : ImageEffects.transparent(icon));
        presentation.append(provider.getName(), enabled ? TextAttribute.REGULAR : TextAttribute.GRAYED);
    }

    private class ExtensionTreeModel implements TreeModel<ModuleExtensionProvider> {
        @RequiredUIAccess
        @Override
        public void buildChildren(
            Function<ModuleExtensionProvider, TreeNode<ModuleExtensionProvider>> nodeFactory,
            @Nullable ModuleExtensionProvider parentValue
        ) {
            String parentId = parentValue == null ? null : parentValue.getId();
            Map<String, ModuleExtensionProvider> providers = getProviders();
            for (ModuleExtensionProvider provider : providers.values()) {
                if (!Objects.equals(provider.getParentId(), parentId)) {
                    continue;
                }

                boolean hasChildren = false;
                for (ModuleExtensionProvider candidate : providers.values()) {
                    if (provider.getId().equals(candidate.getParentId())) {
                        hasChildren = true;
                        break;
                    }
                }

                TreeNode<ModuleExtensionProvider> node = nodeFactory.apply(provider);
                node.setLeaf(!hasChildren);
                node.setRenderer(ExtensionEditor.this::render);
            }
        }

        @Override
        public @Nullable Comparator<TreeNode<ModuleExtensionProvider>> getNodeComparator() {
            return NAME_COMPARATOR;
        }
    }
}
