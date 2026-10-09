/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ide.projectView.impl;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.HelpManager;
import consulo.codeEditor.Editor;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.component.util.BusyObject;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataSink;
import consulo.dataContext.UiDataProvider;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.TextEditor;
import consulo.fileEditor.internal.FileEditorManagerEx;
import consulo.ide.impl.idea.ide.impl.ProjectViewSelectInTarget;
import consulo.ide.impl.idea.ide.projectView.HelpID;
import consulo.ide.impl.idea.ide.projectView.impl.nodes.LibraryGroupNode;
import consulo.ide.impl.idea.ide.projectView.impl.nodes.NamedLibraryElementNode;
import consulo.ide.impl.idea.ide.util.DeleteHandler;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.actions.ModuleDeleteProvider;
import consulo.ide.impl.ui.impl.PopupChooserBuilder;
import consulo.ide.localize.IdeLocalize;
import consulo.ide.util.DirectoryChooserUtil;
import consulo.language.content.ProjectRootsUtil;
import consulo.language.editor.refactoring.ui.CopyPasteDelegator;
import consulo.language.editor.util.EditorHelper;
import consulo.language.editor.util.IdeView;
import consulo.language.psi.*;
import consulo.localHistory.LocalHistory;
import consulo.localHistory.LocalHistoryAction;
import consulo.localize.LocalizeValue;
import consulo.module.Module;
import consulo.module.content.ModuleFileIndex;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.ProjectRootManager;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.module.content.layer.orderEntry.LibraryOrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.project.Project;
import consulo.project.ui.internal.ProjectIdeFocusManager;
import consulo.project.ui.view.ProjectViewAutoScrollFromSourceHandler;
import consulo.project.ui.view.SelectInContext;
import consulo.project.ui.view.SelectInTarget;
import consulo.project.ui.view.internal.ProjectViewSharedSettings;
import consulo.project.ui.view.internal.node.NamedLibraryElement;
import consulo.project.ui.view.tree.*;
import consulo.project.ui.wm.ToolWindowId;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.project.ui.wm.ToolWindowManagerListener;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.CopyProvider;
import consulo.ui.ex.CutProvider;
import consulo.ui.ex.DeleteProvider;
import consulo.ui.ex.PasteProvider;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.internal.GuiUtils;
import consulo.ui.ex.awt.tree.AbstractTreeBuilder;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.ui.ex.awt.tree.TreeVisitor;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.ex.tree.NodeDescriptor;
import consulo.undoRedo.CommandProcessor;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.JBIterable;
import consulo.util.concurrent.AsyncResult;
import consulo.util.dataholder.Key;
import consulo.util.io.URLUtil;
import consulo.util.lang.ref.SimpleReference;
import consulo.util.xml.serializer.InvalidDataException;
import consulo.util.xml.serializer.WriteExternalException;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdom.Element;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Supplier;

@Singleton
@ServiceImpl(profiles = ComponentProfiles.PRODUCTION | ComponentProfiles.AWT)
@State(name = "ProjectView", storages = @Storage(file = StoragePathMacros.WORKSPACE_FILE))
public class ProjectViewImpl extends BaseProjectViewImpl implements QuickActionProvider, BusyObject {
    private final CopyPasteDelegator myCopyPasteDelegator;

    private final AutoScrollToSourceHandler myAutoScrollToSourceHandler;
    private final MyAutoScrollFromSourceHandler myAutoScrollFromSourceHandler;

    private final IdeView myIdeView = new MyIdeView();
    private final MyDeletePSIElementProvider myDeletePSIElementProvider = new MyDeletePSIElementProvider();
    private final ModuleDeleteProvider myDeleteModuleProvider = new ModuleDeleteProvider();

    private SimpleToolWindowPanel myPanel;

    static final Key<ProjectViewImpl> DATA_KEY = Key.create("consulo.ide.impl.idea.ide.projectView.impl.ProjectViewImpl");

    private JPanel myViewContentPanel;
    private final FileEditorManager myFileEditorManager;
    private final MyPanel myDataProvider;
    private final SplitterProportionsData splitterProportions = new SplitterProportionsDataImpl();

