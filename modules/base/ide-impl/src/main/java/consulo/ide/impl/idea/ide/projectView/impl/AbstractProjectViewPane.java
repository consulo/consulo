// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.ide.impl.idea.ide.projectView.impl;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.application.util.registry.Registry;
import consulo.component.extension.ExtensionPointName;
import consulo.component.util.BusyObject;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.dataContext.DataSink;
import consulo.dataContext.UiDataProvider;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.ide.dnd.TransferableWrapper;
import consulo.ide.impl.idea.ide.projectView.BaseProjectTreeBuilder;
import consulo.ide.impl.idea.ide.projectView.impl.nodes.AbstractModuleNode;
import consulo.ide.impl.idea.ide.projectView.impl.nodes.AbstractProjectNode;
import consulo.ide.impl.idea.ide.projectView.impl.nodes.ModuleGroupNode;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.actions.ModuleDeleteProvider;
import consulo.ide.impl.idea.ui.tree.project.ProjectFileNode;
import consulo.ide.localize.IdeLocalize;
import consulo.language.editor.CommonDataKeys;
import consulo.language.editor.LangDataKeys;
import consulo.language.editor.PlatformDataKeys;
import consulo.language.editor.PsiCopyPasteManager;
import consulo.language.editor.refactoring.move.MoveHandler;
import consulo.language.editor.wolfAnalyzer.ProblemListener;
import consulo.language.file.inject.VirtualFileWindow;
import consulo.language.psi.*;
import consulo.language.util.ModuleUtilCore;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.layer.orderEntry.LibraryOrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.navigation.Navigatable;
import consulo.project.Project;
import consulo.project.ui.view.ProjectView;
import consulo.project.ui.view.ProjectViewPane;
import consulo.project.ui.view.ProjectViewPaneOptionProvider;
import consulo.project.ui.view.SelectInTarget;
import consulo.project.ui.view.internal.node.LibraryGroupElement;
import consulo.project.ui.view.internal.node.NamedLibraryElement;
import consulo.project.ui.view.tree.*;
import consulo.project.ui.wm.ToolWindowId;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.DeleteProvider;
import consulo.ui.ex.TreeExpander;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.dnd.*;
import consulo.ui.ex.awt.tree.*;
import consulo.ui.ex.tree.AbstractTreeStructure;
import consulo.ui.ex.tree.NodeDescriptor;
import consulo.ui.ex.util.InvokerSupplier;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.JBIterable;
import consulo.util.concurrent.ActionCallback;
import consulo.util.concurrent.AsyncResult;
import consulo.util.concurrent.Promise;
import consulo.util.concurrent.Promises;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.KeyWithDefaultValue;
import consulo.util.dataholder.UserDataHolderBase;
import consulo.util.lang.Comparing;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.Pair;
import consulo.util.xml.serializer.JDOMExternalizerUtil;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.dnd.DnDConstants;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

@ExtensionAPI(ComponentScope.PROJECT)
public abstract class AbstractProjectViewPane extends UserDataHolderBase implements ProjectViewPane, UiDataProvider, BusyObject {
    private static final Logger LOG = Logger.getInstance(AbstractProjectViewPane.class);
    public static final ExtensionPointName<AbstractProjectViewPane> EP_NAME = ExtensionPointName.create(AbstractProjectViewPane.class);

    protected final Project myProject;
    protected DnDAwareTree myTree;
    protected AbstractTreeStructure myTreeStructure;
    private AbstractTreeBuilder myTreeBuilder;
    private TreeExpander myTreeExpander;

    // subId->Tree state; key may be null
    private final Map<String, TreeState> myReadTreeState = new HashMap<>();
    private final AtomicBoolean myTreeStateRestored = new AtomicBoolean();
    private String mySubId;
    private static final String ELEMENT_SUB_PANE = "subPane";
    private static final String ATTRIBUTE_SUB_ID = "subId";

    private DnDTarget myDropTarget;
    private DnDSource myDragSource;
    private DnDManager myDndManager;

    private final DeleteProvider myDeletePSIElementProvider = new DeleteProvider() {
        @Override
        public boolean canDeleteElement(DataContext dataContext) {
            PsiElement[] elements = getSelectedPSIElements();
            return consulo.ide.impl.idea.ide.util.DeleteHandler.shouldEnableDeleteAction(elements);
        }

        @Override
        public void deleteElement(DataContext dataContext) {
            List<PsiElement> validElements = new ArrayList<>();
            for (PsiElement psiElement : getSelectedPSIElements()) {
                if (psiElement != null && psiElement.isValid()) {
                    validElements.add(psiElement);
                }
            }
            PsiElement[] elements = PsiUtilCore.toPsiElementArray(validElements);
            consulo.localHistory.LocalHistoryAction a =
                consulo.localHistory.LocalHistory.getInstance().startAction(
                    consulo.ide.localize.IdeLocalize.progressDeleting());
            try {
                consulo.ide.impl.idea.ide.util.DeleteHandler.deletePsiElement(elements, myProject);
            }
            finally {
                a.finish();
            }
        }
    };

