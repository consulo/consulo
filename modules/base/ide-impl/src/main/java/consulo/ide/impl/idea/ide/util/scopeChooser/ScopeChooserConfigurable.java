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
package consulo.ide.impl.idea.ide.util.scopeChooser;

import consulo.annotation.component.ExtensionImpl;
import consulo.configurable.*;
import consulo.content.internal.scope.CustomScopesProvider;
import consulo.content.scope.NamedScope;
import consulo.content.scope.NamedScopesHolder;
import consulo.content.scope.PackageSet;
import consulo.execution.localize.ExecutionLocalize;
import consulo.ide.localize.IdeLocalize;
import consulo.language.editor.inspection.localize.InspectionLocalize;
import consulo.language.editor.packageDependency.DependencyValidationManager;
import consulo.language.editor.scope.NamedScopeManager;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.localize.ProjectLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.InputValidator;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.speedSearch.TreeSpeedSearch;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.ui.image.Image;
import consulo.util.lang.Comparing;
import consulo.util.xml.serializer.annotation.AbstractCollection;
import consulo.util.xml.serializer.annotation.Tag;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.*;

/**
 * @author anna
 * @since 2006-07-01
 */
@ExtensionImpl
public class ScopeChooserConfigurable extends MasterDetailsComponent implements SearchableConfigurable, Configurable.NoMargin, ProjectConfigurable {
    public static final String SCOPE_CHOOSER_CONFIGURABLE_UI_KEY = "ScopeChooserConfigurable.UI";
    public static final String PROJECT_SCOPES = "project.scopes";
    private final NamedScopesHolder myLocalScopesManager;
    private final NamedScopesHolder mySharedScopesManager;

    private final Project myProject;

    @Inject
    public ScopeChooserConfigurable(Project project, Provider<MasterDetailsStateService> masterDetailsStateService) {
        super(masterDetailsStateService, new ScopeChooserConfigurableState());
        myLocalScopesManager = NamedScopeManager.getInstance(project);
        mySharedScopesManager = DependencyValidationManager.getInstance(project);
        myProject = project;

        initTree();
    }

    @Override
    protected String getComponentStateKey() {
        return SCOPE_CHOOSER_CONFIGURABLE_UI_KEY;
    }

    @Override
    protected Dimension getPanelPreferredSize() {
        return new Dimension(-1, -1);
    }

    @Override
    protected List<AnAction> createActions(boolean fromPopup) {
        List<AnAction> result = new ArrayList<>();
        result.add(new MyAddAction(fromPopup));
        result.add(new MyDeleteAction(forAll(o -> {
            if (o instanceof MyNode) {
                Object editableObject = ((MyNode) o).getConfigurable().getEditableObject();
                return editableObject instanceof NamedScope;
            }
            return false;
        })));
        result.add(new MyCopyAction());
        result.add(new MySaveAsAction());
        result.add(new MyMoveAction(ExecutionLocalize.moveUpActionName(), PlatformIconGroup.actionsMoveup(), -1));
        result.add(new MyMoveAction(ExecutionLocalize.moveDownActionName(), PlatformIconGroup.actionsMovedown(), 1));
        return result;
    }

    @RequiredUIAccess
    @Override
    public void reset() {
        reloadTree();
        super.reset();
    }


    @Override
    @RequiredUIAccess
    public void apply() throws ConfigurationException {
        Set<MyNode> roots = new HashSet<>();
        roots.add(myRoot);
        checkApply(
            roots,
            ProjectLocalize.renameMessagePrefixScope().get(),
            ProjectLocalize.renameScopeTitle().get()
        );
        super.apply();
        processScopes();

        loadStateOrder();
    }

    @Override
    protected void checkApply(Set<MyNode> rootNodes, String prefix, String title) throws ConfigurationException {
        super.checkApply(rootNodes, prefix, title);
        Set<String> predefinedScopes = new HashSet<>();
        for (CustomScopesProvider scopesProvider : myProject.getExtensionList(CustomScopesProvider.class)) {
            scopesProvider.acceptScopes(namedScope -> predefinedScopes.add(namedScope.getName()));
        }
        for (MyNode rootNode : rootNodes) {
            for (int i = 0; i < rootNode.getChildCount(); i++) {
                MyNode node = (MyNode) rootNode.getChildAt(i);
                MasterDetailsConfigurable scopeConfigurable = node.getConfigurable();
                String name = scopeConfigurable.getDisplayName().get();
                if (predefinedScopes.contains(name)) {
                    selectNodeInTree(node);
                    throw new ConfigurationException(
                        "Scope name equals to predefined one",
                        ProjectLocalize.renameScopeTitle().get()
                    );
                }
            }
        }
    }

