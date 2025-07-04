/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

package consulo.ide.impl.idea.ide.favoritesTreeView;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.HelpManager;
import consulo.bookmark.ui.view.BookmarkNodeProvider;
import consulo.bookmark.ui.view.FavoritesListNode;
import consulo.bookmark.ui.view.FavoritesListProvider;
import consulo.bookmark.ui.view.FavoritesTreeNodeDescriptor;
import consulo.bookmark.ui.view.event.FavoritesListener;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataProvider;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.IdeView;
import consulo.ide.impl.idea.ide.favoritesTreeView.actions.*;
import consulo.ide.impl.idea.ide.ui.customization.CustomizationUtil;
import consulo.ide.impl.idea.ide.util.DeleteHandler;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.actions.ModuleDeleteProvider;
import consulo.ide.localize.IdeLocalize;
import consulo.ide.util.DirectoryChooserUtil;
import consulo.language.editor.LangDataKeys;
import consulo.language.editor.refactoring.ui.CopyPasteDelegator;
import consulo.language.editor.util.EditorHelper;
import consulo.language.psi.*;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.localHistory.LocalHistory;
import consulo.localHistory.LocalHistoryAction;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.navigation.ItemPresentation;
import consulo.navigation.Navigatable;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.ui.view.internal.node.LibraryGroupElement;
import consulo.project.ui.view.internal.node.NamedLibraryElement;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.project.ui.view.tree.ModuleGroup;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.project.ui.wm.dock.DockContainer;
import consulo.project.ui.wm.dock.DockManager;
import consulo.project.ui.wm.dock.DockableContent;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.*;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.dnd.DnDAwareTree;
import consulo.ui.ex.awt.internal.SwingDockContainer;
import consulo.ui.ex.awt.speedSearch.TreeSpeedSearch;
import consulo.ui.ex.awt.tree.EditSourceOnEnterKeyHandler;
import consulo.ui.ex.awt.tree.NodeRenderer;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.ui.ex.awt.tree.action.CollapseAllAction;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.util.collection.ArrayUtil;
import consulo.util.dataholder.Key;
import consulo.util.io.URLUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * @author anna
 * @author Konstantin Bulenkov
 */
public class FavoritesTreeViewPanel extends JPanel implements DataProvider, DockContainer, SwingDockContainer {
    private final FavoritesTreeStructure myFavoritesTreeStructure;
    private FavoritesViewTreeBuilder myBuilder;
    private final CopyPasteDelegator myCopyPasteDelegator;

    public static final Key<FavoritesTreeNodeDescriptor[]> CONTEXT_FAVORITES_ROOTS_DATA_KEY = Key.create("FavoritesRoot");
    public static final Key<DnDAwareTree> FAVORITES_TREE_KEY = Key.create("Favorites.Tree");
    public static final Key<FavoritesViewTreeBuilder> FAVORITES_TREE_BUILDER_KEY = Key.create("Favorites.Tree.Builder");
    public static final Key<String> FAVORITES_LIST_NAME_DATA_KEY = Key.create("FavoritesListName");

    protected Project myProject;
    protected DnDAwareTree myTree;

    private final MyDeletePSIElementProvider myDeletePSIElementProvider = new MyDeletePSIElementProvider();
    private final ModuleDeleteProvider myDeleteModuleProvider = new ModuleDeleteProvider();
    private final AutoScrollToSourceHandler myAutoScrollToSourceHandler;

    private final IdeView myIdeView = new MyIdeView();
    private final FavoritesManagerImpl myFavoritesManager;