    protected AbstractProjectViewPane(Project project) {
        myProject = project;
        ProblemListener problemListener = new ProblemListener() {
            @Override
            public void problemsAppeared(VirtualFile file) {
                queueUpdate();
            }

            @Override
            public void problemsChanged(VirtualFile file) {
                queueUpdate();
            }

            @Override
            public void problemsDisappeared(VirtualFile file) {
                queueUpdate();
            }
        };
        project.getMessageBus().connect(this).subscribe(ProblemListener.class, problemListener);
        Disposer.register(project, this);
    }

    public abstract LocalizeValue getTitle();

    @Override
    public abstract String getId();

    @Override
    @Nullable
    public final String getSubId() {
        return mySubId;
    }

    public final void setSubId(@Nullable String subId) {
        if (Comparing.strEqual(mySubId, subId)) {
            return;
        }
        saveExpandedPaths();
        mySubId = subId;
        onSubIdChange();
    }

    protected void onSubIdChange() {
    }

    public boolean isInitiallyVisible() {
        return true;
    }

    public boolean supportsManualOrder() {
        return false;
    }

    protected String getManualOrderOptionText() {
        return IdeLocalize.actionManualOrder().get();
    }

    /**
     * @return all supported sub views IDs.
     * should return empty array if there is no subViews as in Project/Packages view.
     */
    @Override
    public String[] getSubIds() {
        return ArrayUtil.EMPTY_STRING_ARRAY;
    }

    @Override
    public LocalizeValue getPresentableSubIdName(String subId) {
        throw new IllegalStateException("should not call");
    }

    public abstract JComponent createComponent();

    public JComponent getComponentToFocus() {
        return myTree;
    }

    private TreeExpander getTreeExpander() {
        TreeExpander expander = myTreeExpander;
        if (expander == null) {
            expander = createTreeExpander();
            myTreeExpander = expander;
        }
        return expander;
    }

    protected TreeExpander createTreeExpander() {
        return new DefaultTreeExpander(this::getTree) {
            private boolean isExpandAllAllowed() {
                JTree tree = getTree();
                TreeModel model = tree == null ? null : tree.getModel();
                return model == null || model instanceof AsyncTreeModel || model instanceof InvokerSupplier;
            }

            @Override
            public boolean isExpandAllVisible() {
                return isExpandAllAllowed();// && Registry.is("ide.project.view.expand.all.action.visible", true);
            }

            @Override
            public boolean canExpand() {
                return isExpandAllAllowed() && super.canExpand();
            }

            @Override
            protected void collapseAll(JTree tree, boolean strict, int keepSelectionLevel) {
                super.collapseAll(tree, false, keepSelectionLevel);
            }
        };
    }

    public void expand(@Nullable Object[] path, boolean requestFocus) {
        if (getTreeBuilder() == null || path == null) {
            return;
        }
        AbstractTreeUi ui = getTreeBuilder().getUi();
        if (ui != null) {
            ui.buildNodeForPath(path);
        }

        DefaultMutableTreeNode node = ui == null ? null : ui.getNodeForPath(path);
        if (node == null) {
            return;
        }
        TreePath treePath = new TreePath(node.getPath());
        myTree.expandPath(treePath);
        if (requestFocus) {
            IdeFocusManager.getGlobalInstance()
                .doWhenFocusSettlesDown(() -> IdeFocusManager.getGlobalInstance().requestFocus(myTree, true));
        }
        TreeUtil.selectPath(myTree, treePath);
    }

    @Override
    public void dispose() {
        if (myDndManager != null) {
            if (myDropTarget != null) {
                myDndManager.unregisterTarget(myDropTarget, myTree);
                myDropTarget = null;
            }
            if (myDragSource != null) {
                myDndManager.unregisterSource(myDragSource, myTree);
                myDragSource = null;
            }
            myDndManager = null;
        }
        setTreeBuilder(null);
        myTree = null;
        myTreeStructure = null;
    }

    @Override
    public abstract ActionCallback updateFromRoot(boolean restoreExpandedPaths);

    public void updateFrom(Object element, boolean forceResort, boolean updateStructure) {
        AbstractTreeBuilder builder = getTreeBuilder();
        if (builder != null) {
            builder.queueUpdateFrom(element, forceResort, updateStructure);
        }
        else if (element instanceof PsiElement psiElement) {
            AsyncProjectViewSupport support = getAsyncSupport();
            if (support != null) {
                support.updateByElement(psiElement, updateStructure);
            }
        }
    }

    @Override
    public abstract void select(Object element, VirtualFile file, boolean requestFocus);

    @Override
    public void selectModule(Module module, boolean requestFocus) {
        doSelectModuleOrGroup(module, requestFocus);
    }