    @Inject
    public ProjectViewImpl(
        Project project,
        FileEditorManager fileEditorManager,
        ProjectViewSharedSettings projectViewSharedSettings
    ) {
        super(project, projectViewSharedSettings);

        constructUi();

        myFileEditorManager = fileEditorManager;

        myAutoScrollFromSourceHandler = new MyAutoScrollFromSourceHandler();

        myDataProvider = new MyPanel();

        ClientProperty.put(myDataProvider, UiDataProvider.KEY, this::uiDataSnapshot);
        ClientProperty.put(myDataProvider, UIUtil.NOT_IN_HIERARCHY_COMPONENTS, buildNotInHierarchyIterable());

        myDataProvider.add(myPanel, BorderLayout.CENTER);
        myCopyPasteDelegator = new CopyPasteDelegator(myProject, myPanel) {
            @Override
            @RequiredUIAccess
            protected PsiElement[] getSelectedElements() {
                AbstractProjectViewPane viewPane = getCurrentProjectViewPane();
                return viewPane == null ? PsiElement.EMPTY_ARRAY : viewPane.getSelectedPSIElements();
            }
        };
        myAutoScrollToSourceHandler = new AutoScrollToSourceHandler() {
            @Override
            protected boolean isAutoScrollMode() {
                return isAutoscrollToSource(myCurrentViewId);
            }

            @Override
            protected void setAutoScrollMode(boolean state) {
                setAutoscrollToSource(state, myCurrentViewId);
            }
        };
        project.getMessageBus().connect(this).subscribe(
            ToolWindowManagerListener.class,
            new ToolWindowManagerListener() {
                private boolean toolWindowVisible;

                @Override
                @RequiredUIAccess
                public void stateChanged(ToolWindowManager toolWindowManager) {
                    ToolWindow window = toolWindowManager.getToolWindow(ToolWindowId.PROJECT_VIEW);
                    if (window == null) {
                        return;
                    }
                    if (window.isVisible() && !toolWindowVisible) {
                        String id = getCurrentViewId();
                        if (isAutoscrollToSource(id)) {
                            AbstractProjectViewPane currentProjectViewPane = getCurrentProjectViewPane();

                            if (currentProjectViewPane != null) {
                                myAutoScrollToSourceHandler.onMouseClicked(currentProjectViewPane.getTree());
                            }
                        }
                        if (isAutoscrollFromSource(id)) {
                            myAutoScrollFromSourceHandler.setAutoScrollEnabled(true);
                        }
                    }
                    toolWindowVisible = window.isVisible();
                }
            }
        );
    }

    private Iterable<? extends Component> buildNotInHierarchyIterable() {
        return () -> JBIterable.from(new ArrayList<>(myId2Pane.values()))
            .map(pane -> {
                JComponent last = null;
                for (Component c : UIUtil.uiParents(pane.getComponentToFocus(), false)) {
                    if (c == myDataProvider || !(c instanceof JComponent)) {
                        return null;
                    }
                    last = (JComponent) c;
                }
                return last;
            })
            .filter(Component.class)
            .iterator();
    }

    @RequiredUIAccess
    private void uiDataSnapshot(DataSink sink) {
        sink.set(CutProvider.KEY, myCopyPasteDelegator.getCutProvider());
        sink.set(CopyProvider.KEY, myCopyPasteDelegator.getCopyProvider());
        sink.set(PasteProvider.KEY, myCopyPasteDelegator.getPasteProvider());
        sink.set(IdeView.KEY, myIdeView);
        sink.set(HelpManager.HELP_ID, HelpID.PROJECT_VIEWS);
        sink.set(QuickActionProvider.KEY, ProjectViewImpl.this);
        AbstractProjectViewPane selectedPane = getCurrentProjectViewPane();
        if (selectedPane != null) {
            selectedPane.uiDataSnapshot(sink);
        }
    }

    private void constructUi() {
        myViewContentPanel = new JPanel();
        myPanel = new SimpleToolWindowPanel(true).setProvideQuickActions(false);
        myPanel.setContent(myViewContentPanel);
    }

    @Override
    public String getName() {
        return "Project";
    }

