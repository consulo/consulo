/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.sourceItems;

import consulo.application.Application;
import consulo.compiler.artifact.Artifact;
import consulo.compiler.artifact.ArtifactType;
import consulo.compiler.artifact.ui.ArtifactEditorContext;
import consulo.compiler.artifact.ui.PackagingSourceItem;
import consulo.compiler.artifact.ui.PackagingSourceItemsProvider;
import consulo.compiler.artifact.ui.SourceItemPresentation;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.ArtifactEditorImpl;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.ArtifactTreeContextMenu;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.ArtifactTreePresentations;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.sourceItems.actions.ExtractIntoDefaultLocationAction;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.sourceItems.actions.PackAndPutIntoDefaultLocationAction;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.sourceItems.actions.PutSourceItemIntoDefaultLocationAction;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.sourceItems.actions.SourceItemNavigateAction;
import consulo.ui.Component;
import consulo.ui.DragAndDropTransferHandler;
import consulo.ui.Tree;
import consulo.ui.TreeExecutor;
import consulo.ui.TreeModel;
import consulo.ui.TreeNode;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.clipboard.DataTransfer;
import consulo.ui.clipboard.DataTransferType;
import consulo.ui.ex.TreeExpander;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.CommonActionsManager;
import consulo.ui.ex.tree.PresentationData;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * @author nik
 */
public class SourceItemsTree implements Disposable {
    public static final DataTransferType<List<PackagingSourceItem>> SOURCE_ITEMS = DataTransferType.create("consulo.artifact.source.items");

    private static final Comparator<SourceItemPresentation> COMPARATOR = (o1, o2) -> {
        int weights = o2.getWeight() - o1.getWeight();
        if (weights != 0) {
            return weights;
        }
        return StringUtil.compare(o1.getPresentableName(), o2.getPresentableName(), true);
    };

    private final ArtifactEditorContext myEditorContext;
    private final ArtifactEditorImpl myArtifactsEditor;
    private final Map<PackagingSourceItem, SourceItemPresentation> myPresentations = new HashMap<>();

    private @Nullable Tree<PackagingSourceItem> myTree;

    public SourceItemsTree(ArtifactEditorContext editorContext, ArtifactEditorImpl artifactsEditor) {
        myEditorContext = editorContext;
        myArtifactsEditor = artifactsEditor;
    }

    @RequiredUIAccess
    public Component createUIComponent(Disposable uiDisposable) {
        Tree<PackagingSourceItem> tree = Tree.create(new SourceItemsTreeModel(), TreeExecutor.uiThread());
        tree.setAllowMultipleSelect(true);
        tree.setSpeedSearchConverter(node -> {
            SourceItemPresentation presentation = getPresentation(node.getValue());
            return presentation == null ? "" : presentation.getSearchName();
        });
        tree.setTransferHandler(new SourceItemsDragAndDropHandler());
        Disposer.register(uiDisposable, tree.destroyHook());
        myTree = tree;

        ArtifactTreeContextMenu.install(tree, createPopupGroup(tree));
        return tree;
    }

    private ActionGroup createPopupGroup(Tree<PackagingSourceItem> tree) {
        TreeExpander treeExpander = TreeExpander.of(tree);
        CommonActionsManager actionsManager = CommonActionsManager.getInstance();
        return ActionGroup.newImmutableBuilder()
            .add(new PutSourceItemIntoDefaultLocationAction(this, myArtifactsEditor))
            .add(new PackAndPutIntoDefaultLocationAction(this, myArtifactsEditor))
            .add(new ExtractIntoDefaultLocationAction(this, myArtifactsEditor))
            .addSeparator()
            .add(new SourceItemNavigateAction(this))
            .addSeparator()
            .add(actionsManager.createExpandAllAction(treeExpander))
            .add(actionsManager.createCollapseAllAction(treeExpander))
            .build();
    }

    public @Nullable Tree<PackagingSourceItem> getTree() {
        return myTree;
    }

    @RequiredUIAccess
    public void rebuildTree() {
        Tree<PackagingSourceItem> tree = myTree;
        if (tree != null) {
            tree.refreshAll();
        }
    }

    public @Nullable SourceItemPresentation getPresentation(@Nullable PackagingSourceItem item) {
        return item == null ? null : myPresentations.get(item);
    }