    @RequiredUIAccess
    private void doSelectModuleOrGroup(Object toSelect, boolean requestFocus) {
        ToolWindowManager windowManager = ToolWindowManager.getInstance(myProject);
        Runnable runnable = () -> {
            if (requestFocus) {
                ProjectView projectView = ProjectView.getInstance(myProject);
                if (projectView != null) {
                    projectView.changeView(getId(), getSubId());
                }
            }
            BaseProjectTreeBuilder builder = (BaseProjectTreeBuilder) getTreeBuilder();
            if (builder != null) {
                builder.selectInWidth(
                    toSelect,
                    requestFocus,
                    node -> node instanceof AbstractModuleNode || node instanceof ModuleGroupNode || node instanceof AbstractProjectNode
                );
            }
        };
        if (requestFocus) {
            windowManager.getToolWindow(ToolWindowId.PROJECT_VIEW).activate(runnable);
        }
        else {
            runnable.run();
        }
    }

    @Override
    @RequiredUIAccess
    public void selectModuleGroup(ModuleGroup moduleGroup, boolean requestFocus) {
        doSelectModuleOrGroup(moduleGroup, requestFocus);
    }

    public TreePath[] getSelectionPaths() {
        return myTree == null ? null : myTree.getSelectionPaths();
    }

    public void addToolbarActions(DefaultActionGroup actionGroup) {
    }

    public void addToolbarActionsImpl(DefaultActionGroup actionGroup) {
        addToolbarActions(actionGroup);
        for (ProjectViewPaneOptionProvider provider : ProjectViewPaneOptionProvider.EX_NAME.getExtensionList()) {
            provider.addToolbarActions(this, actionGroup);
        }
    }

    protected <T extends NodeDescriptor> List<T> getSelectedNodes(Class<T> nodeClass) {
        TreePath[] paths = getSelectionPaths();
        if (paths == null) {
            return Collections.emptyList();
        }
        ArrayList<T> result = new ArrayList<>();
        for (TreePath path : paths) {
            T userObject = TreeUtil.getLastUserObject(nodeClass, path);
            if (userObject != null) {
                result.add(userObject);
            }
        }
        return result;
    }

    @Override
    public void uiDataSnapshot(DataSink sink) {
        TreePath[] paths = getSelectionPaths();
        Object[] selectedUserObjects =
            paths == null ? ArrayUtil.EMPTY_OBJECT_ARRAY :
                ArrayUtil.toObjectArray(ContainerUtil.mapNotNull(paths, TreeUtil::getLastUserObject));
        Object[] singleSelectedPathUserObjects =
            paths == null || paths.length != 1 ? null :
                ArrayUtil.toObjectArray(ContainerUtil.map(paths[0].getPath(), TreeUtil::getUserObject));

        if (paths != null) {
            ArrayList<Navigatable> navigatables = new ArrayList<>();
            for (TreePath path : paths) {
                Object node = path.getLastPathComponent();
                Object userObject = TreeUtil.getUserObject(node);
                if (userObject instanceof Navigatable o) {
                    navigatables.add(o);
                }
                else if (node instanceof Navigatable o) {
                    navigatables.add(o);
                }
            }
            Navigatable[] cachedNavigatables = getCachedNavigatablesFromSelectedPaths(paths);
            navigatables.addAll(Arrays.asList(cachedNavigatables));
            sink.set(CommonDataKeys.NAVIGATABLE_ARRAY,
                navigatables.isEmpty() ? null : navigatables.toArray(Navigatable.EMPTY_ARRAY));
        }
        uiDataSnapshotForSelection(sink, selectedUserObjects, singleSelectedPathUserObjects);

        if (myTreeStructure instanceof AbstractTreeStructureBase treeStructure) {
            List<TreeStructureProvider> providers = treeStructure.getProviders();
            if (providers != null && !providers.isEmpty()) {
                //noinspection unchecked
                List<AbstractTreeNode<?>> selection = (List) ContainerUtil.filterIsInstance(
                    selectedUserObjects, AbstractTreeNode.class);
                for (TreeStructureProvider provider : ContainerUtil.reverse(providers)) {
                    provider.uiDataSnapshot(sink, selection);
                }
            }
        }
        sink.set(CommonDataKeys.PROJECT, myProject);
        sink.set(PlatformDataKeys.SELECTED_ITEMS, selectedUserObjects);
        sink.set(PlatformDataKeys.TREE_EXPANDER, getTreeExpander());
    }

