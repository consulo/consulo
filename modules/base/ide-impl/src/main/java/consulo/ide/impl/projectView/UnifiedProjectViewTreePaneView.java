/*
 * Copyright 2013-2026 consulo.io
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
package consulo.ide.impl.projectView;

import consulo.annotation.access.RequiredReadAction;
import consulo.dataContext.DataSink;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.ide.projectView.impl.AbstractProjectViewPSIPane;
import consulo.ide.impl.idea.ide.projectView.impl.GroupByTypeComparator;
import consulo.ide.impl.idea.ide.projectView.impl.ProjectAbstractTreeStructureBase;
import consulo.language.content.ProjectRootsUtil;
import consulo.language.editor.LangDataKeys;
import consulo.language.editor.PlatformDataKeys;
import consulo.language.editor.util.IdeView;
import consulo.language.psi.*;
import consulo.language.psi.event.PsiTreeChangeAdapter;
import consulo.language.psi.event.PsiTreeChangeEvent;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.ProjectRootManager;
import consulo.navigation.Navigatable;
import consulo.project.Project;
import consulo.project.ui.view.ProjectView;
import consulo.project.ui.view.internal.node.LibraryGroupElement;
import consulo.project.ui.view.internal.node.NamedLibraryElement;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.project.ui.view.tree.ModuleGroup;
import consulo.project.ui.view.tree.ProjectViewNode;
import consulo.ui.Component;
import consulo.ui.HasFocus;
import consulo.ui.Tree;
import consulo.ui.TreeNode;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.TreeExpander;
import consulo.ui.ex.tree.ApplicationTreeExecutorFactory;
import consulo.ui.ex.tree.NodeDescriptor;
import consulo.ui.ex.tree.TreeStructureWrappenModel;
import consulo.ui.ex.tree.UITreeState;
import consulo.util.xml.serializer.XmlSerializer;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.status.FileStatusListener;
import consulo.virtualFileSystem.status.FileStatusManager;
import org.jdom.Element;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class UnifiedProjectViewTreePaneView implements UnifiedProjectViewPaneView {
    private static final Logger LOG = Logger.getInstance(UnifiedProjectViewTreePaneView.class);

    private static final String ELEMENT_SUB_PANE = "subPane";
    private static final String ATTRIBUTE_SUB_ID = "subId";

    private final Project myProject;
    private final AbstractProjectViewPSIPane myPane;
    private final Function<PsiDirectory[], IdeView> myIdeViewFactory;
    private final Tree<AbstractTreeNode> myTree;

    private final Map<@Nullable String, UITreeState> myTreeStates = new HashMap<>();

    private final TreeExpander myTreeExpander = new MyTreeExpander();

    private boolean myStructureRefreshScheduled;

    private boolean myShown;

    private @Nullable String myShownSubId;

    @RequiredUIAccess
    public UnifiedProjectViewTreePaneView(
        Project project,
        AbstractProjectViewPSIPane pane,
        ApplicationTreeExecutorFactory treeExecutorFactory,
        Function<PsiDirectory[], IdeView> ideViewFactory
    ) {
        myProject = project;
        myPane = pane;
        myIdeViewFactory = ideViewFactory;

        ProjectAbstractTreeStructureBase structure = pane.createStructure();

        Comparator<NodeDescriptor> nodeOrder = new GroupByTypeComparator(ProjectView.getInstance(project), pane.getId());

        TreeStructureWrappenModel<AbstractTreeNode> model = new TreeStructureWrappenModel<>(structure) {
            @Override
            @RequiredUIAccess
            public boolean onDoubleClick(Tree tree, TreeNode node) {
                if (node.isLeaf()) {
                    AbstractTreeNode value = (AbstractTreeNode) node.getValue();

                    value.navigate(true);

                    return false;
                }

                return true;
            }

            @Override
            public Comparator<TreeNode<AbstractTreeNode>> getNodeComparator() {
                return (o1, o2) -> nodeOrder.compare(o1.getValue(), o2.getValue());
            }
        };

        myTree = Tree.create(
            (AbstractTreeNode) structure.getRootElement(),
            model,
            treeExecutorFactory.forBackgroundThreadWithReadAction(this)
        );
        Disposer.register(this, myTree.destroyHook());

        pane.setRebuildHandler(this::rebuild);

        FileStatusManager.getInstance(project).addFileStatusListener(new FileStatusListener() {
            @Override
            public void fileStatusesChanged() {
                refreshLoadedPresentations(null, false);
            }

            @Override
            public void fileStatusChanged(VirtualFile file) {
                refreshLoadedPresentations(file, false);
            }
        }, this);

        PsiManager.getInstance(project).addPsiTreeChangeListener(new MyPsiTreeChangeListener(), this);
    }

    @Override
    public Component getComponent() {
        return myTree;
    }

    private @Nullable AbstractTreeNode takeSelectedNode() {
        TreeNode<AbstractTreeNode> selectedNode = myTree.getSelectedNode();
        return selectedNode == null ? null : selectedNode.getValue();
    }

    @Override
    @RequiredUIAccess
    public void uiDataSnapshot(DataSink sink) {
        AbstractTreeNode selectedNode = takeSelectedNode();
        Object selected = selectedNode == null ? null : selectedNode.getValue();

        sink.lazy(IdeView.KEY, () -> myIdeViewFactory.apply(directoriesOf(selectedNode)));
        sink.set(PlatformDataKeys.TREE_EXPANDER, myTreeExpander);
        sink.set(Component.KEY, myTree);

        PsiElement selectedElement = selected instanceof PsiElement psiElement && psiElement.isValid() ? psiElement : null;

        if (selectedNode != null) {
            sink.set(PlatformDataKeys.SELECTED_ITEMS, new Object[]{selectedNode});
            sink.set(Navigatable.KEY, selectedNode);
            sink.set(Navigatable.KEY_OF_ARRAY, new Navigatable[]{selectedNode});
        }

        VirtualFile selectedFile = selected instanceof VirtualFile virtualFile
            ? virtualFile
            : PsiUtilCore.getVirtualFile(selectedElement);
        sink.set(VirtualFile.KEY, selectedFile);
        if (selectedFile != null) {
            sink.set(VirtualFile.KEY_OF_ARRAY, new VirtualFile[]{selectedFile});
        }

        sink.set(PsiFile.KEY, selectedElement instanceof PsiFile psiFile ? psiFile : null);

        sink.set(PsiElement.KEY, selectedElement);
        if (selectedElement != null) {
            sink.set(PsiElement.KEY_OF_ARRAY, new PsiElement[]{selectedElement});
        }

        sink.set(PlatformDataKeys.PROJECT_CONTEXT, selected instanceof Project project ? project : null);
        sink.set(LangDataKeys.MODULE_CONTEXT, moduleContext(selected));
        sink.lazy(LangDataKeys.MODULE_CONTEXT_ARRAY, () -> selectedModules(selected));
        sink.set(ModuleGroup.ARRAY_DATA_KEY, selected instanceof ModuleGroup moduleGroup ? new ModuleGroup[]{moduleGroup} : null);
        sink.set(
            LibraryGroupElement.ARRAY_DATA_KEY,
            selected instanceof LibraryGroupElement libraryGroup ? new LibraryGroupElement[]{libraryGroup} : null
        );
        sink.set(
            NamedLibraryElement.ARRAY_DATA_KEY,
            selected instanceof NamedLibraryElement namedLibrary ? new NamedLibraryElement[]{namedLibrary} : null
        );
    }

    @RequiredReadAction
    private static PsiDirectory[] directoriesOf(@Nullable AbstractTreeNode node) {
        PsiElement element = node != null && node.getValue() instanceof PsiElement value && value.isValid() ? value : null;
        if (element instanceof PsiDirectory directory) {
            return new PsiDirectory[]{directory};
        }

        PsiFile containingFile = element == null ? null : element.getContainingFile();
        PsiDirectory parent = containingFile == null ? null : containingFile.getContainingDirectory();
        return parent == null ? PsiDirectory.EMPTY_ARRAY : new PsiDirectory[]{parent};
    }

    private @Nullable Module moduleContext(@Nullable Object selected) {
        if (selected instanceof Module module) {
            return !module.isDisposed() ? module : null;
        }
        if (selected instanceof PsiDirectory directory) {
            return moduleBySingleContentRoot(directory.getVirtualFile());
        }
        if (selected instanceof VirtualFile virtualFile) {
            return moduleBySingleContentRoot(virtualFile);
        }
        return null;
    }

    @RequiredReadAction
    private Module @Nullable [] selectedModules(@Nullable Object selected) {
        List<Module> result = new ArrayList<>();
        if (selected instanceof ModuleGroup moduleGroup) {
            result.addAll(moduleGroup.modulesInGroup(myProject, true));
        }
        else {
            Module module = moduleContext(selected);
            if (module != null) {
                result.add(module);
            }
        }
        return result.isEmpty() ? null : result.toArray(Module.EMPTY_ARRAY);
    }

    /**
     * Project view has the same node for module and its single content root
     * => MODULE_CONTEXT data key should return the module when its content root is selected
     * When there are multiple content roots, they have different nodes under the module node
     * => MODULE_CONTEXT should be only available for the module node
     * otherwise VirtualFileArrayRule will return all module's content roots when just one of them is selected
     */
    private @Nullable Module moduleBySingleContentRoot(VirtualFile file) {
        if (ProjectRootsUtil.isModuleContentRoot(file, myProject)) {
            Module module = ProjectRootManager.getInstance(myProject).getFileIndex().getModuleForFile(file);
            if (module != null && !module.isDisposed() && ModuleRootManager.getInstance(module).getContentRoots().length == 1) {
                return module;
            }
        }

        return null;
    }

    @Override
    @RequiredUIAccess
    public PsiElement[] getSelectedPsiElements() {
        AbstractTreeNode selectedNode = takeSelectedNode();
        return selectedNode != null && selectedNode.getValue() instanceof PsiElement element && element.isValid()
            ? new PsiElement[]{element}
            : PsiElement.EMPTY_ARRAY;
    }

    @Override
    @RequiredUIAccess
    public @Nullable PsiElement getParentOfCurrentSelection() {
        List<TreeNode<AbstractTreeNode>> path = myTree.getSelectedPath();
        if (path.size() < 2) {
            return null;
        }
        AbstractTreeNode parent = path.get(path.size() - 2).getValue();
        if (parent instanceof ProjectViewNode<?> node && node.getValue() instanceof PsiElement element) {
            return element.isValid() ? element : null;
        }
        return null;
    }

    @Override
    @RequiredUIAccess
    public void onShow() {
        String subId = myPane.getSubId();
        if (!myShown) {
            myShown = true;
            myShownSubId = subId;
            restoreExpandedPaths().whenComplete(this::onRestoreFinished);
        }
        else if (!Objects.equals(myShownSubId, subId)) {
            myShownSubId = subId;
            myTree.refreshAll()
                .thenCompose(ignored -> restoreExpandedPaths())
                .whenComplete(this::onRestoreFinished);
        }
    }

    @Override
    @RequiredUIAccess
    public void onHide() {
        saveExpandedPaths();
    }

    private void refreshLoadedPresentations(@Nullable VirtualFile file, boolean refreshChildren) {
        myProject.getUIAccess().giveIfNeed(() -> {
            TreeNode<AbstractTreeNode> root = myTree.getRootNode();
            if (root != null) {
                refreshLoadedPresentations(myTree, root, file, refreshChildren);
            }
        });
    }

    private static void refreshLoadedPresentations(
        Tree<AbstractTreeNode> tree,
        TreeNode<AbstractTreeNode> node,
        @Nullable VirtualFile file,
        boolean refreshChildren
    ) {
        for (TreeNode<AbstractTreeNode> child : node.getLoadedChildren()) {
            if (file == null
                || child.getValue() instanceof ProjectViewNode viewNode && file.equals(viewNode.getVirtualFile())) {
                tree.refreshItem(child, refreshChildren);
            }

            refreshLoadedPresentations(tree, child, file, refreshChildren);
        }
    }

    private void saveExpandedPaths() {
        UITreeState treeState = UITreeState.createOn(myTree);
        if (treeState.isEmpty()) {
            return;
        }

        myTreeStates.put(myPane.getSubId(), treeState);
    }

    private void onRestoreFinished(@Nullable Object ignored, @Nullable Throwable error) {
        if (error != null) {
            LOG.warn("Failed to restore expanded paths of " + myPane.getId(), error);
        }
    }

    private CompletableFuture<?> restoreExpandedPaths() {
        UITreeState treeState = myTreeStates.get(myPane.getSubId());
        if (treeState == null) {
            return CompletableFuture.completedFuture(null);
        }

        return treeState.applyTo(myTree);
    }

    @Override
    public void readExternal(Element paneElement) {
        myTreeStates.clear();
        for (Element subPane : paneElement.getChildren(ELEMENT_SUB_PANE)) {
            UITreeState treeState = XmlSerializer.deserialize(subPane, UITreeState.class);
            if (treeState != null && !treeState.isEmpty()) {
                myTreeStates.put(subPane.getAttributeValue(ATTRIBUTE_SUB_ID), treeState);
            }
        }
    }

    @Override
    public void writeExternal(Element paneElement) {
        saveExpandedPaths();

        for (Map.Entry<@Nullable String, UITreeState> entry : myTreeStates.entrySet()) {
            String subId = entry.getKey();

            paneElement.getChildren(ELEMENT_SUB_PANE).removeIf(it -> Objects.equals(subId, it.getAttributeValue(ATTRIBUTE_SUB_ID)));

            Element subPane = new Element(ELEMENT_SUB_PANE);
            if (subId != null) {
                subPane.setAttribute(ATTRIBUTE_SUB_ID, subId);
            }
            XmlSerializer.serializeInto(entry.getValue(), subPane);
            paneElement.addContent(subPane);
        }
    }

    @Override
    @RequiredUIAccess
    public CompletableFuture<?> rebuild() {
        saveExpandedPaths();

        return myTree.refreshAll()
            .thenCompose(ignored -> restoreExpandedPaths())
            .whenComplete(this::onRestoreFinished);
    }

    @Override
    @RequiredUIAccess
    public CompletableFuture<?> reRestoreExpandedPaths() {
        return myTree.refreshAll()
            .thenComposeAsync(ignored -> restoreExpandedPaths(), UIAccess.current())
            .whenComplete(this::onRestoreFinished);
    }

    @Override
    public void queueUpdate() {
        refreshLoadedPresentations(null, true);
    }

    @Override
    @RequiredUIAccess
    public CompletableFuture<?> select(@Nullable Object element, @Nullable VirtualFile file, boolean requestFocus) {
        TreeNode<AbstractTreeNode> rootNode = myTree.getRootNode();
        if (rootNode == null || element == null && file == null) {
            return CompletableFuture.completedFuture(null);
        }

        CompletableFuture<TreeNode<AbstractTreeNode>> byElement = element == null
            ? CompletableFuture.completedFuture(null)
            : rootNode.findChildDeep(node -> node != null && node.canRepresent(element));

        return byElement
            .thenCompose(found -> found != null || file == null
                ? CompletableFuture.completedFuture(found)
                : rootNode.findChildDeep(node -> node != null && file.equals(virtualFileOf(node))))
            .whenCompleteAsync(
                (treeNode, throwable) -> {
                    if (treeNode != null) {
                        myTree.select(treeNode);
                        if (requestFocus && myTree instanceof HasFocus hasFocus) {
                            hasFocus.focus();
                        }
                    }
                },
                UIAccess.current()
            );
    }

    @RequiredReadAction
    private static @Nullable VirtualFile virtualFileOf(AbstractTreeNode node) {
        if (node instanceof ProjectViewNode<?> viewNode) {
            VirtualFile file = viewNode.getVirtualFile();
            if (file != null) {
                return file;
            }
        }
        return node.getValue() instanceof PsiElement element && element.isValid()
            ? PsiUtilCore.getVirtualFile(element)
            : null;
    }

    private class MyTreeExpander implements TreeExpander {
        @Override
        public void expandAll() {
            myTree.expandAll();
        }

        @Override
        public boolean canExpand() {
            return myTree.isExpandCollapseAllSupported();
        }

        @Override
        public boolean isExpandAllVisible() {
            return canExpand();
        }

        @Override
        public void collapseAll() {
            myTree.collapseAll();
        }

        @Override
        public boolean canCollapse() {
            return canExpand();
        }

        @Override
        public boolean isCollapseAllVisible() {
            return canExpand();
        }
    }

    private class MyPsiTreeChangeListener extends PsiTreeChangeAdapter {
        private final PsiModificationTracker myModificationTracker = PsiManager.getInstance(myProject).getModificationTracker();

        private long myModificationCount = -1;

        @Override
        public void childAdded(PsiTreeChangeEvent event) {
            if (!(event.getNewChild() instanceof PsiWhiteSpace)) {
                structureChanged(event.getFile());
            }
        }

        @Override
        public void childRemoved(PsiTreeChangeEvent event) {
            if (!(event.getOldChild() instanceof PsiWhiteSpace)) {
                structureChanged(event.getFile());
            }
        }

        @Override
        public void childReplaced(PsiTreeChangeEvent event) {
            if (!(event.getOldChild() instanceof PsiWhiteSpace && event.getNewChild() instanceof PsiWhiteSpace)) {
                structureChanged(event.getFile());
            }
        }

        @Override
        public void childMoved(PsiTreeChangeEvent event) {
            structureChanged(event.getFile());
        }

        @Override
        public void childrenChanged(PsiTreeChangeEvent event) {
            structureChanged(event.getFile());
        }

        @Override
        public void propertyChanged(PsiTreeChangeEvent event) {
            String propertyName = event.getPropertyName();

            if (PsiTreeChangeEvent.PROP_ROOTS.equals(propertyName)
                || PsiTreeChangeEvent.PROP_FILE_NAME.equals(propertyName)
                || PsiTreeChangeEvent.PROP_DIRECTORY_NAME.equals(propertyName)
                || PsiTreeChangeEvent.PROP_FILE_TYPES.equals(propertyName)
                || PsiTreeChangeEvent.PROP_UNLOADED_PSI.equals(propertyName)) {
                structureChanged(null);
            }
        }

        private void structureChanged(@Nullable PsiFile psiFile) {
            long newModificationCount = myModificationTracker.getModificationCount();
            if (newModificationCount == myModificationCount) {
                return;
            }
            myModificationCount = newModificationCount;

            VirtualFile file = psiFile != null ? psiFile.getVirtualFile() : null;
            if (file != null) {
                refreshLoadedPresentations(file, true);
                return;
            }

            scheduleStructureRefresh();
        }
    }

    private void scheduleStructureRefresh() {
        if (myStructureRefreshScheduled) {
            return;
        }

        myStructureRefreshScheduled = true;

        myProject.getUIAccess().give(() -> {
            myStructureRefreshScheduled = false;

            rebuild();
        });
    }

    @Override
    public void dispose() {
        myPane.setRebuildHandler(null);
    }
}