    public ScopeChooserConfigurableState getScopesState() {
        return (ScopeChooserConfigurableState) myState;
    }

    @Override
    @RequiredUIAccess
    public boolean isModified() {
        List<String> order = getScopesState().myOrder;
        if (myRoot.getChildCount() != order.size()) {
            return true;
        }
        for (int i = 0; i < myRoot.getChildCount(); i++) {
            MyNode node = (MyNode) myRoot.getChildAt(i);
            ScopeConfigurable scopeConfigurable = (ScopeConfigurable) node.getConfigurable();
            NamedScope namedScope = scopeConfigurable.getEditableObject();
            if (order.size() <= i) {
                return true;
            }
            String name = order.get(i);
            if (!Comparing.strEqual(name, namedScope.getName())) {
                return true;
            }
            if (isInitialized(scopeConfigurable)) {
                NamedScopesHolder holder = scopeConfigurable.getHolder();
                NamedScope scope = holder.getScope(name);
                if (scope == null) {
                    return true;
                }
                if (scopeConfigurable.isModified()) {
                    return true;
                }
            }
        }
        return false;
    }

    private void processScopes() {
        List<NamedScope> localScopes = new ArrayList<>();
        List<NamedScope> sharedScopes = new ArrayList<>();
        for (int i = 0; i < myRoot.getChildCount(); i++) {
            MyNode node = (MyNode) myRoot.getChildAt(i);
            ScopeConfigurable scopeConfigurable = (ScopeConfigurable) node.getConfigurable();
            NamedScope namedScope = scopeConfigurable.getScope();
            if (scopeConfigurable.getHolder() == myLocalScopesManager) {
                localScopes.add(namedScope);
            }
            else {
                sharedScopes.add(namedScope);
            }
        }
        myLocalScopesManager.setScopes(localScopes.toArray(new NamedScope[localScopes.size()]));
        mySharedScopesManager.setScopes(sharedScopes.toArray(new NamedScope[sharedScopes.size()]));
    }

    @RequiredUIAccess
    private void reloadTree() {
        myRoot.removeAllChildren();
        loadScopes(mySharedScopesManager);
        loadScopes(myLocalScopesManager);

        if (isModified()) {
            loadStateOrder();
        }


        List<String> order = getScopesState().myOrder;
        TreeUtil.sort(myRoot, (o1, o2) -> {
            int idx1 = order.indexOf(((MyNode) o1).getDisplayName());
            int idx2 = order.indexOf(((MyNode) o2).getDisplayName());
            return idx1 - idx2;
        });
    }

    private void loadStateOrder() {
        List<String> order = getScopesState().myOrder;
        order.clear();
        for (int i = 0; i < myRoot.getChildCount(); i++) {
            order.add(((MyNode) myRoot.getChildAt(i)).getDisplayName().get());
        }
    }

    private void loadScopes(NamedScopesHolder holder) {
        NamedScope[] scopes = holder.getScopes();
        for (NamedScope scope : scopes) {
            if (isPredefinedScope(scope)) {
                continue;
            }
            myRoot.add(new MyNode(new ScopeConfigurable(scope, holder == mySharedScopesManager, myProject, TREE_UPDATER)));
        }
    }

    private boolean isPredefinedScope(NamedScope scope) {
        return getPredefinedScopes(myProject).contains(scope);
    }

    private static Collection<NamedScope> getPredefinedScopes(Project project) {
        Collection<NamedScope> result = new ArrayList<>();
        result.addAll(NamedScopeManager.getInstance(project).getPredefinedScopes());
        result.addAll(DependencyValidationManager.getInstance(project).getPredefinedScopes());
        return result;
    }

    @Override
    protected void initTree() {
        myTree.getSelectionModel().addTreeSelectionListener(e -> {
            TreePath path = e.getOldLeadSelectionPath();
            if (path != null) {
                MyNode node = (MyNode) path.getLastPathComponent();
                MasterDetailsConfigurable namedConfigurable = node.getConfigurable();
                if (namedConfigurable instanceof ScopeConfigurable) {
                    ((ScopeConfigurable) namedConfigurable).cancelCurrentProgress();
                }
            }
        });
        super.initTree();
        myTree.setShowsRootHandles(false);
        new TreeSpeedSearch(myTree, treePath -> ((MyNode) treePath.getLastPathComponent()).getDisplayName().get(), true);

        myTree.getEmptyText().setText(IdeLocalize.scopesNoScoped());
    }

    @Override
    protected void processRemovedItems() {
        //do nothing
    }

    @Override
    protected boolean wasObjectStored(Object editableObject) {
        if (editableObject instanceof NamedScope) {
            NamedScope scope = (NamedScope) editableObject;
            String scopeName = scope.getName();
            return myLocalScopesManager.getScope(scopeName) != null || mySharedScopesManager.getScope(scopeName) != null;
        }
        return false;
    }