    protected void uiDataSnapshotForSelection(DataSink sink, Object[] selectedUserObjects,
                                              @Nullable Object[] singleSelectedPathUserObjects) {
        sink.lazy(CommonDataKeys.PSI_ELEMENT, () -> {
            PsiElement[] elements = getPsiElements(selectedUserObjects);
            return elements.length == 1 ? elements[0] : null;
        });
        sink.lazy(CommonDataKeys.PSI_ELEMENT_ARRAY, () -> {
            PsiElement[] elements = getPsiElements(selectedUserObjects);
            return elements.length > 0 ? elements : null;
        });
        sink.lazy(PlatformDataKeys.PROJECT_CONTEXT, () -> {
            Object selected = getSingleNodeElement(selectedUserObjects);
            return selected instanceof Project o ? o : null;
        });
        sink.lazy(LangDataKeys.MODULE_CONTEXT, () -> {
            Object selected = getSingleNodeElement(selectedUserObjects);
            return moduleContext(myProject, selected);
        });
        sink.lazy(LangDataKeys.MODULE_CONTEXT_ARRAY, () -> {
            return getSelectedModules(selectedUserObjects);
        });
//        sink.lazy(ProjectView.UNLOADED_MODULES_CONTEXT_KEY, () -> {
//            return Collections.unmodifiableList(getSelectedUnloadedModules(selectedUserObjects));
//        });
        sink.lazy(PlatformDataKeys.DELETE_ELEMENT_PROVIDER, () -> {
            Module[] modules = getSelectedModules(selectedUserObjects);
            if (modules != null || !getSelectedUnloadedModules(selectedUserObjects).isEmpty()) {
                return ModuleDeleteProvider.getInstance();
            }
            LibraryOrderEntry orderEntry = getSelectedLibrary(singleSelectedPathUserObjects);
            if (orderEntry != null) {
                return new DetachLibraryDeleteProvider(myProject, orderEntry);
            }
            return myDeletePSIElementProvider;
        });
        sink.lazy(ModuleGroup.ARRAY_DATA_KEY, () -> {
            List<ModuleGroup> selectedElements = getSelectedValues(selectedUserObjects, ModuleGroup.class);
            return selectedElements.isEmpty() ? null : selectedElements.toArray(new ModuleGroup[0]);
        });
        sink.lazy(LibraryGroupElement.ARRAY_DATA_KEY, () -> {
            List<LibraryGroupElement> selectedElements = getSelectedValues(selectedUserObjects, LibraryGroupElement.class);
            return selectedElements.isEmpty() ? null : selectedElements.toArray(new LibraryGroupElement[0]);
        });
        sink.lazy(NamedLibraryElement.ARRAY_DATA_KEY, () -> {
            List<NamedLibraryElement> selectedElements = getSelectedValues(selectedUserObjects, NamedLibraryElement.class);
            return selectedElements.isEmpty() ? null : selectedElements.toArray(new NamedLibraryElement[0]);
        });
    }

    @RequiredReadAction
    @NotNull
    private PsiElement[] getPsiElements(Object[] selectedUserObjects) {
        List<PsiElement> result = new ArrayList<>();
        for (Object userObject : selectedUserObjects) {
            ContainerUtil.addAllNotNull(result, getElementsFromNode(userObject));
        }
        return PsiUtilCore.toPsiElementArray(result);
    }

    private static @Nullable Object getSingleNodeElement(@Nullable Object[] selectedUserObjects) {
        if (selectedUserObjects.length != 1) {
            return null;
        }
        return getNodeElement(selectedUserObjects[0]);
    }

    @Nullable
    private static Object getNodeElement(@Nullable Object userObject) {
        if (userObject instanceof AbstractTreeNode<?> node) {
            return node.getValue();
        }
        if (userObject instanceof NodeDescriptor<?> descriptor) {
            return descriptor.getElement();
        }
        return null;
    }

    @Nullable
    static Module moduleContext(Project project, @Nullable Object element) {
        if (element instanceof Module module) {
            return module.isDisposed() ? null : module;
        }
        if (element instanceof PsiDirectory directory) {
            return ModuleUtilCore.findModuleForFile(directory.getVirtualFile(), project);
        }
        if (element instanceof VirtualFile file) {
            return ModuleUtilCore.findModuleForFile(file, project);
        }
        return null;
    }

    @Nullable
    private Module[] getSelectedModules(Object[] selectedUserObjects) {
        List<Module> result = new ArrayList<>();
        for (Object value : getSelectedValues(selectedUserObjects)) {
            Module module = moduleContext(myProject, value);
            if (module != null) {
                result.add(module);
            }
            else if (value instanceof ModuleGroup moduleGroup) {
                result.addAll(moduleGroup.modulesInGroup(myProject, true));
            }
        }
        return result.isEmpty() ? null : result.toArray(Module.EMPTY_ARRAY);
    }

    private List<Object> getSelectedUnloadedModules(Object[] selectedUserObjects) {
        // Consulo does not support unloaded modules
        return Collections.emptyList();
    }

    @Nullable
    private static LibraryOrderEntry getSelectedLibrary(@Nullable Object[] userObjectsPath) {
        if (userObjectsPath == null) {
            return null;
        }
        // Check if parent node is a library group node and current node is a named library element node
        Object parentObject = userObjectsPath.length >= 2 ? userObjectsPath[userObjectsPath.length - 2] : null;
        if (!(parentObject instanceof AbstractTreeNode<?> parentNode
            && parentNode.getValue() instanceof LibraryGroupElement)) {
            return null;
        }
        Object userObject = userObjectsPath[userObjectsPath.length - 1];
        if (userObject instanceof AbstractTreeNode<?> node && node.getValue() instanceof NamedLibraryElement namedLib) {
            OrderEntry orderEntry = namedLib.getOrderEntry();
            return orderEntry instanceof LibraryOrderEntry libraryEntry ? libraryEntry : null;
        }
        return null;
    }