    public FavoritesTreeViewPanel(Project project) {
        super(new BorderLayout());
        myProject = project;
        myFavoritesManager = FavoritesManagerImpl.getInstance(myProject);

        myFavoritesTreeStructure = new FavoritesTreeStructure(project);
        DefaultMutableTreeNode root = new DefaultMutableTreeNode();
        root.setUserObject(myFavoritesTreeStructure.getRootElement());
        final DefaultTreeModel treeModel = new DefaultTreeModel(root);
        myTree = new DnDAwareTree(treeModel);
        myBuilder = new FavoritesViewTreeBuilder(myProject, myTree, treeModel, myFavoritesTreeStructure);
        DockManager.getInstance(project).register(this);

        TreeUtil.installActions(myTree);
        UIUtil.setLineStyleAngled(myTree);
        myTree.setRootVisible(false);
        myTree.setShowsRootHandles(true);
        myTree.setLargeModel(true);
        new TreeSpeedSearch(myTree);
        ToolTipManager.sharedInstance().registerComponent(myTree);
        final FavoriteComparator favoriteComparator = new FavoriteComparator();
        myBuilder.setNodeDescriptorComparator((o1, o2) -> {
            if (o1 instanceof FavoritesTreeNodeDescriptor && o2 instanceof FavoritesTreeNodeDescriptor) {
                final FavoritesListNode listNode1 = FavoritesTreeUtil.extractParentList((FavoritesTreeNodeDescriptor) o1);
                final FavoritesListNode listNode2 = FavoritesTreeUtil.extractParentList((FavoritesTreeNodeDescriptor) o2);
                if (listNode1.equals(listNode2)) {
                    final Comparator<FavoritesTreeNodeDescriptor> comparator = myFavoritesManager.getCustomComparator(listNode1.getName());
                    if (comparator != null) {
                        return comparator.compare((FavoritesTreeNodeDescriptor) o1, (FavoritesTreeNodeDescriptor) o2);
                    }
                    else {
                        return favoriteComparator.compare(o1, o2);
                    }
                }
            }
            return o1.getIndex() - o2.getIndex();
        });
        myTree.setCellRenderer(new NodeRenderer() {
            @RequiredUIAccess
            @Override
            public void customizeCellRenderer(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                super.customizeCellRenderer(tree, value, selected, expanded, leaf, row, hasFocus);
                if (value instanceof DefaultMutableTreeNode) {
                    final DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
                    //only favorites roots to explain
                    final Object userObject = node.getUserObject();
                    if (userObject instanceof FavoritesTreeNodeDescriptor) {
                        final FavoritesTreeNodeDescriptor favoritesTreeNodeDescriptor = (FavoritesTreeNodeDescriptor) userObject;
                        AbstractTreeNode treeNode = favoritesTreeNodeDescriptor.getElement();
                        FavoritesListProvider provider = FavoritesTreeUtil.getProvider(myFavoritesManager, favoritesTreeNodeDescriptor);
                        if (provider != null) {
                            Object o = myBuilder.getUi().getElementFor(value);
                            if (o instanceof AbstractTreeNode) {
                                o = ((AbstractTreeNode) o).getValue();
                            }
                            provider.customizeRenderer(this, tree, o, selected, expanded, leaf, row, hasFocus);
                            return;
                        }
                        final ItemPresentation presentation = treeNode.getPresentation();
                        String locationString = presentation.getLocationString();
                        if (locationString == null && node.getParent() != null && node.getParent().getParent() != null && node.getParent().getParent().getParent() == null) {
                            final String location = favoritesTreeNodeDescriptor.getLocation();
                            if (location != null && location.length() > 0) {
                                append(" (" + location + ")", SimpleTextAttributes.GRAY_ATTRIBUTES);
                            }
                        }
                    }
                }
            }
        });
        CustomizationUtil.installPopupHandler(myTree, IdeActions.GROUP_FAVORITES_VIEW_POPUP, ActionPlaces.FAVORITES_VIEW_POPUP);

        EditSourceOnDoubleClickHandler.install(myTree);
        EditSourceOnEnterKeyHandler.install(myTree);
        myCopyPasteDelegator = new CopyPasteDelegator(myProject, this) {
            @Override
            @Nonnull
            protected PsiElement[] getSelectedElements() {
                return getSelectedPsiElements();
            }
        };


        AnAction editActionButton = new EditFavoritesAction();
        editActionButton.registerCustomShortcutSet(CommonShortcuts.CTRL_ENTER, null);


        final AnAction action = ActionManager.getInstance().getAction(IdeActions.ACTION_NEW_ELEMENT);
        action.registerCustomShortcutSet(action.getShortcutSet(), myTree);

        add(ScrollPaneFactory.createScrollPane(myTree, true), BorderLayout.CENTER);
        setBorder(IdeBorderFactory.createEmptyBorder());
        myAutoScrollToSourceHandler = new AutoScrollToSourceHandler() {
            @Override
            protected boolean isAutoScrollMode() {
                return myFavoritesManager.getViewSettings().isAutoScrollToSource();
            }

            @Override
            protected void setAutoScrollMode(boolean state) {
                myFavoritesManager.getViewSettings().setAutoScrollToSource(state);
            }
        };
        myAutoScrollToSourceHandler.install(myTree);
        myFavoritesManager.addFavoritesListener(new FavoritesListener() {
            @Override
            public void rootsChanged() {
                myBuilder.updateFromRoot();
                myTree.repaint();
            }

            @Override
            public void listAdded(String listName) {
                myBuilder.updateFromRoot();
                myTree.repaint();
            }

            @Override
            public void listRemoved(String listName) {
                myBuilder.updateFromRoot();
                myTree.repaint();
            }
        });
    }

