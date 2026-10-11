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
package consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts;

import consulo.compiler.artifact.Artifact;
import consulo.compiler.artifact.ArtifactType;
import consulo.compiler.artifact.ArtifactUtil;
import consulo.compiler.artifact.element.ArtifactRootElement;
import consulo.compiler.artifact.element.ComplexPackagingElement;
import consulo.compiler.artifact.element.CompositePackagingElement;
import consulo.compiler.artifact.element.DirectoryPackagingElement;
import consulo.compiler.artifact.element.PackagingElement;
import consulo.compiler.artifact.element.PackagingElementFactory;
import consulo.compiler.artifact.element.PackagingElementType;
import consulo.compiler.artifact.ui.ArtifactEditorContext;
import consulo.compiler.artifact.ui.PackagingElementPropertiesPanel;
import consulo.compiler.artifact.ui.PackagingSourceItem;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.sourceItems.SourceItemsTree;
import consulo.localize.LocalizeValue;
import consulo.project.localize.ProjectLocalize;
import consulo.ui.Component;
import consulo.ui.DragAndDropTransferHandler;
import consulo.ui.MessageBoxes;
import consulo.ui.Space;
import consulo.ui.Tree;
import consulo.ui.TreeExecutor;
import consulo.ui.TreeModel;
import consulo.ui.TreeNode;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.clipboard.DataTransfer;
import consulo.ui.clipboard.DataTransferType;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class LayoutTreeComponent implements Disposable {
    public static final DataTransferType<List<PackagingElement<?>>> LAYOUT_ELEMENTS =
        DataTransferType.create("consulo.artifact.layout.elements");

    private static final Comparator<LayoutElementInfo> SORT_COMPARATOR = (o1, o2) -> {
        int weights = o2.getPresentation().getWeight() - o1.getPresentation().getWeight();
        if (weights != 0) {
            return weights;
        }
        return StringUtil.compare(o1.getPresentation().getPresentableName(), o2.getPresentation().getPresentableName(), true);
    };

    private final ArtifactEditorImpl myArtifactsEditor;
    private final ComplexElementSubstitutionParameters mySubstitutionParameters;
    private final ArtifactEditorContext myContext;
    private final Artifact myOriginalArtifact;
    private boolean mySortElements;

    private final Map<PackagingElement<?>, LayoutElementInfo> myInfos = new IdentityHashMap<>();
    private final Map<PackagingElement<?>, TreeNode<PackagingElement<?>>> myTreeNodes = new IdentityHashMap<>();
    private List<ArtifactProblem> myProblems = List.of();

    private @Nullable Tree<PackagingElement<?>> myTree;
    private @Nullable DockLayout myPropertiesPanel;
    private @Nullable Disposable myUIDisposable;

    private @Nullable PackagingElement<?> mySelectedElement;
    private @Nullable PackagingElementPropertiesPanel myCurrentPropertiesPanel;
    private boolean myProblemsUpdateQueued;

    public LayoutTreeComponent(
        ArtifactEditorImpl artifactsEditor,
        ComplexElementSubstitutionParameters substitutionParameters,
        ArtifactEditorContext context,
        Artifact originalArtifact,
        boolean sortElements
    ) {
        myArtifactsEditor = artifactsEditor;
        mySubstitutionParameters = substitutionParameters;
        myContext = context;
        myOriginalArtifact = originalArtifact;
        mySortElements = sortElements;
    }

    @RequiredUIAccess
    public Component createUIComponent(Disposable uiDisposable) {
        myUIDisposable = uiDisposable;

        Tree<PackagingElement<?>> tree = Tree.create(new LayoutTreeModel(), TreeExecutor.uiThread());
        tree.setAllowMultipleSelect(true);
        tree.setSpeedSearchConverter(node -> {
            LayoutElementInfo info = getInfo(node.getValue());
            return info == null ? "" : info.getPresentation().getSearchName();
        });
        tree.addSelectListener(event -> updatePropertiesPanel(false));
        tree.setTransferHandler(new LayoutTreeDragAndDropHandler());
        Disposer.register(uiDisposable, tree.destroyHook());
        myTree = tree;

        DockLayout propertiesPanel = DockLayout.create(Space.NONE);
        propertiesPanel.setVisible(false);
        myPropertiesPanel = propertiesPanel;

        DockLayout panel = DockLayout.create(Space.NONE);
        panel.center(ScrollableLayout.create(tree));
        panel.bottom(propertiesPanel);

        rebuildTree();
        return panel;
    }

    public @Nullable Tree<PackagingElement<?>> getTree() {
        return myTree;
    }

    public Artifact getArtifact() {
        return myArtifactsEditor.getArtifact();
    }

    public boolean isSortElements() {
        return mySortElements;
    }

    @RequiredUIAccess
    public void setSortElements(boolean sortElements) {
        mySortElements = sortElements;
        myArtifactsEditor.getContext().getParent().getDefaultSettings().setSortElements(sortElements);
        rebuildTree();
    }

    public @Nullable LayoutElementInfo getInfo(@Nullable PackagingElement<?> value) {
        return value == null ? null : myInfos.get(value);
    }

    public @Nullable LayoutElementInfo getParentNode(LayoutElementInfo node) {
        return getInfo(node.getParentValue());
    }

    public @Nullable LayoutElementInfo getRootNode() {
        for (LayoutElementInfo info : myInfos.values()) {
            if (info.isRoot()) {
                return info;
            }
        }
        return null;
    }

    public LayoutTreeSelection getSelection() {
        Tree<PackagingElement<?>> tree = myTree;
        if (tree == null) {
            return new LayoutTreeSelection(List.of());
        }
        return new LayoutTreeSelection(getInfos(tree.getSelectedNodes()));
    }

    private List<LayoutElementInfo> getInfos(List<TreeNode<PackagingElement<?>>> nodes) {
        List<LayoutElementInfo> infos = new ArrayList<>();
        for (TreeNode<PackagingElement<?>> node : nodes) {
            LayoutElementInfo info = getInfo(node.getValue());
            if (info != null && !infos.contains(info)) {
                infos.add(info);
            }
        }
        return infos;
    }

    @RequiredUIAccess
    public CompletableFuture<?> rebuildTree() {
        return rebuildTree(List.of());
    }

    @RequiredUIAccess
    public CompletableFuture<?> rebuildTree(List<? extends PackagingElement<?>> toSelect) {
        myArtifactsEditor.queueValidation();

        Tree<PackagingElement<?>> tree = myTree;
        if (tree == null) {
            myInfos.clear();
            myTreeNodes.clear();
            return CompletableFuture.completedFuture(null);
        }

        myInfos.clear();
        myTreeNodes.clear();

        UIAccess uiAccess = UIAccess.current();
        return tree.refreshAll()
            .thenComposeAsync(ignored -> expandRoot(tree), uiAccess)
            .thenComposeAsync(ignored -> select(tree, toSelect), uiAccess)
            .whenCompleteAsync((ignored, error) -> {
                if (myTree == tree) {
                    updatePropertiesPanel(true);
                }
            }, uiAccess);
    }

    @RequiredUIAccess
    public CompletableFuture<?> updateAndSelect(List<? extends PackagingElement<?>> toSelect) {
        return rebuildTree(toSelect);
    }

    @RequiredUIAccess
    private CompletableFuture<?> expandRoot(Tree<PackagingElement<?>> tree) {
        if (myTree != tree) {
            return CompletableFuture.completedFuture(null);
        }

        TreeNode<PackagingElement<?>> invisibleRoot = tree.getRootNode();
        if (invisibleRoot == null) {
            return CompletableFuture.completedFuture(null);
        }

        UIAccess uiAccess = UIAccess.current();
        return invisibleRoot.findChild(value -> true).thenComposeAsync(rootNode -> {
            if (rootNode == null || myTree != tree) {
                return CompletableFuture.<Object>completedFuture(null);
            }
            return tree.expand(rootNode).<Object>thenApply(ignored -> null);
        }, uiAccess);
    }

    @RequiredUIAccess
    private CompletableFuture<?> select(Tree<PackagingElement<?>> tree, List<? extends PackagingElement<?>> toSelect) {
        TreeNode<PackagingElement<?>> rootNode = tree.getRootNode();
        if (myTree != tree || toSelect.isEmpty() || rootNode == null) {
            return CompletableFuture.completedFuture(null);
        }

        Set<PackagingElement<?>> targets = Collections.newSetFromMap(new IdentityHashMap<>());
        targets.addAll(toSelect);

        UIAccess uiAccess = UIAccess.current();
        return rootNode.findChildDeep(value -> {
            LayoutElementInfo info = getInfo(value);
            if (info == null) {
                return false;
            }
            for (PackagingElement<?> element : info.getElements()) {
                if (targets.contains(element)) {
                    return true;
                }
            }
            return false;
        }).whenCompleteAsync((node, error) -> {
            if (node != null && myTree == tree) {
                tree.select(node);
            }
        }, uiAccess);
    }

    public void editLayout(Runnable action) {
        myContext.editLayout(myOriginalArtifact, action);
    }

    @RequiredUIAccess
    public void addNewPackagingElement(PackagingElementType<?> type) {
        LayoutElementInfo parentNode = getParentNodeForAdd(getSelection());
        if (parentNode == null) {
            return;
        }

        CompositePackagingElement<?> parent;
        if (parentNode.getFirstElement() instanceof CompositePackagingElement<?> composite) {
            parent = composite;
        }
        else {
            LayoutElementInfo root = getRootNode();
            if (root == null) {
                return;
            }
            parent = getArtifact().getRootElement();
            parentNode = root;
        }

        if (!checkCanAdd(parent, parentNode)) {
            return;
        }

        LayoutElementInfo targetNode = parentNode;
        UIAccess uiAccess = UIAccess.current();
        type.chooseAndCreate(myContext, getArtifact(), parent).whenCompleteAsync((children, error) -> {
            if (error != null || children.isEmpty() || myArtifactsEditor.isDisposed()) {
                return;
            }

            List<PackagingElement<?>> toSelect = new ArrayList<>();
            editLayout(() -> {
                CompositePackagingElement<?> actualParent = getOrCreateModifiableParent(parent, targetNode);
                for (PackagingElement<?> child : children) {
                    toSelect.add(actualParent.addOrFindChild(child));
                }
            });
            myArtifactsEditor.getSourceItemsTree().rebuildTree();
            updateAndSelect(toSelect);
        }, uiAccess);
    }

    private @Nullable LayoutElementInfo getParentNodeForAdd(LayoutTreeSelection selection) {
        LayoutElementInfo node = selection.getNodeIfSingle();
        if (node != null) {
            if (node.getFirstElement() instanceof CompositePackagingElement) {
                return node;
            }
            LayoutElementInfo parent = getParentNode(node);
            if (parent != null) {
                return parent;
            }
        }
        return getRootNode();
    }

    private CompositePackagingElement<?> getOrCreateModifiableParent(CompositePackagingElement<?> parentElement, LayoutElementInfo node) {
        LayoutElementInfo current = node;
        List<String> dirNames = new ArrayList<>();
        while (current != null && !current.isRoot()) {
            if (!(current.getFirstElement() instanceof DirectoryPackagingElement directory)) {
                return parentElement;
            }
            dirNames.add(directory.getDirectoryName());
            current = getParentNode(current);
        }

        if (current == null || !(current.getElementIfSingle() instanceof CompositePackagingElement<?> rootElement)) {
            return parentElement;
        }

        Collections.reverse(dirNames);
        return PackagingElementFactory.getInstance(myContext.getProject()).getOrCreateDirectory(rootElement, StringUtil.join(dirNames, "/"));
    }

    @RequiredUIAccess
    public boolean checkCanModify(PackagingElement<?> element, LayoutElementInfo node) {
        return checkCanModify(node.getNodeSource(element));
    }

    @RequiredUIAccess
    public boolean checkCanModifyChildren(
        PackagingElement<?> parentElement,
        LayoutElementInfo parentNode,
        Collection<LayoutElementInfo> children
    ) {
        List<PackagingNodeSource> sources = new ArrayList<>(parentNode.getNodeSource(parentElement));
        for (LayoutElementInfo child : children) {
            sources.addAll(child.getNodeSources());
        }
        return checkCanModify(sources);
    }

    @RequiredUIAccess
    public boolean checkCanModify(Collection<PackagingNodeSource> nodeSources) {
        if (nodeSources.isEmpty()) {
            return true;
        }

        LocalizeValue message;
        if (nodeSources.size() > 1) {
            message = ProjectLocalize.artifactNodeConsistsOfSeveralElementsError();
        }
        else {
            message = ProjectLocalize.artifactNodeBelongsTo0Error(nodeSources.iterator().next().getPresentableName());
        }
        MessageBoxes.okError(message).showAsync(myTree);
        return false;
    }

    @RequiredUIAccess
    public boolean checkCanAdd(CompositePackagingElement<?> parentElement, LayoutElementInfo parentNode) {
        boolean allParentsAreDirectories = true;
        LayoutElementInfo current = parentNode;
        while (current != null && !current.isRoot()) {
            if (!(current.getFirstElement() instanceof DirectoryPackagingElement)) {
                allParentsAreDirectories = false;
                break;
            }
            current = getParentNode(current);
        }

        return allParentsAreDirectories || checkCanModify(parentElement, parentNode);
    }

    @RequiredUIAccess
    public CompletableFuture<Boolean> checkCanRemove(List<LayoutElementInfo> nodes) {
        Set<PackagingNodeSource> rootSources = new LinkedHashSet<>();
        for (LayoutElementInfo node : nodes) {
            rootSources.addAll(getRootNodeSources(node.getNodeSources()));
        }

        if (rootSources.isEmpty()) {
            return CompletableFuture.completedFuture(Boolean.TRUE);
        }

        LocalizeValue message;
        if (rootSources.size() == 1) {
            message = ProjectLocalize.artifactRemoveElementOf0Question(rootSources.iterator().next().getPresentableName());
        }
        else {
            message = ProjectLocalize.artifactRemoveElementsOf0Question(rootSources.size());
        }
        return MessageBoxes.yesNo().title(ProjectLocalize.artifactRemoveElementsTitle()).text(message).showAsync(myTree);
    }

    @RequiredUIAccess
    public void removeSelectedElements() {
        List<LayoutElementInfo> nodes = getSelection().getNodes();
        if (nodes.isEmpty()) {
            return;
        }

        UIAccess uiAccess = UIAccess.current();
        checkCanRemove(nodes).whenCompleteAsync((remove, error) -> {
            if (error != null || !Boolean.TRUE.equals(remove) || myArtifactsEditor.isDisposed()) {
                return;
            }

            editLayout(() -> removeNodes(nodes));
            myArtifactsEditor.rebuildTries();
        }, uiAccess);
    }

    public void removeNodes(List<LayoutElementInfo> nodes) {
        for (LayoutElementInfo node : nodes) {
            for (PackagingElement<?> element : node.getElements()) {
                List<PackagingNodeSource> nodeSources = node.getNodeSource(element);
                if (nodeSources.isEmpty()) {
                    CompositePackagingElement<?> parent = node.getParentElement(element);
                    if (parent != null) {
                        parent.removeChild(element);
                    }
                }
                else {
                    for (PackagingNodeSource source : getRootNodeSources(nodeSources)) {
                        source.getSourceParentElement().removeChild(source.getSourceElement());
                    }
                }
            }
        }
    }

    private static Collection<PackagingNodeSource> getRootNodeSources(Collection<PackagingNodeSource> nodeSources) {
        Set<PackagingNodeSource> result = new LinkedHashSet<>();
        collectRootNodeSources(nodeSources, result);
        return result;
    }

    private static void collectRootNodeSources(Collection<PackagingNodeSource> nodeSources, Set<PackagingNodeSource> result) {
        for (PackagingNodeSource nodeSource : nodeSources) {
            Collection<PackagingNodeSource> parentSources = nodeSource.getParentSources();
            if (parentSources.isEmpty()) {
                result.add(nodeSource);
            }
            else {
                collectRootNodeSources(parentSources, result);
            }
        }
    }

    @RequiredUIAccess
    public void setRootElement(CompositePackagingElement<?> rootElement) {
        myContext.getOrCreateModifiableArtifactModel().getOrCreateModifiableArtifact(myOriginalArtifact).setRootElement(rootElement);
        rebuildTree();
        myArtifactsEditor.getSourceItemsTree().rebuildTree();
    }

    public CompositePackagingElement<?> getRootElement() {
        return myContext.getRootElement(myOriginalArtifact);
    }

    @RequiredUIAccess
    public void putIntoDefaultLocations(List<? extends PackagingSourceItem> items) {
        List<PackagingElement<?>> toSelect = new ArrayList<>();
        editLayout(() -> {
            CompositePackagingElement<?> rootElement = getArtifact().getRootElement();
            ArtifactType artifactType = getArtifact().getArtifactType();
            PackagingElementFactory factory = PackagingElementFactory.getInstance(myContext.getProject());
            for (PackagingSourceItem item : items) {
                String path = artifactType.getDefaultPathFor(item);
                if (path != null) {
                    CompositePackagingElement<?> directory = factory.getOrCreateDirectory(rootElement, path);
                    toSelect.addAll(directory.addOrFindChildren(item.createElements(myContext)));
                }
            }
        });

        myArtifactsEditor.getSourceItemsTree().rebuildTree();
        updateAndSelect(toSelect);
    }

    @RequiredUIAccess
    public void putElements(String path, List<? extends PackagingElement<?>> elements) {
        List<PackagingElement<?>> toSelect = new ArrayList<>();
        editLayout(() -> {
            CompositePackagingElement<?> directory =
                PackagingElementFactory.getInstance(myContext.getProject()).getOrCreateDirectory(getArtifact().getRootElement(), path);
            toSelect.addAll(directory.addOrFindChildren(elements));
        });
        myArtifactsEditor.getSourceItemsTree().rebuildTree();
        updateAndSelect(toSelect);
    }

    @RequiredUIAccess
    public void packInto(List<? extends PackagingSourceItem> items, String pathToJar) {
        List<PackagingElement<?>> toSelect = new ArrayList<>();
        CompositePackagingElement<?> rootElement = getArtifact().getRootElement();
        editLayout(() -> {
            CompositePackagingElement<?> archive =
                PackagingElementFactory.getInstance(myContext.getProject()).getOrCreateArchive(rootElement, pathToJar);
            for (PackagingSourceItem item : items) {
                archive.addOrFindChildren(item.createElements(myContext));
            }
            toSelect.add(archive);
        });

        myArtifactsEditor.getSourceItemsTree().rebuildTree();
        updateAndSelect(toSelect);
    }

    @RequiredUIAccess
    public void showContent(LayoutElementInfo node) {
        mySubstitutionParameters.setShowContent(node.getElements());
        rebuildTree();
    }

    @RequiredUIAccess
    public void updatePropertiesPanel(boolean force) {
        PackagingElement<?> selected = getSelection().getElementIfSingle();
        if (!force && selected == mySelectedElement) {
            return;
        }

        saveElementProperties();
        disposePropertiesPanel();

        mySelectedElement = selected;

        DockLayout propertiesPanel = myPropertiesPanel;
        Disposable uiDisposable = myUIDisposable;
        if (propertiesPanel == null || uiDisposable == null) {
            return;
        }

        PackagingElementPropertiesPanel panel = selected == null ? null : createPropertiesPanel(selected);
        if (panel == null) {
            propertiesPanel.setVisible(false);
            return;
        }

        myCurrentPropertiesPanel = panel;
        propertiesPanel.center(ScrollableLayout.create(panel.createUIComponent(uiDisposable)));
        panel.reset();
        propertiesPanel.setVisible(true);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private @Nullable PackagingElementPropertiesPanel createPropertiesPanel(PackagingElement<?> element) {
        PackagingElementType type = element.getType();
        return type.createElementPropertiesPanel(element, myContext);
    }

    @RequiredUIAccess
    public void saveElementProperties() {
        PackagingElementPropertiesPanel panel = myCurrentPropertiesPanel;
        if (panel != null && panel.isModified()) {
            editLayout(panel::apply);
        }
    }

    @RequiredUIAccess
    private void disposePropertiesPanel() {
        PackagingElementPropertiesPanel panel = myCurrentPropertiesPanel;
        if (panel != null) {
            panel.disposeUIResources();
        }
        myCurrentPropertiesPanel = null;
    }

    @RequiredUIAccess
    public boolean isPropertiesModified() {
        PackagingElementPropertiesPanel panel = myCurrentPropertiesPanel;
        return panel != null && panel.isModified();
    }

    @RequiredUIAccess
    public void resetElementProperties() {
        PackagingElementPropertiesPanel panel = myCurrentPropertiesPanel;
        if (panel != null) {
            panel.reset();
        }
    }

    @RequiredUIAccess
    public void setProblems(List<ArtifactProblem> problems) {
        myProblems = List.copyOf(problems);
        updateProblems();
    }

    @RequiredUIAccess
    private void queueProblemsUpdate() {
        if (myProblemsUpdateQueued) {
            return;
        }
        myProblemsUpdateQueued = true;

        UIAccess.current().give(() -> {
            myProblemsUpdateQueued = false;
            updateProblems();
        });
    }

    @RequiredUIAccess
    private void updateProblems() {
        Map<LayoutElementInfo, List<ArtifactProblem>> problems = new IdentityHashMap<>();
        LayoutElementInfo root = getRootNode();
        if (root != null) {
            for (ArtifactProblem problem : myProblems) {
                List<PackagingElement<?>> pathToPlace = problem.pathToPlace();
                if (pathToPlace == null || pathToPlace.isEmpty()) {
                    continue;
                }

                for (LayoutElementInfo node : getNodesByPath(root, pathToPlace.subList(1, pathToPlace.size()))) {
                    problems.computeIfAbsent(node, key -> new ArrayList<>()).add(problem);
                }
            }
        }

        Tree<PackagingElement<?>> tree = myTree;
        for (LayoutElementInfo info : myInfos.values()) {
            List<ArtifactProblem> nodeProblems = problems.getOrDefault(info, List.of());
            if (!nodeProblems.equals(info.getProblems())) {
                info.setProblems(nodeProblems);

                TreeNode<PackagingElement<?>> treeNode = myTreeNodes.get(info.getFirstElement());
                if (tree != null && treeNode != null) {
                    tree.refreshItem(treeNode);
                }
            }
        }
    }

    private static List<LayoutElementInfo> getNodesByPath(LayoutElementInfo root, List<PackagingElement<?>> pathToPlace) {
        List<LayoutElementInfo> result = new ArrayList<>();
        LayoutElementInfo current = root;
        result.add(current);

        int i = 0;
        while (current != null && i < pathToPlace.size()) {
            PackagingElement<?> element = pathToPlace.get(i);
            LayoutElementInfo next = null;

            search:
            for (LayoutElementInfo child : current.getChildren()) {
                for (PackagingElement<?> childElement : child.getElements()) {
                    if (childElement.isEqualTo(element)) {
                        next = child;
                        break search;
                    }
                }
                for (PackagingNodeSource nodeSource : child.getNodeSources()) {
                    if (nodeSource.getSourceElement().isEqualTo(element)) {
                        next = current;
                        break search;
                    }
                }
            }

            current = next;
            if (current != null) {
                result.add(current);
            }
            i++;
        }
        return result;
    }

    @Override
    @RequiredUIAccess
    public void dispose() {
        disposePropertiesPanel();
        myInfos.clear();
        myTreeNodes.clear();
        myTree = null;
        myPropertiesPanel = null;
        myUIDisposable = null;
    }

    private List<LayoutElementInfo> buildChildInfos(LayoutElementInfo parentInfo) {
        List<LayoutElementInfo> result = new ArrayList<>();
        Set<PackagingElement<?>> processed = Collections.newSetFromMap(new IdentityHashMap<>());
        ArtifactType artifactType = getArtifact().getArtifactType();
        for (PackagingElement<?> element : parentInfo.getElements()) {
            if (element instanceof CompositePackagingElement<?> composite) {
                addInfos(composite.getChildren(), parentInfo, composite, parentInfo.getNodeSource(element), result, processed, artifactType);
            }
        }

        if (mySortElements) {
            result.sort(SORT_COMPARATOR);
        }
        parentInfo.setChildren(List.copyOf(result));
        return result;
    }

    private void addInfos(
        List<? extends PackagingElement<?>> elements,
        LayoutElementInfo parentInfo,
        CompositePackagingElement<?> parentElement,
        Collection<PackagingNodeSource> nodeSources,
        List<LayoutElementInfo> infos,
        Set<PackagingElement<?>> processed,
        ArtifactType artifactType
    ) {
        PackagingElement<?> parentValue = parentInfo.getFirstElement();
        for (PackagingElement<?> element : elements) {
            LayoutElementInfo previous = findEqual(infos, element);
            if (previous != null) {
                previous.addElement(element, parentElement, nodeSources);
                continue;
            }

            if (element instanceof ArtifactRootElement) {
                throw new AssertionError("artifact root not expected here");
            }

            if (!(element instanceof CompositePackagingElement) && element instanceof ComplexPackagingElement<?> complexElement
                && processed.add(element) && mySubstitutionParameters.shouldSubstitute(complexElement)) {
                List<? extends PackagingElement<?>> substitution = complexElement.getSubstitution(myContext, artifactType);
                if (substitution != null) {
                    String presentableName = complexElement.createPresentation(myContext).getPresentableName();
                    PackagingNodeSource source = new PackagingNodeSource(complexElement, parentValue, parentElement, nodeSources, presentableName);
                    addInfos(substitution, parentInfo, parentElement, List.of(source), infos, processed, artifactType);
                    continue;
                }
            }

            PackagingElement<?> value = toUniqueValue(element, parentValue);
            infos.add(new LayoutElementInfo(value, parentValue, parentElement, nodeSources, value.createPresentation(myContext)));
        }
    }

    private PackagingElement<?> toUniqueValue(PackagingElement<?> element, PackagingElement<?> parentValue) {
        LayoutElementInfo existing = myInfos.get(element);
        if (existing == null || existing.getParentValue() == parentValue) {
            return element;
        }
        return ArtifactUtil.copyWithChildren(element, myContext.getProject());
    }

    private static @Nullable LayoutElementInfo findEqual(List<LayoutElementInfo> infos, PackagingElement<?> element) {
        for (LayoutElementInfo info : infos) {
            if (info.getFirstElement().isEqualTo(element)) {
                return info;
            }
        }
        return null;
    }

    @RequiredUIAccess
    private void registerNode(Function<PackagingElement<?>, TreeNode<PackagingElement<?>>> nodeFactory, LayoutElementInfo info) {
        PackagingElement<?> value = info.getFirstElement();
        myInfos.put(value, info);

        TreeNode<PackagingElement<?>> node = nodeFactory.apply(value);
        node.setLeaf(!(value instanceof CompositePackagingElement));
        node.setRenderer((element, presentation) -> {
            List<ArtifactProblem> problems = info.getProblems();
            ArtifactTreePresentations.render(info.getPresentationData(), getTooltip(info, problems), !problems.isEmpty(), presentation);
        });
        myTreeNodes.put(value, node);
    }

    private static String getTooltip(LayoutElementInfo info, List<ArtifactProblem> problems) {
        if (problems.isEmpty()) {
            return StringUtil.notNullize(info.getPresentation().getTooltipText());
        }

        StringBuilder builder = new StringBuilder();
        for (ArtifactProblem problem : problems) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(problem.message());
        }
        return builder.toString();
    }

    private @Nullable LayoutElementInfo findDropTarget(LayoutElementInfo target, DragAndDropTransferHandler.DropPosition position) {
        LayoutElementInfo current = target;
        if (position != DragAndDropTransferHandler.DropPosition.INTO) {
            LayoutElementInfo parent = getParentNode(target);
            if (parent != null) {
                current = parent;
            }
        }

        while (current != null) {
            if (current.getFirstElement() instanceof CompositePackagingElement) {
                return current;
            }
            current = getParentNode(current);
        }
        return null;
    }

    private boolean isAncestorOrSelf(LayoutElementInfo ancestor, LayoutElementInfo node) {
        LayoutElementInfo current = node;
        while (current != null) {
            if (current == ancestor) {
                return true;
            }
            current = getParentNode(current);
        }
        return false;
    }

    @RequiredUIAccess
    private void moveNodes(List<LayoutElementInfo> nodes, LayoutElementInfo targetNode, CompositePackagingElement<?> targetElement) {
        if (!checkCanAdd(targetElement, targetNode)) {
            return;
        }

        UIAccess uiAccess = UIAccess.current();
        checkCanRemove(nodes).whenCompleteAsync((remove, error) -> {
            if (error != null || !Boolean.TRUE.equals(remove) || myArtifactsEditor.isDisposed()) {
                return;
            }

            List<PackagingElement<?>> toSelect = new ArrayList<>();
            editLayout(() -> {
                List<PackagingElement<?>> copies = new ArrayList<>();
                for (LayoutElementInfo node : nodes) {
                    for (PackagingElement<?> element : node.getElements()) {
                        copies.add(ArtifactUtil.copyWithChildren(element, myContext.getProject()));
                    }
                }

                removeNodes(nodes);

                CompositePackagingElement<?> parent = getOrCreateModifiableParent(targetElement, targetNode);
                for (PackagingElement<?> copy : copies) {
                    toSelect.add(parent.addOrFindChild(copy));
                }
            });
            myArtifactsEditor.getSourceItemsTree().rebuildTree();
            updateAndSelect(toSelect);
        }, uiAccess);
    }

    @RequiredUIAccess
    private void addSourceItems(List<PackagingSourceItem> items, LayoutElementInfo targetNode, CompositePackagingElement<?> targetElement) {
        if (!checkCanAdd(targetElement, targetNode)) {
            return;
        }

        List<PackagingElement<?>> toSelect = new ArrayList<>();
        editLayout(() -> {
            CompositePackagingElement<?> parent = getOrCreateModifiableParent(targetElement, targetNode);
            for (PackagingSourceItem item : items) {
                for (PackagingElement<?> element : item.createElements(myContext)) {
                    toSelect.add(parent.addOrFindChild(element));
                }
            }
        });
        myArtifactsEditor.getSourceItemsTree().rebuildTree();
        updateAndSelect(toSelect);
    }

    private class LayoutTreeModel implements TreeModel<PackagingElement<?>> {
        @Override
        @RequiredUIAccess
        public void buildChildren(
            Function<PackagingElement<?>, TreeNode<PackagingElement<?>>> nodeFactory,
            @Nullable PackagingElement<?> parentValue
        ) {
            if (parentValue == null) {
                CompositePackagingElement<?> rootElement = getRootElement();
                registerNode(nodeFactory, new LayoutElementInfo(rootElement, null, null, List.of(), rootElement.createPresentation(myContext)));
                return;
            }

            LayoutElementInfo parentInfo = getInfo(parentValue);
            if (parentInfo == null) {
                return;
            }

            for (LayoutElementInfo info : buildChildInfos(parentInfo)) {
                registerNode(nodeFactory, info);
            }

            if (!myProblems.isEmpty()) {
                queueProblemsUpdate();
            }
        }

        @Override
        public boolean isNeedBuildChildrenBeforeOpen(TreeNode<PackagingElement<?>> node) {
            return node.getValue() instanceof CompositePackagingElement;
        }

        @Override
        @RequiredUIAccess
        public boolean onDoubleClick(Tree<PackagingElement<?>> tree, TreeNode<PackagingElement<?>> node) {
            LayoutElementInfo info = getInfo(node.getValue());
            if (info != null && info.getFirstElement() instanceof ComplexPackagingElement
                && !(info.getFirstElement() instanceof CompositePackagingElement)) {
                showContent(info);
                return false;
            }
            return true;
        }
    }

    private class LayoutTreeDragAndDropHandler implements DragAndDropTransferHandler<TreeNode<PackagingElement<?>>> {
        @Override
        public @Nullable DataTransfer createTransfer(Component component) {
            return null;
        }

        @Override
        public @Nullable DataTransfer createDragTransfer(Component component, List<TreeNode<PackagingElement<?>>> items, boolean move) {
            List<LayoutElementInfo> nodes = getInfos(items);
            if (nodes.isEmpty()) {
                return null;
            }

            List<PackagingElement<?>> elements = new ArrayList<>();
            StringBuilder text = new StringBuilder();
            for (LayoutElementInfo node : nodes) {
                if (node.isRoot()) {
                    return null;
                }
                elements.addAll(node.getElements());
                if (text.length() > 0) {
                    text.append('\n');
                }
                text.append(node.getPresentation().getPresentableName());
            }

            return DataTransfer.builder().put(LAYOUT_ELEMENTS, elements).put(DataTransferType.TEXT, text.toString()).build();
        }

        @Override
        @RequiredUIAccess
        public boolean drop(Component component, DropContext<TreeNode<PackagingElement<?>>> context) {
            LayoutElementInfo target = getInfo(context.getTarget().getValue());
            if (target == null) {
                return false;
            }

            LayoutElementInfo targetNode = findDropTarget(target, context.getPosition());
            if (targetNode == null || !(targetNode.getFirstElement() instanceof CompositePackagingElement<?> targetElement)) {
                return false;
            }

            List<TreeNode<PackagingElement<?>>> items = context.getItems();
            if (!items.isEmpty()) {
                List<LayoutElementInfo> nodes = getInfos(items);
                if (nodes.isEmpty()) {
                    return false;
                }
                for (LayoutElementInfo node : nodes) {
                    if (node.isRoot() || isAncestorOrSelf(node, targetNode)) {
                        return false;
                    }
                }

                if (!context.isCheckOnly()) {
                    moveNodes(nodes, targetNode, targetElement);
                }
                return true;
            }

            List<PackagingSourceItem> sourceItems = context.getTransfer().get(SourceItemsTree.SOURCE_ITEMS);
            if (sourceItems == null || sourceItems.isEmpty()) {
                return false;
            }

            if (!context.isCheckOnly()) {
                addSourceItems(sourceItems, targetNode, targetElement);
            }
            return true;
        }
    }
}