    protected Navigatable[] getCachedNavigatablesFromSelectedPaths(TreePath[] paths) {
        return Navigatable.EMPTY_ARRAY;
    }

    private <T> List<T> getSelectedValues(@Nullable Object[] selectedUserObjects, Class<T> aClass) {
        return ContainerUtil.filterIsInstance(getSelectedValues(selectedUserObjects), aClass);
    }

    public final Object[] getSelectedValues(@Nullable Object[] selectedUserObjects) {
        List<Object> result = new ArrayList<>(selectedUserObjects.length);
        for (Object userObject : selectedUserObjects) {
            Object valueFromNode = getValueFromNode(userObject);
            if (valueFromNode instanceof Object[]) {
                for (Object value : (Object[]) valueFromNode) {
                    if (value != null) {
                        result.add(value);
                    }
                }
            }
            else if (valueFromNode != null) {
                result.add(valueFromNode);
            }
        }
        return ArrayUtil.toObjectArray(result);
    }

    // used for sorting tabs in the tabbed pane
    public abstract int getWeight();

    @Override
    public abstract SelectInTarget createSelectInTarget();

    public final TreePath getSelectedPath() {
        return myTree == null ? null : TreeUtil.getSelectedPathIfOne(myTree);
    }

    @Override
    public final NodeDescriptor getSelectedDescriptor() {
        return TreeUtil.getLastUserObject(NodeDescriptor.class, getSelectedPath());
    }

    /**
     * @see TreeUtil#getUserObject(Object)
     * @deprecated AbstractProjectViewPane#getSelectedPath
     */
    @Deprecated
    public final DefaultMutableTreeNode getSelectedNode() {
        TreePath path = getSelectedPath();
        return path == null ? null : ObjectUtil.tryCast(path.getLastPathComponent(), DefaultMutableTreeNode.class);
    }

    public final Object getSelectedElement() {
        Object[] elements = getSelectedElements();
        return elements.length == 1 ? elements[0] : null;
    }

    public final PsiElement[] getSelectedPSIElements() {
        TreePath[] paths = getSelectionPaths();
        if (paths == null) {
            return PsiElement.EMPTY_ARRAY;
        }
        List<PsiElement> result = new ArrayList<>();
        for (TreePath path : paths) {
            result.addAll(getElementsFromNode(path.getLastPathComponent()));
        }
        return PsiUtilCore.toPsiElementArray(result);
    }

    public List<PsiElement> getElementsFromNode(@Nullable Object node) {
        Object value = getValueFromNode(node);
        JBIterable<?> it = value instanceof PsiElement || value instanceof VirtualFile
            ? JBIterable.of(value)
            : value instanceof Object[] objArr
            ? JBIterable.of(objArr)
            : value instanceof Iterable
            ? JBIterable.from((Iterable<?>) value)
            : JBIterable.of(TreeUtil.getUserObject(node));
        return it
            .flatten(o -> o instanceof RootsProvider rootsProvider ? rootsProvider.getRoots() : Collections.singleton(o))
            .map(o -> o instanceof VirtualFile virtualFile ? PsiUtilCore.findFileSystemItem(myProject, virtualFile) : o)
            .filter(PsiElement.class)
            .filter(PsiElement::isValid)
            .toList();
    }

    /**
     * @deprecated use {@link AbstractProjectViewPane#getElementsFromNode(Object)}
     **/
    @Deprecated
    @Nullable
    public PsiElement getPSIElementFromNode(@Nullable TreeNode node) {
        return ContainerUtil.getFirstItem(getElementsFromNode(node));
    }

    @Nullable
    @RequiredReadAction
    protected Module getNodeModule(@Nullable Object element) {
        if (element instanceof PsiElement psiElement) {
            return ModuleUtilCore.findModuleForPsiElement(psiElement);
        }
        return null;
    }

    public final Object[] getSelectedElements() {
        TreePath[] paths = getSelectionPaths();
        if (paths == null) {
            return PsiElement.EMPTY_ARRAY;
        }
        ArrayList<Object> list = new ArrayList<>(paths.length);
        for (TreePath path : paths) {
            Object lastPathComponent = path.getLastPathComponent();
            Object element = getValueFromNode(lastPathComponent);
            if (element instanceof Object[] array) {
                Collections.addAll(list, array);
            }
            else if (element != null) {
                list.add(element);
            }
        }
        return ArrayUtil.toObjectArray(list);
    }

    @Nullable
    public Object getValueFromNode(@Nullable Object node) {
        return extractValueFromNode(node);
    }

    /**
     * @deprecated use {@link AbstractProjectViewPane#getValueFromNode(Object)}
     **/
    @Deprecated
    protected Object exhumeElementFromNode(DefaultMutableTreeNode node) {
        return getValueFromNode(node);
    }