    private static boolean traverseDepth(final AbstractTreeNode node, final TreeUtil.Traverse traverse) {
        if (!traverse.accept(node)) {
            return false;
        }
        final Collection<? extends AbstractTreeNode> children = node.getChildren();
        for (AbstractTreeNode child : children) {
            child.setParent(node);
            if (!traverseDepth(child, traverse)) {
                return false;
            }
        }
        return true;
    }

    public void selectElement(final Object selector, final VirtualFile file, final boolean requestFocus) {
        myBuilder.select(selector, file, requestFocus);
    }

    @Override
    public void dispose() {
        Disposer.dispose(myBuilder);
        myBuilder = null;
    }

    public DnDAwareTree getTree() {
        return myTree;
    }

    @Nonnull
    private PsiElement[] getSelectedPsiElements() {
        final Object[] elements = getSelectedNodeElements();
        if (elements == null) {
            return PsiElement.EMPTY_ARRAY;
        }
        ArrayList<PsiElement> result = new ArrayList<>();
        for (Object element : elements) {
            if (element instanceof PsiElement) {
                result.add((PsiElement) element);
            }
            else if (element instanceof SmartPsiElementPointer) {
                PsiElement psiElement = ((SmartPsiElementPointer) element).getElement();
                if (psiElement != null) {
                    result.add(psiElement);
                }
            }
            else {
                for (BookmarkNodeProvider provider : myProject.getExtensionList(BookmarkNodeProvider.class)) {
                    final PsiElement psiElement = provider.getPsiElement(element);
                    if (psiElement != null) {
                        result.add(psiElement);
                        break;
                    }
                }
            }
        }
        return PsiUtilCore.toPsiElementArray(result);
    }

    public FavoritesTreeStructure getFavoritesTreeStructure() {
        return myFavoritesTreeStructure;
    }

