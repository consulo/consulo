/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.vcs.changes.ui;

import consulo.application.impl.internal.IdeaModalityState;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.dataContext.DataContext;
import consulo.ide.impl.idea.openapi.vcs.changes.ChangeListManagerImpl;
import consulo.ide.impl.idea.openapi.vcs.changes.actions.MoveChangesToAnotherListAction;
import consulo.ide.impl.idea.openapi.vcs.changes.actions.RollbackDialogAction;
import consulo.ide.impl.idea.util.EventDispatcher;
import consulo.localize.LocalizeValue;
import consulo.util.collection.ContainerUtil;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.Label;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.action.*;
import consulo.ui.ex.action.event.AnActionListener;
import consulo.ui.ex.awt.ColoredListCellRenderer;
import consulo.ui.ex.awt.ComboBox;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.VcsConfiguration;
import consulo.versionControlSystem.VcsDataKeys;
import consulo.versionControlSystem.change.*;
import consulo.versionControlSystem.impl.internal.change.ui.awt.*;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.List;
import java.util.*;

import static java.util.stream.Collectors.toList;

public class MultipleChangeListBrowser extends ChangesBrowserBase<Object> {
    @Nonnull
    private final ChangeListChooser myChangeListChooser;
    @Nonnull
    final ChangeListListener myChangeListListener = new MyChangeListListener();
    @Nonnull
    private final EventDispatcher<SelectedListChangeListener> myDispatcher = EventDispatcher.create(SelectedListChangeListener.class);
    @Nullable
    private final Runnable myRebuildListListener;
    @Nonnull
    private final VcsConfiguration myVcsConfiguration;
    private final boolean myUnversionedFilesEnabled;
    private Collection<Change> myAllChanges;
    private boolean myInRebuildList;
    private AnAction myMoveActionWithCustomShortcut;

    // todo terrible constructor
    public MultipleChangeListBrowser(
        @Nonnull Project project,
        @Nonnull List<? extends ChangeList> changeLists,
        @Nonnull List<Object> changes,
        @Nullable ChangeList initialListSelection,
        boolean capableOfExcludingChanges,
        boolean highlightProblems,
        @Nullable Runnable rebuildListListener,
        @Nullable Runnable inclusionListener,
        boolean unversionedFilesEnabled
    ) {
        super(
            project,
            changes,
            capableOfExcludingChanges,
            highlightProblems,
            inclusionListener,
            ChangesBrowser.MyUseCase.LOCAL_CHANGES,
            null,
            Object.class
        );
        myRebuildListListener = rebuildListListener;
        myVcsConfiguration = ObjectUtil.assertNotNull(VcsConfiguration.getInstance(myProject));
        myUnversionedFilesEnabled = unversionedFilesEnabled;

        init();
        setInitialSelection(changeLists, changes, initialListSelection);

        myChangeListChooser = new ChangeListChooser();
        myChangeListChooser.updateLists(changeLists);
        myHeaderPanel.add(myChangeListChooser, BorderLayout.EAST);
        ChangeListManager.getInstance(myProject).addChangeListListener(myChangeListListener);

        setupRebuildListForActions();
        rebuildList();
    }

    private void setupRebuildListForActions() {
        ActionManager actionManager = ActionManager.getInstance();
        final AnAction moveAction = actionManager.getAction(IdeActions.MOVE_TO_ANOTHER_CHANGE_LIST);
        final AnAction deleteAction = actionManager.getAction(IdeActions.DELETE_UNVERSIONED_FILES);

        actionManager.addAnActionListener(
            new AnActionListener() {
                @Override
                public void afterActionPerformed(AnAction action, DataContext dataContext, AnActionEvent event) {
                    if (moveAction.equals(action) || myMoveActionWithCustomShortcut != null && myMoveActionWithCustomShortcut.equals(action)) {
                        rebuildList();
                    }
                    else if (deleteAction.equals(action)) {
                        UnversionedViewDialog.refreshChanges(myProject, MultipleChangeListBrowser.this);
                    }
                }
            },
            this
        );
    }

