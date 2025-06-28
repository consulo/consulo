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

package consulo.ide.impl.idea.ide.hierarchy;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.HelpManager;
import consulo.application.ReadAction;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.content.scope.NamedScope;
import consulo.content.scope.NamedScopesHolder;
import consulo.dataContext.DataContext;
import consulo.disposer.Disposer;
import consulo.fileEditor.impl.internal.OpenFileDescriptorImpl;
import consulo.ide.impl.idea.ide.OccurenceNavigatorSupport;
import consulo.ide.impl.idea.ide.dnd.TransferableWrapper;
import consulo.ide.impl.idea.ide.hierarchy.actions.BrowseHierarchyActionBase;
import consulo.ide.impl.idea.ide.hierarchy.scope.*;
import consulo.ide.impl.idea.ide.projectView.impl.ProjectViewTree;
import consulo.ide.impl.idea.ide.util.scopeChooser.EditScopesDialog;
import consulo.ide.localize.IdeLocalize;
import consulo.language.editor.PsiCopyPasteManager;
import consulo.language.editor.hierarchy.HierarchyProvider;
import consulo.language.psi.*;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.navigation.Navigatable;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.ui.view.tree.ApplicationFileColorManager;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.OccurenceNavigator;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.EditSourceOnDoubleClickHandler;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.ScrollPaneFactory;
import consulo.ui.ex.awt.action.ComboBoxAction;
import consulo.ui.ex.awt.dnd.*;
import consulo.ui.ex.awt.tree.Tree;
import consulo.ui.ex.awt.util.Alarm;
import consulo.ui.ex.awt.util.ScreenUtil;
import consulo.ui.ex.tree.NodeDescriptor;
import consulo.util.dataholder.Key;
import consulo.util.lang.Couple;
import consulo.util.lang.Pair;
import consulo.util.lang.ref.SimpleReference;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import java.awt.*;
import java.io.File;
import java.text.MessageFormat;
import java.util.List;
import java.util.*;
import java.util.function.Function;

public abstract class HierarchyBrowserBaseEx extends HierarchyBrowserBase implements OccurenceNavigator {
    private static final Logger LOG = Logger.getInstance(HierarchyBrowserBaseEx.class);

    private static final String HELP_ID = "reference.toolWindows.hierarchy";

    protected final Hashtable<String, HierarchyTreeBuilder> myBuilders = new Hashtable<>();
    private final Hashtable<String, JTree> myType2TreeMap = new Hashtable<>();