    @Override
    public Object getData(@Nonnull Key<?> dataId) {
        if (Project.KEY == dataId) {
            return myProject;
        }
        if (Navigatable.KEY == dataId) {
            final FavoritesTreeNodeDescriptor[] selectedNodeDescriptors = FavoritesTreeUtil.getSelectedNodeDescriptors(myTree);
            return selectedNodeDescriptors.length == 1 ? selectedNodeDescriptors[0].getElement() : null;
        }
        if (Navigatable.KEY_OF_ARRAY == dataId) {
            final List<Navigatable> selectedElements = getSelectedElements(Navigatable.class);
            return selectedElements.isEmpty() ? null : selectedElements.toArray(new Navigatable[selectedElements.size()]);
        }

        if (CutProvider.KEY == dataId) {
            return myCopyPasteDelegator.getCutProvider();
        }
        if (CopyProvider.KEY == dataId) {
            return myCopyPasteDelegator.getCopyProvider();
        }
        if (PasteProvider.KEY == dataId) {
            return myCopyPasteDelegator.getPasteProvider();
        }
        if (HelpManager.HELP_ID == dataId) {
            return "reference.toolWindows.favorites";
        }
        if (LangDataKeys.NO_NEW_ACTION == dataId) {
            return Boolean.TRUE;
        }
        if (PsiElement.KEY == dataId) {
            PsiElement[] elements = getSelectedPsiElements();
            if (elements.length != 1) {
                return null;
            }
            return elements[0] != null && elements[0].isValid() ? elements[0] : null;
        }
        if (PsiElement.KEY_OF_ARRAY == dataId) {
            final PsiElement[] elements = getSelectedPsiElements();
            ArrayList<PsiElement> result = new ArrayList<>();
            for (PsiElement element : elements) {
                if (element.isValid()) {
                    result.add(element);
                }
            }
            return result.isEmpty() ? null : PsiUtilCore.toPsiElementArray(result);
        }

        if (IdeView.KEY == dataId) {
            return myIdeView;
        }

        if (LangDataKeys.TARGET_PSI_ELEMENT == dataId) {
            return null;
        }

        if (LangDataKeys.MODULE_CONTEXT == dataId) {
            Module[] selected = getSelectedModules();
            return selected != null && selected.length == 1 ? selected[0] : null;
        }
        if (LangDataKeys.MODULE_CONTEXT_ARRAY == dataId) {
            return getSelectedModules();
        }

        if (DeleteProvider.KEY == dataId) {
            final Object[] elements = getSelectedNodeElements();
            return elements != null && elements.length >= 1 && elements[0] instanceof Module ? myDeleteModuleProvider : myDeletePSIElementProvider;
        }
        if (ModuleGroup.ARRAY_DATA_KEY == dataId) {
            final List<ModuleGroup> selectedElements = getSelectedElements(ModuleGroup.class);
            return selectedElements.isEmpty() ? null : selectedElements.toArray(new ModuleGroup[selectedElements.size()]);
        }
        if (LibraryGroupElement.ARRAY_DATA_KEY == dataId) {
            final List<LibraryGroupElement> selectedElements = getSelectedElements(LibraryGroupElement.class);
            return selectedElements.isEmpty() ? null : selectedElements.toArray(new LibraryGroupElement[selectedElements.size()]);
        }
        if (NamedLibraryElement.ARRAY_DATA_KEY == dataId) {
            final List<NamedLibraryElement> selectedElements = getSelectedElements(NamedLibraryElement.class);
            return selectedElements.isEmpty() ? null : selectedElements.toArray(new NamedLibraryElement[selectedElements.size()]);
        }
        if (CONTEXT_FAVORITES_ROOTS_DATA_KEY == dataId) {
            List<FavoritesTreeNodeDescriptor> result = new ArrayList<>();
            FavoritesTreeNodeDescriptor[] selectedNodeDescriptors = FavoritesTreeUtil.getSelectedNodeDescriptors(myTree);
            for (FavoritesTreeNodeDescriptor selectedNodeDescriptor : selectedNodeDescriptors) {
                if (FavoritesTreeUtil.getProvider(myFavoritesManager, selectedNodeDescriptor) != null) {
                    continue;
                }
                FavoritesTreeNodeDescriptor root = selectedNodeDescriptor.getFavoritesRoot();
                if (root != null && root.getElement() instanceof FavoritesListNode) {
                    result.add(selectedNodeDescriptor);
                }
            }
            return result.toArray(new FavoritesTreeNodeDescriptor[result.size()]);
        }
        if (FAVORITES_TREE_KEY == dataId) {
            return myTree;
        }
        if (FAVORITES_TREE_BUILDER_KEY == dataId) {
            return myBuilder;
        }
        if (FAVORITES_LIST_NAME_DATA_KEY == dataId) {
            final FavoritesTreeNodeDescriptor[] descriptors = FavoritesTreeUtil.getSelectedNodeDescriptors(myTree);
            Set<String> selectedNames = new HashSet<>();
            for (FavoritesTreeNodeDescriptor descriptor : descriptors) {
                FavoritesListNode node = FavoritesTreeUtil.extractParentList(descriptor);
                if (node != null) {
                    selectedNames.add(node.getValue());
                }
            }

            if (selectedNames.size() == 1) {
                return selectedNames.iterator().next();
            }
            return null;
        }
        FavoritesTreeNodeDescriptor[] descriptors = FavoritesTreeUtil.getSelectedNodeDescriptors(myTree);
        if (descriptors.length > 0) {
            List<AbstractTreeNode> nodes = new ArrayList<>();
            for (FavoritesTreeNodeDescriptor descriptor : descriptors) {
                nodes.add(descriptor.getElement());
            }
            return myFavoritesTreeStructure.getDataFromProviders(nodes, dataId);
        }
        return null;
    }

    private Set<FavoritesListNode> getSelectedListsNodes() {
        final Set<FavoritesListNode> result = new HashSet<>();
        final FavoritesTreeNodeDescriptor[] descriptors = FavoritesTreeUtil.getSelectedNodeDescriptors(myTree);
        for (FavoritesTreeNodeDescriptor descriptor : descriptors) {
            final FavoritesListNode listNode = FavoritesTreeUtil.extractParentList(descriptor);
            if (listNode != null) {
                result.add(listNode);
            }
        }
        return result;
    }