    @Nonnull
    @Override
    public LocalizeValue getDisplayName() {
        return IdeLocalize.scopesDisplayName();
    }

    @RequiredUIAccess
    @Override
    protected void updateSelection(@Nullable MasterDetailsConfigurable configurable) {
        super.updateSelection(configurable);
        if (configurable instanceof ScopeConfigurable scopeConfigurable) {
            scopeConfigurable.restoreCanceledProgress();
        }
    }

    @Nullable
    @Override
    protected String getEmptySelectionString() {
        return "Select a scope to view or edit its details here";
    }

    private String createUniqueName() {
        String str = InspectionLocalize.inspectionProfileUnnamed().get();
        HashSet<String> treeScopes = new HashSet<>();
        obtainCurrentScopes(treeScopes);
        if (!treeScopes.contains(str)) {
            return str;
        }
        int i = 1;
        while (true) {
            if (!treeScopes.contains(str + i)) {
                return str + i;
            }
            i++;
        }
    }

    private void obtainCurrentScopes(HashSet<String> scopes) {
        for (int i = 0; i < myRoot.getChildCount(); i++) {
            MyNode node = (MyNode) myRoot.getChildAt(i);
            NamedScope scope = (NamedScope) node.getConfigurable().getEditableObject();
            scopes.add(scope.getName());
        }
    }

    private void addNewScope(NamedScope scope, boolean isLocal) {
        MyNode nodeToAdd = new MyNode(new ScopeConfigurable(scope, !isLocal, myProject, TREE_UPDATER));
        myRoot.add(nodeToAdd);
        ((DefaultTreeModel) myTree.getModel()).reload(myRoot);
        selectNodeInTree(nodeToAdd);
    }

    @RequiredUIAccess
    private void createScope(final boolean isLocal, @Nonnull LocalizeValue title, PackageSet set) {
        String newName = Messages.showInputDialog(
            myTree,
            IdeLocalize.addScopeNameLabel().get(),
            title.get(),
            UIUtil.getInformationIcon(),
            createUniqueName(),
            new InputValidator() {
                @RequiredUIAccess
                @Override
                public boolean checkInput(String inputString) {
                    NamedScopesHolder holder = isLocal ? myLocalScopesManager : mySharedScopesManager;
                    for (NamedScope scope : holder.getPredefinedScopes()) {
                        if (Comparing.strEqual(scope.getName(), inputString.trim())) {
                            return false;
                        }
                    }
                    return inputString.trim().length() > 0;
                }

                @RequiredUIAccess
                @Override
                public boolean canClose(String inputString) {
                    return checkInput(inputString);
                }
            }
        );
        if (newName != null) {
            NamedScope scope = new NamedScope(newName, set);
            addNewScope(scope, isLocal);
        }
    }

    @Override
    @Nonnull
    public String getId() {
        return PROJECT_SCOPES;
    }

    @Nullable
    @Override
    public String getParentId() {
        return StandardConfigurableIds.GENERAL_GROUP;
    }

    @Override
    @Nullable
    public Runnable enableSearch(String option) {
        return null;
    }

    private class MyAddAction extends ActionGroup implements ActionGroupWithPreselection {
        private AnAction[] myChildren;
        private final boolean myFromPopup;

        public MyAddAction(boolean fromPopup) {
            super(IdeLocalize.addScopePopupTitle(), true);
            myFromPopup = fromPopup;
            Presentation presentation = getTemplatePresentation();
            presentation.setIcon(PlatformIconGroup.generalAdd());
            setShortcutSet(CommonShortcuts.INSERT);
        }

        @Override
        @RequiredUIAccess
        public void update(@Nonnull AnActionEvent e) {
            super.update(e);
            if (myFromPopup) {
                setPopup(false);
            }
        }

        @Override
        @Nonnull
        public AnAction[] getChildren(@Nullable AnActionEvent e) {
            if (myChildren == null) {
                myChildren = new AnAction[2];
                myChildren[0] = new AnAction(
                    IdeLocalize.addLocalScopeActionText(),
                    IdeLocalize.addLocalScopeActionText(),
                    myLocalScopesManager.getIcon()
                ) {
                    @RequiredUIAccess
                    @Override
                    public void actionPerformed(@Nonnull AnActionEvent e) {
                        createScope(true, IdeLocalize.addScopeDialogTitle(), null);
                    }
                };
                myChildren[1] = new AnAction(
                    IdeLocalize.addSharedScopeActionText(),
                    IdeLocalize.addSharedScopeActionText(),
                    mySharedScopesManager.getIcon()
                ) {
                    @RequiredUIAccess
                    @Override
                    public void actionPerformed(@Nonnull AnActionEvent e) {
                        createScope(false, IdeLocalize.addScopeDialogTitle(), null);
                    }
                };
            }
            if (myFromPopup) {
                AnAction action = myChildren[getDefaultIndex()];
                action.getTemplatePresentation().setIcon(PlatformIconGroup.generalAdd());
                return new AnAction[]{action};
            }
            return myChildren;
        }

