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
import org.jdom.Attribute;
import org.jdom.Element;
import consulo.application.HelpManager;
import consulo.dataContext.DataSink;
import consulo.dataContext.UiDataProvider;
import consulo.disposer.Disposable;
import consulo.ide.impl.idea.ide.projectView.HelpID;
import consulo.ide.impl.idea.ide.ui.customization.CustomizationUtil;
import consulo.ide.impl.idea.ide.projectView.impl.AbstractProjectViewPane;
import consulo.ide.impl.idea.ide.projectView.impl.GroupByTypeComparator;
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
import consulo.language.psi.PsiManager;
import consulo.language.psi.PsiUtilCore;
import consulo.language.psi.PsiWhiteSpace;
import consulo.language.psi.event.PsiTreeChangeAdapter;
import consulo.language.psi.event.PsiTreeChangeEvent;
import consulo.language.psi.PsiModificationTracker;
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
import consulo.project.ui.view.tree.ProjectViewNode;
import consulo.project.ui.view.tree.PsiDirectoryNode;
import consulo.fileEditor.FileEditorManager;
import consulo.ui.Tree;
import consulo.ui.TreeNode;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.application.dumb.DumbAware;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.action.ToggleAction;
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
import consulo.virtualFileSystem.status.FileStatusListener;
import consulo.virtualFileSystem.status.FileStatusManager;
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

            // the provider hangs off the layout around the tree, but a popup anchors to the tree - it is the one
            // which knows where its selected row ended up
            if (myTree != null) {
                sink.set(consulo.ui.Component.KEY, myTree);
            }
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

    // the same names the awt view reads and writes - one workspace is shared by both
    private static final String ELEMENT_NAVIGATOR = "navigator";
    private static final String ELEMENT_SORT_BY_TYPE = "sortByType";
    private static final String ELEMENT_MANUAL_ORDER = "manualOrder";
    private static final String ELEMENT_ABBREVIATE_PACKAGE_NAMES = "abbreviatePackageNames";
    private static final String ELEMENT_SHOW_LIBRARY_CONTENTS = "showLibraryContents";
    private static final String ELEMENT_FOLDERS_ALWAYS_ON_TOP = "foldersAlwaysOnTop";
    private static final String ATTRIBUTE_VALUE = "value";

    private final Map<String, Boolean> mySortByType = new HashMap<>();
    private final Map<String, Boolean> myManualOrder = new HashMap<>();
    private final Map<String, Boolean> myAbbreviatePackageNames = new HashMap<>();
    private final Map<String, Boolean> myShowLibraryContents = new HashMap<>();
    private boolean myFoldersAlwaysOnTop = true;

    private final Project myProject;
    private final Map<String, SelectInTarget> mySelectInTargets = new LinkedHashMap<>();

    private AbstractProjectViewPane myCurrentPane;

    private Tree<AbstractTreeNode> myTree;

    /**
     * Read back before the tree exists, and kept afterwards so that the state of a session which is already
     * gone - the tool window content is built anew for every ui - is still what gets written out.
     */
    private @Nullable UITreeState myReadTreeState;

    private boolean myStructureRefreshScheduled;

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
        // an option the pane contributes - Show Excluded Files - changes what the structure answers, and asks
        // for the rebuild through the pane it belongs to
        projectViewPane.setRebuildHandler(this::resortTree);

        SelectInTarget selectInTarget = projectViewPane.createSelectInTarget();
        if (selectInTarget != null) {
            mySelectInTargets.put(projectViewPane.getId(), selectInTarget);
        }

        ProjectAbstractTreeStructureBase structure = projectViewPane.createStructure();

        // the pane builds the very same one for the awt tree - the order of a level is a property of the view
        // rather than of the frontend drawing it
        Comparator<NodeDescriptor> nodeOrder = new GroupByTypeComparator(this, projectViewPane.getId());

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

            @Override
            public Comparator<TreeNode<AbstractTreeNode>> getNodeComparator() {
                return (o1, o2) -> nodeOrder.compare(o1.getValue(), o2.getValue());
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

        // the awt view listens the same way - a status change re-renders the node of that file, a global one
        // re-renders everything. the renderer builds its descriptor anew on every render, so re-reading the
        // item is all a colour change needs
        FileStatusManager.getInstance(myProject).addFileStatusListener(new FileStatusListener() {
            @Override
            public void fileStatusesChanged() {
                refreshLoadedPresentations(null);
            }

            @Override
            public void fileStatusChanged(VirtualFile file) {
                refreshLoadedPresentations(file);
            }
        }, this);

        // a status listener only re-reads the nodes which are already there, so a created or deleted file has to
        // come from the psi tree - which is where the awt view takes it from as well
        PsiManager.getInstance(myProject).addPsiTreeChangeListener(new MyPsiTreeChangeListener(), this);

        List<AnAction> titleActions = new ArrayList<>();
        createTitleActions(titleActions);
        if (!titleActions.isEmpty()) {
            toolWindow.setTitleActions(titleActions.toArray(AnAction[]::new));
        }

        toolWindow.setAdditionalGearActions(createViewOptionsGroup());
    }

    private void createTitleActions(List<? super AnAction> titleActions) {
        titleActions.add(ActionManager.getInstance().getAction(ProjectViewToolbarGroup.class));
    }

    /**
     * The awt view hands the very same toggles to its tool window as gear actions - the options belong to the
     * view, and the ones a pane contributes ({@code Show Excluded Files} among them) come from the pane.
     */
    private DefaultActionGroup createViewOptionsGroup() {
        DefaultActionGroup group = new DefaultActionGroup();
        group.add(new ManualOrderAction());
        group.add(new SortByTypeAction());
        group.add(new FoldersAlwaysOnTopAction());

        AbstractProjectViewPane pane = myCurrentPane;
        if (pane != null) {
            pane.addToolbarActionsImpl(group);
        }

        return group;
    }

    private class ManualOrderAction extends ToggleAction implements DumbAware {
        private ManualOrderAction() {
            super(
                IdeLocalize.actionManualOrder(),
                IdeLocalize.actionManualOrder(),
                PlatformIconGroup.objectbrowserSorted()
            );
        }

        @Override
        public boolean isSelected(AnActionEvent event) {
            return isManualOrder(getCurrentViewId());
        }

        @Override
        @RequiredUIAccess
        public void setSelected(AnActionEvent event, boolean flag) {
            setManualOrder(getCurrentViewId(), flag);
        }

        @RequiredUIAccess
        @Override
        public void update(AnActionEvent e) {
            super.update(e);
            AbstractProjectViewPane pane = getCurrentProjectViewPane();
            e.getPresentation().setEnabledAndVisible(pane != null && pane.supportsManualOrder());
        }
    }

    private class SortByTypeAction extends ToggleAction implements DumbAware {
        private SortByTypeAction() {
            super(
                IdeLocalize.actionSortByType(),
                IdeLocalize.actionSortByType(),
                PlatformIconGroup.objectbrowserSortbytype()
            );
        }

        @Override
        public boolean isSelected(AnActionEvent event) {
            return isSortByType(getCurrentViewId());
        }

        @Override
        @RequiredUIAccess
        public void setSelected(AnActionEvent event, boolean flag) {
            setSortByType(getCurrentViewId(), flag);
        }

        @RequiredUIAccess
        @Override
        public void update(AnActionEvent e) {
            super.update(e);
            AbstractProjectViewPane pane = getCurrentProjectViewPane();
            e.getPresentation().setEnabledAndVisible(pane != null && pane.supportsSortByType());
        }
    }

    private class FoldersAlwaysOnTopAction extends ToggleAction implements DumbAware {
        private FoldersAlwaysOnTopAction() {
            super(LocalizeValue.localizeTODO("Folders Always on Top"));
        }

        @Override
        public boolean isSelected(AnActionEvent event) {
            return isFoldersAlwaysOnTop();
        }

        @Override
        @RequiredUIAccess
        public void setSelected(AnActionEvent event, boolean flag) {
            setFoldersAlwaysOnTop(flag);
        }

        @RequiredUIAccess
        @Override
        public void update(AnActionEvent e) {
            super.update(e);
            AbstractProjectViewPane pane = getCurrentProjectViewPane();
            e.getPresentation().setEnabledAndVisible(pane != null && pane.supportsFoldersAlwaysOnTop());
        }
    }

    /**
     * Re-renders the loaded nodes standing for the file, every loaded node when there is no file to point
     * at. Only what is already built is walked - a node that was never opened renders itself with the
     * current status when it first appears.
     */
    private void refreshLoadedPresentations(@Nullable VirtualFile file) {
        refreshLoadedPresentations(file, false);
    }

    private void refreshLoadedPresentations(@Nullable VirtualFile file, boolean refreshChildren) {
        Tree<AbstractTreeNode> tree = myTree;
        if (tree == null) {
            return;
        }

        myProject.getUIAccess().giveIfNeed(() -> {
            TreeNode<AbstractTreeNode> root = tree.getRootNode();
            if (root != null) {
                refreshLoadedPresentations(tree, root, file, refreshChildren);
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
        if (myTree == null) {
            return;
        }

        UITreeState treeState = UITreeState.createOn(myTree);
        if (treeState.isEmpty()) {
            // an empty snapshot cannot tell a deliberately collapsed tree from one whose restore has not run
            // yet - dumb mode holds the walk back - and writing it over the read state is what carried a saved
            // layout away on close
            return;
        }

        myReadTreeState = treeState;
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

        writeNavigator(element);

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

    /**
     * The awt view keeps an option per pane as an attribute named after the pane, and a single
     * {@code foldersAlwaysOnTop} carrying its own value - only the entries this view understands are rewritten,
     * the rest of the navigator is carried over from what was read.
     */
    private void writeNavigator(Element element) {
        Element navigator = element.getChild(ELEMENT_NAVIGATOR);
        if (navigator == null) {
            navigator = new Element(ELEMENT_NAVIGATOR);
            element.addContent(navigator);
        }

        writeOption(navigator, mySortByType, ELEMENT_SORT_BY_TYPE);
        writeOption(navigator, myManualOrder, ELEMENT_MANUAL_ORDER);
        writeOption(navigator, myAbbreviatePackageNames, ELEMENT_ABBREVIATE_PACKAGE_NAMES);
        writeOption(navigator, myShowLibraryContents, ELEMENT_SHOW_LIBRARY_CONTENTS);

        navigator.removeChildren(ELEMENT_FOLDERS_ALWAYS_ON_TOP);
        Element folders = new Element(ELEMENT_FOLDERS_ALWAYS_ON_TOP);
        folders.setAttribute(ATTRIBUTE_VALUE, Boolean.toString(myFoldersAlwaysOnTop));
        navigator.addContent(folders);
    }

    private static void writeOption(Element navigator, Map<String, Boolean> options, String optionName) {
        navigator.removeChildren(optionName);

        Element element = new Element(optionName);
        for (Map.Entry<String, Boolean> entry : options.entrySet()) {
            if (entry.getKey() != null) {
                element.setAttribute(entry.getKey(), Boolean.toString(entry.getValue()));
            }
        }
        navigator.addContent(element);
    }

    private static void readOption(@Nullable Element node, Map<String, Boolean> options) {
        if (node == null) {
            return;
        }
        for (Attribute attribute : node.getAttributes()) {
            options.put(attribute.getName(), Boolean.TRUE.toString().equals(attribute.getValue()));
        }
    }

    @Override
    public void loadState(Element state) {
        myLoadedState = state.clone();

        Element navigator = state.getChild(ELEMENT_NAVIGATOR);
        if (navigator != null) {
            readOption(navigator.getChild(ELEMENT_SORT_BY_TYPE), mySortByType);
            readOption(navigator.getChild(ELEMENT_MANUAL_ORDER), myManualOrder);
            readOption(navigator.getChild(ELEMENT_ABBREVIATE_PACKAGE_NAMES), myAbbreviatePackageNames);
            readOption(navigator.getChild(ELEMENT_SHOW_LIBRARY_CONTENTS), myShowLibraryContents);

            Element folders = navigator.getChild(ELEMENT_FOLDERS_ALWAYS_ON_TOP);
            if (folders != null) {
                myFoldersAlwaysOnTop = Boolean.parseBoolean(folders.getAttributeValue(ATTRIBUTE_VALUE));
            }
        }

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
        return getPaneOption(myShowLibraryContents, paneId);
    }

    @Override
    public void setShowLibraryContents(boolean showLibraryContents, String paneId) {
        setPaneOption(myShowLibraryContents, paneId, showLibraryContents);
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
        return getPaneOption(myAbbreviatePackageNames, paneId);
    }

    @Override
    public void setAbbreviatePackageNames(boolean abbreviatePackageNames, String paneId) {
        setPaneOption(myAbbreviatePackageNames, paneId, abbreviatePackageNames);
    }

    @Override
    public String getCurrentViewId() {
        return getPaneId();
    }

    @Override
    public boolean isManualOrder(String paneId) {
        return getPaneOption(myManualOrder, paneId);
    }

    @Override
    public void setManualOrder(String paneId, boolean enabled) {
        setPaneOption(myManualOrder, paneId, enabled);
    }

    @Override
    public boolean isFoldersAlwaysOnTop() {
        return myFoldersAlwaysOnTop;
    }

    public void setFoldersAlwaysOnTop(boolean foldersAlwaysOnTop) {
        if (myFoldersAlwaysOnTop != foldersAlwaysOnTop) {
            myFoldersAlwaysOnTop = foldersAlwaysOnTop;
            resortTree();
        }
    }

    private boolean getPaneOption(Map<String, Boolean> options, String paneId) {
        return Boolean.TRUE.equals(options.get(paneId));
    }

    private void setPaneOption(Map<String, Boolean> options, String paneId, boolean value) {
        if (getPaneOption(options, paneId) != value) {
            options.put(paneId, value);
            resortTree();
        }
    }

    /**
     * {@code ProjectViewPsiTreeChangeListener} answers a change by queueing the subtree which holds it. A change
     * inside one file is scoped the same way here - only the loaded nodes of that file are re-read, since typing
     * cannot move any other row. Only a change with no file to point at - a file created or removed, a root
     * changed - is answered by rebuilding, and the strokes of one edit are collapsed into a single answer by the
     * psi modification count, which the awt listener reads for the same reason.
     */
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

    /**
     * Every change of one modification asks for the same rebuild, and the ones which arrive while it is still
     * being asked for are the same rebuild again - so only the first schedules it.
     */
    private void scheduleStructureRefresh() {
        if (myTree == null || myStructureRefreshScheduled) {
            return;
        }

        UIAccess uiAccess = myProject.getUIAccess();

        myStructureRefreshScheduled = true;

        uiAccess.give(() -> {
            myStructureRefreshScheduled = false;

            resortTree();
        });
    }

    /**
     * The order of a level is settled while its children are built, and they are held from then on - so the tree
     * builds them again rather than laying the ones it has out anew. That replaces every node, which is what
     * carries the open ones away, so the paths are taken first and walked again after - the awt view stores and
     * restores them around {@code updateFromRoot} for the same reason.
     */
    @RequiredUIAccess
    private void resortTree() {
        if (myTree == null) {
            return;
        }

        saveExpandedPaths();

        myTree.refreshAll().thenRun(this::restoreExpandedPaths);
    }

    @Override
    public void selectPsiElement(PsiElement element, boolean requestFocus) {
    }

    @Override
    @RequiredUIAccess
    public void reRestoreExpandedPaths() {
        Tree<AbstractTreeNode> tree = myTree;
        if (tree == null) {
            return;
        }

        tree.refreshAll().thenRunAsync(this::restoreExpandedPaths, UIAccess.current());
    }

    @Override
    @RequiredUIAccess
    public void scrollFromSource() {
        Tree<AbstractTreeNode> tree = myTree;
        if (tree == null) {
            return;
        }

        VirtualFile[] selectedFiles = FileEditorManager.getInstance(myProject).getSelectedFiles();
        if (selectedFiles.length == 0) {
            return;
        }

        TreeNode<AbstractTreeNode> rootNode = tree.getRootNode();
        if (rootNode == null) {
            return;
        }

        VirtualFile file = selectedFiles[0];

        rootNode.findChildDeep(node -> node != null && file.equals(virtualFileOf(node)))
            .whenCompleteAsync((treeNode, throwable) -> {
                if (treeNode != null) {
                    tree.select(treeNode);
                }
            }, UIAccess.current());
    }

    private static @Nullable VirtualFile virtualFileOf(AbstractTreeNode node) {
        return node.getValue() instanceof PsiElement element && element.isValid()
            ? PsiUtilCore.getVirtualFile(element)
            : null;
    }

    @Override
    public boolean isSortByType(String paneId) {
        return getPaneOption(mySortByType, paneId);
    }

    @Override
    public void setSortByType(String paneId, boolean sortByType) {
        setPaneOption(mySortByType, paneId, sortByType);
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