    private <T> List<T> getSelectedElements(Class<T> klass) {
        final Object[] elements = getSelectedNodeElements();
        ArrayList<T> result = new ArrayList<>();
        if (elements == null) {
            return result;
        }
        for (Object element : elements) {
            if (element == null) {
                continue;
            }
            if (klass.isAssignableFrom(element.getClass())) {
                //noinspection unchecked
                result.add((T) element);
            }
        }
        return result;
    }

    private Module[] getSelectedModules() {
        final Object[] elements = getSelectedNodeElements();
        if (elements == null) {
            return null;
        }
        ArrayList<Module> result = new ArrayList<>();
        for (Object element : elements) {
            if (element instanceof Module) {
                result.add((Module) element);
            }
            else if (element instanceof ModuleGroup) {
                result.addAll(((ModuleGroup) element).modulesInGroup(myProject, true));
            }
        }

        return result.isEmpty() ? null : result.toArray(new Module[result.size()]);
    }

    @RequiredReadAction
    private Object[] getSelectedNodeElements() {
        final FavoritesTreeNodeDescriptor[] selectedNodeDescriptors = FavoritesTreeUtil.getSelectedNodeDescriptors(myTree);
        ArrayList<Object> result = new ArrayList<>();
        for (FavoritesTreeNodeDescriptor selectedNodeDescriptor : selectedNodeDescriptors) {
            if (selectedNodeDescriptor != null) {
                Object value = selectedNodeDescriptor.getElement().getValue();
                if (value instanceof SmartPsiElementPointer elementPointer) {
                    value = elementPointer.getElement();
                }
                result.add(value);
            }
        }
        return ArrayUtil.toObjectArray(result);
    }

    public void setupToolWindow(ToolWindow window) {
        final CollapseAllAction collapseAction = new CollapseAllAction(myTree);
        collapseAction.getTemplatePresentation().setIcon(PlatformIconGroup.actionsCollapseall());

        ActionManager actionManager = ActionManager.getInstance();

        AnAction addBookmarksList = actionManager.getAction(IdeActions.ADD_NEW_FAVORITES_LIST);

        AnAction editActionButton = new EditFavoritesAction();
        editActionButton.registerCustomShortcutSet(CommonShortcuts.CTRL_ENTER, null);

        AnAction deleteActionButton = new DeleteFromFavoritesAction();

        window.setTitleActions(addBookmarksList,
            AnSeparator.create(),
            deleteActionButton,
            editActionButton,
            collapseAction
        );

        final DefaultActionGroup group = new DefaultActionGroup();

        group.add(new FavoritesFlattenPackagesAction(myProject, myBuilder));
        group.add(new FavoritesCompactEmptyMiddlePackagesAction(myProject, myBuilder));
        group.addAction(new FavoritesAbbreviatePackageNamesAction(myProject, myBuilder));

        group.add(new FavoritesShowMembersAction(myProject, myBuilder));

        final FavoritesAutoscrollFromSourceHandler handler = new FavoritesAutoscrollFromSourceHandler(myProject, myBuilder);
        handler.install();
        group.add(handler.createToggleAction());

        group.add(new FavoritesAutoScrollToSourceAction(myProject, myAutoScrollToSourceHandler, myBuilder));
        window.setAdditionalGearActions(group);
    }

    public static String getQualifiedName(final VirtualFile file) {
        return file.getPresentableUrl();
    }

    public FavoritesViewTreeBuilder getBuilder() {
        return myBuilder;
    }

    @Nullable
    FavoritesListNode findFavoritesListNode(Point point) {
        final TreePath path = myTree.getClosestPathForLocation(point.x, point.y);
        final FavoritesListNode node = getListNodeFromPath(path);
        return node == null ? (FavoritesListNode) ((FavoritesRootNode) myFavoritesTreeStructure.getRootElement()).getChildren().iterator().next() : node;
    }