    private boolean isShowUnversioned() {
        return myUnversionedFilesEnabled && myVcsConfiguration.SHOW_UNVERSIONED_FILES_WHILE_COMMIT;
    }

    @Override
    protected void setInitialSelection(
        @Nonnull List<? extends ChangeList> changeLists,
        @Nonnull List<Object> changes,
        @Nullable ChangeList initialListSelection
    ) {
        myAllChanges = new ArrayList<>();
        mySelectedChangeList = initialListSelection;

        for (ChangeList list : changeLists) {
            if (list instanceof LocalChangeList) {
                myAllChanges.addAll(list.getChanges());
                if (initialListSelection == null && ContainerUtil.intersects(list.getChanges(), changes)) {
                    mySelectedChangeList = list;
                }
            }
        }

        if (mySelectedChangeList == null) {
            mySelectedChangeList = ObjectUtil.chooseNotNull(findDefaultList(changeLists), ContainerUtil.getFirstItem(changeLists));
        }
    }

    @Override
    public void dispose() {
        ChangeListManager.getInstance(myProject).removeChangeListListener(myChangeListListener);
    }

    public void addSelectedListChangeListener(@Nonnull SelectedListChangeListener listener) {
        myDispatcher.addListener(listener);
    }

    private void setSelectedList(@Nullable ChangeList list) {
        mySelectedChangeList = list;
        rebuildList();
        myDispatcher.getMulticaster().selectedListChanged();
    }

    @Override
    public void rebuildList() {
        if (myInRebuildList) {
            return;
        }
        try {
            myInRebuildList = true;

            myAllChanges = getLocalChanges();
            updateListsInChooser();
            super.rebuildList();
            if (myRebuildListListener != null) {
                myRebuildListListener.run();
            }
        }
        finally {
            myInRebuildList = false;
        }
    }

    @Nonnull
    private Collection<Change> getLocalChanges() {
        Collection<Change> result = new ArrayList<>();
        ChangeListManager manager = ChangeListManager.getInstance(myProject);

        for (LocalChangeList list : manager.getChangeListsCopy()) {
            for (Change change : list.getChanges()) {
                result.add(change);
            }
        }

        return result;
    }

    @Override
    @Nonnull
    public List<Change> getCurrentIncludedChanges() {
        Collection<Object> includedObjects = myViewer.getIncludedChanges();

        return mySelectedChangeList.getChanges().stream().filter(includedObjects::contains).collect(toList());
    }

    @Nonnull
    @Override
    protected DefaultTreeModel buildTreeModel(
        @Nonnull List<Object> objects,
        @Nullable ChangeNodeDecorator changeNodeDecorator,
        boolean showFlatten
    ) {
        ChangeListManagerImpl manager = ChangeListManagerImpl.getInstanceImpl(myProject);
        TreeModelBuilder builder = new TreeModelBuilder(myProject, showFlatten);

        builder.setChanges(findChanges(objects), changeNodeDecorator);
        if (isShowUnversioned()) {
            builder.setUnversioned(manager.getUnversionedFiles());
        }

        return builder.build();
    }

    @Nonnull
    @Override
    protected List<Object> getSelectedObjects(@Nonnull ChangesBrowserNode<Object> node) {
        List<Object> result = new ArrayList<>();

        result.addAll(node.getAllChangesUnder());
        if (isShowUnversioned() && isUnderUnversioned(node)) {
            result.addAll(node.getAllFilesUnder());
        }

        return result;
    }

    @Nullable
    @Override
    protected Object getLeadSelectedObject(@Nonnull ChangesBrowserNode node) {
        Object result = null;
        Object userObject = node.getUserObject();

        if (userObject instanceof Change || isShowUnversioned() && isUnderUnversioned(node) && userObject instanceof VirtualFile) {
            result = userObject;
        }

        return result;
    }

    @Nonnull
    @Override
    public List<Object> getCurrentDisplayedObjects() {
        //noinspection unchecked
        return (List) getCurrentDisplayedChanges();
    }