    @Nullable
    public static Object extractValueFromNode(@Nullable Object node) {
        Object userObject = TreeUtil.getUserObject(node);
        Object element = null;
        if (userObject instanceof AbstractTreeNode descriptor) {
            element = descriptor.getValue();
        }
        else if (userObject instanceof NodeDescriptor descriptor) {
            element = descriptor.getElement();
            if (element instanceof AbstractTreeNode treeNode) {
                element = treeNode.getValue();
            }
        }
        else if (userObject != null) {
            element = userObject;
        }
        return element;
    }

    public AbstractTreeBuilder getTreeBuilder() {
        return myTreeBuilder;
    }

    public AbstractTreeStructure getTreeStructure() {
        return myTreeStructure;
    }

    public void readExternal(Element element) {
        List<Element> subPanes = element.getChildren(ELEMENT_SUB_PANE);
        for (Element subPane : subPanes) {
            String subId = subPane.getAttributeValue(ATTRIBUTE_SUB_ID);
            TreeState treeState = TreeState.createFrom(subPane);
            if (!treeState.isEmpty()) {
                myReadTreeState.put(subId, treeState);
            }
        }

        for (ProjectViewPaneOptionProvider provider : ProjectViewPaneOptionProvider.EX_NAME.getExtensionList()) {
            KeyWithDefaultValue key = provider.getKey();

            String valueOfKey = JDOMExternalizerUtil.readField(element, key.toString());
            if (valueOfKey != null) {
                putUserData(key, provider.parseValue(valueOfKey));
            }
        }
    }

