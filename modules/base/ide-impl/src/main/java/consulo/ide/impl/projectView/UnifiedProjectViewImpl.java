/*
 * Copyright 2013-2017 consulo.io
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
import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import org.jdom.Element;
import consulo.application.HelpManager;
import consulo.dataContext.DataSink;
import consulo.dataContext.UiDataProvider;
import consulo.disposer.Disposable;
import consulo.ide.impl.idea.ide.projectView.HelpID;
import consulo.ide.impl.idea.ide.ui.customization.CustomizationUtil;
import consulo.ide.impl.idea.ide.projectView.impl.AbstractProjectViewPane;
import consulo.ide.impl.idea.ide.projectView.impl.ProjectAbstractTreeStructureBase;
import consulo.ide.impl.idea.ide.projectView.impl.ProjectViewPaneImpl;
import consulo.ide.impl.idea.ide.projectView.impl.nodes.LibraryGroupNode;
import consulo.ide.impl.idea.ide.projectView.impl.nodes.NamedLibraryElementNode;
import consulo.ide.impl.idea.ide.projectView.actions.ProjectViewToolbarGroup;
import consulo.ide.localize.IdeLocalize;
import consulo.language.content.ProjectRootsUtil;
import consulo.language.editor.LangDataKeys;
import consulo.language.editor.PlatformDataKeys;
import consulo.language.editor.refactoring.ui.CopyPasteDelegator;
import consulo.language.editor.util.EditorHelper;
import consulo.language.editor.util.IdeView;
import consulo.ide.util.DirectoryChooserUtil;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiUtilCore;
import consulo.localize.LocalizeValue;
import consulo.module.Module;
import consulo.module.content.ModuleFileIndex;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.ProjectRootManager;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.module.content.layer.orderEntry.LibraryOrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.navigation.Navigatable;
import consulo.project.Project;
import consulo.project.ui.view.ProjectViewPane;
import consulo.project.ui.view.SelectInTarget;
import consulo.project.ui.view.internal.ProjectViewEx;
import consulo.project.ui.view.internal.node.LibraryGroupElement;
import consulo.project.ui.view.internal.node.NamedLibraryElement;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.project.ui.view.tree.ModuleGroup;
import consulo.project.ui.view.tree.PsiDirectoryNode;
import consulo.ui.Tree;
import consulo.ui.TreeNode;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.TreeExpander;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentFactory;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.ex.tree.NodeDescriptor;
import consulo.ui.ex.tree.TreeStructureWrappenModel;
import consulo.ui.ex.tree.UITreeState;
import consulo.ui.layout.WrappedLayout;
import consulo.undoRedo.CommandProcessor;
import consulo.util.concurrent.AsyncResult;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.util.*;

/**
 * @author VISTALL
 * @since 2017-10-23
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.UNIFIED)
@State(name = "ProjectView", storages = @Storage(file = StoragePathMacros.WORKSPACE_FILE))
public class UnifiedProjectViewImpl implements ProjectViewEx, PersistentStateComponent<Element>, Disposable {
    private final class MyDataProvider implements UiDataProvider {
        /**
         * The selection lives in the consulo.ui tree, and reading it means touching the ui component. Every
         * {@code sink.lazy} value is computed later and off the ui thread, so it has to be captured here - the
         * awt pane snapshots {@code selectedUserObjects} in the very same way.
         */
        private @Nullable AbstractTreeNode takeSelectedNode() {
            TreeNode<AbstractTreeNode> selectedNode = myTree == null ? null : myTree.getSelectedNode();
            return selectedNode == null ? null : selectedNode.getValue();
        }

        private @Nullable Object takeSelectedNodeElement() {
            AbstractTreeNode selectedNode = takeSelectedNode();
            if (selectedNode != null) {
                return selectedNode.getValue();
            }

            AbstractProjectViewPane currentProjectViewPane = getCurrentProjectViewPane();
            if (currentProjectViewPane == null) { // can happen if not initialized yet
                return null;
            }

            DefaultMutableTreeNode node = currentProjectViewPane.getSelectedNode();
            if (node == null) {
                return null;
            }
            Object userObject = node.getUserObject();
            return userObject instanceof AbstractTreeNode treeNode ? treeNode.getValue()
                : userObject instanceof NodeDescriptor nodeDescriptor ? nodeDescriptor.getElement() : null;
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

        @Override
        public void uiDataSnapshot(DataSink sink) {
            sink.set(Project.KEY, myProject);
            sink.set(HelpManager.HELP_ID, HelpID.PROJECT_VIEWS);

            // without the view every action that creates something - the whole New group - has nowhere to put it
            // and expands to nothing, and cut/copy/paste have no provider to run through
            sink.set(IdeView.KEY, myIdeView);
            sink.set(PlatformDataKeys.TREE_EXPANDER, myTreeExpander);
            if (myCopyPasteDelegator != null) {
                sink.set(PlatformDataKeys.CUT_PROVIDER, myCopyPasteDelegator.getCutProvider());
                sink.set(PlatformDataKeys.COPY_PROVIDER, myCopyPasteDelegator.getCopyProvider());
                sink.set(PlatformDataKeys.PASTE_PROVIDER, myCopyPasteDelegator.getPasteProvider());
            }

            AbstractTreeNode selectedNode = takeSelectedNode();
            Object selected = takeSelectedNodeElement();

            // set, not lazy - the selection is already in hand, and a lazy supplier does not survive into the
            // pre cached context the async action update and the navigation bar are built from
            PsiElement selectedElement = selected instanceof PsiElement psiElement && psiElement.isValid() ? psiElement : null;

            if (selectedNode != null) {
                sink.set(PlatformDataKeys.SELECTED_ITEMS, new Object[]{selectedNode});
            }

            // the node itself is the navigatable the awt pane publishes, not its value - Jump to Source and the
            // rest of the navigation actions read only the array key
            Navigatable navigatable = selectedNode != null ? selectedNode
                : selected instanceof Navigatable selectedNavigatable ? selectedNavigatable : null;
            sink.set(Navigatable.KEY, navigatable);
            if (navigatable != null) {
                sink.set(Navigatable.KEY_OF_ARRAY, new Navigatable[]{navigatable});
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
            else {
                sink.lazy(PsiElement.KEY_OF_ARRAY, () -> {
                    AbstractProjectViewPane pane = getCurrentProjectViewPane();
                    if (pane == null) {
                        return null;
                    }
                    PsiElement[] elements = pane.getSelectedPSIElements();
                    return elements.length == 0 ? null : elements;
                });
            }

            sink.set(PlatformDataKeys.PROJECT_CONTEXT, selected instanceof Project project ? project : null);
            sink.set(LangDataKeys.MODULE_CONTEXT, moduleContext(selected));
            sink.lazy(LangDataKeys.MODULE_CONTEXT_ARRAY, () -> getSelectedModules());
            sink.lazy(ModuleGroup.ARRAY_DATA_KEY, () -> {
                List<ModuleGroup> selectedElements = getSelectedElements(ModuleGroup.class);
                return selectedElements.isEmpty() ? null : selectedElements.toArray(new ModuleGroup[selectedElements.size()]);
            });
            sink.lazy(LibraryGroupElement.ARRAY_DATA_KEY, () -> {
                List<LibraryGroupElement> selectedElements = getSelectedElements(LibraryGroupElement.class);
                return selectedElements.isEmpty() ? null : selectedElements.toArray(new LibraryGroupElement[selectedElements.size()]);
            });
            sink.lazy(NamedLibraryElement.ARRAY_DATA_KEY, () -> {
                List<NamedLibraryElement> selectedElements = getSelectedElements(NamedLibraryElement.class);
                return selectedElements.isEmpty() ? null : selectedElements.toArray(new NamedLibraryElement[selectedElements.size()]);
            });

            // last, not first: the sink keeps the first value put under a key, and the swing pane has no tree of
            // its own, so it would publish an empty selection that shadows everything derived from the ui tree
            AbstractProjectViewPane currentProjectViewPane = getCurrentProjectViewPane();
            if (currentProjectViewPane != null) {
                currentProjectViewPane.uiDataSnapshot(sink);
            }
        }

        private @Nullable LibraryOrderEntry getSelectedLibrary() {
            AbstractProjectViewPane viewPane = getCurrentProjectViewPane();
            DefaultMutableTreeNode node = viewPane != null ? viewPane.getSelectedNode() : null;
            if (node == null) {
                return null;
            }
            DefaultMutableTreeNode parent = (DefaultMutableTreeNode)node.getParent();
            if (parent == null) {
                return null;
            }
            Object userObject = parent.getUserObject();
            if (userObject instanceof LibraryGroupNode) {
                userObject = node.getUserObject();
                if (userObject instanceof NamedLibraryElementNode namedLibraryElementNode) {
                    NamedLibraryElement element = namedLibraryElementNode.getValue();
                    OrderEntry orderEntry = element.getOrderEntry();
                    return orderEntry instanceof LibraryOrderEntry libraryOrderEntry ? libraryOrderEntry : null;
                }
                PsiDirectory directory = ((PsiDirectoryNode)userObject).getValue();
                VirtualFile virtualFile = directory.getVirtualFile();
                Module module = (Module)((AbstractTreeNode)((DefaultMutableTreeNode)parent.getParent()).getUserObject()).getValue();

                if (module == null) {
                    return null;
                }
                ModuleFileIndex index = ModuleRootManager.getInstance(module).getFileIndex();
                OrderEntry entry = index.getOrderEntryForFile(virtualFile);
                if (entry instanceof LibraryOrderEntry libraryOrderEntry) {
                    return libraryOrderEntry;
                }
            }

            return null;
        }

        @RequiredUIAccess
        private void detachLibrary(LibraryOrderEntry orderEntry, Project project) {
            Module module = orderEntry.getOwnerModule();
            LocalizeValue message = IdeLocalize.detachLibraryFromModule(orderEntry.getPresentableName(), module.getName());
            LocalizeValue title = IdeLocalize.detachLibrary();
            int ret = Messages.showOkCancelDialog(project, message.get(), title.get(), UIUtil.getQuestionIcon());
            if (ret != Messages.OK) {
                return;
            }
            CommandProcessor.getInstance().newCommand()
                .project(module.getProject())
                .name(title)
                .inWriteAction()
                .run(() -> {
                    ModuleRootManager rootManager = ModuleRootManager.getInstance(module);
                    OrderEntry[] orderEntries = rootManager.getOrderEntries();
                    ModifiableRootModel model = rootManager.getModifiableModel();
                    OrderEntry[] modifiableEntries = model.getOrderEntries();
                    for (int i = 0; i < orderEntries.length; i++) {
                        OrderEntry entry = orderEntries[i];
                        if (entry instanceof LibraryOrderEntry libraryOrderEntry
                            && libraryOrderEntry.getLibrary() == orderEntry.getLibrary()) {
                            model.removeOrderEntry(modifiableEntries[i]);
                        }
                    }
                    model.commit();
                });
        }

        @RequiredReadAction
        private @Nullable Module[] getSelectedModules() {
            AbstractProjectViewPane viewPane = getCurrentProjectViewPane();
            if (viewPane == null) {
                return null;
            }
            Object[] elements = viewPane.getSelectedElements();
            ArrayList<Module> result = new ArrayList<>();
            for (Object element : elements) {
                if (element instanceof Module module) {
                    if (!module.isDisposed()) {
                        result.add(module);
                    }
                }
                else if (element instanceof ModuleGroup moduleGroup) {
                    Collection<Module> modules = moduleGroup.modulesInGroup(myProject, true);
                    result.addAll(modules);
                }
                else if (element instanceof PsiDirectory directory) {
                    Module module = moduleBySingleContentRoot(directory.getVirtualFile());
                    if (module != null) {
                        result.add(module);
                    }
                }
                else if (element instanceof VirtualFile virtualFile) {
                    Module module = moduleBySingleContentRoot(virtualFile);
                    if (module != null) {
                        result.add(module);
                    }
                }
            }

            if (result.isEmpty()) {
                return null;
            }
            else {
                return result.toArray(new Module[result.size()]);
            }
        }
    }

    private static final String ELEMENT_PANES = "panes";
    private static final String ELEMENT_PANE = "pane";
    private static final String ELEMENT_SUB_PANE = "subPane";
    private static final String ATTRIBUTE_ID = "id";

    private final Project myProject;
    private final Map<String, SelectInTarget> mySelectInTargets = new LinkedHashMap<>();

    private AbstractProjectViewPane myCurrentPane;

    private Tree<AbstractTreeNode> myTree;

    /**
     * Read back before the tree exists, and kept afterwards so that the state of a session which is already
     * gone - the tool window content is built anew for every ui - is still what gets written out.
     */
    private @Nullable UITreeState myReadTreeState;

    private @Nullable Element myLoadedState;

    private final IdeView myIdeView = new MyIdeView();

    private final TreeExpander myTreeExpander = new MyTreeExpander();

    private @Nullable CopyPasteDelegator myCopyPasteDelegator;

    /**
     * Backs the expand all and collapse all title actions. A tree which cannot walk its own nodes hides them
     * rather than showing them disabled, which is what the platform does for a view without an expander.
     */
    private final class MyTreeExpander implements TreeExpander {
        @Override
        public void expandAll() {
            if (myTree != null) {
                myTree.expandAll();
            }
        }

        @Override
        public boolean canExpand() {
            return myTree != null && myTree.isExpandCollapseAllSupported();
        }

        @Override
        public boolean isExpandAllVisible() {
            return canExpand();
        }

        @Override
        public void collapseAll() {
            if (myTree != null) {
                myTree.collapseAll();
            }
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

    /**
     * The selection of this view lives in the consulo.ui tree, so everything that needs it reads it from there
     * and not from the swing pane, which never has one.
     */
    private @Nullable PsiElement getSelectedPsiElement() {
        TreeNode<AbstractTreeNode> selectedNode = myTree == null ? null : myTree.getSelectedNode();
        if (selectedNode == null) {
            return null;
        }

        AbstractTreeNode value = selectedNode.getValue();
        return value != null && value.getValue() instanceof PsiElement element && element.isValid() ? element : null;
    }

    private final class MyIdeView implements IdeView {
        @Override
        public void selectElement(PsiElement element) {
            selectPsiElement(element, false);

            if (element != null && !(element instanceof PsiDirectory)) {
                EditorHelper.openInEditor(element, false);
            }
        }

        @Override
        public PsiDirectory[] getDirectories() {
            PsiElement selected = getSelectedPsiElement();
            if (selected instanceof PsiDirectory directory) {
                return new PsiDirectory[]{directory};
            }

            // creating something next to the selected file means creating it in the folder that holds it
            PsiFile containingFile = selected == null ? null : selected.getContainingFile();
            PsiDirectory parent = containingFile == null ? null : containingFile.getContainingDirectory();
            return parent == null ? PsiDirectory.EMPTY_ARRAY : new PsiDirectory[]{parent};
        }

        @Override
        public PsiDirectory getOrChooseDirectory() {
            return DirectoryChooserUtil.getOrChooseDirectory(this);
        }
    }

    @Inject
    public UnifiedProjectViewImpl(Project project) {
        myProject = project;
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
    public AsyncResult<Void> selectCB(Object element, VirtualFile file, boolean requestFocus) {
        return AsyncResult.resolved(null);
    }

    @RequiredUIAccess
    @Override
    public void setupToolWindow(ToolWindow toolWindow, boolean loadPaneExtensions) {
        ProjectViewPaneImpl projectViewPane = null;
        for (AbstractProjectViewPane pane : AbstractProjectViewPane.EP_NAME.getExtensions(myProject)) {
            if (pane instanceof ProjectViewPaneImpl projectViewPaneImpl) {
                projectViewPane = projectViewPaneImpl;
            }
        }

        assert projectViewPane != null;

        myCurrentPane = projectViewPane;

        SelectInTarget selectInTarget = projectViewPane.createSelectInTarget();
        if (selectInTarget != null) {
            mySelectInTargets.put(projectViewPane.getId(), selectInTarget);
        }

        ProjectAbstractTreeStructureBase structure = projectViewPane.createStructure();

        TreeStructureWrappenModel<AbstractTreeNode> model = new TreeStructureWrappenModel<>(structure) {
            @Override
            public boolean onDoubleClick(Tree tree, TreeNode node) {
                if (node.isLeaf()) {
                    AbstractTreeNode value = (AbstractTreeNode)node.getValue();

                    value.navigate(true);

                    return false;
                }

                return true;
            }
        };

        myTree = Tree.create((AbstractTreeNode)structure.getRootElement(), model, this);
        WrappedLayout wrappedLayout = WrappedLayout.create(myTree);
        wrappedLayout.putUserData(UiDataProvider.KEY, new MyDataProvider());

        if (TargetAWT.to(wrappedLayout) instanceof JComponent popupTarget) {
            // the delegator only needs the component to route the copy and paste key strokes through
            myCopyPasteDelegator = new CopyPasteDelegator(myProject, popupTarget) {
                @Override
                @RequiredUIAccess
                protected PsiElement[] getSelectedElements() {
                    PsiElement selected = getSelectedPsiElement();
                    return selected == null ? PsiElement.EMPTY_ARRAY : new PsiElement[]{selected};
                }
            };

            // the popup reads the same context as the actions do, so it is installed on the component that holds
            // the provider rather than on the tree itself
            CustomizationUtil.installPopupHandler(popupTarget, IdeActions.GROUP_PROJECT_VIEW_POPUP, ActionPlaces.PROJECT_VIEW_POPUP);
        }

        Content content = ContentFactory.getInstance().createUIContent(wrappedLayout, "Project", true);

        toolWindow.getContentManager().addContent(content);

        // the tree has to be in the tool window before the walk opens anything - an expand of a component the
        // frontend has not built yet is not carried over when it finally renders
        restoreExpandedPaths();

        List<AnAction> titleActions = new ArrayList<>();
        createTitleActions(titleActions);
        if (!titleActions.isEmpty()) {
            toolWindow.setTitleActions(titleActions.toArray(AnAction[]::new));
        }
    }

    private void createTitleActions(List<? super AnAction> titleActions) {
        titleActions.add(ActionManager.getInstance().getAction(ProjectViewToolbarGroup.class));
    }

    private void saveExpandedPaths() {
        if (myTree == null) {
            return;
        }

        UITreeState treeState = UITreeState.createOn(myTree);
        myReadTreeState = treeState.isEmpty() ? null : treeState;
    }

    private void restoreExpandedPaths() {
        if (myTree != null && myReadTreeState != null) {
            myReadTreeState.applyTo(myTree);
        }
    }

    /**
     * The awt project view writes the same component, and both frontends open the same project directory - the
     * state is read and written in its layout, and everything this view does not model is carried over from what
     * was read rather than dropped.
     */
    @Override
    public @Nullable Element getState() {
        saveExpandedPaths();

        Element element = myLoadedState == null ? new Element("state") : myLoadedState.clone();

        Element panes = element.getChild(ELEMENT_PANES);
        if (panes == null) {
            panes = new Element(ELEMENT_PANES);
            element.addContent(panes);
        }

        Element pane = findPane(panes);
        if (pane == null) {
            pane = new Element(ELEMENT_PANE);
            pane.setAttribute(ATTRIBUTE_ID, getPaneId());
            panes.addContent(pane);
        }

        pane.removeChildren(ELEMENT_SUB_PANE);

        if (myReadTreeState != null) {
            Element subPane = new Element(ELEMENT_SUB_PANE);
            myReadTreeState.writeExternal(subPane);
            pane.addContent(subPane);
        }

        return element;
    }

    @Override
    public void loadState(Element state) {
        myLoadedState = state.clone();

        Element panes = state.getChild(ELEMENT_PANES);
        Element pane = panes == null ? null : findPane(panes);
        Element subPane = pane == null ? null : pane.getChild(ELEMENT_SUB_PANE);

        UITreeState treeState = UITreeState.createFrom(subPane);
        myReadTreeState = treeState.isEmpty() ? null : treeState;
    }

    private @Nullable Element findPane(Element panes) {
        for (Element pane : panes.getChildren(ELEMENT_PANE)) {
            if (getPaneId().equals(pane.getAttributeValue(ATTRIBUTE_ID))) {
                return pane;
            }
        }
        return null;
    }

    private String getPaneId() {
        return myCurrentPane == null ? ProjectViewPaneImpl.ID : myCurrentPane.getId();
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> getSelectedElements(Class<T> klass) {
        List<T> result = new ArrayList<>();
        AbstractProjectViewPane viewPane = getCurrentProjectViewPane();
        if (viewPane == null) {
            return result;
        }
        Object[] elements = viewPane.getSelectedElements();
        for (Object element : elements) {
            //element still valid
            if (element != null && klass.isAssignableFrom(element.getClass())) {
                result.add((T)element);
            }
        }
        return result;
    }

    @Override
    public AsyncResult<Void> changeViewCB(String viewId, String subId) {
        return AsyncResult.done(null);
    }

    @Override
    public @Nullable PsiElement getParentOfCurrentSelection() {
        return null;
    }

    @Override
    public void changeView(String viewId) {
    }

    @Override
    public void changeView(String viewId, String subId) {
    }

    @Override
    public void changeView() {
    }

    @Override
    public void refresh() {
    }

    @Override
    public boolean isAutoscrollToSource(String paneId) {
        return false;
    }

    @Override
    public boolean isFlattenPackages(String paneId) {
        return false;
    }

    @Override
    public boolean isShowMembers(String paneId) {
        return false;
    }

    @Override
    public boolean isHideEmptyMiddlePackages(String paneId) {
        return false;
    }

    @Override
    public void setHideEmptyPackages(boolean hideEmptyPackages, String paneId) {
    }

    @Override
    public boolean isShowLibraryContents(String paneId) {
        return false;
    }

    @Override
    public void setShowLibraryContents(boolean showLibraryContents, String paneId) {
    }

    @Override
    public boolean isShowModules(String paneId) {
        return false;
    }

    @Override
    public void setShowModules(boolean showModules, String paneId) {
    }

    @Override
    public void addProjectPane(ProjectViewPane pane) {
    }

    @Override
    public void removeProjectPane(ProjectViewPane instance) {
    }

    @Override
    public ProjectViewPane getProjectViewPaneById(String id) {
        return null;
    }

    @Override
    public boolean isAutoscrollFromSource(String paneId) {
        return false;
    }

    @Override
    public boolean isAbbreviatePackageNames(String paneId) {
        return false;
    }

    @Override
    public void setAbbreviatePackageNames(boolean abbreviatePackageNames, String paneId) {
    }

    @Override
    public String getCurrentViewId() {
        return null;
    }

    @Override
    public boolean isManualOrder(String paneId) {
        return false;
    }

    @Override
    public void setManualOrder(String paneId, boolean enabled) {
    }

    @Override
    public void selectPsiElement(PsiElement element, boolean requestFocus) {
    }

    @Override
    public boolean isSortByType(String paneId) {
        return false;
    }

    @Override
    public void setSortByType(String paneId, boolean sortByType) {
    }

    @Override
    public AbstractProjectViewPane getCurrentProjectViewPane() {
        return myCurrentPane;
    }

    @Override
    public Collection<String> getPaneIds() {
        return null;
    }

    @Override
    public Collection<SelectInTarget> getSelectInTargets() {
        return mySelectInTargets.values();
    }

    @Override
    public void dispose() {
    }
}