        @Override
        public ActionGroup getActionGroup() {
            return this;
        }

        @Override
        public int getDefaultIndex() {
            TreePath selectionPath = myTree.getSelectionPath();
            if (selectionPath != null) {
                MyNode node = (MyNode) selectionPath.getLastPathComponent();
                Object editableObject = node.getConfigurable().getEditableObject();
                if (editableObject instanceof NamedScope) {
                    editableObject = ((MyNode) node.getParent()).getConfigurable().getEditableObject();
                }
                if (editableObject instanceof NamedScopeManager) {
                    return 0;
                }
                else if (editableObject instanceof DependencyValidationManager) {
                    return 1;
                }
            }
            return 0;
        }
    }


    private class MyMoveAction extends AnAction {
        private final int myDirection;

        protected MyMoveAction(LocalizeValue text, Image icon, int direction) {
            super(text, text, icon);
            myDirection = direction;
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(@Nonnull AnActionEvent e) {
            TreeUtil.moveSelectedRow(myTree, myDirection);
        }

        @Override
        @RequiredUIAccess
        public void update(AnActionEvent e) {
            Presentation presentation = e.getPresentation();
            presentation.setEnabled(false);
            TreePath selectionPath = myTree.getSelectionPath();
            if (selectionPath != null) {
                DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) selectionPath.getLastPathComponent();
                if (treeNode.getUserObject() instanceof ScopeConfigurable) {
                    if (myDirection < 0) {
                        presentation.setEnabled(treeNode.getPreviousSibling() != null);
                    }
                    else {
                        presentation.setEnabled(treeNode.getNextSibling() != null);
                    }
                }
            }
        }
    }

    private class MyCopyAction extends AnAction {
        public MyCopyAction() {
            super(
                ExecutionLocalize.copyConfigurationActionName(),
                ExecutionLocalize.copyConfigurationActionName(),
                PlatformIconGroup.actionsCopy()
            );
            registerCustomShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_MASK)), myTree);
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(@Nonnull AnActionEvent e) {
            NamedScope scope = (NamedScope) getSelectedObject();
            if (scope != null) {
                NamedScope newScope = scope.createCopy();
                ScopeConfigurable configurable =
                    (ScopeConfigurable) ((MyNode) myTree.getSelectionPath().getLastPathComponent()).getConfigurable();
                addNewScope(new NamedScope(createUniqueName(), newScope.getValue()), configurable.getHolder() == myLocalScopesManager);
            }
        }

        @Override
        @RequiredUIAccess
        public void update(AnActionEvent e) {
            e.getPresentation().setEnabled(getSelectedObject() instanceof NamedScope);
        }
    }

    private class MySaveAsAction extends AnAction {
        public MySaveAsAction() {
            super(
                ExecutionLocalize.actionNameSaveAsConfiguration(),
                ExecutionLocalize.actionNameSaveAsConfiguration(),
                PlatformIconGroup.actionsMenu_saveall()
            );
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(@Nonnull AnActionEvent e) {
            TreePath selectionPath = myTree.getSelectionPath();
            if (selectionPath != null) {
                MyNode node = (MyNode) selectionPath.getLastPathComponent();
                MasterDetailsConfigurable configurable = node.getConfigurable();
                if (configurable instanceof ScopeConfigurable) {
                    ScopeConfigurable scopeConfigurable = (ScopeConfigurable) configurable;
                    PackageSet set = scopeConfigurable.getEditableObject().getValue();
                    if (set != null) {
                        if (scopeConfigurable.getHolder() == mySharedScopesManager) {
                            createScope(false, IdeLocalize.scopesSaveDialogTitleShared(), set.createCopy());
                        }
                        else {
                            createScope(true, IdeLocalize.scopesSaveDialogTitleLocal(), set.createCopy());
                        }
                    }
                }
            }
        }

        @Override
        @RequiredUIAccess
        public void update(AnActionEvent e) {
            e.getPresentation().setEnabled(getSelectedObject() instanceof NamedScope);
        }
    }

    public static class ScopeChooserConfigurableState extends MasterDetailsState {
        @Tag("order")
        @AbstractCollection(surroundWithTag = false, elementTag = "scope", elementValueAttribute = "name")
        public List<String> myOrder = new ArrayList<>();
    }
}