    private final RefreshAction myRefreshAction = new RefreshAction();
    private final Alarm myAlarm = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, this);
    private SmartPsiElementPointer mySmartPsiElementPointer;
    private final CardLayout myCardLayout;
    private final JPanel myTreePanel;
    protected String myCurrentViewType;

    private boolean myCachedIsValidBase;

    private final Map<String, OccurenceNavigator> myOccurrenceNavigators = new HashMap<>();

    private static final OccurenceNavigator EMPTY_NAVIGATOR = new OccurenceNavigator() {
        @Override
        public boolean hasNextOccurence() {
            return false;
        }

        @Override
        public boolean hasPreviousOccurence() {
            return false;
        }

        @Override
        public OccurenceInfo goNextOccurence() {
            return null;
        }

        @Override
        public OccurenceInfo goPreviousOccurence() {
            return null;
        }

        @Override
        public String getNextOccurenceActionName() {
            return "";
        }

        @Override
        public String getPreviousOccurenceActionName() {
            return "";
        }
    };
    private final Map<String, HierarchyScope> myType2ScopeMap = new HashMap<>();

    public HierarchyBrowserBaseEx(@Nonnull Project project, @Nonnull PsiElement element) {
        super(project);

        setHierarchyBase(element);

        myCardLayout = new CardLayout();
        myTreePanel = new JPanel(myCardLayout);

        createTrees(myType2TreeMap);

        HierarchyBrowserManager.State state = HierarchyBrowserManager.getInstance(project).getState();
        assert state != null;
        for (String type : myType2TreeMap.keySet()) {
            myType2ScopeMap.put(type, HierarchyScope.find(myProject, state.SCOPE));
        }

        Enumeration<String> keys = myType2TreeMap.keys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            JTree tree = myType2TreeMap.get(key);
            myOccurrenceNavigators.put(key, new OccurenceNavigatorSupport(tree) {
                @Override
                @Nullable
                protected Navigatable createDescriptorForNode(@Nonnull DefaultMutableTreeNode node) {
                    HierarchyNodeDescriptor descriptor = getDescriptor(node);
                    if (descriptor == null) {
                        return null;
                    }
                    PsiElement psiElement = getOpenFileElementFromDescriptor(descriptor);
                    if (psiElement == null || !psiElement.isValid()) {
                        return null;
                    }
                    VirtualFile virtualFile = psiElement.getContainingFile().getVirtualFile();
                    if (virtualFile == null) {
                        return null;
                    }
                    return new OpenFileDescriptorImpl(psiElement.getProject(), virtualFile, psiElement.getTextOffset());
                }

                @Override
                public String getNextOccurenceActionName() {
                    return getNextOccurenceActionNameImpl();
                }

                @Override
                public String getPreviousOccurenceActionName() {
                    return getPrevOccurenceActionNameImpl();
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
        buildUi(createToolbar(getActionPlace(), HELP_ID).getComponent(), contentPanel);
    }

    @Nullable
    protected PsiElement getOpenFileElementFromDescriptor(@Nonnull HierarchyNodeDescriptor descriptor) {
        return getElementFromDescriptor(descriptor);
    }

    @Override
    @Nullable
    protected abstract PsiElement getElementFromDescriptor(@Nonnull HierarchyNodeDescriptor descriptor);

    @Nonnull
    protected abstract String getPrevOccurenceActionNameImpl();

    @Nonnull
    protected abstract String getNextOccurenceActionNameImpl();

    protected abstract void createTrees(@Nonnull Map<String, JTree> trees);

    @Nullable
    protected abstract JPanel createLegendPanel();

    protected abstract boolean isApplicableElement(@Nonnull PsiElement element);

    @Nullable
    protected abstract HierarchyTreeStructure createHierarchyTreeStructure(@Nonnull String type, @Nonnull PsiElement psiElement);

    @Nullable
    protected abstract Comparator<NodeDescriptor> getComparator();

    @Nonnull
    protected abstract String getActionPlace();

    @Nonnull
    protected abstract Key<?> getBrowserDataKey();

    protected final JTree createTree(boolean dndAware) {
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
                        public Pair<Image, Point> createDraggedImage(DnDAction action, Point dragOrigin, @Nonnull DnDDragStartBean bean) {
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

    protected void setHierarchyBase(@Nonnull PsiElement element) {
        mySmartPsiElementPointer = SmartPointerManager.getInstance(myProject).createSmartPsiElementPointer(element);
    }

    private void restoreCursor() {
        myAlarm.cancelAllRequests();
        setCursor(Cursor.getDefaultCursor());
    }

    private void setWaitCursor() {
        myAlarm.addRequest(() -> setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)), 100);
    }

    @RequiredReadAction
    public void changeView(@Nonnull String typeName) {
        changeView(typeName, true);
    }

    @RequiredReadAction
    public final void changeView(@Nonnull String typeName, boolean requestFocus) {
        myCurrentViewType = typeName;

        PsiElement element = mySmartPsiElementPointer.getElement();
        if (element == null || !isApplicableElement(element)) {
            return;
        }

        if (myContent != null) {
            String displayName = getContentDisplayName(typeName, element);
            if (displayName != null) {
                myContent.setDisplayName(displayName);
            }
        }

        myCardLayout.show(myTreePanel, typeName);

        if (!myBuilders.containsKey(typeName)) {
            try {
                setWaitCursor();
                // create builder
                JTree tree = myType2TreeMap.get(typeName);
                DefaultTreeModel model = new DefaultTreeModel(new DefaultMutableTreeNode(""));
                tree.setModel(model);

                PsiDocumentManager.getInstance(myProject).commitAllDocuments();
                HierarchyTreeStructure structure = createHierarchyTreeStructure(typeName, element);
                if (structure == null) {
                    return;
                }
                Comparator<NodeDescriptor> comparator = getComparator();
                HierarchyTreeBuilder builder = new HierarchyTreeBuilder(myProject, tree, model, structure, comparator);

                myBuilders.put(typeName, builder);
                Disposer.register(this, builder);
                Disposer.register(builder, () -> myBuilders.remove(typeName));

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
    @Nullable
    protected String getContentDisplayName(@Nonnull String typeName, @Nonnull PsiElement element) {
        if (element instanceof PsiNamedElement namedElement) {
            return MessageFormat.format(typeName, namedElement.getName());
        }
        return null;
    }

    @Override
    protected void appendActions(@Nonnull DefaultActionGroup actionGroup, String helpID) {
        prependActions(actionGroup);
        actionGroup.add(myRefreshAction);
        super.appendActions(actionGroup, helpID);
    }

    protected void prependActions(DefaultActionGroup actionGroup) {
    }

    @Override
    public boolean hasNextOccurence() {
        return getOccurrenceNavigator().hasNextOccurence();
    }

    private OccurenceNavigator getOccurrenceNavigator() {
        if (myCurrentViewType == null) {
            return EMPTY_NAVIGATOR;
        }
        OccurenceNavigator navigator = myOccurrenceNavigators.get(myCurrentViewType);
        return navigator != null ? navigator : EMPTY_NAVIGATOR;
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
        return myBuilders.get(myCurrentViewType);
    }

    @RequiredReadAction
    boolean isValidBase() {
        if (PsiDocumentManager.getInstance(myProject).getUncommittedDocuments().length > 0) {
            return myCachedIsValidBase;
        }

        PsiElement element = mySmartPsiElementPointer.getElement();
        myCachedIsValidBase = element != null && isApplicableElement(element) && element.isValid();
        return myCachedIsValidBase;
    }

    @Override
    protected JTree getCurrentTree() {
        if (myCurrentViewType == null) {
            return null;
        }
        return myType2TreeMap.get(myCurrentViewType);
    }

    String getCurrentViewType() {
        return myCurrentViewType;
    }

    @Override
    public Object getData(@Nonnull Key<?> dataId) {
        if (getBrowserDataKey() == dataId) {
            return this;
        }
        if (HelpManager.HELP_ID == dataId) {
            return HELP_ID;
        }
        return super.getData(dataId);
    }

    private void disposeBuilders() {
        Collection<HierarchyTreeBuilder> builders = new ArrayList<>(myBuilders.values());
        for (HierarchyTreeBuilder builder : builders) {
            Disposer.dispose(builder);
        }
        myBuilders.clear();
    }

    @RequiredReadAction
    void doRefresh(boolean currentBuilderOnly) {
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
        if (element == null || !isApplicableElement(element)) {
            return;
        }
        String currentViewType = myCurrentViewType;

        if (currentBuilderOnly) {
            Disposer.dispose(getCurrentBuilder());
        }
        else {
            disposeBuilders();
        }
        setHierarchyBase(element);
        validate();
        myProject.getApplication().invokeLater(() -> {
            changeView(currentViewType);
            HierarchyTreeBuilder builder = getCurrentBuilder();
            builder.restoreExpandedAndSelectedInfo(storedInfo.get());
        });
    }

    @Nonnull
    protected LocalizeValue getCurrentScopeName() {
        if (myCurrentViewType == null) {
            return LocalizeValue.empty();
        }
        HierarchyScope scope = myType2ScopeMap.get(myCurrentViewType);
        return scope != null ? scope.getPresentableName() : LocalizeValue.empty();
    }

    @Deprecated
    protected String getCurrentScopeType() {
        if (myCurrentViewType == null) {
            return null;
        }
        HierarchyScope scope = myType2ScopeMap.get(myCurrentViewType);
        return scope != null ? scope.getId() : null;
    }

    protected class AlphaSortAction extends ToggleAction {
        public AlphaSortAction() {
            super(IdeLocalize.actionSortAlphabetically(), IdeLocalize.actionSortAlphabetically(), PlatformIconGroup.objectbrowserSorted());
        }

        @Override
        public final boolean isSelected(@Nonnull AnActionEvent event) {
            HierarchyBrowserManager.State state = HierarchyBrowserManager.getInstance(myProject).getState();
            assert state != null;
            return state.SORT_ALPHABETICALLY;
        }

        @Override
        public final void setSelected(@Nonnull AnActionEvent event, boolean flag) {
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
        public final void update(@Nonnull AnActionEvent event) {
            super.update(event);
            Presentation presentation = event.getPresentation();
            presentation.setEnabled(ReadAction.compute(HierarchyBrowserBaseEx.this::isValidBase));
        }
    }

    static class BaseOnThisElementAction<H extends HierarchyProvider> extends AnAction {
        private final String myActionId;
        private final Key<?> myBrowserDataKey;
        @Nonnull
        private final Class<H> myHierarchyProviderClass;

        BaseOnThisElementAction(
            @Nonnull LocalizeValue text,
            @Nonnull String actionId,
            @Nonnull Key<?> browserDataKey,
            @Nonnull Class<H> hierarchyProviderClass
        ) {
            super(text);
            myActionId = actionId;
            myBrowserDataKey = browserDataKey;
            myHierarchyProviderClass = hierarchyProviderClass;
        }

        @Override
        @RequiredUIAccess
        public final void actionPerformed(@Nonnull AnActionEvent event) {
            DataContext dataContext = event.getDataContext();
            HierarchyBrowserBaseEx browser = (HierarchyBrowserBaseEx) dataContext.getData(myBrowserDataKey);
            if (browser == null) {
                return;
            }

            PsiElement selectedElement = browser.getSelectedElement();
            if (selectedElement == null || !browser.isApplicableElement(selectedElement)) {
                return;
            }

            String currentViewType = browser.myCurrentViewType;
            Disposer.dispose(browser);
            H provider = BrowseHierarchyActionBase.findProvider(
                myHierarchyProviderClass,
                selectedElement,
                selectedElement.getContainingFile(),
                event.getDataContext()
            );

            UIAccess uiAccess = UIAccess.current();

            BrowseHierarchyActionBase.createAndAddToPanel(selectedElement.getProject(), provider, selectedElement, b -> {
                uiAccess.give(() -> ((HierarchyBrowserBaseEx) b).changeView(correctViewType(browser, currentViewType)));
            });
        }

        protected String correctViewType(HierarchyBrowserBaseEx browser, String viewType) {
            return viewType;
        }

        @Override
        @RequiredUIAccess
        public final void update(@Nonnull AnActionEvent event) {
            Presentation presentation = event.getPresentation();

            registerCustomShortcutSet(ActionManager.getInstance().getAction(myActionId).getShortcutSet(), null);

            DataContext dataContext = event.getDataContext();
            HierarchyBrowserBaseEx browser = (HierarchyBrowserBaseEx) dataContext.getData(myBrowserDataKey);
            if (browser == null) {
                presentation.setVisible(false);
                presentation.setEnabled(false);
                return;
            }

            presentation.setVisible(true);

            PsiElement selectedElement = browser.getSelectedElement();
            if (selectedElement == null || !browser.isApplicableElement(selectedElement)) {
                presentation.setEnabled(false);
                presentation.setVisible(false);
                return;
            }

            presentation.setEnabled(isEnabled(browser, selectedElement));
            LocalizeValue nonDefaultText = getNonDefaultText(browser, selectedElement);
            if (nonDefaultText != LocalizeValue.empty()) {
                presentation.setTextValue(nonDefaultText);
            }
        }

        @RequiredReadAction
        protected boolean isEnabled(@Nonnull HierarchyBrowserBaseEx browser, @Nonnull PsiElement element) {
            return !element.equals(browser.mySmartPsiElementPointer.getElement()) && element.isValid();
        }

        @Nullable
        protected LocalizeValue getNonDefaultText(@Nonnull HierarchyBrowserBaseEx browser, @Nonnull PsiElement element) {
            return LocalizeValue.empty();
        }
    }

    private class RefreshAction extends consulo.ui.ex.action.RefreshAction {
        public RefreshAction() {
            super(IdeLocalize.actionRefresh(), IdeLocalize.actionRefresh(), PlatformIconGroup.actionsRefresh());
        }

        @Override
        @RequiredUIAccess
        public final void actionPerformed(AnActionEvent e) {
            doRefresh(false);
        }

        @Override
        @RequiredUIAccess
        public final void update(AnActionEvent event) {
            Presentation presentation = event.getPresentation();
            presentation.setEnabled(isValidBase());
        }
    }

    public class ChangeScopeAction extends ComboBoxAction {
        @Override
        @RequiredUIAccess
        public final void update(@Nonnull AnActionEvent e) {
            Presentation presentation = e.getPresentation();
            Project project = e.getData(Project.KEY);
            if (project == null) {
                return;
            }
            presentation.setEnabled(isEnabled());
            presentation.setTextValue(getCurrentScopeName());
        }

        protected boolean isEnabled() {
            return true;
        }

        @Override
        @Nonnull
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

        private void selectScope(@Nonnull HierarchyScope scope) {
            myType2ScopeMap.put(myCurrentViewType, scope);
            HierarchyBrowserManager.State state = HierarchyBrowserManager.getInstance(myProject).getState();
            assert state != null;
            state.SCOPE = scope.getId();

            // invokeLater is called to update state of button before long tree building operation
            myProject.getApplication().invokeLater(() -> {
                doRefresh(true); // scope is kept per type so other builders doesn't need to be refreshed
            });
        }

        @Nonnull
        @Override
        public final JComponent createCustomComponent(@Nonnull Presentation presentation, @Nonnull String place) {
            JPanel panel = new JPanel(new GridBagLayout());
            panel.add(
                new JLabel(IdeLocalize.labelScope().get()),
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
            @Nonnull
            private final HierarchyScope myScope;

            public MenuAction(@Nonnull HierarchyScope scope) {
                super(scope.getPresentableName());
                myScope = scope;
            }

            @RequiredUIAccess
            @Override
            public final void actionPerformed(@Nonnull AnActionEvent e) {
                selectScope(myScope);
            }
        }

        private final class ConfigureScopesAction extends AnAction {
            private ConfigureScopesAction() {
                super(LocalizeValue.localizeTODO("Configure..."));
            }

            @RequiredUIAccess
            @Override
            public void actionPerformed(@Nonnull AnActionEvent e) {
                EditScopesDialog.showDialog(myProject, null);
                if (!getValidScopes().contains(myType2ScopeMap.get(myCurrentViewType))) {
                    selectScope(AllHierarchyScope.INSTANCE);
                }
            }
        }
    }
}