    public List<PackagingSourceItem> getSelectedSourceItems() {
        Tree<PackagingSourceItem> tree = myTree;
        if (tree == null) {
            return List.of();
        }

        List<PackagingSourceItem> items = new ArrayList<>();
        for (TreeNode<PackagingSourceItem> node : tree.getSelectedNodes()) {
            PackagingSourceItem item = node.getValue();
            if (item != null) {
                items.add(item);
            }
        }
        return items;
    }

    public List<PackagingSourceItem> getSelectedItems() {
        return getProvidingItems(getSelectedSourceItems());
    }

    private static List<PackagingSourceItem> getProvidingItems(List<PackagingSourceItem> items) {
        List<PackagingSourceItem> result = new ArrayList<>();
        for (PackagingSourceItem item : items) {
            if (item.isProvideElements()) {
                result.add(item);
            }
        }
        return result;
    }

    @Override
    @RequiredUIAccess
    public void dispose() {
        myPresentations.clear();
        myTree = null;
    }

    private class SourceItemsTreeModel implements TreeModel<PackagingSourceItem> {
        @Override
        @RequiredUIAccess
        public void buildChildren(Function<PackagingSourceItem, TreeNode<PackagingSourceItem>> nodeFactory, @Nullable PackagingSourceItem parentValue) {
            Artifact artifact = myArtifactsEditor.getArtifact();
            ArtifactType artifactType = artifact.getArtifactType();

            List<SourceItemEntry> entries = new ArrayList<>();
            Application.get().getExtensionPoint(PackagingSourceItemsProvider.class).forEach(provider -> {
                for (PackagingSourceItem item : provider.getSourceItems(myEditorContext, artifact, parentValue)) {
                    if (artifactType.isSuitableItem(item)) {
                        entries.add(new SourceItemEntry(item, item.createPresentation(myEditorContext)));
                    }
                }
            });
            entries.sort((o1, o2) -> COMPARATOR.compare(o1.presentation(), o2.presentation()));

            for (SourceItemEntry entry : entries) {
                SourceItemPresentation presentation = entry.presentation();
                myPresentations.put(entry.item(), presentation);

                PresentationData presentationData = ArtifactTreePresentations.createPresentationData(presentation);
                String tooltip = StringUtil.notNullize(presentation.getTooltipText());

                TreeNode<PackagingSourceItem> node = nodeFactory.apply(entry.item());
                node.setRenderer((value, itemPresentation) -> ArtifactTreePresentations.render(presentationData, tooltip, false, itemPresentation));
            }
        }

        @Override
        public boolean isNeedBuildChildrenBeforeOpen(TreeNode<PackagingSourceItem> node) {
            return true;
        }

        @Override
        @RequiredUIAccess
        public boolean onDoubleClick(Tree<PackagingSourceItem> tree, TreeNode<PackagingSourceItem> node) {
            PackagingSourceItem item = node.getValue();
            if (item != null && item.isProvideElements() && node.isLeaf()) {
                myArtifactsEditor.getLayoutTreeComponent().putIntoDefaultLocations(List.of(item));
                return false;
            }
            return true;
        }
    }

    private class SourceItemsDragAndDropHandler implements DragAndDropTransferHandler<TreeNode<PackagingSourceItem>> {
        @Override
        public @Nullable DataTransfer createTransfer(Component component) {
            return null;
        }

        @Override
        public @Nullable DataTransfer createDragTransfer(Component component, List<TreeNode<PackagingSourceItem>> items, boolean move) {
            List<PackagingSourceItem> sourceItems = new ArrayList<>();
            for (TreeNode<PackagingSourceItem> node : items) {
                PackagingSourceItem item = node.getValue();
                if (item != null) {
                    sourceItems.add(item);
                }
            }

            List<PackagingSourceItem> providing = getProvidingItems(sourceItems);
            if (providing.isEmpty()) {
                return null;
            }

            StringBuilder text = new StringBuilder();
            for (PackagingSourceItem item : providing) {
                SourceItemPresentation presentation = getPresentation(item);
                if (presentation != null) {
                    if (text.length() > 0) {
                        text.append('\n');
                    }
                    text.append(presentation.getPresentableName());
                }
            }

            return DataTransfer.builder().put(SOURCE_ITEMS, List.copyOf(providing)).put(DataTransferType.TEXT, text.toString()).build();
        }

        @Override
        public boolean drop(Component component, DropContext<TreeNode<PackagingSourceItem>> context) {
            return false;
        }
    }

    private record SourceItemEntry(PackagingSourceItem item, SourceItemPresentation presentation) {
    }
}
