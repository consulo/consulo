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
package consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot;

import consulo.annotation.DeprecationInfo;
import consulo.compiler.artifact.Artifact;
import consulo.configurable.Configurable;
import consulo.configurable.ConfigurationException;
import consulo.configurable.MasterDetailsConfigurable;
import consulo.configurable.SearchableConfigurable;
import consulo.content.bundle.Sdk;
import consulo.content.library.Library;
import consulo.content.library.LibraryTable;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot.daemon.ProjectStructureElement;
import consulo.localize.LocalizeValue;
import consulo.module.Module;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.TreeExpander;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.MasterDetailsComponent;
import consulo.ui.ex.awt.MasterDetailsState;
import consulo.ui.ex.awt.MasterDetailsStateService;
import consulo.ui.ex.awt.speedSearch.TreeSpeedSearch;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.ui.ex.keymap.Keymap;
import consulo.ui.ex.keymap.KeymapManager;
import consulo.ui.image.Image;
import consulo.util.collection.ContainerUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Provider;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.List;
import java.util.*;

public abstract class BaseStructureConfigurable extends MasterDetailsComponent implements SearchableConfigurable, Configurable.NoMargin, Configurable.NoScroll {
    protected boolean myUiDisposed = true;

    private boolean myWasTreeInitialized;

    protected boolean myAutoScrollEnabled = true;

    protected BaseStructureConfigurable(Provider<MasterDetailsStateService> masterDetailsStateService, MasterDetailsState state) {
        super(masterDetailsStateService, state);
    }

    protected BaseStructureConfigurable(Provider<MasterDetailsStateService> masterDetailsStateService) {
        super(masterDetailsStateService);
    }

    @Override
    protected void initTree() {
        if (myWasTreeInitialized) {
            return;
        }
        myWasTreeInitialized = true;

        super.initTree();
        new TreeSpeedSearch(myTree, treePath -> ((MyNode)treePath.getLastPathComponent()).getDisplayName().get(), true);
        ToolTipManager.sharedInstance().registerComponent(myTree);
        myTree.setCellRenderer(new ProjectStructureElementRenderer(null));
    }

    @Override
    @RequiredUIAccess
    public void disposeUIResources() {
        if (myUiDisposed) {
            return;
        }

        super.disposeUIResources();

        myUiDisposed = true;

        myAutoScrollHandler.cancelAllRequests();

        ///myContext.getDaemonAnalyzer().clear();
    }

    public void checkCanApply() throws ConfigurationException {
    }

    protected void addCollapseExpandActions(List<AnAction> result) {
        TreeExpander expander = new TreeExpander() {
            @Override
            public void expandAll() {
                TreeUtil.expandAll(myTree);
            }

            @Override
            public boolean canExpand() {
                return true;
            }

            @Override
            public void collapseAll() {
                TreeUtil.collapseAll(myTree, 0);
            }

            @Override
            public boolean canCollapse() {
                return true;
            }
        };
        CommonActionsManager actionsManager = CommonActionsManager.getInstance();
        result.add(actionsManager.createExpandAllAction(expander, myTree));
        result.add(actionsManager.createCollapseAllAction(expander, myTree));
    }

    @Nullable
    public ProjectStructureElement getSelectedElement() {
        TreePath selectionPath = myTree.getSelectionPath();
        if (selectionPath != null && selectionPath.getLastPathComponent() instanceof MyNode node
            && node.getConfigurable() instanceof ProjectStructureElementConfigurable configurable) {
            return configurable.getProjectStructureElement();
        }
        return null;
    }

    private class MyFindUsagesAction extends FindUsagesInProjectStructureActionBase {

        public MyFindUsagesAction(JComponent parentComponent) {
            super(parentComponent);
        }

        @Override
        protected boolean isEnabled() {
            TreePath selectionPath = myTree.getSelectionPath();
            if (selectionPath != null) {
                MyNode node = (MyNode)selectionPath.getLastPathComponent();
                return !node.isDisplayInBold();
            }
            else {
                return false;
            }
        }

        @Override
        protected ProjectStructureElement getSelectedElement() {
            return BaseStructureConfigurable.this.getSelectedElement();
        }

        @Override
        protected RelativePoint getPointToShowResults() {
            int selectedRow = myTree.getSelectionRows()[0];
            Rectangle rowBounds = myTree.getRowBounds(selectedRow);
            Point location = rowBounds.getLocation();
            location.x += rowBounds.width;
            return new RelativePoint(myTree, location);
        }
    }

    @RequiredUIAccess
    @Override
    public void reset() {
        //myContext.reset();

        myUiDisposed = false;

        if (!myWasTreeInitialized) {
            initTree();
            myTree.setShowsRootHandles(false);
            loadTree();
        }
        else {
            resetUI();
            myTree.setShowsRootHandles(false);
            loadTree();
        }
        //for (ProjectStructureElement element : getProjectStructureElements()) {
        //    myContext.getDaemonAnalyzer().queueUpdate(element);
        //}

        super.reset();
    }