    @Nonnull
    @Override
    public List<VirtualFile> getIncludedUnversionedFiles() {
        return isShowUnversioned()
            ? ContainerUtil.findAll(myViewer.getIncludedChanges(), VirtualFile.class)
            : Collections.<VirtualFile>emptyList();
    }

    @Override
    public int getUnversionedFilesCount() {
        int result = 0;

        if (isShowUnversioned()) {
            ChangesBrowserUnversionedFilesNode node = findUnversionedFilesNode();

            if (node != null) {
                result = node.getFileCount();
            }
        }

        return result;
    }

    @Nullable
    private ChangesBrowserUnversionedFilesNode findUnversionedFilesNode() {
        //noinspection unchecked
        Enumeration<TreeNode> nodes = myViewer.getRoot().breadthFirstEnumeration();

        return ContainerUtil.findInstance(
            consulo.ide.impl.idea.util.containers.ContainerUtil.iterate(nodes),
            ChangesBrowserUnversionedFilesNode.class
        );
    }

    @Nonnull
    @Override
    public List<Change> getSelectedChanges() {
        Set<Change> changes = new LinkedHashSet<>();
        TreePath[] paths = myViewer.getSelectionPaths();

        if (paths != null) {
            for (TreePath path : paths) {
                ChangesBrowserNode<?> node = (ChangesBrowserNode) path.getLastPathComponent();
                changes.addAll(node.getAllChangesUnder());
            }
        }

        return ContainerUtil.newArrayList(changes);
    }

    @Nonnull
    @Override
    public List<Change> getAllChanges() {
        return myViewer.getRoot().getAllChangesUnder();
    }

    @Override
    @Nonnull
    public Set<AbstractVcs> getAffectedVcses() {
        return ChangesUtil.getAffectedVcses(myAllChanges, myProject);
    }

    @Override
    protected void buildToolBar(@Nonnull DefaultActionGroup toolBarGroup) {
        super.buildToolBar(toolBarGroup);

        toolBarGroup.add(new AnAction(
            LocalizeValue.localizeTODO("Refresh Changes"),
            LocalizeValue.empty(),
            PlatformIconGroup.actionsRefresh()
        ) {
            @Override
            @RequiredUIAccess
            public void actionPerformed(@Nonnull AnActionEvent e) {
                rebuildList();
            }
        });
        if (myUnversionedFilesEnabled) {
            toolBarGroup.add(new ShowHideUnversionedFilesAction());
            toolBarGroup.add(UnversionedViewDialog.getUnversionedActionGroup());
        }
        else {
            toolBarGroup.add(ActionManager.getInstance().getAction(IdeActions.MOVE_TO_ANOTHER_CHANGE_LIST));
        }
        // We do not add "Delete" key shortcut for deleting unversioned files as this shortcut is already used to uncheck
        // checkboxes in the tree.
        myMoveActionWithCustomShortcut =
            EmptyAction.registerWithShortcutSet(IdeActions.MOVE_TO_ANOTHER_CHANGE_LIST, CommonShortcuts.getMove(), myViewer);

        RollbackDialogAction rollback = new RollbackDialogAction();
        rollback.registerCustomShortcutSet(this, null);
        toolBarGroup.add(rollback);

        EditSourceForDialogAction editSourceAction = new EditSourceForDialogAction(this);
        editSourceAction.registerCustomShortcutSet(CommonShortcuts.getEditSource(), this);
        toolBarGroup.add(editSourceAction);
    }

    @Override
    protected void afterDiffRefresh() {
        rebuildList();
        setDataIsDirty(false);
        myProject.getApplication()
            .invokeLater(() -> IdeFocusManager.findInstance().requestFocus(myViewer.getPreferredFocusedComponent(), true));
    }

    @Override
    protected List<AnAction> createDiffActions() {
        List<AnAction> actions = super.createDiffActions();
        actions.add(new MoveAction());
        return actions;
    }