    @Override
    public List<AnAction> getActions(boolean originalProvider) {
        List<AnAction> result = new ArrayList<>();

        DefaultActionGroup views = new DefaultActionGroup(LocalizeValue.localizeTODO("Change View"), true);

        ChangeViewAction lastHeader = null;
        for (int i = 0; i < myContentManager.getContentCount(); i++) {
            Content each = myContentManager.getContent(i);
            if (each == null) {
                continue;
            }

            String id = each.getUserData(ID_KEY);
            String subId = each.getUserData(SUB_ID_KEY);
            ChangeViewAction newHeader = new ChangeViewAction(id, subId);

            if (lastHeader != null) {
                boolean lastHasKids = lastHeader.mySubId != null;
                boolean newHasKids = newHeader.mySubId != null;
                if (lastHasKids != newHasKids || lastHasKids && lastHeader.myId != newHeader.myId) {
                    views.add(AnSeparator.getInstance());
                }
            }

            views.add(newHeader);
            lastHeader = newHeader;
        }
        result.add(views);
        result.add(AnSeparator.getInstance());

        if (myActionGroup != null) {
            Collections.addAll(result, myActionGroup.getChildren(null));
        }

        return result;
    }

    private class ChangeViewAction extends LegacyAnAction {
        private final String myId;
        private final @Nullable String mySubId;

        private ChangeViewAction(String id, @Nullable String subId) {
            myId = id;
            mySubId = subId;
        }

        @RequiredUIAccess
        @Override
        public void update(AnActionEvent e) {
            AbstractProjectViewPane pane = getProjectViewPaneById(myId);
            e.getPresentation().setText(mySubId != null ? pane.getPresentableSubIdName(mySubId) : pane.getTitle());
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            changeView(myId, mySubId);
        }
    }

    @Override
    @RequiredUIAccess
    protected void showPane(AbstractProjectViewPane newPane) {
        AbstractProjectViewPane currentPane = getCurrentProjectViewPane();
        PsiElement selectedPsiElement = null;
        if (currentPane != null) {
            if (currentPane != newPane) {
                currentPane.saveExpandedPaths();
            }
            PsiElement[] elements = currentPane.getSelectedPSIElements();
            if (elements.length > 0) {
                selectedPsiElement = elements[0];
            }
        }
        myViewContentPanel.removeAll();
        JComponent component = newPane.createComponent();
        UIUtil.removeScrollBorder(component);
        myViewContentPanel.setLayout(new BorderLayout());
        myViewContentPanel.add(component, BorderLayout.CENTER);
        myCurrentViewId = newPane.getId();
        String newSubId = myCurrentViewSubId = newPane.getSubId();
        myViewContentPanel.revalidate();
        myViewContentPanel.repaint();
        createToolbarActions();

        myAutoScrollToSourceHandler.install(newPane.myTree);

        ProjectIdeFocusManager.getInstance(myProject).requestFocusInProject(newPane.getComponentToFocus(), myProject);

        newPane.restoreExpandedPaths();
        if (selectedPsiElement != null && newSubId != null) {
            VirtualFile virtualFile = PsiUtilCore.getVirtualFile(selectedPsiElement);
            ProjectViewSelectInTarget target = virtualFile == null ? null : getProjectViewSelectInTarget(newPane);
            if (target != null && target.isSubIdSelectable(newSubId, new SelectInContext() {
                @Override
                public Project getProject() {
                    return myProject;
                }

                @Override
                public VirtualFile getVirtualFile() {
                    return virtualFile;
                }

                @Override
                public Object getSelectorInFile() {
                    return null;
                }
            })) {
                newPane.select(selectedPsiElement, virtualFile, true);
            }
        }
        myAutoScrollToSourceHandler.onMouseClicked(newPane.myTree);
    }

    @Override
    protected Content createPaneContent(String title) {
        Content content = getContentManager().getFactory().createContent(getComponent(), title, false);
        content.setPreferredFocusedComponent(() -> {
            AbstractProjectViewPane current = getCurrentProjectViewPane();
            return current != null ? current.getComponentToFocus() : null;
        });
        content.setBusyObject(this);
        return content;
    }

    @Override
    @RequiredUIAccess
    protected void installToolWindow(ToolWindow toolWindow) {
        myAutoScrollFromSourceHandler.install();

        GuiUtils.replaceJSplitPaneWithIDEASplitter(myPanel);
        SwingUtilities.invokeLater(() -> splitterProportions.restoreSplitterProportions(myPanel));
    }