    public void writeExternal(Element element) {
        saveExpandedPaths();
        for (Map.Entry<String, TreeState> entry : myReadTreeState.entrySet()) {
            String subId = entry.getKey();
            TreeState treeState = entry.getValue();
            Element subPane = new Element(ELEMENT_SUB_PANE);
            if (subId != null) {
                subPane.setAttribute(ATTRIBUTE_SUB_ID, subId);
            }
            treeState.writeExternal(subPane);
            element.addContent(subPane);
        }

        for (ProjectViewPaneOptionProvider provider : ProjectViewPaneOptionProvider.EX_NAME.getExtensionList()) {
            KeyWithDefaultValue key = provider.getKey();
            Object value = getUserData(key);
            //noinspection unchecked
            String stringValue = provider.toString(value);
            if (stringValue != null) {
                JDOMExternalizerUtil.writeField(element, key.toString(), stringValue);
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getUserData(Key<T> key) {
        T value = super.getUserData(key);
        if (value == null && key instanceof KeyWithDefaultValue keyWithDefaultValue) {
            return (T) keyWithDefaultValue.getDefaultValue();
        }
        return value;
    }

    protected void saveExpandedPaths() {
        myTreeStateRestored.set(false);
        if (myTree != null) {
            TreeState treeState = TreeState.createOn(myTree);
            if (!treeState.isEmpty()) {
                myReadTreeState.put(getSubId(), treeState);
            }
            else {
                myReadTreeState.remove(getSubId());
            }
        }
    }

    public final void restoreExpandedPaths() {
        if (myTree == null || myTreeStateRestored.getAndSet(true)) {
            return;
        }
        TreeState treeState = myReadTreeState.get(getSubId());
        if (treeState != null && !treeState.isEmpty()) {
            treeState.applyTo(myTree);
        }
        else if (myTree.isSelectionEmpty()) {
            TreeUtil.promiseSelectFirst(myTree);
        }
    }

    protected Comparator<NodeDescriptor> createComparator() {
        return new GroupByTypeComparator(ProjectView.getInstance(myProject), getId());
    }

    public void installComparator() {
        installComparator(getTreeBuilder());
    }

    void installComparator(AbstractTreeBuilder treeBuilder) {
        installComparator(treeBuilder, createComparator());
    }

    @TestOnly
    public void installComparator(Comparator<? super NodeDescriptor> comparator) {
        installComparator(getTreeBuilder(), comparator);
    }

    protected void installComparator(AbstractTreeBuilder builder, Comparator<? super NodeDescriptor> comparator) {
        if (builder != null) {
            builder.setNodeDescriptorComparator(comparator);
        }
    }

    public JTree getTree() {
        return myTree;
    }

    public PsiDirectory[] getSelectedDirectories() {
        List<PsiDirectory> directories = new ArrayList<>();
        for (PsiDirectoryNode node : getSelectedNodes(PsiDirectoryNode.class)) {
            PsiDirectory directory = node.getValue();
            if (directory != null) {
                directories.add(directory);
                Object parentValue = node.getParent().getValue();
                if (parentValue instanceof PsiDirectory && Registry.is("projectView.choose.directory.on.compacted.middle.packages")) {
                    while (true) {
                        directory = directory.getParentDirectory();
                        if (directory == null || directory.equals(parentValue)) {
                            break;
                        }
                        directories.add(directory);
                    }
                }
            }
        }
        if (!directories.isEmpty()) {
            return directories.toArray(PsiDirectory.EMPTY_ARRAY);
        }

        PsiElement[] elements = getSelectedPSIElements();
        if (elements.length == 1) {
            PsiElement element = elements[0];
            if (element instanceof PsiDirectory directory) {
                return new PsiDirectory[]{directory};
            }
            else if (element instanceof PsiDirectoryContainer directoryContainer) {
                return directoryContainer.getDirectories();
            }
            else {
                PsiFile containingFile = element.getContainingFile();
                if (containingFile != null) {
                    PsiDirectory psiDirectory = containingFile.getContainingDirectory();
                    if (psiDirectory != null) {
                        return new PsiDirectory[]{psiDirectory};
                    }
                    VirtualFile file = containingFile.getVirtualFile();
                    if (file instanceof VirtualFileWindow virtualFileWindow) {
                        VirtualFile delegate = virtualFileWindow.getDelegate();
                        PsiFile delegatePsiFile = containingFile.getManager().findFile(delegate);
                        if (delegatePsiFile != null && delegatePsiFile.getContainingDirectory() != null) {
                            return new PsiDirectory[]{delegatePsiFile.getContainingDirectory()};
                        }
                    }
                    return PsiDirectory.EMPTY_ARRAY;
                }
            }
        }
        else {
            TreePath path = getSelectedPath();
            if (path != null) {
                Object component = path.getLastPathComponent();
                if (component instanceof DefaultMutableTreeNode treeNode) {
                    return getSelectedDirectoriesInAmbiguousCase(treeNode.getUserObject());
                }
                return getSelectedDirectoriesInAmbiguousCase(component);
            }
        }
        return PsiDirectory.EMPTY_ARRAY;
    }

    protected PsiDirectory[] getSelectedDirectoriesInAmbiguousCase(Object userObject) {
        if (userObject instanceof AbstractModuleNode moduleNode) {
            Module module = moduleNode.getValue();
            if (module != null) {
                ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
                VirtualFile[] sourceRoots = moduleRootManager.getSourceRoots();
                List<PsiDirectory> dirs = new ArrayList<>(sourceRoots.length);
                PsiManager psiManager = PsiManager.getInstance(myProject);
                for (VirtualFile sourceRoot : sourceRoots) {
                    PsiDirectory directory = psiManager.findDirectory(sourceRoot);
                    if (directory != null) {
                        dirs.add(directory);
                    }
                }
                return dirs.toArray(PsiDirectory.EMPTY_ARRAY);
            }
        }
        else if (userObject instanceof ProjectViewNode projectViewNode) {
            VirtualFile file = projectViewNode.getVirtualFile();
            if (file != null && file.isValid() && file.isDirectory()) {
                PsiDirectory directory = PsiManager.getInstance(myProject).findDirectory(file);
                if (directory != null) {
                    return new PsiDirectory[]{directory};
                }
            }
        }
        return PsiDirectory.EMPTY_ARRAY;
    }

    // Drag'n'Drop stuff

    protected void enableDnD() {
        if (!myProject.getApplication().isHeadlessEnvironment()) {
            myDropTarget = new ProjectViewDropTarget(myTree, myProject) {
                @Nullable
                @Override
                protected PsiElement getPsiElement(TreePath path) {
                    return ContainerUtil.getFirstItem(getElementsFromNode(path.getLastPathComponent()));
                }

                @Nullable
                @Override
                protected Module getModule(PsiElement element) {
                    return getNodeModule(element);
                }

                @Override
                public void cleanUpOnLeave() {
                    beforeDnDLeave();
                    super.cleanUpOnLeave();
                }

                @Override
                public boolean update(DnDEvent event) {
                    beforeDnDUpdate();
                    return super.update(event);
                }
            };
            myDragSource = new MyDragSource();
            myDndManager = DnDManager.getInstance();
            myDndManager.registerSource(myDragSource, myTree);
            myDndManager.registerTarget(myDropTarget, myTree);
        }
    }

    protected void beforeDnDUpdate() {
    }

    protected void beforeDnDLeave() {
    }

    public void setTreeBuilder(AbstractTreeBuilder treeBuilder) {
        if (treeBuilder != null) {
            Disposer.register(this, treeBuilder);
// needs refactoring for project view first
//      treeBuilder.setCanYieldUpdate(true);
        }
        myTreeBuilder = treeBuilder;
    }

    public boolean supportsFoldersAlwaysOnTop() {
        return true;
    }

    public boolean supportsSortByType() {
        return true;
    }

    private class MyDragSource implements DnDSource {
        @Override
        public boolean canStartDragging(DnDAction action, Point dragOrigin) {
            if ((action.getActionId() & DnDConstants.ACTION_COPY_OR_MOVE) == 0) {
                return false;
            }
            Object[] elements = getSelectedElements();
            PsiElement[] psiElements = getSelectedPSIElements();
            DataContext dataContext = DataManager.getInstance().getDataContext(myTree);
            return psiElements.length > 0 || canDragElements(elements, dataContext, action.getActionId());
        }

        @Override
        public DnDDragStartBean startDragging(DnDAction action, Point dragOrigin) {
            PsiElement[] psiElements = getSelectedPSIElements();
            TreePath[] paths = getSelectionPaths();
            return new DnDDragStartBean(new TransferableWrapper() {

                @Override
                public List<File> asFileList() {
                    return PsiCopyPasteManager.asFileList(psiElements);
                }

                @Nullable
                @Override
                public TreePath[] getTreePaths() {
                    return paths;
                }

                @Override
                public TreeNode[] getTreeNodes() {
                    return TreePathUtil.toTreeNodes(getTreePaths());
                }

                @Override
                public PsiElement[] getPsiElements() {
                    return psiElements;
                }
            });
        }

        @Nullable
        @Override
        public Pair<Image, Point> createDraggedImage(DnDAction action, Point dragOrigin, DnDDragStartBean bean) {
            TreePath[] paths = getSelectionPaths();
            if (paths == null) {
                return null;
            }

            int count = paths.length;

            JLabel label = new JLabel(String.format("%s item%s", count, count == 1 ? "" : "s"));
            label.setOpaque(true);
            label.setForeground(myTree.getForeground());
            label.setBackground(myTree.getBackground());
            label.setFont(myTree.getFont());
            label.setSize(label.getPreferredSize());
            BufferedImage image = UIUtil.createImage(label.getWidth(), label.getHeight(), BufferedImage.TYPE_INT_ARGB);

            Graphics2D g2 = (Graphics2D) image.getGraphics();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
            label.paint(g2);
            g2.dispose();

            return new Pair<>(image, new Point(-image.getWidth(null), -image.getHeight(null)));
        }

        @Override
        public void dragDropEnd() {
        }

        @Override
        public void dropActionChanged(int gestureModifiers) {
        }
    }

    private static boolean canDragElements(Object[] elements, DataContext dataContext, int dragAction) {
        for (Object element : elements) {
            if (element instanceof Module) {
                return true;
            }
        }
        return dragAction == DnDConstants.ACTION_MOVE && MoveHandler.canMove(dataContext);
    }

    @Override
    public AsyncResult<Void> getReady(Object requestor) {
        if (myTreeBuilder == null) {
            return AsyncResult.resolved();
        }
        if (myTreeBuilder.isDisposed()) {
            return AsyncResult.rejected();
        }
        ActionCallback ready = myTreeBuilder.getUi().getReady(requestor);
        AsyncResult<Void> result = new AsyncResult<>();
        ready.doWhenDone(result::setDone);
        ready.doWhenRejected(result::reject);
        return result;
    }

    @TestOnly
    @Deprecated
    public Promise<TreePath> promisePathToElement(Object element) {
        AbstractTreeBuilder builder = getTreeBuilder();
        if (builder != null) {
            DefaultMutableTreeNode node = builder.getNodeForElement(element);
            if (node == null) {
                return Promises.rejectedPromise();
            }
            return Promises.resolvedPromise(new TreePath(node.getPath()));
        }
        TreeVisitor visitor = createVisitor(element);
        if (visitor == null || myTree == null) {
            return Promises.rejectedPromise();
        }
        return TreeUtil.promiseVisit(myTree, visitor);
    }

    AsyncProjectViewSupport getAsyncSupport() {
        return null;
    }

    @Override
    public void queueUpdate() {
        AbstractTreeBuilder treeBuilder = getTreeBuilder();
        if (treeBuilder != null) {
            treeBuilder.queueUpdate();
        }
    }

    static List<TreeVisitor> createVisitors(Object... objects) {
        List<TreeVisitor> list = new ArrayList<>();
        for (Object object : objects) {
            TreeVisitor visitor = createVisitor(object);
            ContainerUtil.addIfNotNull(list, visitor);
        }
        return Collections.unmodifiableList(list);
    }

    @Nullable
    public static TreeVisitor createVisitor(Object object) {
        if (object instanceof AbstractTreeNode node) {
            object = node.getValue();
        }
        if (object instanceof ProjectFileNode node) {
            object = node.getVirtualFile();
        }
        if (object instanceof VirtualFile virtualFile) {
            return createVisitor(virtualFile);
        }
        if (object instanceof PsiElement element) {
            return createVisitor(element);
        }
        LOG.warn("unsupported object: " + object);
        return null;
    }

    public static TreeVisitor createVisitor(VirtualFile file) {
        return createVisitor(null, file);
    }

    @Nullable
    public static TreeVisitor createVisitor(PsiElement element) {
        return createVisitor(element, null);
    }

    @Nullable
    public static TreeVisitor createVisitor(@Nullable PsiElement element, @Nullable VirtualFile file) {
        return createVisitor(element, file, null);
    }

    @Nullable
    static TreeVisitor createVisitor(@Nullable PsiElement element, @Nullable VirtualFile file, @Nullable List<? super TreePath> collector) {
        Predicate<? super TreePath> predicate = collector == null ? null : path -> {
            collector.add(path);
            return false;
        };
        if (element != null && element.isValid()) {
            return new ProjectViewNodeVisitor(element, file, predicate);
        }
        if (file != null) {
            return new ProjectViewFileVisitor(file, predicate);
        }
        LOG.warn(element != null ? "element invalidated: " + element : "cannot create visitor without element and/or file");
        return null;
    }
}
