/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.desktop.awt.language.editor.hierarchy;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.application.HelpManager;
import consulo.application.ReadAction;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.content.scope.NamedScope;
import consulo.content.scope.NamedScopesHolder;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataSink;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.ide.OccurenceNavigatorSupport;
import consulo.ide.impl.idea.ide.dnd.TransferableWrapper;
import consulo.ide.impl.idea.ide.projectView.impl.ProjectViewTree;
import consulo.ide.impl.idea.ide.util.DeleteHandler;
import consulo.ide.impl.idea.ide.util.scopeChooser.EditScopesDialog;
import consulo.ide.localize.IdeLocalize;
import consulo.language.editor.PsiCopyPasteManager;
import consulo.language.editor.hierarchy.*;
import consulo.language.editor.impl.internal.hierarchy.BaseOnThisElementAction;
import consulo.language.editor.impl.internal.hierarchy.ChangeViewTypeAction;
import consulo.language.editor.impl.internal.hierarchy.HierarchyBrowserManager;
import consulo.language.editor.impl.internal.hierarchy.scope.*;
import consulo.language.editor.internal.hierarchy.HierarchyBrowser;
import consulo.language.editor.localize.LanguageEditorLocalize;
import consulo.language.psi.*;
import consulo.language.psi.event.PsiTreeChangeAdapter;
import consulo.language.psi.event.PsiTreeChangeEvent;
import consulo.localHistory.LocalHistory;
import consulo.localHistory.LocalHistoryAction;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.navigation.Navigatable;
import consulo.navigation.OpenFileDescriptorFactory;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.ui.view.tree.ApplicationFileColorManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.DeleteProvider;
import consulo.ui.ex.OccurenceNavigator;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.EditSourceOnDoubleClickHandler;
import consulo.ui.ex.awt.JBLabel;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.PopupHandler;
import consulo.ui.ex.awt.ScrollPaneFactory;
import consulo.ui.ex.awt.action.ComboBoxAction;
import consulo.ui.ex.awt.dnd.*;
import consulo.ui.ex.awt.internal.laf.MultiLineLabelUI;
import consulo.ui.ex.awt.tree.Tree;
import consulo.ui.ex.awt.util.Alarm;
import consulo.ui.ex.awt.util.ScreenUtil;
import consulo.ui.ex.tree.AlphaComparator;
import consulo.ui.ex.tree.IndexComparator;
import consulo.ui.ex.tree.NodeDescriptor;
import consulo.util.lang.Couple;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ref.SimpleReference;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.status.FileStatusListener;
import consulo.virtualFileSystem.status.FileStatusManager;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.*;
import java.util.function.Function;

/**
 * The one hierarchy view the platform ships. Everything language-specific about it - which views it offers,
 * how each tree is built, which toggles sit on its toolbar - is read off the {@link HierarchyModel} it was
 * handed; this class owns only the widgets, the state and the actions around them.
 *
 * @author VISTALL
 * @since 2026-09-19
 */
public class DesktopAWTHierarchyBrowser extends DesktopAWTHierarchyBrowserBase implements HierarchyBrowser, OccurenceNavigator {
    private static final Logger LOG = Logger.getInstance(DesktopAWTHierarchyBrowser.class);

    private static final String HELP_ID = "reference.toolWindows.hierarchy";

    private final HierarchyModel<PsiElement> myModel;
    private final Map<String, HierarchyViewType> myViewTypes = new LinkedHashMap<>();

    private final Hashtable<String, HierarchyTreeBuilder> myBuilders = new Hashtable<>();
    private final Hashtable<String, JTree> myType2TreeMap = new Hashtable<>();
    private final Map<String, OccurenceNavigator> myOccurrenceNavigators = new HashMap<>();
    private final Map<String, HierarchyScope> myType2ScopeMap = new HashMap<>();
    private final Map<String, Boolean> myOptionStates = new HashMap<>();

