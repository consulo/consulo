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

import consulo.application.Application;
import consulo.compiler.artifact.Artifact;
import consulo.compiler.artifact.ArtifactType;
import consulo.compiler.artifact.element.*;
import consulo.compiler.artifact.ui.ArtifactEditorContext;
import consulo.compiler.artifact.ui.PackagingElementPropertiesPanel;
import consulo.compiler.artifact.ui.PackagingSourceItem;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.nodes.ArtifactRootNode;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.nodes.PackagingElementNode;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.nodes.PackagingNodeSource;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.nodes.PackagingTreeNodeFactory;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.dnd.DnDEvent;
import consulo.ui.ex.awt.dnd.DnDManager;
import consulo.ui.ex.awt.dnd.DnDTarget;
import consulo.ui.ex.awt.tree.*;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.TestOnly;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.List;
import java.util.*;

public class LayoutTreeComponent implements DnDTarget, Disposable {
    private static final String EMPTY_CARD = "<empty>";
    private static final String PROPERTIES_CARD = "properties";
    private final ArtifactEditorImpl myArtifactsEditor;
    private final LayoutTree myTree;
    private final JPanel myTreePanel;
    private final ComplexElementSubstitutionParameters mySubstitutionParameters;
    private final ArtifactEditorContext myContext;
    private final Artifact myOriginalArtifact;
    private SelectedElementInfo<?> mySelectedElementInfo = new SelectedElementInfo<>(null);
    private JPanel myPropertiesPanelWrapper;
    private JPanel myPropertiesPanel;
    private final LayoutTreeBuilder myBuilder;
    private boolean mySortElements;
    private final LayoutTreeStructure myTreeStructure;

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
        myTree = new LayoutTree(myArtifactsEditor);
        myTreeStructure = new LayoutTreeStructure();
        myBuilder = new LayoutTreeBuilder();
        Disposer.register(this, myTree);
        Disposer.register(this, myBuilder);

