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
package consulo.versionControlSystem.distributed.impl.internal.push;

import consulo.dataContext.DataSink;
import consulo.dataContext.UiDataProvider;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.CheckBoxTree;
import consulo.ui.CheckBoxTreePolicy;
import consulo.ui.Component;
import consulo.ui.Hyperlink;
import consulo.ui.Label;
import consulo.ui.Space;
import consulo.ui.TextAttribute;
import consulo.ui.TextItemPresentation;
import consulo.ui.TreeModel;
import consulo.ui.TreeNode;
import consulo.ui.TreeNodeEditor;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareToggleAction;
import consulo.ui.ex.util.TextAttributeUtil;
import consulo.ui.image.Image;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.layout.SplitLayoutPosition;
import consulo.ui.layout.TwoComponentSplitLayout;
import consulo.ui.style.ComponentColors;
import consulo.versionControlSystem.VcsDataKeys;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.ChangesBrowserUtil;
import consulo.versionControlSystem.distributed.DvcsUtil;
import consulo.versionControlSystem.distributed.localize.DistributedVcsLocalize;
import consulo.versionControlSystem.distributed.push.PushSettings;
import consulo.versionControlSystem.distributed.push.PushSource;
import consulo.versionControlSystem.distributed.push.PushTarget;
import consulo.versionControlSystem.distributed.push.PushTargetPanel;
import consulo.versionControlSystem.distributed.repository.Repository;
import consulo.versionControlSystem.history.TextRevisionNumber;
import consulo.versionControlSystem.history.VcsRevisionNumber;
import consulo.versionControlSystem.log.Hash;
import consulo.versionControlSystem.log.VcsFullCommitDetails;
import consulo.versionControlSystem.log.util.VcsUserUtil;
import consulo.versionControlSystem.ui.VcsChangesBrowser;
import consulo.versionControlSystem.ui.VcsChangesDialogFactory;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PushLog {
    private static final String ARROW = " → ";

    private final Project myProject;
    private final PushController myController;
    private final List<PushRepoModel<Repository, PushSource, PushTarget>> myModels;
    private final boolean myAllowSyncStrategy;
    private final PushSettings mySettings;

    private final Map<PushRepoModel<?, ?, ?>, TreeNode<Object>> myNodes = new HashMap<>();
    private final Set<PushRepoModel<?, ?, ?>> myPendingExpand = new LinkedHashSet<>();
    private final CheckBoxTree<Object> myTree;
    private final VcsChangesBrowser myChangesBrowser;
    private final PushCommitDetailsPanel myDetailsPanel;
    private final TwoComponentSplitLayout myDetailsSplitter;
    private final Label myValidation = Label.create();
    private final DockLayout myRoot = DockLayout.create();

    private boolean myDetailsAvailable;
    private boolean mySyncStrategy;
    private @Nullable String mySyncRenderedText;
    private boolean myUpdatingChecks;
    private boolean myShouldRepaint;

    @RequiredUIAccess
    PushLog(Project project,
            PushController controller,
            List<PushRepoModel<Repository, PushSource, PushTarget>> models,
            boolean allowSyncStrategy,
            Disposable uiDisposable) {
        myProject = project;
        myController = controller;
        myModels = models;
        myAllowSyncStrategy = allowSyncStrategy;
        mySettings = project.getInstance(PushSettings.class);

        myTree = CheckBoxTree.create(null, createTreeModel());
        myTree.setAllowMultipleSelect(true);
        myTree.setCheckPolicy(CheckBoxTreePolicy.INDEPENDENT);
        myTree.setCheckableFilter(value -> value instanceof PushRepoModel<?, ?, ?> model && !model.isSingle());
        myTree.setCheckEnabledFilter(value -> value instanceof PushRepoModel<?, ?, ?> model && model.isEnabled());
        myTree.setNodeEditor(new TargetEditor());
        myTree.addCheckListener(event -> {
            if (!myUpdatingChecks && event.getValue() instanceof PushRepoModel<?, ?, ?> model) {
                myController.onSelectionChanged(model, event.isChecked());
                refreshRepository(model, true);
            }
        });
        myTree.addSelectListener(event -> onSelectionChanges());
        myTree.addDoubleClickListener(event -> {
            Object value = event.getValue().getValue();
            if (value instanceof PushErrorRow error) {
                myController.handleError(error);
            }
            else if (value instanceof PushMoreCommitsRow more) {
                myController.addMoreCommits(more.repository());
            }
        });

        myChangesBrowser = myProject.getApplication().getInstance(VcsChangesDialogFactory.class)
            .changesBrowser(myProject)
            .emptyText(DistributedVcsLocalize.pushNoCommitsSelected())
            .toolbarAction(new ShowDetailsAction())
            .build(uiDisposable);

        myDetailsPanel = new PushCommitDetailsPanel();
        myDetailsSplitter = TwoComponentSplitLayout.create(SplitLayoutPosition.VERTICAL).withProportion(67);
        myDetailsSplitter.setFirstComponent(myChangesBrowser.getComponent());

        DockLayout left = DockLayout.create();
        left.center(ScrollableLayout.create(myTree));
        if (myAllowSyncStrategy) {
            Hyperlink editAll = Hyperlink.create(DistributedVcsLocalize.pushEditAllTargets(), event -> startSyncEditing());
            DockLayout editAllRow = DockLayout.create();
            editAllRow.right(editAll);
            editAllRow.paddingBuilder().allSet(Space.SMALL).apply();
            left.bottom(editAllRow);
        }

        TwoComponentSplitLayout splitter = TwoComponentSplitLayout.create(SplitLayoutPosition.HORIZONTAL).withProportion(50);
        splitter.setFirstComponent(left);
        splitter.setSecondComponent(myDetailsSplitter);

        myValidation.setForegroundColor(ComponentColors.ERROR_FOREGROUND);
        myValidation.setVisible(false);

        myRoot.putUserData(UiDataProvider.KEY, this::uiDataSnapshot);
        myRoot.center(splitter);
        myRoot.bottom(myValidation);
    }

    private TreeModel<Object> createTreeModel() {
        return (nodeFactory, parentValue) -> {
            if (parentValue == null) {
                for (PushRepoModel<Repository, PushSource, PushTarget> model : myModels) {
                    TreeNode<Object> node = nodeFactory.apply(model);
                    node.setLeaf(false);
                    node.setRenderer(this::render);
                    myNodes.put(model, node);
                }
                if (!myPendingExpand.isEmpty()) {
                    UIAccess.current().give(this::expandPending);
                }
            }
            else if (parentValue instanceof PushRepoModel<?, ?, ?> model) {
                for (Object row : model.getRows()) {
                    TreeNode<Object> node = nodeFactory.apply(row);
                    node.setLeaf(true);
                    node.setRenderer(this::render);
                }
            }
        };
    }

    public Component getComponent() {
        return myRoot;
    }

    public Component getPreferredFocusedComponent() {
        return myTree;
    }

    private void render(Object value, TextItemPresentation presentation) {
        if (value instanceof PushRepoModel<?, ?, ?> model) {
            renderRepository(model, presentation);
        }
        else if (value instanceof PushCommitRow row) {
            presentation.append(row.commit().getSubject(), active(row.repository().isSelected(), SimpleTextAttributes.REGULAR_ATTRIBUTES));
            presentation.withTooltip(DistributedVcsLocalize.pushCommitNodeTooltip0Hash1Date2Author3Message(
                DvcsUtil.getShortHash(row.commit().getId().asString()),
                DvcsUtil.getDateString(row.commit()),
                VcsUserUtil.getShortPresentation(row.commit().getAuthor()),
                row.commit().getFullMessage()
            ));
        }
        else if (value instanceof PushErrorRow row) {
            presentation.append(row.error().getText(), TextAttribute.ERROR);
            if (row.error().hasHandler() && row.error().getHandlerText().isNotEmpty()) {
                presentation.append(LocalizeValue.of(" "));
                presentation.append(row.error().getHandlerText(), TextAttributeUtil.toTextAttribute(SimpleTextAttributes.LINK_PLAIN_ATTRIBUTES));
            }
        }
        else if (value instanceof PushMoreCommitsRow) {
            presentation.append(DistributedVcsLocalize.pushLoadMoreCommits(), TextAttributeUtil.toTextAttribute(SimpleTextAttributes.LINK_PLAIN_ATTRIBUTES));
        }
    }

    private void renderRepository(PushRepoModel<?, ?, ?> model, TextItemPresentation presentation) {
        if (model.isLoading()) {
            presentation.withIcon(Image.busy(Image.DEFAULT_ICON_SIZE));
        }

        PushTargetPanel<?> targetPanel = model.getTargetPanel();
        if (model.isSingle()) {
            presentation.append(model.getSource().getPresentation() + ARROW);
            targetPanel.render(presentation, false, true, null);
            return;
        }

        TextAttribute details = active(model.isSelected(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
        presentation.append(model.getRepositoryName() + "  ", details);
        presentation.append(model.getSource().getPresentation() + ARROW, details);
        String syncText = mySyncStrategy && myTree.isEditing() ? mySyncRenderedText : null;
        targetPanel.render(presentation, false, model.isSelected(), syncText);
    }

    private static TextAttribute active(boolean active, SimpleTextAttributes attributes) {
        return TextAttributeUtil.toTextAttribute(active ? attributes : SimpleTextAttributes.GRAYED_ATTRIBUTES);
    }

    @RequiredUIAccess
    private void startSyncEditing() {
        mySyncStrategy = true;
        PushRepoModel<?, ?, ?> modelToEdit = getFirstNodeToEdit();
        TreeNode<Object> node = modelToEdit == null ? null : myNodes.get(modelToEdit);
        if (node == null || !myTree.startEditing(node)) {
            resetEditSync();
        }
    }

    private @Nullable PushRepoModel<?, ?, ?> getFirstNodeToEdit() {
        TreeNode<Object> selected = myTree.getSelectedNode();
        if (selected != null && selected.getValue() instanceof PushRepoModel<?, ?, ?> model && model.isEditable()) {
            return model;
        }
        for (PushRepoModel<?, ?, ?> model : myModels) {
            if (model.isEditable()) {
                return model;
            }
        }
        return null;
    }

    @RequiredUIAccess
    private void onSelectionChanges() {
        List<TreeNode<Object>> selected = myTree.getSelectedNodes();
        for (TreeNode<Object> node : selected) {
            if (node.getValue() instanceof PushMoreCommitsRow more) {
                myController.addMoreCommits(more.repository());
                return;
            }
        }

        List<PushCommitRow> commits = getSelectedCommitRows();
        myChangesBrowser.setEmptyText(commits.isEmpty() ? DistributedVcsLocalize.pushNoCommitsSelected() : DistributedVcsLocalize.pushNoDifferences());
        myChangesBrowser.setChanges(collectAllChanges(commits));
        updateDetailsPanel(commits, selected);
    }

    @RequiredUIAccess
    private void updateDetailsPanel(List<PushCommitRow> commits, List<TreeNode<Object>> selected) {
        boolean noRepositories = true;
        for (TreeNode<Object> node : selected) {
            if (node.getValue() instanceof PushRepoModel<?, ?, ?>) {
                noRepositories = false;
                break;
            }
        }

        myDetailsAvailable = commits.size() == 1 && noRepositories;
        if (myDetailsAvailable) {
            myDetailsPanel.setCommit(commits.get(0).commit());
        }
        updateDetailsVisibility();
    }

    @RequiredUIAccess
    private void updateDetailsVisibility() {
        myDetailsSplitter.setSecondComponent(myDetailsAvailable && mySettings.getShowDetailsInPushDialog() ? myDetailsPanel.getComponent() : null);
    }

    private static List<Change> collectAllChanges(List<PushCommitRow> commits) {
        List<Change> changes = new ArrayList<>();
        for (PushCommitRow row : commits) {
            changes.addAll(row.commit().getChanges());
        }
        return ChangesBrowserUtil.zipChanges(changes);
    }

    private List<PushCommitRow> getSelectedCommitRows() {
        Set<PushCommitRow> rows = new LinkedHashSet<>();
        for (TreeNode<Object> node : myTree.getSelectedNodes()) {
            if (node.getValue() instanceof PushRepoModel<?, ?, ?> model) {
                List<Object> children = model.getRows();
                for (int i = children.size() - 1; i >= 0; i--) {
                    if (children.get(i) instanceof PushCommitRow row) {
                        rows.add(row);
                    }
                }
            }
        }
        for (TreeNode<Object> node : myTree.getSelectedNodes()) {
            if (node.getValue() instanceof PushCommitRow row) {
                rows.add(row);
            }
        }
        return new ArrayList<>(rows);
    }

    private void uiDataSnapshot(DataSink sink) {
        List<PushCommitRow> commits = getSelectedCommitRows();
        sink.set(VcsDataKeys.CHANGES, collectAllChanges(commits).toArray(Change[]::new));

        List<VcsRevisionNumber> revisions = new ArrayList<>();
        for (PushCommitRow row : commits) {
            Hash hash = row.commit().getId();
            revisions.add(new TextRevisionNumber(hash.asString(), hash.toShortString()));
        }
        sink.set(VcsDataKeys.VCS_REVISION_NUMBERS, revisions.toArray(VcsRevisionNumber[]::new));
    }

    @RequiredUIAccess
    public void highlightNodeOrFirst(@Nullable PushRepoModel<?, ?, ?> model) {
        Object target = model != null ? model : (myModels.isEmpty() ? null : myModels.get(0));
        if (target == null) {
            return;
        }
        select(target);
    }

    @RequiredUIAccess
    public void selectIfNothingSelected(PushRepoModel<?, ?, ?> model) {
        if (myTree.getSelectedNodes().isEmpty()) {
            select(model);
        }
    }

    @RequiredUIAccess
    private void select(Object value) {
        TreeNode<Object> root = myTree.getRootNode();
        if (root == null) {
            return;
        }
        UIAccess uiAccess = UIAccess.current();
        root.findChild(child -> child == value).whenCompleteAsync((node, error) -> {
            if (node != null) {
                myTree.select(node);
            }
        }, uiAccess);
    }

    @RequiredUIAccess
    public void setChecked(PushRepoModel<?, ?, ?> model, boolean checked) {
        myUpdatingChecks = true;
        try {
            myTree.setChecked(model, checked);
        }
        finally {
            myUpdatingChecks = false;
        }
        refreshRepository(model, true);
    }

    @RequiredUIAccess
    public void syncChecks() {
        myUpdatingChecks = true;
        try {
            for (PushRepoModel<?, ?, ?> model : myModels) {
                myTree.setChecked(model, model.isSelected());
            }
        }
        finally {
            myUpdatingChecks = false;
        }
    }

    @RequiredUIAccess
    public boolean isEditing() {
        return myTree.isEditing();
    }

    @RequiredUIAccess
    public void refreshRepository(PushRepoModel<?, ?, ?> model) {
        refreshRepository(model, false);
    }

    @RequiredUIAccess
    private void refreshRepository(PushRepoModel<?, ?, ?> model, boolean children) {
        TreeNode<Object> node = myNodes.get(model);
        if (node != null) {
            myTree.refreshItem(node, children && !myTree.isEditing());
        }
    }

    @RequiredUIAccess
    public void setChildren(PushRepoModel<?, ?, ?> model) {
        if (myTree.isEditing()) {
            myShouldRepaint = true;
            refreshRepository(model, false);
            return;
        }
        refreshNode(model);
        if (isSelected(model)) {
            onSelectionChanges();
        }
    }

    @RequiredUIAccess
    private void expandPending() {
        List<PushRepoModel<?, ?, ?>> pending = new ArrayList<>(myPendingExpand);
        myPendingExpand.clear();
        for (PushRepoModel<?, ?, ?> model : pending) {
            refreshNode(model);
        }
    }

    @RequiredUIAccess
    private void refreshNode(PushRepoModel<?, ?, ?> model) {
        TreeNode<Object> node = myNodes.get(model);
        if (node == null) {
            if (model.isSelected() && !model.getRows().isEmpty()) {
                myPendingExpand.add(model);
            }
            return;
        }
        myTree.refreshItem(node, true);
        if (model.isSelected() && !model.getRows().isEmpty()) {
            myTree.expand(node);
        }
    }

    @RequiredUIAccess
    private void refreshAll() {
        for (PushRepoModel<?, ?, ?> model : myModels) {
            refreshNode(model);
        }
        myShouldRepaint = false;
    }

    private boolean isSelected(PushRepoModel<?, ?, ?> model) {
        for (TreeNode<Object> node : myTree.getSelectedNodes()) {
            if (node.getValue() == model) {
                return true;
            }
        }
        return false;
    }

    @RequiredUIAccess
    public void fireEditorUpdated(String currentText) {
        if (mySyncStrategy) {
            for (PushRepoModel<?, ?, ?> model : myModels) {
                if (model.isEditable()) {
                    model.getTargetPanel().forceUpdateEditableUiModel(currentText);
                }
            }
            mySyncRenderedText = currentText;
            for (PushRepoModel<?, ?, ?> model : myModels) {
                refreshRepository(model, false);
            }
        }
    }

    private void resetEditSync() {
        mySyncStrategy = false;
        mySyncRenderedText = null;
    }

    @RequiredUIAccess
    private void showValidation(LocalizeValue message) {
        myValidation.setText(message);
        myValidation.setVisible(message.isNotEmpty());
    }

    @RequiredUIAccess
    private void editingFinished() {
        showValidation(LocalizeValue.empty());
        if (myShouldRepaint) {
            refreshAll();
        }
        else {
            for (PushRepoModel<?, ?, ?> model : myModels) {
                refreshRepository(model, false);
            }
        }
        myController.getDialog().updateOkActions();
    }

    private class TargetEditor implements TreeNodeEditor<Object> {
        @Override
        public boolean isEditable(Object value) {
            return value instanceof PushRepoModel<?, ?, ?> model && model.isEditable();
        }

        @RequiredUIAccess
        @Override
        public Component createEditor(Object value) {
            PushRepoModel<?, ?, ?> model = (PushRepoModel<?, ?, ?>) value;
            PushTargetPanel<?> targetPanel = model.getTargetPanel();

            HorizontalLayout layout = HorizontalLayout.create(Space.SMALL);
            if (!model.isSingle()) {
                Label name = Label.create(LocalizeValue.of(model.getRepositoryName()));
                name.setForegroundColor(ComponentColors.DISABLED_TEXT);
                layout.add(name);
            }
            if (targetPanel.showSourceWhenEditing()) {
                layout.add(Label.create(LocalizeValue.of(model.getSource().getPresentation() + ARROW)));
            }
            layout.add(targetPanel.getComponent());
            return layout;
        }

        @RequiredUIAccess
        @Override
        public void editingStarted(Object value, Component editor) {
            ((PushRepoModel<?, ?, ?>) value).getTargetPanel().editingStarted();
            myController.getDialog().updateOkActions();
        }

        @RequiredUIAccess
        @Override
        @SuppressWarnings("unchecked")
        public boolean stopEditing(Object value, Component editor) {
            PushRepoModel<Repository, PushSource, PushTarget> model = (PushRepoModel<Repository, PushSource, PushTarget>) value;
            LocalizeValue error = model.getTargetPanel().verify();
            if (error.isNotEmpty()) {
                showValidation(error);
                return false;
            }

            if (mySyncStrategy) {
                resetEditSync();
                for (PushRepoModel<Repository, PushSource, PushTarget> other : myModels) {
                    myController.fireOnChange(other);
                }
            }
            else {
                myController.fireOnChange(model);
            }
            UIAccess.current().give(PushLog.this::editingFinished);
            return true;
        }

        @RequiredUIAccess
        @Override
        public void cancelEditing(Object value, Component editor) {
            ((PushRepoModel<?, ?, ?>) value).getTargetPanel().fireOnCancel();
            resetEditSync();
            UIAccess.current().give(PushLog.this::editingFinished);
        }
    }

    private class ShowDetailsAction extends DumbAwareToggleAction {
        private ShowDetailsAction() {
            super(DistributedVcsLocalize.pushShowDetails(), LocalizeValue.empty(), PlatformIconGroup.actionsPreviewdetails());
        }

        @Override
        public void update(AnActionEvent e) {
            super.update(e);
            e.getPresentation().setEnabled(myDetailsAvailable);
        }

        @Override
        public boolean isSelected(AnActionEvent e) {
            return mySettings.getShowDetailsInPushDialog();
        }

        @RequiredUIAccess
        @Override
        public void setSelected(AnActionEvent e, boolean state) {
            mySettings.setShowDetailsInPushDialog(state);
            updateDetailsVisibility();
        }
    }
}
