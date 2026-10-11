/*
 * Copyright 2013-2026 consulo.io
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
package consulo.language.editor.impl.internal.intention.setting;

import consulo.application.Application;
import consulo.disposer.Disposable;
import consulo.language.editor.internal.intention.IntentionActionMetaData;
import consulo.language.editor.internal.intention.IntentionManagerSettings;
import consulo.language.editor.localize.CodeInsightLocalize;
import consulo.language.editor.ui.EditorBoxBuilderFactory;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.CheckBoxTreePolicy;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.Space;
import consulo.ui.TextBox;
import consulo.ui.TextBoxStyle;
import consulo.ui.TreeNode;
import consulo.ui.TriStateCheckBoxTree;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.TreeCheckEvent;
import consulo.ui.event.details.ProgrammaticInputDetails;
import consulo.ui.ex.tree.ApplicationTreeExecutorFactory;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.layout.SplitLayoutPosition;
import consulo.ui.layout.TwoComponentSplitLayout;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ThreeState;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author VISTALL
 * @since 2026-10-11
 */
public class IntentionSettingsPanel implements Disposable {
    private static final long FILTER_DELAY = 300;
    private static final int TREE_PROPORTION = 40;

    private final IntentionManagerSettings mySettings;
    private final Map<IntentionActionMetaData, Boolean> myStates = new HashMap<>();

    private final IntentionTreeModel myModel = new IntentionTreeModel();
    private final TriStateCheckBoxTree<IntentionTreeNode> myTree;
    private final TextBox myFilter;
    private final IntentionDescriptionPanel myDescription;
    private final TwoComponentSplitLayout myRoot;

    private @Nullable ScheduledFuture<?> myQueuedFilter;
    private boolean mySyncQueued;
    private boolean myDisposed;

    @RequiredUIAccess
    public IntentionSettingsPanel(Application application) {
        mySettings = IntentionManagerSettings.getInstance();

        myTree = TriStateCheckBoxTree.create(
            myModel,
            application.getInstance(ApplicationTreeExecutorFactory.class).forBackgroundThreadWithoutReadAction(this)
        );
        myTree.setCheckPolicy(CheckBoxTreePolicy.INDEPENDENT);
        myTree.setAllowMultipleSelect(true);
        myTree.addCheckListener(this::onCheck);
        myTree.addSelectListener(event -> onSelected(event.getValue()));

        myFilter = TextBox.create();
        myFilter.setPlaceholder(CodeInsightLocalize.intentionSettingsFilterPlaceholder());
        myFilter.addStyle(TextBoxStyle.HAS_CLEAR_BUTTON);
        Label searchIcon = Label.create();
        searchIcon.setImage(PlatformIconGroup.actionsSearch());
        myFilter.setPrefixComponent(searchIcon);
        myFilter.addValueListener(event -> queueFilter(StringUtil.notNullize(event.getValue())));

        DockLayout left = DockLayout.create(Space.SMALL);
        left.top(myFilter);
        left.center(ScrollableLayout.create(myTree));
        left.paddingBuilder().allSet(Space.MEDIUM).apply();

        myDescription = new IntentionDescriptionPanel(application.getInstance(EditorBoxBuilderFactory.class));

        myRoot = TwoComponentSplitLayout.create(SplitLayoutPosition.HORIZONTAL);
        myRoot.setFirstComponent(left);
        myRoot.setSecondComponent(myDescription.getComponent());
        myRoot.setProportion(TREE_PROPORTION);
    }

    public Component getComponent() {
        return myRoot;
    }

    public Component getPreferredFocusedComponent() {
        return myTree;
    }

    @RequiredUIAccess
    public void reset() {
        myStates.clear();
        refilter(StringUtil.notNullize(myFilter.getValue()));
    }

    public boolean isModified() {
        for (Map.Entry<IntentionActionMetaData, Boolean> entry : myStates.entrySet()) {
            if (mySettings.isEnabled(entry.getKey()) != entry.getValue()) {
                return true;
            }
        }
        return false;
    }

    public void apply() {
        for (Map.Entry<IntentionActionMetaData, Boolean> entry : myStates.entrySet()) {
            mySettings.setEnabled(entry.getKey(), entry.getValue());
        }
    }

    @RequiredUIAccess
    public void showOption(String option) {
        myFilter.setValue(option);
    }