    static FavoritesListNode getListNodeFromPath(TreePath path) {
        if (path != null && path.getPathCount() > 1) {
            final Object o = path.getPath()[1];
            if (o instanceof DefaultMutableTreeNode) {
                final Object obj = ((DefaultMutableTreeNode) o).getUserObject();
                if (obj instanceof FavoritesTreeNodeDescriptor) {
                    final AbstractTreeNode node = ((FavoritesTreeNodeDescriptor) obj).getElement();
                    if (node instanceof FavoritesListNode) {
                        return (FavoritesListNode) node;
                    }
                }
            }
        }
        return null;
    }

    void dropPsiElements(FavoritesManagerImpl mgr, FavoritesListNode node, PsiElement[] elements) {
        if (elements != null && elements.length > 0) {
            final ArrayList<AbstractTreeNode> nodes = new ArrayList<>();
            for (PsiElement element : elements) {
                if (element instanceof SmartPsiElementPointer elementPointer) {
                    element = elementPointer.getElement();
                }
                final Collection<AbstractTreeNode> tmp = AddToFavoritesAction.createNodes(myProject, null, element, true, FavoritesManagerImpl.getInstance(myProject).getViewSettings());
                nodes.addAll(tmp);
                mgr.addRoots(node.getValue(), nodes);
            }
            myBuilder.select(nodes.toArray(), null);
        }
    }

    private final class MyDeletePSIElementProvider implements DeleteProvider {
        @Override
        public boolean canDeleteElement(@Nonnull DataContext dataContext) {
            final PsiElement[] elements = getElementsToDelete();
            return DeleteHandler.shouldEnableDeleteAction(elements);
        }

        @Override
        public void deleteElement(@Nonnull DataContext dataContext) {
            List<PsiElement> allElements = Arrays.asList(getElementsToDelete());
            List<PsiElement> validElements = new ArrayList<>();
            for (PsiElement psiElement : allElements) {
                if (psiElement != null && psiElement.isValid()) {
                    validElements.add(psiElement);
                }
            }
            final PsiElement[] elements = PsiUtilCore.toPsiElementArray(validElements);

            LocalHistoryAction a = LocalHistory.getInstance().startAction(IdeLocalize.progressDeleting());
            try {
                DeleteHandler.deletePsiElement(elements, myProject);
            }
            finally {
                a.finish();
            }
        }

        @RequiredReadAction
        private PsiElement[] getElementsToDelete() {
            ArrayList<PsiElement> result = new ArrayList<>();
            Object[] elements = getSelectedNodeElements();
            for (int idx = 0; elements != null && idx < elements.length; idx++) {
                if (elements[idx] instanceof PsiElement) {
                    final PsiElement element = (PsiElement) elements[idx];
                    result.add(element);
                    if (element instanceof PsiDirectory) {
                        final VirtualFile virtualFile = ((PsiDirectory) element).getVirtualFile();
                        final String path = virtualFile.getPath();
                        if (path.endsWith(URLUtil.ARCHIVE_SEPARATOR)) { // if is jar-file root
                            final VirtualFile vFile = LocalFileSystem.getInstance()
                                .findFileByPath(path.substring(0, path.length() - URLUtil.ARCHIVE_SEPARATOR.length()));
                            if (vFile != null) {
                                final PsiFile psiFile = PsiManager.getInstance(myProject).findFile(vFile);
                                if (psiFile != null) {
                                    elements[idx] = psiFile;
                                }
                            }
                        }
                    }
                }
            }

            return PsiUtilCore.toPsiElementArray(result);
        }
    }

    private final class MyIdeView implements IdeView {
        @Override
        public void selectElement(final PsiElement element) {
            if (element != null) {
                selectPsiElement(element, false);
                boolean requestFocus = true;
                final boolean isDirectory = element instanceof PsiDirectory;
                if (!isDirectory) {
                    Editor editor = EditorHelper.openInEditor(element);
                    if (editor != null) {
                        ToolWindowManager.getInstance(myProject).activateEditorComponent();
                        requestFocus = false;
                    }
                }
                if (requestFocus) {
                    selectPsiElement(element, true);
                }
            }
        }

        private void selectPsiElement(PsiElement element, boolean requestFocus) {
            VirtualFile virtualFile = PsiUtilCore.getVirtualFile(element);
            FavoritesTreeViewPanel.this.selectElement(element, virtualFile, requestFocus);
        }

