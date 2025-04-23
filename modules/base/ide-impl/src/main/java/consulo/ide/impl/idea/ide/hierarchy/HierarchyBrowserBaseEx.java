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
import consulo.application.AllIcons;
import consulo.application.Application;
import consulo.application.HelpManager;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.content.scope.NamedScope;
import consulo.content.scope.NamedScopesHolder;
import consulo.dataContext.DataContext;
import consulo.disposer.Disposer;
import consulo.fileEditor.impl.internal.OpenFileDescriptorImpl;
import consulo.ide.impl.idea.ide.OccurenceNavigatorSupport;
import consulo.ide.impl.idea.ide.dnd.TransferableWrapper;
import consulo.ide.impl.idea.ide.hierarchy.actions.BrowseHierarchyActionBase;
import consulo.ide.impl.idea.ide.projectView.impl.ProjectViewTree;
import consulo.ide.impl.idea.ide.util.scopeChooser.EditScopesDialog;
import consulo.ide.localize.IdeLocalize;
import consulo.project.ui.view.tree.ApplicationFileColorManager;
import consulo.language.editor.PsiCopyPasteManager;
import consulo.language.editor.hierarchy.HierarchyBrowser;
import consulo.language.editor.hierarchy.HierarchyProvider;
import consulo.language.psi.*;
import consulo.logging.Logger;
import consulo.navigation.Navigatable;
import consulo.project.Project;
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
import consulo.util.lang.ref.Ref;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NonNls;

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

    @NonNls
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
    private final Map<String, String> myType2ScopeMap = new HashMap<>();

    public HierarchyBrowserBaseEx(@Nonnull Project project, @Nonnull PsiElement element) {
        super(project);

        setHierarchyBase(element);

        myCardLayout = new CardLayout();
        myTreePanel = new JPanel(myCardLayout);

        createTrees(myType2TreeMap);

        final HierarchyBrowserManager.State state = HierarchyBrowserManager.getInstance(project).getState();
        for (String type : myType2TreeMap.keySet()) {
            myType2ScopeMap.put(type, state.SCOPE != null ? state.SCOPE : IdeLocalize.hierarchyScopeAll().get());
        }

        final Enumeration<String> keys = myType2TreeMap.keys();
        while (keys.hasMoreElements()) {
            final String key = keys.nextElement();
            final JTree tree = myType2TreeMap.get(key);
            myOccurrenceNavigators.put(key, new OccurenceNavigatorSupport(tree) {
                @Override
                @Nullable
                protected Navigatable createDescriptorForNode(DefaultMutableTreeNode node) {
                    final HierarchyNodeDescriptor descriptor = getDescriptor(node);
                    if (descriptor == null) {
                        return null;
                    }
                    PsiElement psiElement = getOpenFileElementFromDescriptor(descriptor);
                    if (psiElement == null || !psiElement.isValid()) {
                        return null;
                    }
                    final VirtualFile virtualFile = psiElement.getContainingFile().getVirtualFile();
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

        final JPanel legendPanel = createLegendPanel();
        final JPanel contentPanel;
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
        final Tree tree;
        final Function<Object, PsiElement> toPsiConverter = o -> {
            if (o instanceof HierarchyNodeDescriptor) {
                return ((HierarchyNodeDescriptor)o).getContainingFile();
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
                DnDManager.getInstance().registerSource(new DnDSource() {
                    @Override
                    public boolean canStartDragging(final DnDAction action, final Point dragOrigin) {
                        return getSelectedElements().length > 0;
                    }

                    @Override
                    public DnDDragStartBean startDragging(final DnDAction action, final Point dragOrigin) {
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
                    public Pair<Image, Point> createDraggedImage(
                        final DnDAction action,
                        final Point dragOrigin,
                        @Nonnull DnDDragStartBean bean
                    ) {
                        return null;
                    }

                    @Override
                    public void dragDropEnd() {
                    }

                    @Override
                    public void dropActionChanged(final int gestureModifiers) {
                    }
                }, tree);
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
    public void changeView(@Nonnull final String typeName) {
        changeView(typeName, true);
    }

    @RequiredReadAction
    public final void changeView(@Nonnull final String typeName, boolean requestFocus) {
        myCurrentViewType = typeName;

        final PsiElement element = mySmartPsiElementPointer.getElement();
        if (element == null || !isApplicableElement(element)) {
            return;
        }

        if (myContent != null) {
            final String displayName = getContentDisplayName(typeName, element);
            if (displayName != null) {
                myContent.setDisplayName(displayName);
            }
        }

        myCardLayout.show(myTreePanel, typeName);

        if (!myBuilders.containsKey(typeName)) {
            try {
                setWaitCursor();
                // create builder
                final JTree tree = myType2TreeMap.get(typeName);
                final DefaultTreeModel model = new DefaultTreeModel(new DefaultMutableTreeNode(""));
                tree.setModel(model);

                PsiDocumentManager.getInstance(myProject).commitAllDocuments();
                final HierarchyTreeStructure structure = createHierarchyTreeStructure(typeName, element);
                if (structure == null) {
                    return;
                }
                final Comparator<NodeDescriptor> comparator = getComparator();
                final HierarchyTreeBuilder builder = new HierarchyTreeBuilder(myProject, tree, model, structure, comparator);

                myBuilders.put(typeName, builder);
                Disposer.register(this, builder);
                Disposer.register(builder, () -> myBuilders.remove(typeName));

                final HierarchyNodeDescriptor descriptor = structure.getBaseDescriptor();
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

    protected void prependActions(final DefaultActionGroup actionGroup) {
    }

    @Override
    public boolean hasNextOccurence() {
        return getOccurrenceNavigator().hasNextOccurence();
    }

    private OccurenceNavigator getOccurrenceNavigator() {
        if (myCurrentViewType == null) {
            return EMPTY_NAVIGATOR;
        }
        final OccurenceNavigator navigator = myOccurrenceNavigators.get(myCurrentViewType);
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
    final boolean isValidBase() {
        if (PsiDocumentManager.getInstance(myProject).getUncommittedDocuments().length > 0) {
            return myCachedIsValidBase;
        }

        final PsiElement element = mySmartPsiElementPointer.getElement();
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
    public Object getData(@Nonnull final Key<?> dataId) {
        if (getBrowserDataKey() == dataId) {
            return this;
        }
        if (HelpManager.HELP_ID == dataId) {
            return HELP_ID;
        }
        return super.getData(dataId);
    }

    private void disposeBuilders() {
        final Collection<HierarchyTreeBuilder> builders = new ArrayList<>(myBuilders.values());
        for (final HierarchyTreeBuilder builder : builders) {
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

        final Ref<Couple<List<Object>>> storedInfo = new Ref<>();
        if (myCurrentViewType != null) {
            final HierarchyTreeBuilder builder = getCurrentBuilder();
            storedInfo.set(builder.storeExpandedAndSelectedInfo());
        }

        final PsiElement element = mySmartPsiElementPointer.getElement();
        if (element == null || !isApplicableElement(element)) {
            return;
        }
        final String currentViewType = myCurrentViewType;

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
            final HierarchyTreeBuilder builder = getCurrentBuilder();
            builder.restoreExpandedAndSelectedInfo(storedInfo.get());
        });
    }

    protected String getCurrentScopeType() {
        if (myCurrentViewType == null) {
            return null;
        }
        return myType2ScopeMap.get(myCurrentViewType);
    }

    protected class AlphaSortAction extends ToggleAction {
        public AlphaSortAction() {
            super(IdeLocalize.actionSortAlphabetically(), IdeLocalize.actionSortAlphabetically(), AllIcons.ObjectBrowser.Sorted);
        }

        @Override
        public final boolean isSelected(@Nonnull final AnActionEvent event) {
            return HierarchyBrowserManager.getInstance(myProject).getState().SORT_ALPHABETICALLY;
        }

        @Override
        public final void setSelected(@Nonnull final AnActionEvent event, final boolean flag) {
            final HierarchyBrowserManager hierarchyBrowserManager = HierarchyBrowserManager.getInstance(myProject);
            hierarchyBrowserManager.getState().SORT_ALPHABETICALLY = flag;
            final Comparator<NodeDescriptor> comparator = getComparator();
            final Collection<HierarchyTreeBuilder> builders = myBuilders.values();
            for (final HierarchyTreeBuilder builder : builders) {
                builder.setNodeDescriptorComparator(comparator);
            }
        }

        @RequiredUIAccess
        @Override
        public final void update(@Nonnull final AnActionEvent event) {
            super.update(event);
            final Presentation presentation = event.getPresentation();
            presentation.setEnabled(isValidBase());
        }
    }

    static class BaseOnThisElementAction<H extends HierarchyProvider> extends AnAction {
        private final String myActionId;
        private final Key<?> myBrowserDataKey;
        @Nonnull
        private final Class<H> myHierarchyProviderClass;

        BaseOnThisElementAction(
            @Nonnull String text,
            @Nonnull String actionId,
            @Nonnull Key<?> browserDataKey,
            @Nonnull Class<H> hierarchyProviderClass
        ) {
            super(text);
            myActionId = actionId;
            myBrowserDataKey = browserDataKey;
            myHierarchyProviderClass = hierarchyProviderClass;
        }

        @RequiredUIAccess
        @Override
        public final void actionPerformed(@Nonnull final AnActionEvent event) {
            final DataContext dataContext = event.getDataContext();
            final HierarchyBrowserBaseEx browser = (HierarchyBrowserBaseEx)dataContext.getData(myBrowserDataKey);
            if (browser == null) {
                return;
            }

            final PsiElement selectedElement = browser.getSelectedElement();
            if (selectedElement == null || !browser.isApplicableElement(selectedElement)) {
                return;
            }

            final String currentViewType = browser.myCurrentViewType;
            Disposer.dispose(browser);
            final H provider = BrowseHierarchyActionBase.findProvider(
                myHierarchyProviderClass,
                selectedElement,
                selectedElement.getContainingFile(),
                event.getDataContext()
            );
            final HierarchyBrowser newBrowser =
                BrowseHierarchyActionBase.createAndAddToPanel(selectedElement.getProject(), provider, selectedElement);
            Application.get().invokeLater(() -> ((HierarchyBrowserBaseEx)newBrowser).changeView(correctViewType(browser, currentViewType)));
        }

        protected String correctViewType(HierarchyBrowserBaseEx browser, String viewType) {
            return viewType;
        }

        @RequiredUIAccess
        @Override
        public final void update(@Nonnull final AnActionEvent event) {
            final Presentation presentation = event.getPresentation();

            registerCustomShortcutSet(ActionManager.getInstance().getAction(myActionId).getShortcutSet(), null);

            final DataContext dataContext = event.getDataContext();
            final HierarchyBrowserBaseEx browser = (HierarchyBrowserBaseEx)dataContext.getData(myBrowserDataKey);
            if (browser == null) {
                presentation.setVisible(false);
                presentation.setEnabled(false);
                return;
            }

            presentation.setVisible(true);

            final PsiElement selectedElement = browser.getSelectedElement();
            if (selectedElement == null || !browser.isApplicableElement(selectedElement)) {
                presentation.setEnabled(false);
                presentation.setVisible(false);
                return;
            }

            presentation.setEnabled(isEnabled(browser, selectedElement));
            String nonDefaultText = getNonDefaultText(browser, selectedElement);
            if (nonDefaultText != null) {
                presentation.setText(nonDefaultText);
            }
        }

        protected boolean isEnabled(@Nonnull HierarchyBrowserBaseEx browser, @Nonnull PsiElement element) {
            return !element.equals(browser.mySmartPsiElementPointer.getElement()) && element.isValid();
        }

        @Nullable
        protected String getNonDefaultText(@Nonnull HierarchyBrowserBaseEx browser, @Nonnull PsiElement element) {
            return null;
        }
    }

    private class RefreshAction extends consulo.ui.ex.action.RefreshAction {
        public RefreshAction() {
            super(IdeLocalize.actionRefresh(), IdeLocalize.actionRefresh(), AllIcons.Actions.Refresh);
        }

        @RequiredUIAccess
        @Override
        public final void actionPerformed(final AnActionEvent e) {
            doRefresh(false);
        }

        @RequiredUIAccess
        @Override
        public final void update(final AnActionEvent event) {
            final Presentation presentation = event.getPresentation();
            presentation.setEnabled(isValidBase());
        }
    }

    public class ChangeScopeAction extends ComboBoxAction {
        @RequiredUIAccess
        @Override
        public final void update(@Nonnull final AnActionEvent e) {
            final Presentation presentation = e.getPresentation();
            final Project project = e.getData(Project.KEY);
            if (project == null) {
                return;
            }
            presentation.setEnabled(isEnabled());
            presentation.setText(getCurrentScopeType());
        }

        protected boolean isEnabled() {
            return true;
        }

        @Override
        @Nonnull
        public final DefaultActionGroup createPopupActionGroup(JComponent component) {
            final DefaultActionGroup group = new DefaultActionGroup();

            for (String name : getValidScopeNames()) {
                group.add(new MenuAction(name));
            }

            group.add(new ConfigureScopesAction());

            return group;
        }

        private Collection<String> getValidScopeNames() {
            List<String> result = new ArrayList<>();
            result.add(IdeLocalize.hierarchyScopeProject().get());
            result.add(IdeLocalize.hierarchyScopeTest().get());
            result.add(IdeLocalize.hierarchyScopeAll().get());
            result.add(IdeLocalize.hierarchyScopeThisClass().get());

            final NamedScopesHolder[] holders = NamedScopesHolder.getAllNamedScopeHolders(myProject);
            for (NamedScopesHolder holder : holders) {
                NamedScope[] scopes = holder.getEditableScopes(); //predefined scopes already included
                for (NamedScope scope : scopes) {
                    result.add(scope.getName());
                }
            }
            return result;
        }

        private void selectScope(final String scopeType) {
            myType2ScopeMap.put(myCurrentViewType, scopeType);
            HierarchyBrowserManager.getInstance(myProject).getState().SCOPE = scopeType;

            // invokeLater is called to update state of button before long tree building operation
            myProject.getApplication().invokeLater(() -> {
                doRefresh(true); // scope is kept per type so other builders doesn't need to be refreshed
            });
        }

        @Nonnull
        @Override
        public final JComponent createCustomComponent(final Presentation presentation, String place) {
            final JPanel panel = new JPanel(new GridBagLayout());
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
            private final String myScopeType;

            public MenuAction(final String scopeType) {
                super(scopeType);
                myScopeType = scopeType;
            }

            @RequiredUIAccess
            @Override
            public final void actionPerformed(@Nonnull final AnActionEvent e) {
                selectScope(myScopeType);
            }
        }

        private final class ConfigureScopesAction extends AnAction {
            private ConfigureScopesAction() {
                super("Configure...");
            }

            @RequiredUIAccess
            @Override
            public void actionPerformed(@Nonnull AnActionEvent e) {
                EditScopesDialog.showDialog(myProject, null);
                if (!getValidScopeNames().contains(myType2ScopeMap.get(myCurrentViewType))) {
                    selectScope(IdeLocalize.hierarchyScopeAll().get());
                }
            }
        }
    }
}