    @RequiredUIAccess
    public void selectIntention(LocalizeValue actionText) {
        TreeNode<IntentionTreeNode> root = myTree.getRootNode();
        if (root == null) {
            return;
        }

        UIAccess uiAccess = UIAccess.current();
        root.findChildDeep(value -> value instanceof IntentionLeafNode leaf && leaf.getText().equals(actionText))
            .whenCompleteAsync((node, error) -> {
                if (node != null && !myDisposed) {
                    myTree.select(node);
                }
            }, uiAccess);
    }

    private boolean isEnabled(IntentionActionMetaData metaData) {
        return myStates.computeIfAbsent(metaData, mySettings::isEnabled);
    }

    @RequiredUIAccess
    private void queueFilter(String text) {
        ScheduledFuture<?> queued = myQueuedFilter;
        if (queued != null) {
            queued.cancel(false);
        }

        myQueuedFilter = UIAccess.current().getScheduler().schedule(() -> {
            myQueuedFilter = null;
            if (!myDisposed) {
                refilter(text);
            }
        }, FILTER_DELAY, TimeUnit.MILLISECONDS);
    }

    @RequiredUIAccess
    private void refilter(String text) {
        myModel.setFilter(text);

        UIAccess uiAccess = UIAccess.current();
        myTree.refreshAll().thenComposeAsync(ignored -> {
            TreeNode<IntentionTreeNode> root = myTree.getRootNode();
            return root == null ? CompletableFuture.completedFuture(null) : root.findChild(value -> true);
        }, uiAccess).whenCompleteAsync((value, error) -> {
            if (myDisposed) {
                return;
            }

            syncChecks();

            if (myModel.getFilter() == null) {
                myTree.collapseAll();
            }
            else {
                myTree.expandAll();
            }
        }, uiAccess);
    }

    @RequiredUIAccess
    private void onCheck(TreeCheckEvent<IntentionTreeNode> event) {
        if (event.getInputDetails() instanceof ProgrammaticInputDetails) {
            return;
        }

        boolean enabled = event.getState() == ThreeState.YES;
        IntentionTreeNode value = event.getValue();
        if (value instanceof IntentionLeafNode leaf) {
            myStates.put(leaf.metaData(), enabled);
        }
        else if (value instanceof IntentionGroupNode group) {
            for (IntentionActionMetaData metaData : myModel.getVisible()) {
                if (IntentionTreeModel.startsWith(metaData.myCategory, group.path())) {
                    myStates.put(metaData, enabled);
                }
            }
        }

        queueSync();
    }

    @RequiredUIAccess
    private void queueSync() {
        if (mySyncQueued) {
            return;
        }

        mySyncQueued = true;
        UIAccess.current().execute(() -> {
            mySyncQueued = false;
            if (!myDisposed) {
                syncChecks();
            }
        });
    }

    @RequiredUIAccess
    private void syncChecks() {
        Map<IntentionGroupNode, boolean[]> groups = new LinkedHashMap<>();

        for (IntentionActionMetaData metaData : myModel.getVisible()) {
            boolean enabled = isEnabled(metaData);
            myTree.setCheckState(new IntentionLeafNode(metaData), enabled ? ThreeState.YES : ThreeState.NO);

            String[] category = metaData.myCategory;
            List<String> path = new ArrayList<>(category.length);
            for (String part : category) {
                path.add(part);

                boolean[] state = groups.computeIfAbsent(new IntentionGroupNode(List.copyOf(path)), group -> new boolean[2]);
                state[enabled ? 0 : 1] = true;
            }
        }

        for (Map.Entry<IntentionGroupNode, boolean[]> entry : groups.entrySet()) {
            boolean[] state = entry.getValue();
            ThreeState groupState = state[0] && state[1] ? ThreeState.UNSURE : state[0] ? ThreeState.YES : ThreeState.NO;
            myTree.setCheckState(entry.getKey(), groupState);
        }
    }

    @RequiredUIAccess
    private void onSelected(@Nullable TreeNode<IntentionTreeNode> node) {
        IntentionTreeNode value = node == null ? null : node.getValue();
        if (value instanceof IntentionLeafNode leaf) {
            myDescription.showIntention(leaf.metaData(), myModel.getFilter());
        }
        else if (value instanceof IntentionGroupNode group) {
            myDescription.showCategory(group.getText());
        }
    }

    @Override
    public void dispose() {
        myDisposed = true;

        ScheduledFuture<?> queued = myQueuedFilter;
        if (queued != null) {
            queued.cancel(false);
        }
    }
}