    private final RefreshAction myRefreshAction = new RefreshAction();
    private final MyDeleteProvider myDeleteProvider = new MyDeleteProvider();
    private final Alarm myAlarm = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, this);

    private SmartPsiElementPointer mySmartPsiElementPointer;
    private final CardLayout myCardLayout;
    private final JPanel myTreePanel;
    private HierarchyViewType myCurrentViewType;

    private boolean myCachedIsValidBase;

    @SuppressWarnings("unchecked")
    public DesktopAWTHierarchyBrowser(Project project, HierarchyModel<?> model) {
        super(project);

        myModel = (HierarchyModel<PsiElement>)model;

        for (HierarchyViewType viewType : myModel.getViewTypes()) {
            myViewTypes.put(viewType.getId(), viewType);
        }

        setHierarchyBase(myModel.getTarget());

        myCardLayout = new CardLayout();
        myTreePanel = new JPanel(myCardLayout);

        createTrees();

        HierarchyBrowserManager.State state = HierarchyBrowserManager.getInstance(project).getState();
        assert state != null;
        for (String type : myType2TreeMap.keySet()) {
            myType2ScopeMap.put(type, HierarchyScopes.find(myProject, state.SCOPE));
        }
        for (HierarchyOption option : myModel.getOptions()) {
            Boolean persisted = state.OPTIONS.get(option.getId());
            myOptionStates.put(option.getId(), persisted != null ? persisted : option.getDefaultValue());
        }

        Enumeration<String> keys = myType2TreeMap.keys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            JTree tree = myType2TreeMap.get(key);
            myOccurrenceNavigators.put(key, new OccurenceNavigatorSupport(tree) {
                @Override
                protected @Nullable Navigatable createDescriptorForNode(DefaultMutableTreeNode node) {
                    HierarchyNodeDescriptor descriptor = getDescriptor(node);
                    if (descriptor == null) {
                        return null;
                    }
                    PsiElement psiElement = descriptor.getOpenFileElement();
                    if (psiElement == null || !psiElement.isValid()) {
                        return null;
                    }
                    VirtualFile virtualFile = psiElement.getContainingFile().getVirtualFile();
                    if (virtualFile == null) {
                        return null;
                    }
                    return OpenFileDescriptorFactory.getInstance(psiElement.getProject())
                        .newBuilder(virtualFile)
                        .offset(psiElement.getTextOffset())
                        .build();
                }

                @Override
                public String getNextOccurenceActionName() {
                    return myModel.getKind().getNextOccurrenceText().get();
                }

                @Override
                public String getPreviousOccurenceActionName() {
                    return myModel.getKind().getPreviousOccurrenceText().get();
                }
            });
            myTreePanel.add(ScrollPaneFactory.createScrollPane(tree), key);
        }

        JPanel legendPanel = createLegendPanel();
        JPanel contentPanel;
        if (legendPanel != null) {
            contentPanel = new JPanel(new BorderLayout());
            contentPanel.add(myTreePanel, BorderLayout.CENTER);
            contentPanel.add(legendPanel, BorderLayout.SOUTH);
        }
        else {
            contentPanel = myTreePanel;
        }
        buildUi(createToolbar(myModel.getKind().getActionPlace(), HELP_ID).getComponent(), contentPanel);

        PsiManager.getInstance(project).addPsiTreeChangeListener(new MyPsiTreeChangeListener(), this);
        FileStatusManager.getInstance(project).addFileStatusListener(new MyFileStatusListener(), this);
    }

    @Override
    public HierarchyModel<? extends PsiElement> getModel() {
        return myModel;
    }

    @Override
    public HierarchyKind getKind() {
        return myModel.getKind();
    }

    @Override
    public @Nullable HierarchyViewType getCurrentViewType() {
        return myCurrentViewType;
    }

    @Override
    public @Nullable PsiElement getHierarchyBase() {
        return mySmartPsiElementPointer.getElement();
    }

    @Override
    @RequiredReadAction
    public boolean isApplicableElement(PsiElement element) {
        return myModel.isApplicableElement(element);
    }

    @Override
    @RequiredReadAction
    public boolean canBeBase(PsiElement element) {
        return myModel.canBeBase(element);
    }

    @Override
    @RequiredReadAction
    public LocalizeValue getBaseOnThisText(PsiElement element) {
        return myModel.getBaseOnThisText(element);
    }

    @Override
    @RequiredReadAction
    public HierarchyViewType correctViewType(HierarchyViewType viewType) {
        return myModel.correctViewType(viewType);
    }

    @Override
    @RequiredReadAction
    public boolean isViewTypeEnabled(HierarchyViewType viewType) {
        return myViewTypes.containsKey(viewType.getId()) && myModel.isViewTypeEnabled(viewType) && isValidBase();
    }

    private void createTrees() {
        HierarchyKind kind = myModel.getKind();
        ActionGroup popupGroup = findPopupActionGroup();
        ShortcutSet shortcutSet = BaseOnThisElementAction.findShortcutSet(kind.getShortcutActionId());
        BaseOnThisElementAction baseOnThisAction = new BaseOnThisElementAction();

        for (HierarchyViewType viewType : myViewTypes.values()) {
            JTree tree = createTree(kind.isDragAndDropEnabled());
            if (popupGroup != null) {
                PopupHandler.installPopupHandler(tree, popupGroup, kind.getPopupPlace(), ActionManager.getInstance());
            }
            if (shortcutSet != null) {
                baseOnThisAction.registerCustomShortcutSet(shortcutSet, tree);
            }
            myType2TreeMap.put(viewType.getId(), tree);
        }
    }

    private @Nullable ActionGroup findPopupActionGroup() {
        String groupId = myModel.getPopupActionGroupId();
        if (StringUtil.isEmpty(groupId)) {
            return null;
        }
        AnAction action = ActionManager.getInstance().getAction(groupId);
        if (action instanceof ActionGroup group) {
            return group;
        }
        LOG.warn("Hierarchy popup action group is not registered: " + groupId);
        return null;
    }

    private @Nullable JPanel createLegendPanel() {
        List<HierarchyLegendEntry> legend = myModel.getLegend();
        if (legend.isEmpty()) {
            return null;
        }

        JPanel panel = new JPanel(new GridBagLayout());

        GridBagConstraints gc = new GridBagConstraints(
            0,
            0,
            1,
            1,
            1,
            0,
            GridBagConstraints.WEST,
            GridBagConstraints.HORIZONTAL,
            JBUI.insets(3, 5, 0, 5),
            0,
            0
        );

        for (HierarchyLegendEntry entry : legend) {
            JLabel label = new JBLabel(entry.text().get(), entry.icon(), SwingConstants.LEFT);
            label.setUI(new MultiLineLabelUI());
            label.setIconTextGap(10);
            panel.add(label, gc);
            gc.gridy++;
        }

        return panel;
    }

    private void invalidateBuilders() {
        for (HierarchyTreeBuilder builder : new ArrayList<>(myBuilders.values())) {
            builder.invalidate();
        }
    }

    private final class MyPsiTreeChangeListener extends PsiTreeChangeAdapter {
        @Override
        public void childAdded(PsiTreeChangeEvent event) {
            invalidateBuilders();
        }

        @Override
        public void childRemoved(PsiTreeChangeEvent event) {
            invalidateBuilders();
        }

        @Override
        public void childReplaced(PsiTreeChangeEvent event) {
            invalidateBuilders();
        }

        @Override
        public void childMoved(PsiTreeChangeEvent event) {
            invalidateBuilders();
        }

        @Override
        public void childrenChanged(PsiTreeChangeEvent event) {
            invalidateBuilders();
        }

        @Override
        public void propertyChanged(PsiTreeChangeEvent event) {
            invalidateBuilders();
        }
    }

    private final class MyFileStatusListener implements FileStatusListener {
        @Override
        public void fileStatusesChanged() {
            invalidateBuilders();
        }

        @Override
        public void fileStatusChanged(VirtualFile virtualFile) {
            invalidateBuilders();
        }
    }

    private JTree createTree(boolean dndAware) {
        ApplicationFileColorManager applicationFileColorManager = ApplicationFileColorManager.getInstance();
        Tree tree;
        Function<Object, PsiElement> toPsiConverter = o -> {
            if (o instanceof HierarchyNodeDescriptor hierarchyNodeDescriptor) {
                return hierarchyNodeDescriptor.getContainingFile();
            }
            return null;
        };

        if (dndAware) {
            tree = new DnDAwareTree(new DefaultTreeModel(new DefaultMutableTreeNode(""))) {
                @Override
                public void removeNotify() {
                    super.removeNotify();
                    if (ScreenUtil.isStandardAddRemoveNotify(this)) {
                        myRefreshAction.unregisterCustomShortcutSet(this);
                    }
                }

                @Override
                public boolean isFileColorsEnabled() {
                    return ProjectViewTree.isFileColorsEnabledFor(applicationFileColorManager, this);
                }

                @Override
                public ColorValue getFileColorFor(Object object) {
                    return ProjectViewTree.getColorForElement(toPsiConverter.apply(object));
                }
            };

            if (!myProject.getApplication().isHeadlessEnvironment()) {
                DnDManager.getInstance().registerSource(
                    new DnDSource() {
                        @Override
                        public boolean canStartDragging(DnDAction action, Point dragOrigin) {
                            return getSelectedElements().length > 0;
                        }

                        @Override
                        public DnDDragStartBean startDragging(DnDAction action, Point dragOrigin) {
                            return new DnDDragStartBean(new TransferableWrapper() {
                                @Override
                                public TreeNode[] getTreeNodes() {
                                    return tree.getSelectedNodes(TreeNode.class, null);
                                }

                                @Override
                                public PsiElement[] getPsiElements() {
                                    return getSelectedElements();
                                }

                                @Override
                                public List<File> asFileList() {
                                    return PsiCopyPasteManager.asFileList(getPsiElements());
                                }
                            });
                        }

                        @Override
                        public Pair<Image, Point> createDraggedImage(DnDAction action, Point dragOrigin, DnDDragStartBean bean) {
                            return null;
                        }

                        @Override
                        public void dragDropEnd() {
                        }

                        @Override
                        public void dropActionChanged(int gestureModifiers) {
                        }
                    },
                    tree
                );
            }
        }
        else {
            tree = new Tree(new DefaultTreeModel(new DefaultMutableTreeNode(""))) {
                @Override
                public void removeNotify() {
                    super.removeNotify();
                    if (ScreenUtil.isStandardAddRemoveNotify(this)) {
                        myRefreshAction.unregisterCustomShortcutSet(this);
                    }
                }

                @Override
                public boolean isFileColorsEnabled() {
                    return ProjectViewTree.isFileColorsEnabledFor(applicationFileColorManager, this);
                }

                @Override
                public ColorValue getFileColorFor(Object object) {
                    return ProjectViewTree.getColorForElement(toPsiConverter.apply(object));
                }
            };
        }
        configureTree(tree);
        EditSourceOnDoubleClickHandler.install(tree);
        myRefreshAction.registerShortcutOn(tree);

        return tree;
    }

    private void setHierarchyBase(PsiElement element) {
        mySmartPsiElementPointer = SmartPointerManager.getInstance(myProject).createSmartPsiElementPointer(element);
    }

    private void restoreCursor() {
        myAlarm.cancelAllRequests();
        setCursor(Cursor.getDefaultCursor());
    }

    private void setWaitCursor() {
        myAlarm.addRequest(() -> setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)), 100);
    }

    @Override
    @RequiredReadAction
    public void changeToDefaultView(boolean requestFocus) {
        changeView(myModel.correctViewType(myModel.getDefaultViewType()), requestFocus);
    }

    @Override
    @RequiredReadAction
    public void changeViewType(HierarchyViewType viewType) {
        changeView(viewType, true);
    }

    @RequiredReadAction
    public final void changeView(HierarchyViewType viewType, boolean requestFocus) {
        HierarchyViewType effectiveViewType = myViewTypes.containsKey(viewType.getId()) ? viewType : myModel.getDefaultViewType();
        myCurrentViewType = effectiveViewType;
        String typeId = effectiveViewType.getId();

        PsiElement element = mySmartPsiElementPointer.getElement();
        if (element == null || !myModel.isApplicableElement(element)) {
            return;
        }

        if (myContent != null) {
            LocalizeValue displayName = myModel.getContentDisplayName(effectiveViewType, element);
            if (displayName.isNotEmpty()) {
                myContent.setDisplayName(displayName.get());
            }
        }

        myCardLayout.show(myTreePanel, typeId);

        if (!myBuilders.containsKey(typeId)) {
            try {
                setWaitCursor();
                // create builder
                JTree tree = myType2TreeMap.get(typeId);
                DefaultTreeModel model = new DefaultTreeModel(new DefaultMutableTreeNode(""));
                tree.setModel(model);

                PsiDocumentManager.getInstance(myProject).commitAllDocuments();
                HierarchyTreeStructure structure = createHierarchyTreeStructure(effectiveViewType, element);
                if (structure == null) {
                    return;
                }
                Comparator<NodeDescriptor> comparator = getComparator();
                HierarchyTreeBuilder builder = new HierarchyTreeBuilder(tree, model, structure, comparator);

                myBuilders.put(typeId, builder);
                Disposer.register(this, builder);
                Disposer.register(builder, () -> myBuilders.remove(typeId));

                HierarchyNodeDescriptor descriptor = structure.getBaseDescriptor();
                builder.select(descriptor, () -> builder.expand(descriptor, null));
            }
            finally {
                restoreCursor();
            }
        }

        if (requestFocus) {
            IdeFocusManager.getGlobalInstance()
                .doWhenFocusSettlesDown(() -> IdeFocusManager.getGlobalInstance().requestFocus(getCurrentTree(), true));
        }
    }

    @RequiredReadAction
    private @Nullable HierarchyTreeStructure createHierarchyTreeStructure(HierarchyViewType viewType, PsiElement element) {
        HierarchyScope scope = myModel.isScopeSupported() ? myType2ScopeMap.get(viewType.getId()) : null;
        return myModel.createTreeStructure(new RequestImpl(myProject, element, viewType, scope, new HashMap<>(myOptionStates)));
    }

    private Comparator<NodeDescriptor> getComparator() {
        HierarchyBrowserManager.State state = HierarchyBrowserManager.getInstance(myProject).getState();
        assert state != null;
        return state.SORT_ALPHABETICALLY ? AlphaComparator.INSTANCE : IndexComparator.INSTANCE;
    }

    @Override
    protected void appendActions(DefaultActionGroup actionGroup, String helpID) {
        if (myViewTypes.size() > 1) {
            for (HierarchyViewType viewType : myViewTypes.values()) {
                actionGroup.add(new ChangeViewTypeAction(viewType));
            }
        }
        actionGroup.add(new AlphaSortAction());
        for (HierarchyOption option : myModel.getOptions()) {
            actionGroup.add(new ToggleOptionAction(option));
        }
        if (myModel.isScopeSupported()) {
            actionGroup.add(new ChangeScopeAction());
        }
        actionGroup.add(myRefreshAction);
        super.appendActions(actionGroup, helpID);
    }

    @Override
    public boolean hasNextOccurence() {
        return getOccurrenceNavigator().hasNextOccurence();
    }

    private OccurenceNavigator getOccurrenceNavigator() {
        if (myCurrentViewType == null) {
            return OccurenceNavigator.EMPTY;
        }
        OccurenceNavigator navigator = myOccurrenceNavigators.get(myCurrentViewType.getId());
        return navigator != null ? navigator : OccurenceNavigator.EMPTY;
    }

    @Override
    public boolean hasPreviousOccurence() {
        return getOccurrenceNavigator().hasPreviousOccurence();
    }

    @Override
    public OccurenceInfo goNextOccurence() {
        return getOccurrenceNavigator().goNextOccurence();
    }

    @Override
    public OccurenceInfo goPreviousOccurence() {
        return getOccurrenceNavigator().goPreviousOccurence();
    }

    @Override
    public String getNextOccurenceActionName() {
        return getOccurrenceNavigator().getNextOccurenceActionName();
    }

    @Override
    public String getPreviousOccurenceActionName() {
        return getOccurrenceNavigator().getPreviousOccurenceActionName();
    }

    @Override
    protected HierarchyTreeBuilder getCurrentBuilder() {
        return myCurrentViewType != null ? myBuilders.get(myCurrentViewType.getId()) : null;
    }

    @RequiredReadAction
    private boolean isValidBase() {
        if (PsiDocumentManager.getInstance(myProject).getUncommittedDocuments().length > 0) {
            return myCachedIsValidBase;
        }

        PsiElement element = mySmartPsiElementPointer.getElement();
        myCachedIsValidBase = element != null && myModel.isApplicableElement(element) && element.isValid();
        return myCachedIsValidBase;
    }

    @Override
    protected JTree getCurrentTree() {
        if (myCurrentViewType == null) {
            return null;
        }
        return myType2TreeMap.get(myCurrentViewType.getId());
    }

    @Override
    public void uiDataSnapshot(DataSink sink) {
        super.uiDataSnapshot(sink);
        sink.set(HierarchyBrowser.KEY, this);
        sink.set(HelpManager.HELP_ID, HELP_ID);
        sink.set(DeleteProvider.KEY, myDeleteProvider);
        myModel.uiDataSnapshot(sink, List.of(getSelectedDescriptors()));
    }

    private void disposeBuilders() {
        Collection<HierarchyTreeBuilder> builders = new ArrayList<>(myBuilders.values());
        for (HierarchyTreeBuilder builder : builders) {
            Disposer.dispose(builder);
        }
        myBuilders.clear();
    }

    @RequiredReadAction
    private void doRefresh(boolean currentBuilderOnly) {
        if (currentBuilderOnly) {
            LOG.assertTrue(myCurrentViewType != null);
        }

        if (!isValidBase()) {
            return;
        }

        if (getCurrentBuilder() == null) {
            return; // seems like we are in the middle of refresh already
        }

        SimpleReference<Couple<List<Object>>> storedInfo = new SimpleReference<>();
        if (myCurrentViewType != null) {
            HierarchyTreeBuilder builder = getCurrentBuilder();
            storedInfo.set(builder.storeExpandedAndSelectedInfo());
        }

        PsiElement element = mySmartPsiElementPointer.getElement();
        if (element == null || !myModel.isApplicableElement(element)) {
            return;
        }
        HierarchyViewType currentViewType = myCurrentViewType;

        if (currentBuilderOnly) {
            Disposer.dispose(getCurrentBuilder());
        }
        else {
            disposeBuilders();
        }
        setHierarchyBase(element);
        validate();
        myProject.getApplication().invokeLater(() -> {
            changeViewType(currentViewType);
            HierarchyTreeBuilder builder = getCurrentBuilder();
            builder.restoreExpandedAndSelectedInfo(storedInfo.get());
        });
    }

    private LocalizeValue getCurrentScopeName() {
        if (myCurrentViewType == null) {
            return LocalizeValue.empty();
        }
        HierarchyScope scope = myType2ScopeMap.get(myCurrentViewType.getId());
        return scope != null ? scope.getPresentableName() : LocalizeValue.empty();
    }

    private boolean isOptionEnabled(HierarchyOption option) {
        Boolean value = myOptionStates.get(option.getId());
        return value != null ? value : option.getDefaultValue();
    }

    private void setOptionEnabled(HierarchyOption option, boolean value) {
        myOptionStates.put(option.getId(), value);
        HierarchyBrowserManager.State state = HierarchyBrowserManager.getInstance(myProject).getState();
        assert state != null;
        state.OPTIONS.put(option.getId(), value);
    }

    private static final class RequestImpl implements HierarchyRequest<PsiElement> {
        private final Project myProject;
        private final PsiElement myTarget;
        private final HierarchyViewType myViewType;
        private final @Nullable HierarchyScope myScope;
        private final Map<String, Boolean> myOptionStates;

        private RequestImpl(
            Project project,
            PsiElement target,
            HierarchyViewType viewType,
            @Nullable HierarchyScope scope,
            Map<String, Boolean> optionStates
        ) {
            myProject = project;
            myTarget = target;
            myViewType = viewType;
            myScope = scope;
            myOptionStates = optionStates;
        }

        @Override
        public Project getProject() {
            return myProject;
        }

        @Override
        public PsiElement getTarget() {
            return myTarget;
        }

        @Override
        public HierarchyViewType getViewType() {
            return myViewType;
        }

        @Override
        public @Nullable HierarchyScope getScope() {
            return myScope;
        }

        @Override
        public boolean isEnabled(HierarchyOption option) {
            Boolean value = myOptionStates.get(option.getId());
            return value != null ? value : option.getDefaultValue();
        }
    }

    private final class MyDeleteProvider implements DeleteProvider {
        @Override
        @RequiredUIAccess
        public void deleteElement(DataContext dataContext) {
            HierarchyNodeDescriptor descriptor = getDeletableDescriptor();
            if (descriptor == null) {
                return;
            }
            PsiElement element = descriptor.getHierarchyElement();
            if (element == null) {
                return;
            }
            LocalHistoryAction action =
                LocalHistory.getInstance().startAction(IdeLocalize.progressDeletingClass(descriptor.getQualifiedName()));
            try {
                DeleteHandler.deletePsiElement(new PsiElement[]{element}, myProject);
            }
            finally {
                action.finish();
            }
        }

        @Override
        public boolean canDeleteElement(DataContext dataContext) {
            HierarchyNodeDescriptor descriptor = getDeletableDescriptor();
            PsiElement element = descriptor != null ? descriptor.getHierarchyElement() : null;
            return element != null && DeleteHandler.shouldEnableDeleteAction(new PsiElement[]{element});
        }

        private @Nullable HierarchyNodeDescriptor getDeletableDescriptor() {
            HierarchyNodeDescriptor descriptor = getSelectedDescriptor();
            return descriptor != null && descriptor.canBeDeleted() ? descriptor : null;
        }
    }

    private final class AlphaSortAction extends ToggleAction {
        private AlphaSortAction() {
            super(
                LanguageEditorLocalize.actionSortAlphabetically(),
                LanguageEditorLocalize.actionSortAlphabetically(),
                PlatformIconGroup.objectbrowserSorted()
            );
        }

        @Override
        public final boolean isSelected(AnActionEvent event) {
            HierarchyBrowserManager.State state = HierarchyBrowserManager.getInstance(myProject).getState();
            assert state != null;
            return state.SORT_ALPHABETICALLY;
        }

        @Override
        @RequiredUIAccess
        public final void setSelected(AnActionEvent event, boolean flag) {
            HierarchyBrowserManager.State state = HierarchyBrowserManager.getInstance(myProject).getState();
            assert state != null;
            state.SORT_ALPHABETICALLY = flag;
            Comparator<NodeDescriptor> comparator = getComparator();
            Collection<HierarchyTreeBuilder> builders = myBuilders.values();
            for (HierarchyTreeBuilder builder : builders) {
                builder.setNodeDescriptorComparator(comparator);
            }
        }

        @Override
        public final void update(AnActionEvent event) {
            super.update(event);
            Presentation presentation = event.getPresentation();
            presentation.setEnabled(ReadAction.compute(DesktopAWTHierarchyBrowser.this::isValidBase));
        }
    }

    private final class ToggleOptionAction extends ToggleAction {
        private final HierarchyOption myOption;

        private ToggleOptionAction(HierarchyOption option) {
            super(option.getText(), LocalizeValue.empty(), option.getIcon());
            myOption = option;
        }

        @Override
        public boolean isSelected(AnActionEvent event) {
            return isOptionEnabled(myOption);
        }

        @Override
        @RequiredUIAccess
        public void setSelected(AnActionEvent event, boolean flag) {
            setOptionEnabled(myOption, flag);

            // invokeLater is called to update state of button before long tree building operation
            Application.get().invokeLater(() -> doRefresh(true));
        }

        @Override
        public void update(AnActionEvent event) {
            super.update(event);
            event.getPresentation().setEnabled(ReadAction.compute(DesktopAWTHierarchyBrowser.this::isValidBase));
        }
    }

    private final class RefreshAction extends consulo.ui.ex.action.RefreshAction {
        private RefreshAction() {
            super(LanguageEditorLocalize.actionRefresh(), LanguageEditorLocalize.actionRefresh(), PlatformIconGroup.actionsRefresh());
        }

        @Override
        @RequiredUIAccess
        public final void actionPerformed(AnActionEvent e) {
            doRefresh(false);
        }

        @Override
        public final void update(AnActionEvent event) {
            event.getPresentation().setEnabled(ReadAction.compute(DesktopAWTHierarchyBrowser.this::isValidBase));
        }
    }

    private final class ChangeScopeAction extends ComboBoxAction {
        @Override
        public final void update(AnActionEvent e) {
            Presentation presentation = e.getPresentation();
            Project project = e.getData(Project.KEY);
            if (project == null) {
                return;
            }
            presentation.setEnabled(myCurrentViewType != null && myModel.isScopeApplicable(myCurrentViewType));
            presentation.setText(getCurrentScopeName());
        }

        @Override
        public final DefaultActionGroup createPopupActionGroup(JComponent component) {
            DefaultActionGroup group = new DefaultActionGroup();

            for (HierarchyScope scope : getValidScopes()) {
                group.add(new MenuAction(scope));
            }

            group.add(new ConfigureScopesAction());

            return group;
        }

        private Collection<HierarchyScope> getValidScopes() {
            List<HierarchyScope> result = new ArrayList<>();
            result.add(ProductionHierarchyScope.INSTANCE);
            result.add(TestHierarchyScope.INSTANCE);
            result.add(AllHierarchyScope.INSTANCE);
            result.add(ThisClassHierarchyScope.INSTANCE);

            NamedScopesHolder[] holders = NamedScopesHolder.getAllNamedScopeHolders(myProject);
            for (NamedScopesHolder holder : holders) {
                NamedScope[] scopes = holder.getEditableScopes(); //predefined scopes already included
                for (NamedScope scope : scopes) {
                    result.add(new NamedHierarchyScope(scope));
                }
            }
            return result;
        }

        private void selectScope(HierarchyScope scope) {
            if (myCurrentViewType == null) {
                return;
            }
            myType2ScopeMap.put(myCurrentViewType.getId(), scope);
            HierarchyBrowserManager.State state = HierarchyBrowserManager.getInstance(myProject).getState();
            assert state != null;
            state.SCOPE = scope.getId();

            // invokeLater is called to update state of button before long tree building operation
            myProject.getApplication().invokeLater(() -> {
                doRefresh(true); // scope is kept per type so other builders doesn't need to be refreshed
            });
        }

        @Override
        public final JComponent createCustomComponent(Presentation presentation, String place) {
            JPanel panel = new JPanel(new GridBagLayout());
            panel.add(
                new JLabel(LanguageEditorLocalize.labelScope().get()),
                new GridBagConstraints(
                    0, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.BOTH,
                    JBUI.insetsLeft(5), 0, 0
                )
            );
            panel.add(
                super.createCustomComponent(presentation, place),
                new GridBagConstraints(
                    1, 0, 1, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH,
                    JBUI.emptyInsets(), 0, 0
                )
            );
            return panel;
        }

        private final class MenuAction extends AnAction {
            private final HierarchyScope myScope;

            private MenuAction(HierarchyScope scope) {
                super(scope.getPresentableName());
                myScope = scope;
            }

            @Override
            @RequiredUIAccess
            public final void actionPerformed(AnActionEvent e) {
                selectScope(myScope);
            }
        }

        private final class ConfigureScopesAction extends AnAction {
            private ConfigureScopesAction() {
                super(LocalizeValue.localizeTODO("Configure..."));
            }

            @Override
            @RequiredUIAccess
            public void actionPerformed(AnActionEvent e) {
                EditScopesDialog.showDialog(myProject, null);
                if (myCurrentViewType != null && !getValidScopes().contains(myType2ScopeMap.get(myCurrentViewType.getId()))) {
                    selectScope(AllHierarchyScope.INSTANCE);
                }
            }
        }
    }
}