        @Nullable
        @RequiredReadAction
        private PsiDirectory[] getSelectedDirectories() {
            if (myBuilder == null) {
                return null;
            }
            final Object[] selectedNodeElements = getSelectedNodeElements();
            if (selectedNodeElements.length != 1) {
                return null;
            }
            for (BookmarkNodeProvider nodeProvider : myProject.getExtensionList(BookmarkNodeProvider.class)) {
                final PsiElement psiElement = nodeProvider.getPsiElement(selectedNodeElements[0]);
                if (psiElement instanceof PsiDirectory) {
                    return new PsiDirectory[]{(PsiDirectory) psiElement};
                }
                else if (psiElement instanceof PsiDirectoryContainer directoryContainer) {
                    final String moduleName = nodeProvider.getElementModuleName(selectedNodeElements[0]);
                    GlobalSearchScope searchScope = GlobalSearchScope.projectScope(myProject);
                    if (moduleName != null) {
                        final Module module = ModuleManager.getInstance(myProject).findModuleByName(moduleName);
                        if (module != null) {
                            searchScope = GlobalSearchScope.moduleScope(module);
                        }
                    }
                    return directoryContainer.getDirectories(searchScope);
                }
                else if (psiElement != null) {
                    PsiFile file = psiElement.getContainingFile();
                    if (file != null) {
                        PsiDirectory parent = file.getParent();
                        if (parent != null) {
                            return new PsiDirectory[]{parent};
                        }
                    }
                }
            }
            return selectedNodeElements[0] instanceof PsiDirectory ? new PsiDirectory[]{(PsiDirectory) selectedNodeElements[0]} : null;
        }

        @Override
        public PsiDirectory[] getDirectories() {
            final PsiDirectory[] directories = getSelectedDirectories();
            return directories == null ? PsiDirectory.EMPTY_ARRAY : directories;
        }

        @Override
        public PsiDirectory getOrChooseDirectory() {
            return DirectoryChooserUtil.getOrChooseDirectory(this);
        }
    }

    //DockContainer methods

    @Override
    public RelativeRectangle getAcceptArea() {
        return new RelativeRectangle(myTree);
    }

    @Override
    public RelativeRectangle getAcceptAreaFallback() {
        return getAcceptArea();
    }

    @Nonnull
    @Override
    public ContentResponse getContentResponse(@Nonnull DockableContent content, RelativePoint point) {
        if (content.getKey() instanceof VirtualFile) {
            return ContentResponse.ACCEPT_COPY;
        }
        return ContentResponse.DENY;
    }

    @Override
    public JComponent getContainerComponent() {
        return this;
    }

    @Override
    @RequiredReadAction
    public void add(@Nonnull DockableContent content, RelativePoint dropTarget) {
        if (content.getKey() instanceof VirtualFile vFile) {
            final PsiFileSystemItem psiFile = vFile.isDirectory()
                ? PsiManager.getInstance(myProject).findDirectory(vFile)
                : PsiManager.getInstance(myProject).findFile(vFile);
            Point p = dropTarget.getScreenPoint();
            SwingUtilities.convertPointFromScreen(p, myTree);
            FavoritesListNode node = findFavoritesListNode(p);
            if (node != null && psiFile != null) {
                dropPsiElements(myFavoritesManager, node, new PsiElement[]{psiFile});
            }
        }
    }

    @Override
    public void closeAll() {
    }

    @Override
    public void addListener(Listener listener, Disposable parent) {
        throw new UnsupportedOperationException("Method is not supported");
    }

    @Override
    public boolean isEmpty() {
        return myTree.isEmpty();
    }

    @Nullable
    @Override
    public Image startDropOver(@Nonnull DockableContent content, RelativePoint point) {
        return null;
    }

    @Nullable
    @Override
    public Image processDropOver(@Nonnull DockableContent content, RelativePoint point) {
        Point p = point.getScreenPoint();
        SwingUtilities.convertPointFromScreen(p, myTree);
        TreePath treePath = myTree.getClosestPathForLocation(p.x, p.y);
        FavoritesListNode node = getListNodeFromPath(treePath);
        treePath = node != null ? myTree.getPath(node) : null;
        if (treePath != null) {
            myTree.setSelectionPath(treePath);
        }
        return null;
    }

    @Override
    public void resetDropOver(@Nonnull DockableContent content) {
    }

    @Override
    public boolean isDisposeWhenEmpty() {
        return false;
    }

    @Override
    public void showNotify() {
    }

    @Override
    public void hideNotify() {
    }
}