        myTree.addTreeSelectionListener(e -> updatePropertiesPanel(false));
        createPropertiesPanel();
        myTreePanel = new JPanel(new BorderLayout());
        myTreePanel.add(ScrollPaneFactory.createScrollPane(myTree, true), BorderLayout.CENTER);
        myTreePanel.add(myPropertiesPanelWrapper, BorderLayout.SOUTH);
        if (!Application.get().isUnitTestMode()) {
            DnDManager.getInstance().registerTarget(this, myTree);
        }
    }

    @Nullable
    private WeightBasedComparator getComparator() {
        return mySortElements ? new WeightBasedComparator(true) : null;
    }

    public void setSortElements(boolean sortElements) {
        mySortElements = sortElements;
        myBuilder.setNodeDescriptorComparator(getComparator());
        myArtifactsEditor.getContext().getParent().getDefaultSettings().setSortElements(sortElements);
    }

    @Nullable
    private static PackagingElementNode getNode(Object value) {
        if (!(value instanceof DefaultMutableTreeNode)) {
            return null;
        }
        Object userObject = ((DefaultMutableTreeNode) value).getUserObject();
        return userObject instanceof PackagingElementNode ? (PackagingElementNode) userObject : null;
    }

    private void createPropertiesPanel() {
        myPropertiesPanel = new JPanel(new BorderLayout());
        JPanel emptyPanel = new JPanel();
        emptyPanel.setMinimumSize(new Dimension(0, 0));
        emptyPanel.setPreferredSize(new Dimension(0, 0));

        myPropertiesPanelWrapper = new JPanel(new CardLayout());
        myPropertiesPanel.setBorder(new CustomLineBorder(UIUtil.getBorderColor(), 1, 0, 0, 0));
        myPropertiesPanelWrapper.add(EMPTY_CARD, emptyPanel);
        myPropertiesPanelWrapper.add(PROPERTIES_CARD, myPropertiesPanel);
    }

    public Artifact getArtifact() {
        return myArtifactsEditor.getArtifact();
    }

    public LayoutTree getLayoutTree() {
        return myTree;
    }

    @RequiredUIAccess
    public void updatePropertiesPanel(boolean force) {
        PackagingElement<?> selected = getSelection().getElementIfSingle();
        if (!force && Comparing.equal(selected, mySelectedElementInfo.myElement)) {
            return;
        }
        mySelectedElementInfo.save();
        mySelectedElementInfo = new SelectedElementInfo<PackagingElement<?>>(selected);
        mySelectedElementInfo.showPropertiesPanel();
    }

    @RequiredUIAccess
    public void saveElementProperties() {
        mySelectedElementInfo.save();
    }

    @RequiredUIAccess
    public void rebuildTree() {
        myBuilder.updateFromRoot(true);
        updatePropertiesPanel(true);
        myArtifactsEditor.queueValidation();
    }

    public LayoutTreeSelection getSelection() {
        return myTree.getSelection();
    }

    @RequiredUIAccess
    public void addNewPackagingElement(@Nonnull PackagingElementType<?> type) {
        PackagingElementNode<?> parentNode = getParentNode(myTree.getSelection());
        PackagingElement<?> element = parentNode.getFirstElement();
        CompositePackagingElement<?> parent;
        if (element instanceof CompositePackagingElement<?>) {
            parent = (CompositePackagingElement<?>) element;
        }
        else {
            parent = getArtifact().getRootElement();
            parentNode = myTree.getRootPackagingNode();
        }
        if (!checkCanAdd(parent, parentNode)) {
            return;
        }

        List<? extends PackagingElement<?>> children = type.chooseAndCreate(myContext, getArtifact(), parent);
        PackagingElementNode<?> finalParentNode = parentNode;
        editLayout(() -> {
            CompositePackagingElement<?> actualParent = getOrCreateModifiableParent(parent, finalParentNode);
            for (PackagingElement<?> child : children) {
                actualParent.addOrFindChild(child);
            }
        });
        updateAndSelect(parentNode, children);
    }

    private CompositePackagingElement<?> getOrCreateModifiableParent(
        CompositePackagingElement<?> parentElement,
        PackagingElementNode<?> node
    ) {
        PackagingElementNode<?> current = node;
        List<String> dirNames = new ArrayList<>();
        while (current != null && !(current instanceof ArtifactRootNode)) {
            PackagingElement<?> packagingElement = current.getFirstElement();
            if (!(packagingElement instanceof DirectoryPackagingElement)) {
                return parentElement;
            }
            dirNames.add(((DirectoryPackagingElement) packagingElement).getDirectoryName());
            current = current.getParentNode();
        }

        if (current == null) {
            return parentElement;
        }
        PackagingElement<?> rootElement = current.getElementIfSingle();
        if (!(rootElement instanceof CompositePackagingElement<?>)) {
            return parentElement;
        }

        Collections.reverse(dirNames);
        String path = StringUtil.join(dirNames, "/");
        return PackagingElementFactory.getInstance(myContext.getProject())
            .getOrCreateDirectory((CompositePackagingElement<?>) rootElement, path);
    }

    @RequiredUIAccess
    public boolean checkCanModify(@Nonnull PackagingElement<?> element, @Nonnull PackagingElementNode<?> node) {
        return checkCanModify(node.getNodeSource(element));
    }

    @RequiredUIAccess
    public boolean checkCanModifyChildren(
        @Nonnull PackagingElement<?> parentElement,
        @Nonnull PackagingElementNode<?> parentNode,
        @Nonnull Collection<? extends PackagingElementNode<?>> children
    ) {
        List<PackagingNodeSource> sources = new ArrayList<>(parentNode.getNodeSource(parentElement));
        for (PackagingElementNode<?> child : children) {
            sources.addAll(child.getNodeSources());
        }
        return checkCanModify(sources);
    }

    @RequiredUIAccess
    public boolean checkCanModify(Collection<PackagingNodeSource> nodeSources) {
        if (nodeSources.isEmpty()) {
            return true;
        }

        if (nodeSources.size() > 1) {
            Messages.showErrorDialog(
                myArtifactsEditor.getMainComponent(),
                "The selected node consist of several elements so it cannot be edited.\n" +
                    "Switch off 'Show content of elements' checkbox to edit the output layout."
            );
        }
        else {
            PackagingNodeSource source = ContainerUtil.getFirstItem(nodeSources, null);
            if (source != null) {
                Messages.showErrorDialog(
                    myArtifactsEditor.getMainComponent(),
                    "The selected node belongs to '" + source.getPresentableName() + "' element so it cannot be edited.\n" +
                        "Switch off 'Show content of elements' checkbox to edit the output layout."
                );
            }
        }
        return false;
    }

    @RequiredUIAccess
    public boolean checkCanAdd(CompositePackagingElement<?> parentElement, PackagingElementNode<?> parentNode) {
        boolean allParentsAreDirectories = true;
        PackagingElementNode<?> current = parentNode;
        while (current != null && !(current instanceof ArtifactRootNode)) {
            PackagingElement<?> element = current.getFirstElement();
            if (!(element instanceof DirectoryPackagingElement)) {
                allParentsAreDirectories = false;
                break;
            }
            current = current.getParentNode();
        }

        return allParentsAreDirectories || checkCanModify(parentElement, parentNode);
    }

    @RequiredUIAccess
    public boolean checkCanRemove(List<? extends PackagingElementNode<?>> nodes) {
        Set<PackagingNodeSource> rootSources = new HashSet<>();
        for (PackagingElementNode<?> node : nodes) {
            rootSources.addAll(getRootNodeSources(node.getNodeSources()));
        }

        if (!rootSources.isEmpty()) {
            String message;
            if (rootSources.size() == 1) {
                String name = rootSources.iterator().next().getPresentableName();
                message = "The selected node belongs to '" + name + "' element. " +
                    "Do you want to remove the whole '" + name + "' element from the artifact?";
            }
            else {
                message = "The selected node belongs to " + nodes.size() + " elements. " +
                    "Do you want to remove all these elements from the artifact?";
            }
            int answer = Messages.showYesNoDialog(myArtifactsEditor.getMainComponent(), message, "Remove Elements", null);
            if (answer != 0) {
                return false;
            }
        }
        return true;
    }

    public void updateAndSelect(PackagingElementNode<?> node, List<? extends PackagingElement<?>> toSelect) {
        myArtifactsEditor.queueValidation();
        DefaultMutableTreeNode treeNode = TreeUtil.findNodeWithObject(myTree.getRootNode(), node);
        myTreeStructure.clearCaches();
        myBuilder.addSubtreeToUpdate(
            treeNode,
            () -> {
                List<PackagingElementNode<?>> nodes = myTree.findNodes(toSelect);
                myBuilder.select(ArrayUtil.toObjectArray(nodes), null);
            }
        );
    }

    public void selectNode(@Nonnull String parentPath, @Nonnull PackagingElement<?> element) {
        PackagingElementNode<?> parent = myTree.findCompositeNodeByPath(parentPath);
        if (parent == null) {
            return;
        }

        for (SimpleNode node : parent.getChildren()) {
            if (node instanceof PackagingElementNode) {
                List<? extends PackagingElement<?>> elements = ((PackagingElementNode<?>) node).getPackagingElements();
                for (PackagingElement<?> packagingElement : elements) {
                    if (packagingElement.isEqualTo(element)) {
                        myBuilder.select(node);
                        return;
                    }
                }
            }
        }
    }

    @TestOnly
    public void selectNode(@Nonnull String parentPath, @Nonnull String nodeName) {
        PackagingElementNode<?> parent = myTree.findCompositeNodeByPath(parentPath);
        if (parent == null) {
            return;
        }

        for (SimpleNode node : parent.getChildren()) {
            if (node instanceof PackagingElementNode) {
                if (nodeName.equals(((PackagingElementNode) node).getElementPresentation().getSearchName())) {
                    myBuilder.select(node);
                    return;
                }
            }
        }
    }

    public void editLayout(Runnable action) {
        myContext.editLayout(myOriginalArtifact, action);
    }

    @RequiredUIAccess
    public void removeSelectedElements() {
        LayoutTreeSelection selection = myTree.getSelection();
        if (!checkCanRemove(selection.getNodes())) {
            return;
        }

        editLayout(() -> removeNodes(selection.getNodes()));

        myArtifactsEditor.rebuildTries();
    }

    public void removeNodes(List<PackagingElementNode<?>> nodes) {
        Set<PackagingElement<?>> parents = new HashSet<>();
        for (PackagingElementNode<?> node : nodes) {
            List<? extends PackagingElement<?>> toDelete = node.getPackagingElements();
            for (PackagingElement<?> element : toDelete) {
                Collection<PackagingNodeSource> nodeSources = node.getNodeSource(element);
                if (nodeSources.isEmpty()) {
                    CompositePackagingElement<?> parent = node.getParentElement(element);
                    if (parent != null) {
                        parents.add(parent);
                        parent.removeChild(element);
                    }
                }
                else {
                    Collection<PackagingNodeSource> rootSources = getRootNodeSources(nodeSources);
                    for (PackagingNodeSource source : rootSources) {
                        parents.add(source.getSourceParentElement());
                        source.getSourceParentElement().removeChild(source.getSourceElement());
                    }
                }
            }
        }
        List<PackagingElementNode<?>> parentNodes = myTree.findNodes(parents);
        for (PackagingElementNode<?> parentNode : parentNodes) {
            myTree.addSubtreeToUpdate(parentNode);
        }
    }

    private static Collection<PackagingNodeSource> getRootNodeSources(Collection<PackagingNodeSource> nodeSources) {
        Set<PackagingNodeSource> result = new HashSet<>();
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

    private PackagingElementNode<?> getParentNode(LayoutTreeSelection selection) {
        PackagingElementNode<?> node = selection.getNodeIfSingle();
        if (node != null) {
            if (node.getFirstElement() instanceof CompositePackagingElement) {
                return node;
            }
            PackagingElementNode<?> parent = node.getParentNode();
            if (parent != null) {
                return parent;
            }
        }
        return myTree.getRootPackagingNode();
    }

    public JPanel getTreePanel() {
        return myTreePanel;
    }

    @Override
    public void dispose() {
        if (!Application.get().isUnitTestMode()) {
            DnDManager.getInstance().unregisterTarget(this, myTree);
        }
    }

    @Override
    public boolean update(DnDEvent aEvent) {
        aEvent.setDropPossible(false);
        aEvent.hideHighlighter();
        Object object = aEvent.getAttachedObject();
        if (object instanceof PackagingElementDraggingObject) {
            DefaultMutableTreeNode parent = findParentCompositeElementNode(aEvent.getRelativePoint().getPoint(myTree));
            if (parent != null) {
                PackagingElementDraggingObject draggingObject = (PackagingElementDraggingObject) object;
                PackagingElementNode node = getNode(parent);
                if (node != null && draggingObject.canDropInto(node)) {
                    PackagingElement element = node.getFirstElement();
                    if (element instanceof CompositePackagingElement) {
                        draggingObject.setTargetNode(node);
                        draggingObject.setTargetElement((CompositePackagingElement<?>) element);
                        Rectangle bounds = myTree.getPathBounds(TreeUtil.getPathFromRoot(parent));
                        aEvent.setHighlighting(new RelativeRectangle(myTree, bounds), DnDEvent.DropTargetHighlightingType.RECTANGLE);
                        aEvent.setDropPossible(true);
                    }
                }
            }
        }
        return false;
    }

    @Override
    @RequiredUIAccess
    public void drop(DnDEvent aEvent) {
        Object object = aEvent.getAttachedObject();
        if (object instanceof PackagingElementDraggingObject draggingObject) {
            PackagingElementNode<?> targetNode = draggingObject.getTargetNode();
            CompositePackagingElement<?> targetElement = draggingObject.getTargetElement();
            if (targetElement == null || targetNode == null || !draggingObject.checkCanDrop()) {
                return;
            }
            if (!checkCanAdd(targetElement, targetNode)) {
                return;
            }
            List<PackagingElement<?>> toSelect = new ArrayList<>();
            editLayout(() -> {
                draggingObject.beforeDrop();
                CompositePackagingElement<?> parent = getOrCreateModifiableParent(targetElement, targetNode);
                for (PackagingElement<?> element : draggingObject.createPackagingElements(myContext)) {
                    toSelect.add(element);
                    parent.addOrFindChild(element);
                }
            });
            updateAndSelect(targetNode, toSelect);
            myArtifactsEditor.getSourceItemsTree().rebuildTree();
        }
    }

    @Nullable
    private DefaultMutableTreeNode findParentCompositeElementNode(Point point) {
        TreePath path = myTree.getPathForLocation(point.x, point.y);
        while (path != null) {
            PackagingElement<?> element = myTree.getElementByPath(path);
            if (element instanceof CompositePackagingElement) {
                return (DefaultMutableTreeNode) path.getLastPathComponent();
            }
            path = path.getParentPath();
        }
        return null;
    }

    @Override
    public void cleanUpOnLeave() {
    }

    @Override
    public void updateDraggedImage(Image image, Point dropPoint, Point imageOffset) {
    }

    public void startRenaming(TreePath path) {
        myTree.startEditingAtPath(path);
    }

    public boolean isEditing() {
        return myTree.isEditing();
    }

    @RequiredUIAccess
    public void setRootElement(CompositePackagingElement<?> rootElement) {
        myContext.getOrCreateModifiableArtifactModel().getOrCreateModifiableArtifact(myOriginalArtifact).setRootElement(rootElement);
        myTreeStructure.updateRootElement();
        DefaultMutableTreeNode node = myTree.getRootNode();
        node.setUserObject(myTreeStructure.getRootElement());
        myBuilder.updateNode(node);
        rebuildTree();
        myArtifactsEditor.getSourceItemsTree().rebuildTree();
    }

    public CompositePackagingElement<?> getRootElement() {
        return myContext.getRootElement(myOriginalArtifact);
    }

    public void updateTreeNodesPresentation() {
        myBuilder.updateFromRoot(false);
    }

    public void updateRootNode() {
        myBuilder.updateNode(myTree.getRootNode());
    }

    public void initTree() {
        myBuilder.initRootNode();
        mySelectedElementInfo.showPropertiesPanel();
    }

    public void putIntoDefaultLocations(@Nonnull List<? extends PackagingSourceItem> items) {
        List<PackagingElement<?>> toSelect = new ArrayList<>();
        editLayout(() -> {
            CompositePackagingElement<?> rootElement = getArtifact().getRootElement();
            ArtifactType artifactType = getArtifact().getArtifactType();
            for (PackagingSourceItem item : items) {
                String path = artifactType.getDefaultPathFor(item);
                if (path != null) {
                    CompositePackagingElement<?> directory =
                        PackagingElementFactory.getInstance(myContext.getProject()).getOrCreateDirectory(rootElement, path);
                    List<? extends PackagingElement<?>> elements = item.createElements(myContext);
                    toSelect.addAll(directory.addOrFindChildren(elements));
                }
            }
        });

        myArtifactsEditor.getSourceItemsTree().rebuildTree();
        updateAndSelect(myTree.getRootPackagingNode(), toSelect);
    }

    public void putElements(@Nonnull String path, @Nonnull List<? extends PackagingElement<?>> elements) {
        List<PackagingElement<?>> toSelect = new ArrayList<>();
        editLayout(() -> {
            CompositePackagingElement<?> directory =
                PackagingElementFactory.getInstance(myContext.getProject()).getOrCreateDirectory(getArtifact().getRootElement(), path);
            toSelect.addAll(directory.addOrFindChildren(elements));
        });
        myArtifactsEditor.getSourceItemsTree().rebuildTree();
        updateAndSelect(myTree.getRootPackagingNode(), toSelect);
    }

    public void packInto(@Nonnull List<? extends PackagingSourceItem> items, String pathToJar) {
        List<PackagingElement<?>> toSelect = new ArrayList<>();
        CompositePackagingElement<?> rootElement = getArtifact().getRootElement();
        editLayout(() -> {
            CompositePackagingElement<?> archive =
                PackagingElementFactory.getInstance(myContext.getProject()).getOrCreateArchive(rootElement, pathToJar);
            for (PackagingSourceItem item : items) {
                List<? extends PackagingElement<?>> elements = item.createElements(myContext);
                archive.addOrFindChildren(elements);
            }
            toSelect.add(archive);
        });

        myArtifactsEditor.getSourceItemsTree().rebuildTree();
        updateAndSelect(myTree.getRootPackagingNode(), toSelect);
    }

    @RequiredUIAccess
    public boolean isPropertiesModified() {
        PackagingElementPropertiesPanel panel = mySelectedElementInfo.myCurrentPanel;
        return panel != null && panel.isModified();
    }

    @RequiredUIAccess
    public void resetElementProperties() {
        PackagingElementPropertiesPanel panel = mySelectedElementInfo.myCurrentPanel;
        if (panel != null) {
            panel.reset();
        }
    }

    public boolean isSortElements() {
        return mySortElements;
    }

    private class SelectedElementInfo<E extends PackagingElement<?>> {
        private final E myElement;
        private PackagingElementPropertiesPanel myCurrentPanel;

        @RequiredUIAccess
        private SelectedElementInfo(@Nullable E element) {
            myElement = element;
            if (myElement != null) {
                //noinspection unchecked
                myCurrentPanel = element.getType().createElementPropertiesPanel(myElement, myContext);
                myPropertiesPanel.removeAll();
                if (myCurrentPanel != null) {
                    myPropertiesPanel.add(BorderLayout.CENTER, ScrollPaneFactory.createScrollPane(myCurrentPanel.createComponent(), true));
                    myCurrentPanel.reset();
                    myPropertiesPanel.revalidate();
                }
            }
        }

        @RequiredUIAccess
        public void save() {
            if (myCurrentPanel != null && myCurrentPanel.isModified()) {
                editLayout(() -> myCurrentPanel.apply());
            }
        }

        public void showPropertiesPanel() {
            CardLayout cardLayout = (CardLayout) myPropertiesPanelWrapper.getLayout();
            if (myCurrentPanel != null) {
                cardLayout.show(myPropertiesPanelWrapper, PROPERTIES_CARD);
            }
            else {
                cardLayout.show(myPropertiesPanelWrapper, EMPTY_CARD);
            }
            myPropertiesPanelWrapper.repaint();
        }
    }

    private class LayoutTreeStructure extends SimpleTreeStructure {
        private ArtifactRootNode myRootNode;

        @Nonnull
        @Override
        public Object getRootElement() {
            if (myRootNode == null) {
                myRootNode = PackagingTreeNodeFactory.createRootNode(
                    myArtifactsEditor,
                    myContext,
                    mySubstitutionParameters,
                    getArtifact().getArtifactType()
                );
            }
            return myRootNode;
        }

        public void updateRootElement() {
            myRootNode = null;
        }
    }

    private class LayoutTreeBuilder extends SimpleTreeBuilder {
        public LayoutTreeBuilder() {
            super(
                LayoutTreeComponent.this.myTree,
                LayoutTreeComponent.this.myTree.getBuilderModel(),
                LayoutTreeComponent.this.myTreeStructure,
                LayoutTreeComponent.this.getComparator()
            );
        }

        @Override
        public void updateNode(DefaultMutableTreeNode node) {
            super.updateNode(node);
        }
    }
}