    @Nonnull
    protected Collection<? extends ProjectStructureElement> getProjectStructureElements() {
        return Collections.emptyList();
    }

    protected abstract void loadTree();

    @Override
    @Nonnull
    protected List<AnAction> createActions(boolean fromPopup) {
        List<AnAction> result = new ArrayList<>();
        AbstractAddGroup addAction = createAddAction();
        if (addAction != null) {
            result.add(addAction);
        }
        result.add(new MyRemoveAction());

        List<? extends AnAction> copyActions = createCopyActions(fromPopup);
        result.addAll(copyActions);
        result.add(AnSeparator.getInstance());

        result.add(new MyFindUsagesAction(myTree));

        return result;
    }

    @Nonnull
    protected List<? extends AnAction> createCopyActions(boolean fromPopup) {
        return Collections.emptyList();
    }

    public void onStructureUnselected() {
    }

    public void onStructureSelected() {
    }

    @Nullable
    protected abstract AbstractAddGroup createAddAction();

    protected class MyRemoveAction extends MyDeleteAction {
        public MyRemoveAction() {
            super(objects -> {
                Object[] editableObjects = ContainerUtil.mapNotNull(
                    objects,
                    object -> {
                        if (object instanceof MyNode node) {
                            MasterDetailsConfigurable namedConfigurable = node.getConfigurable();
                            if (namedConfigurable != null) {
                                return namedConfigurable.getEditableObject();
                            }
                        }
                        return null;
                    },
                    new Object[0]
                );
                return editableObjects.length == objects.length && canBeRemoved(editableObjects);
            });
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(@Nonnull AnActionEvent e) {
            TreePath[] paths = myTree.getSelectionPaths();
            if (paths == null) {
                return;
            }

            Set<TreePath> pathsToRemove = new HashSet<>();
            for (TreePath path : paths) {
                if (removeFromModel(path)) {
                    pathsToRemove.add(path);
                }
            }
            removePaths(pathsToRemove.toArray(new TreePath[pathsToRemove.size()]));
        }

        private boolean removeFromModel(TreePath selectionPath) {
            Object last = selectionPath.getLastPathComponent();

            if (!(last instanceof MyNode node)) {
                return false;
            }

            MasterDetailsConfigurable configurable = node.getConfigurable();
            if (configurable == null) {
                return false;
            }
            Object editableObject = configurable.getEditableObject();

            return removeObject(editableObject);
        }
    }

    protected boolean canBeRemoved(Object[] editableObjects) {
        for (Object editableObject : editableObjects) {
            if (!canObjectBeRemoved(editableObject)) {
                return false;
            }
        }
        return true;
    }

    private static boolean canObjectBeRemoved(Object editableObject) {
        if (editableObject instanceof Sdk || editableObject instanceof Module || editableObject instanceof Artifact) {
            return true;
        }
        if (editableObject instanceof Library library) {
            LibraryTable table = library.getTable();
            return table == null || table.isEditable();
        }
        return false;
    }

    protected boolean removeObject(Object editableObject) {
        // TODO keep only removeModule() and removeFacet() here because other removeXXX() are empty here and overridden in subclasses?
        // Override removeObject() instead?
        if (editableObject instanceof Sdk sdk) {
            removeSdk(sdk);
        }
        else if (editableObject instanceof Module module) {
            if (!removeModule(module)) {
                return false;
            }
        }
        else if (editableObject instanceof Library library) {
            if (!removeLibrary(library)) {
                return false;
            }
        }
        else if (editableObject instanceof Artifact artifact) {
            removeArtifact(artifact);
        }
        return true;
    }

    protected void removeArtifact(Artifact artifact) {
    }

    protected boolean removeLibrary(Library library) {
        return false;
    }

    protected boolean removeModule(Module module) {
        return true;
    }

    protected void removeSdk(Sdk editableObject) {
    }

    protected abstract static class AbstractAddGroup extends ActionGroup implements ActionGroupWithPreselection {
        protected AbstractAddGroup(@Nonnull LocalizeValue text, Image icon) {
            super(text, true);

            Presentation presentation = getTemplatePresentation();
            presentation.setIcon(icon);

            Keymap active = KeymapManager.getInstance().getActiveKeymap();
            if (active != null) {
                Shortcut[] shortcuts = active.getShortcuts("NewElement");
                setShortcutSet(new CustomShortcutSet(shortcuts));
            }
        }

        public AbstractAddGroup(@Nonnull LocalizeValue text) {
            this(text, PlatformIconGroup.generalAdd());
        }

        @Deprecated
        @DeprecationInfo("Use variant with LocalizeValue")
        protected AbstractAddGroup(String text, Image icon) {
            this(LocalizeValue.ofNullable(text), icon);
        }

        @Deprecated
        @DeprecationInfo("Use variant with LocalizeValue")
        public AbstractAddGroup(String text) {
            this(LocalizeValue.ofNullable(text));
        }

        @Override
        public ActionGroup getActionGroup() {
            return this;
        }

        @Override
        public int getDefaultIndex() {
            return 0;
        }
    }
}