    @Override
    @RequiredUIAccess
    protected Runnable saveSelection(@Nullable AbstractProjectViewPane pane) {
        SelectionInfo selectionInfo = SelectionInfo.create(pane);
        return () -> selectionInfo.apply(pane);
    }

    @Override
    protected void addAutoScrollActions(DefaultActionGroup actionGroup) {
        actionGroup.addAction(myAutoScrollToSourceHandler.createToggleAction()).setAsSecondary(true);
        actionGroup.addAction(myAutoScrollFromSourceHandler.createToggleAction()).setAsSecondary(true);
    }

    @Override
    @RequiredUIAccess
    public void reRestoreExpandedPaths() {
        AbstractProjectViewPane pane = getCurrentProjectViewPane();
        if (pane == null) {
            return;
        }
        pane.updateFromRoot(false).doWhenDone(pane::reRestoreExpandedPaths);
    }

    @Override
    public JComponent getComponent() {
        return myDataProvider;
    }

    @Override
    @RequiredUIAccess
    public PsiElement getParentOfCurrentSelection() {
        AbstractProjectViewPane viewPane = getCurrentProjectViewPane();
        if (viewPane == null) {
            return null;
        }
        TreePath path = viewPane.getSelectedPath();
        if (path == null) {
            return null;
        }
        path = path.getParentPath();
        if (path == null) {
            return null;
        }
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        Object userObject = node.getUserObject();
        if (userObject instanceof ProjectViewNode descriptor && descriptor.getValue() instanceof PsiElement psiElement) {
            return psiElement.isValid() ? psiElement : null;
        }
        return null;
    }