    private void updateListsInChooser() {
        Runnable runnable = () -> myChangeListChooser.updateLists(ChangeListManager.getInstance(myProject).getChangeListsCopy());
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        }
        else {
            myProject.getApplication().invokeLater(runnable, IdeaModalityState.stateForComponent(this));
        }
    }

    @Nullable
    private static ChangeList findDefaultList(@Nonnull List<? extends ChangeList> lists) {
        return ContainerUtil.find(
            lists,
            list -> list instanceof LocalChangeList localChangeList && localChangeList.isDefault()
        );
    }

    private class ChangeListChooser extends JPanel {
        private final static int MAX_LEN = 35;
        @Nonnull
        private final ComboBox myChooser;

        public ChangeListChooser() {
            super(new BorderLayout(4, 2));
            myChooser = new ComboBox();
            myChooser.setRenderer(new ColoredListCellRenderer<LocalChangeList>() {
                @Override
                protected void customizeCellRenderer(
                    @Nonnull JList<? extends LocalChangeList> list,
                    LocalChangeList value,
                    int index,
                    boolean selected,
                    boolean hasFocus
                ) {
                    if (value != null) {
                        String name = StringUtil.shortenTextWithEllipsis(value.getName().trim(), MAX_LEN, 0);

                        append(
                            name,
                            value.isDefault() ? SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES : SimpleTextAttributes.REGULAR_ATTRIBUTES
                        );
                    }
                }
            });

            myChooser.addItemListener(e -> {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    LocalChangeList changeList = (LocalChangeList) myChooser.getSelectedItem();
                    setSelectedList(changeList);
                    myChooser.setToolTipText(changeList == null ? "" : (changeList.getName()));
                }
            });

            myChooser.setEditable(false);
            add(myChooser, BorderLayout.CENTER);

            Label label = Label.create(VcsLocalize.commitDialogChangelistLabel());
            label.setTarget(TargetAWT.wrap(myChooser));
            add(TargetAWT.to(label), BorderLayout.WEST);
        }

        public void updateLists(@Nonnull List<? extends ChangeList> lists) {
            //noinspection unchecked
            myChooser.setModel(new DefaultComboBoxModel(lists.toArray()));
            myChooser.setEnabled(lists.size() > 1);
            if (lists.contains(mySelectedChangeList)) {
                myChooser.setSelectedItem(mySelectedChangeList);
            }
            else {
                if (myChooser.getItemCount() > 0) {
                    myChooser.setSelectedIndex(0);
                }
            }
            mySelectedChangeList = (ChangeList) myChooser.getSelectedItem();
        }
    }

    private class MyChangeListListener implements ChangeListListener {
        @Override
        public void changeListAdded(ChangeList list) {
            updateListsInChooser();
        }
    }

    private class ShowHideUnversionedFilesAction extends ToggleAction {

        private ShowHideUnversionedFilesAction() {
            super(LocalizeValue.localizeTODO("Show Unversioned Files"), LocalizeValue.empty(), PlatformIconGroup.actionsCancel());
        }

        @Override
        public void update(@Nonnull AnActionEvent e) {
            super.update(e);

            e.getPresentation().setEnabledAndVisible(e.isFromActionToolbar());
        }

        @Override
        public boolean isSelected(@Nonnull AnActionEvent e) {
            return myVcsConfiguration.SHOW_UNVERSIONED_FILES_WHILE_COMMIT;
        }

        @Override
        @RequiredUIAccess
        public void setSelected(@Nonnull AnActionEvent e, boolean state) {
            myVcsConfiguration.SHOW_UNVERSIONED_FILES_WHILE_COMMIT = state;
            rebuildList();
        }
    }

    private class MoveAction extends MoveChangesToAnotherListAction {
        @Override
        protected boolean isEnabled(@Nonnull AnActionEvent e) {
            return e.hasData(VcsDataKeys.CURRENT_CHANGE) && super.isEnabled(e);
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(@Nonnull AnActionEvent e) {
            Change change = e.getRequiredData(VcsDataKeys.CURRENT_CHANGE);
            askAndMove(myProject, Collections.singletonList(change), Collections.<VirtualFile>emptyList());
        }
    }
}