    @Override
    @RequiredUIAccess
    public void changeView() {
        List<AbstractProjectViewPane> views = new ArrayList<>(myId2Pane.values());
        views.remove(getCurrentProjectViewPane());
        Collections.sort(views, PANE_WEIGHT_COMPARATOR);

        JList<AbstractProjectViewPane> list = new JBList(ArrayUtil.toObjectArray(views));
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                AbstractProjectViewPane pane = (AbstractProjectViewPane) value;
                setText(pane.getTitle().get());
                return this;
            }
        });

        if (!views.isEmpty()) {
            list.setSelectedValue(views.get(0), true);
        }
        @RequiredUIAccess
        Runnable runnable = () -> {
            if (list.getSelectedIndex() < 0) {
                return;
            }
            AbstractProjectViewPane pane = list.getSelectedValue();
            changeView(pane.getId());
        };

        new PopupChooserBuilder(list)
            .setTitle(IdeLocalize.titlePopupViews().get())
            .setItemChoosenCallback(runnable)
            .createPopup()
            .showInCenterOf(getComponent());
    }

    private final class MyDeletePSIElementProvider implements DeleteProvider {
        @Override
        @RequiredUIAccess
        public boolean canDeleteElement(DataContext dataContext) {
            PsiElement[] elements = getElementsToDelete();
            return DeleteHandler.shouldEnableDeleteAction(elements);
        }

        @Override
        @RequiredUIAccess
        public void deleteElement(DataContext dataContext) {
            List<PsiElement> allElements = Arrays.asList(getElementsToDelete());
            List<PsiElement> validElements = new ArrayList<>();
            for (PsiElement psiElement : allElements) {
                if (psiElement != null && psiElement.isValid()) {
                    validElements.add(psiElement);
                }
            }
            PsiElement[] elements = PsiUtilCore.toPsiElementArray(validElements);

            LocalHistoryAction a = LocalHistory.getInstance().startAction(IdeLocalize.progressDeleting());
            try {
                DeleteHandler.deletePsiElement(elements, myProject);
            }
            finally {
                a.finish();
            }
        }

        @RequiredUIAccess
        private PsiElement[] getElementsToDelete() {
            AbstractProjectViewPane viewPane = getCurrentProjectViewPane();
            PsiElement[] elements = viewPane.getSelectedPSIElements();
            for (int idx = 0; idx < elements.length; idx++) {
                PsiElement element = elements[idx];
                if (element instanceof PsiDirectory directory) {
                    if (isHideEmptyMiddlePackages(viewPane.getId())
                        && directory.getChildren().length == 0
                        && !BaseProjectViewDirectoryHelper.skipDirectory(directory)) {
                        while (true) {
                            PsiDirectory parent = directory.getParentDirectory();
                            if (parent == null) {
                                break;
                            }
                            if (BaseProjectViewDirectoryHelper.skipDirectory(parent) ||
                                PsiPackageHelper.getInstance(myProject).getQualifiedName(parent, false).length() == 0) {
                                break;
                            }
                            PsiElement[] children = parent.getChildren();
                            if (children.length == 0 || children.length == 1 && children[0] == directory) {
                                directory = parent;
                            }
                            else {
                                break;
                            }
                        }
                        elements[idx] = directory;
                    }
                    VirtualFile virtualFile = directory.getVirtualFile();
                    String path = virtualFile.getPath();
                    if (path.endsWith(URLUtil.ARCHIVE_SEPARATOR)) {
                        VirtualFile vFile = LocalFileSystem.getInstance()
                            .findFileByPath(path.substring(0, path.length() - URLUtil.ARCHIVE_SEPARATOR.length()));
                        if (vFile != null) {
                            PsiFile psiFile = PsiManager.getInstance(myProject).findFile(vFile);
                            if (psiFile != null) {
                                elements[idx] = psiFile;
                            }
                        }
                    }
                }
            }
            return elements;
        }

    }

    private final class MyPanel extends JPanel {
        MyPanel() {
            super(new BorderLayout());
        }

        @RequiredUIAccess
        private @Nullable LibraryOrderEntry getSelectedLibrary() {
            AbstractProjectViewPane viewPane = getCurrentProjectViewPane();
            DefaultMutableTreeNode node = viewPane != null ? viewPane.getSelectedNode() : null;
            if (node == null) {
                return null;
            }
            DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
            if (parent == null) {
                return null;
            }
            Object userObject = parent.getUserObject();
            if (userObject instanceof LibraryGroupNode libraryGroupNode) {
                userObject = node.getUserObject();
                if (userObject instanceof NamedLibraryElementNode namedLibraryElementNode) {
                    NamedLibraryElement element = namedLibraryElementNode.getValue();
                    OrderEntry orderEntry = element.getOrderEntry();
                    return orderEntry instanceof LibraryOrderEntry libraryOrderEntry ? libraryOrderEntry : null;
                }
                PsiDirectory directory = ((PsiDirectoryNode) userObject).getValue();
                VirtualFile virtualFile = directory.getVirtualFile();
                Module module = (Module) ((AbstractTreeNode) ((DefaultMutableTreeNode) parent.getParent()).getUserObject()).getValue();

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
                        if (entry instanceof LibraryOrderEntry libraryOrderEntry && libraryOrderEntry.getLibrary() == orderEntry.getLibrary()) {
                            model.removeOrderEntry(modifiableEntries[i]);
                        }
                    }
                    model.commit();
                });
        }

        @RequiredUIAccess
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

    @RequiredUIAccess
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
                result.add((T) element);
            }
        }
        return result;
    }

    private final class MyIdeView implements IdeView {
        @Override
        @RequiredUIAccess
        public void selectElement(PsiElement element) {
            selectPsiElement(element, false);
            boolean requestFocus = true;
            if (element != null && !(element instanceof PsiDirectory)) {
                FileEditor editor = EditorHelper.openInEditor(element, false);
                if (editor != null) {
                    ToolWindowManager.getInstance(myProject).activateEditorComponent();
                    requestFocus = false;
                }
            }

            if (requestFocus) {
                selectPsiElement(element, true);
            }
        }

        @Override
        @RequiredUIAccess
        public PsiDirectory[] getDirectories() {
            AbstractProjectViewPane viewPane = getCurrentProjectViewPane();
            if (viewPane != null) {
                SimpleReference<PsiDirectory[]> ref = SimpleReference.create();
                Application.get().tryRunReadAction(ref, viewPane::getSelectedDirectories);
                return Objects.requireNonNullElse(ref.get(), PsiDirectory.EMPTY_ARRAY);
            }

            return PsiDirectory.EMPTY_ARRAY;
        }

        @Override
        @RequiredUIAccess
        public PsiDirectory getOrChooseDirectory() {
            return DirectoryChooserUtil.getOrChooseDirectory(this);
        }
    }

    @Override
    protected void readNavigatorState(Element navigatorElement) {
        try {
            splitterProportions.readExternal(navigatorElement);
        }
        catch (InvalidDataException e) {
            // ignore
        }
    }

    @Override
    protected void writeNavigatorState(Element navigatorElement) {
        splitterProportions.saveSplitterProportions(myPanel);
        try {
            splitterProportions.writeExternal(navigatorElement);
        }
        catch (WriteExternalException e) {
            // ignore
        }
    }

    private static class SelectionInfo {
        private final Object[] myElements;

        private SelectionInfo(Object[] elements) {
            myElements = elements;
        }

        public void apply(AbstractProjectViewPane viewPane) {
            if (viewPane == null) {
                return;
            }
            AbstractTreeBuilder treeBuilder = viewPane.getTreeBuilder();
            JTree tree = viewPane.myTree;
            if (treeBuilder != null) {
                DefaultTreeModel treeModel = (DefaultTreeModel) tree.getModel();
                List<TreePath> paths = new ArrayList<>(myElements.length);
                for (Object element : myElements) {
                    DefaultMutableTreeNode node = treeBuilder.getNodeForElement(element);
                    if (node == null) {
                        treeBuilder.buildNodeForElement(element);
                        node = treeBuilder.getNodeForElement(element);
                    }
                    if (node != null) {
                        paths.add(new TreePath(treeModel.getPathToRoot(node)));
                    }
                }
                if (!paths.isEmpty()) {
                    tree.setSelectionPaths(paths.toArray(new TreePath[0]));
                }
            }
            else {
                List<TreeVisitor> visitors = AbstractProjectViewPane.createVisitors(myElements);
                if (1 == visitors.size()) {
                    TreeUtil.promiseSelect(tree, visitors.get(0));
                }
                else if (!visitors.isEmpty()) {
                    TreeUtil.promiseSelect(tree, visitors.stream());
                }
            }
        }

        public static SelectionInfo create(AbstractProjectViewPane viewPane) {
            List<Object> selectedElements = Collections.emptyList();
            if (viewPane != null) {
                TreePath[] selectionPaths = viewPane.getSelectionPaths();
                if (selectionPaths != null) {
                    selectedElements = new ArrayList<>();
                    for (TreePath path : selectionPaths) {
                        NodeDescriptor descriptor = TreeUtil.getLastUserObject(NodeDescriptor.class, path);
                        if (descriptor != null) {
                            selectedElements.add(descriptor.getElement());
                        }
                    }
                }
            }
            return new SelectionInfo(selectedElements.toArray());
        }
    }

    private class MyAutoScrollFromSourceHandler extends ProjectViewAutoScrollFromSourceHandler {
        private MyAutoScrollFromSourceHandler() {
            super(ProjectViewImpl.this.myProject, TargetAWT.wrap(myViewContentPanel), ProjectViewImpl.this);
        }

        @Override
        @RequiredUIAccess
        protected void selectElementFromEditor(FileEditor fileEditor) {
            if (myProject.isDisposed() || !myViewContentPanel.isShowing()) {
                return;
            }
            if (isAutoscrollFromSource(getCurrentViewId())) {
                if (fileEditor instanceof TextEditor textEditor) {
                    Editor editor = textEditor.getEditor();
                    selectElementAtCaretNotLosingFocus(editor);
                }
                else {
                    SelectInTarget target = getCurrentSelectInTarget();
                    if (target != null) {
                        VirtualFile file = FileEditorManagerEx.getInstanceEx(myProject).getFile(fileEditor);
                        if (file != null && file.isValid()) {
                            PsiFile psiFile = PsiManager.getInstance(myProject).findFile(file);
                            if (psiFile != null) {
                                MySelectInContext selectInContext = new MySelectInContext(psiFile, null) {
                                    @Override
                                    @RequiredReadAction
                                    public Object getSelectorInFile() {
                                        return psiFile;
                                    }
                                };

                                if (target.canSelect(selectInContext)) {
                                    target.selectIn(selectInContext, false);
                                }
                            }
                        }
                    }
                }
            }
        }

        @RequiredUIAccess
        public void scrollFromSource() {
            FileEditorManager fileEditorManager = FileEditorManager.getInstance(myProject);
            Editor selectedTextEditor = fileEditorManager.getSelectedTextEditor();
            if (selectedTextEditor != null) {
                selectElementAtCaret(selectedTextEditor);
                return;
            }
            FileEditor[] editors = fileEditorManager.getSelectedEditors();
            for (FileEditor fileEditor : editors) {
                if (fileEditor instanceof TextEditor textEditor) {
                    Editor editor = textEditor.getEditor();
                    selectElementAtCaret(editor);
                    return;
                }
            }
            VirtualFile[] selectedFiles = fileEditorManager.getSelectedFiles();
            if (selectedFiles.length > 0) {
                PsiFile file = PsiManager.getInstance(myProject).findFile(selectedFiles[0]);
                if (file != null) {
                    scrollFromFile(file, null);
                }
            }
        }

        @RequiredUIAccess
        private void selectElementAtCaretNotLosingFocus(Editor editor) {
            AbstractProjectViewPane pane = getCurrentProjectViewPane();
            if (pane != null && !IJSwingUtilities.hasFocus(pane.getComponentToFocus())) {
                selectElementAtCaret(editor);
            }
        }

        @RequiredUIAccess
        private void selectElementAtCaret(Editor editor) {
            PsiFile file = PsiDocumentManager.getInstance(myProject).getPsiFile(editor.getDocument());
            if (file == null) {
                return;
            }

            scrollFromFile(file, editor);
        }

        @RequiredUIAccess
        private void scrollFromFile(PsiFile file, @Nullable Editor editor) {
            SmartPsiElementPointer<PsiFile> pointer = SmartPointerManager.getInstance(myProject).createSmartPsiElementPointer(file);
            PsiDocumentManager.getInstance(myProject).performLaterWhenAllCommitted(() -> {
                SelectInTarget target = getCurrentSelectInTarget();
                if (target == null) {
                    return;
                }

                PsiFile restoredPsi = pointer.getElement();
                if (restoredPsi == null) {
                    return;
                }

                MySelectInContext selectInContext = new MySelectInContext(restoredPsi, editor);

                if (target.canSelect(selectInContext)) {
                    target.selectIn(selectInContext, false);
                }
            });
        }

        @Override
        protected boolean isAutoScrollEnabled() {
            return isAutoscrollFromSource(myCurrentViewId);
        }

        @Override
        @RequiredUIAccess
        protected void setAutoScrollEnabled(boolean state) {
            setAutoscrollFromSource(state, myCurrentViewId);
            if (state) {
                Editor editor = myFileEditorManager.getSelectedTextEditor();
                if (editor != null) {
                    selectElementAtCaretNotLosingFocus(editor);
                }
            }
            createToolbarActions();
        }

        private class MySelectInContext implements SelectInContext {
            private final PsiFile myPsiFile;
            private final @Nullable Editor myEditor;

            private MySelectInContext(PsiFile psiFile, @Nullable Editor editor) {
                myPsiFile = psiFile;
                myEditor = editor;
            }

            @Override
            public Project getProject() {
                return myProject;
            }

            private PsiFile getPsiFile() {
                return myPsiFile;
            }

            @Override
            public Supplier<FileEditor> getFileEditorProvider() {
                return () -> myFileEditorManager.openFile(myPsiFile.getContainingFile().getVirtualFile(), false)[0];
            }

            @RequiredReadAction
            private PsiElement getPsiElement() {
                PsiElement e = null;
                if (myEditor != null) {
                    int offset = myEditor.getCaretModel().getOffset();
                    if (PsiDocumentManager.getInstance(myProject).hasUncommitedDocuments()) {
                        PsiDocumentManager.getInstance(myProject).commitAllDocuments();
                    }
                    e = getPsiFile().findElementAt(offset);
                }
                if (e == null) {
                    e = getPsiFile();
                }
                return e;
            }

            @Override
            public VirtualFile getVirtualFile() {
                return getPsiFile().getVirtualFile();
            }

            @Override
            @RequiredReadAction
            public Object getSelectorInFile() {
                return getPsiElement();
            }
        }
    }

    @Override
    @RequiredUIAccess
    public void scrollFromSource() {
        myAutoScrollFromSourceHandler.scrollFromSource();
    }

    @Override
    public AsyncResult<Void> getReady(Object requestor) {
        AbstractProjectViewPane pane = myId2Pane.get(myCurrentViewSubId);
        if (pane == null) {
            pane = myId2Pane.get(myCurrentViewId);
        }
        return pane != null ? pane.getReady(requestor) : AsyncResult.done(null);
    }
}
