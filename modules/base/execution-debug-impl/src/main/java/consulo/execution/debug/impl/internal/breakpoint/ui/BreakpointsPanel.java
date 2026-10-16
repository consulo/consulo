/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.execution.debug.impl.internal.breakpoint.ui;

import consulo.codeEditor.util.popup.ItemWrapper;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.execution.debug.XDebuggerManager;
import consulo.execution.debug.breakpoint.XBreakpoint;
import consulo.execution.debug.breakpoint.XBreakpointType;
import consulo.execution.debug.breakpoint.ui.XBreakpointGroup;
import consulo.execution.debug.breakpoint.ui.XBreakpointGroupingRule;
import consulo.execution.debug.impl.internal.action.handler.XBreakpointsDialogState;
import consulo.execution.debug.impl.internal.breakpoint.XBreakpointBase;
import consulo.execution.debug.impl.internal.breakpoint.XBreakpointManagerImpl;
import consulo.execution.debug.impl.internal.breakpoint.XBreakpointUtil;
import consulo.execution.debug.localize.XDebuggerLocalize;
import consulo.language.editor.ui.DetailViewImpl;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.CheckBoxTree;
import consulo.ui.CheckBoxTreePolicy;
import consulo.ui.Component;
import consulo.ui.Tree;
import consulo.ui.TreeModel;
import consulo.ui.TreeNode;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ContextMenuEvent;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.toolbar.AddAction;
import consulo.ui.ex.toolbar.DownMoveAction;
import consulo.ui.ex.toolbar.EditAction;
import consulo.ui.ex.toolbar.RemoveAction;
import consulo.ui.ex.toolbar.ToolbarDecoratorBuilder;
import consulo.ui.ex.toolbar.ToolbarDecoratorBuilderFactory;
import consulo.ui.ex.toolbar.UpMoveAction;
import consulo.ui.layout.SplitLayoutPosition;
import consulo.ui.layout.TwoComponentSplitLayout;
import consulo.util.collection.ContainerUtil;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class BreakpointsPanel {
    private static final int DETAIL_UPDATE_DELAY_MS = 100;
    private static final int MASTER_PROPORTION = 30;

    private final Project myProject;
    private @Nullable Object myRequestedBreakpoint;
    private boolean myBuilt;
    private final List<BreakpointPanelProvider> myBreakpointsPanelProviders;
    private final Runnable myOnDispose;

    private final List<BreakpointItem> myBreakpointItems = new ArrayList<>();
    private final List<XBreakpointGroupingRule> myRulesAvailable = new ArrayList<>();
    private final Set<XBreakpointGroupingRule> myRulesEnabled = new TreeSet<>(XBreakpointGroupingRule.PRIORITY_COMPARATOR);
    private final List<ToggleAction> myToggleRuleActions = new ArrayList<>();

    private Map<@Nullable Object, List<Object>> myChildren = Map.of();

    private @Nullable CheckBoxTree<Object> myTree;
    private @Nullable DetailViewImpl myDetailView;
    private boolean myInBuild;
    private int myBuildStamp;
    private boolean myRebuildScheduled;
    private boolean myRebuildReloadsDetail;
    private int myDetailUpdateStamp;

    public BreakpointsPanel(Project project, List<BreakpointPanelProvider> providers, Runnable onDispose) {
        myProject = project;
        myBreakpointsPanelProviders = providers;
        myOnDispose = onDispose;

        collectGroupingRules();
        collectItems(null);
    }

    private XBreakpointManagerImpl getBreakpointManager() {
        return (XBreakpointManagerImpl) XDebuggerManager.getInstance(myProject).getBreakpointManager();
    }

    public @Nullable Component getPreferredFocusedComponent() {
        return myTree;
    }

    @RequiredUIAccess
    public Component createComponent(Disposable uiDisposable) {
        DetailViewImpl detailView = new DetailViewImpl(myProject);
        myDetailView = detailView;

        CheckBoxTree<Object> tree = CheckBoxTree.create(createModel());
        Disposer.register(uiDisposable, tree.destroyHook());
        tree.setCheckPolicy(CheckBoxTreePolicy.FULL_SYNC);
        tree.setAllowMultipleSelect(true);
        myTree = tree;

        tree.addCheckListener(event -> onCheckChanged());
        tree.addSelectListener(event -> {
            if (!myInBuild) {
                saveCurrentItem();
                updateDetailView(false);
            }
        });
        tree.addContextMenuListener(this::showContextMenu);

        new NavigateAction().registerCustomShortcutSet(CustomShortcutSet.fromString("ENTER"), tree, uiDisposable);
        new NavigateAction().registerCustomShortcutSet(
            ActionManager.getInstance().getAction(IdeActions.ACTION_EDIT_SOURCE).getShortcutSet(),
            tree,
            uiDisposable
        );

        ToolbarDecoratorBuilderFactory decoratorFactory = myProject.getApplication().getInstance(ToolbarDecoratorBuilderFactory.class);
        ToolbarDecoratorBuilder<Object> decorator = decoratorFactory.create(tree)
            .addOrReplaceAction(new AddBreakpointAction())
            .addOrReplaceAction(new RemoveBreakpointAction())
            .disableAction(EditAction.class)
            .disableAction(UpMoveAction.class)
            .disableAction(DownMoveAction.class)
            .withToolbarPosition(ActionToolbarPosition.TOP);
        for (ToggleAction action : myToggleRuleActions) {
            decorator.addExtraAction(action);
        }

        TwoComponentSplitLayout layout = TwoComponentSplitLayout.create(SplitLayoutPosition.HORIZONTAL);
        layout.setProportion(MASTER_PROPORTION);
        layout.setFirstComponent(decorator.build());
        layout.setSecondComponent(detailView.getUIComponent());

        BreakpointPanelProvider.BreakpointsListener listener = breakpoint -> UIAccess.current().giveIfNeed(() -> scheduleRebuild(breakpoint));
        Disposable listenerDisposable = Disposable.newDisposable();
        Disposer.register(uiDisposable, listenerDisposable);
        for (BreakpointPanelProvider provider : myBreakpointsPanelProviders) {
            provider.addListener(listener, myProject, listenerDisposable);
        }

        Disposer.register(uiDisposable, this::dispose);

        UIAccess uiAccess = UIAccess.current();
        rebuildTree(false, true).whenComplete((result, error) -> uiAccess.giveIfNeed(() -> {
            myBuilt = true;
            initSelection();
        }));

        return layout;
    }

    private TreeModel<Object> createModel() {
        return new TreeModel<>() {
            @Override
            public void buildChildren(Function<Object, TreeNode<Object>> nodeFactory, @Nullable Object parentValue) {
                for (Object child : myChildren.getOrDefault(parentValue, List.of())) {
                    TreeNode<Object> node = nodeFactory.apply(child);
                    if (child instanceof BreakpointGroupPath path) {
                        XBreakpointGroup group = path.group();
                        node.setLeaf(false);
                        node.setRenderer((value, presentation) -> {
                            presentation.withIcon(group.getIcon(true));
                            presentation.append(group.getName());
                        });
                    }
                    else if (child instanceof BreakpointItem item) {
                        node.setLeaf(true);
                        node.setRenderer((value, presentation) -> item.setupRenderer(presentation));
                    }
                }
            }

            @Override
            public boolean onDoubleClick(Tree<Object> tree, TreeNode<Object> node) {
                if (node.getValue() instanceof BreakpointItem) {
                    navigate(false);
                    return false;
                }
                return true;
            }
        };
    }

    @RequiredUIAccess
    private void scheduleRebuild(Object changedBreakpoint) {
        BreakpointItem current = getCurrentItem();
        if (current != null && current.getBreakpoint() == changedBreakpoint) {
            myRebuildReloadsDetail = true;
        }
        if (myRebuildScheduled) {
            return;
        }
        myRebuildScheduled = true;

        UIAccess.current().give(() -> {
            myRebuildScheduled = false;
            boolean reloadDetail = myRebuildReloadsDetail;
            myRebuildReloadsDetail = false;
            if (myTree == null) {
                return;
            }
            collectItems(reloadDetail ? null : getCurrentItem());
            rebuildTree(true, reloadDetail);
        });
    }

    private void collectItems(@Nullable BreakpointItem keep) {
        if (myBreakpointsPanelProviders.isEmpty()) {
            return;
        }
        for (BreakpointItem item : myBreakpointItems) {
            if (item != keep) {
                item.dispose();
            }
        }
        myBreakpointItems.clear();
        for (BreakpointPanelProvider panelProvider : myBreakpointsPanelProviders) {
            panelProvider.provideBreakpointItems(myProject, myBreakpointItems);
        }

        if (keep != null) {
            int index = myBreakpointItems.indexOf(keep);
            if (index >= 0) {
                myBreakpointItems.get(index).dispose();
                myBreakpointItems.set(index, keep);
            }
            else {
                keep.dispose();
            }
        }
    }

    private @Nullable BreakpointItem getCurrentItem() {
        DetailViewImpl detailView = myDetailView;
        return detailView != null && detailView.getCurrentItem() instanceof BreakpointItem item ? item : null;
    }

    @SuppressWarnings("unchecked")
    private void collectStructure() {
        Map<@Nullable Object, List<Object>> children = new HashMap<>();
        for (BreakpointItem item : myBreakpointItems) {
            Object parent = null;
            List<XBreakpointGroup> path = new ArrayList<>();
            for (XBreakpointGroupingRule rule : myRulesEnabled) {
                XBreakpointGroup group = rule.getGroup(item.getBreakpoint(), Collections.emptyList());
                if (group == null) {
                    continue;
                }
                path.add(group);
                BreakpointGroupPath groupPath = new BreakpointGroupPath(List.copyOf(path));
                List<Object> siblings = children.computeIfAbsent(parent, key -> new ArrayList<>());
                if (!siblings.contains(groupPath)) {
                    siblings.add(groupPath);
                }
                parent = groupPath;
            }
            children.computeIfAbsent(parent, key -> new ArrayList<>()).add(item);
        }

        for (List<Object> list : children.values()) {
            list.sort(BreakpointsPanel::compareValues);
        }
        myChildren = children;
    }

    private static int compareValues(Object o1, Object o2) {
        if (o1 instanceof BreakpointItem b1 && o2 instanceof BreakpointItem b2) {
            boolean default1 = b1.isDefaultBreakpoint();
            boolean default2 = b2.isDefaultBreakpoint();
            if (default1 != default2) {
                return default1 ? -1 : 1;
            }
            return b1.compareTo(b2);
        }
        if (o1 instanceof BreakpointGroupPath g1 && o2 instanceof BreakpointGroupPath g2) {
            if (g1.level() != g2.level()) {
                return g1.level() - g2.level();
            }
            return g1.group().compareTo(g2.group());
        }
        return o1 instanceof BreakpointGroupPath ? -1 : 1;
    }

    @RequiredUIAccess
    private CompletableFuture<?> rebuildTree(boolean keepSelection, boolean reloadDetail) {
        CheckBoxTree<Object> tree = myTree;
        if (tree == null) {
            return CompletableFuture.completedFuture(null);
        }

        List<BreakpointItem> selected = keepSelection ? getSelectedBreakpoints(false) : List.of();

        collectStructure();

        UIAccess uiAccess = UIAccess.current();
        int stamp = ++myBuildStamp;
        myInBuild = true;
        return tree.refreshAll()
            .thenCompose(result -> tree.expandAll())
            .whenCompleteAsync((result, error) -> {
                if (stamp != myBuildStamp) {
                    return;
                }
                try {
                    for (BreakpointItem item : myBreakpointItems) {
                        tree.setChecked(item, item.isEnabled());
                    }
                }
                finally {
                    myInBuild = false;
                }

                if (keepSelection) {
                    BreakpointItem target = ContainerUtil.find(selected, myBreakpointItems::contains);
                    if (target == null && firstItem(null) instanceof BreakpointItem first) {
                        target = first;
                    }
                    if (target != null) {
                        selectBreakpointItem(target);
                    }
                }

                if (reloadDetail) {
                    DetailViewImpl detailView = myDetailView;
                    if (detailView != null) {
                        detailView.setCurrentItem(null);
                    }
                    updateDetailView(true);
                }
            }, uiAccess);
    }

    @RequiredUIAccess
    public void requestSelection(@Nullable Object breakpoint) {
        if (breakpoint == null) {
            return;
        }
        if (myBuilt) {
            selectBreakpoint(breakpoint);
        }
        else {
            myRequestedBreakpoint = breakpoint;
        }
    }

    @RequiredUIAccess
    private void initSelection() {
        Object requested = myRequestedBreakpoint;
        myRequestedBreakpoint = null;
        if (requested != null && selectBreakpoint(requested)) {
            return;
        }
        if (!myBreakpointItems.isEmpty()) {
            Object first = firstItem(null);
            if (first instanceof BreakpointItem item) {
                selectBreakpointItem(item);
            }
        }
    }

    private @Nullable Object firstItem(@Nullable Object parent) {
        for (Object child : myChildren.getOrDefault(parent, List.of())) {
            if (child instanceof BreakpointItem) {
                return child;
            }
            Object nested = firstItem(child);
            if (nested != null) {
                return nested;
            }
        }
        return null;
    }

    @RequiredUIAccess
    private boolean selectBreakpoint(Object breakpoint) {
        for (BreakpointItem item : myBreakpointItems) {
            if (item.getBreakpoint() == breakpoint) {
                selectBreakpointItem(item);
                return true;
            }
        }
        return false;
    }

    @RequiredUIAccess
    private void selectBreakpointItem(BreakpointItem item) {
        CheckBoxTree<Object> tree = myTree;
        TreeNode<Object> root = tree == null ? null : tree.getRootNode();
        if (root == null) {
            return;
        }

        UIAccess uiAccess = UIAccess.current();
        root.findChildDeep(item::equals).whenCompleteAsync((node, error) -> {
            if (node != null) {
                tree.select(node);
            }
        }, uiAccess);
    }

    @RequiredUIAccess
    private void onCheckChanged() {
        CheckBoxTree<Object> tree = myTree;
        if (myInBuild || tree == null) {
            return;
        }

        boolean changed = false;
        for (BreakpointItem item : List.copyOf(myBreakpointItems)) {
            boolean checked = tree.isChecked(item);
            if (checked != item.isEnabled()) {
                item.saveState();
                item.setEnabled(checked);
                changed = true;
            }
        }

        if (changed) {
            updateDetailView(true);
        }
    }

    @RequiredUIAccess
    private List<BreakpointItem> getSelectedBreakpoints(boolean traverse) {
        CheckBoxTree<Object> tree = myTree;
        if (tree == null) {
            return List.of();
        }

        List<BreakpointItem> result = new ArrayList<>();
        for (TreeNode<Object> node : tree.getSelectedNodes()) {
            Object value = node.getValue();
            if (value instanceof BreakpointItem item) {
                if (!result.contains(item)) {
                    result.add(item);
                }
            }
            else if (traverse && value != null) {
                collectItems(value, result);
            }
        }
        return result;
    }

    private void collectItems(Object parent, List<BreakpointItem> result) {
        for (Object child : myChildren.getOrDefault(parent, List.of())) {
            if (child instanceof BreakpointItem item) {
                if (!result.contains(item)) {
                    result.add(item);
                }
            }
            else {
                collectItems(child, result);
            }
        }
    }

    @RequiredUIAccess
    private void updateDetailView(boolean now) {
        int stamp = ++myDetailUpdateStamp;
        if (now) {
            doUpdateDetailView();
            return;
        }

        UIAccess uiAccess = UIAccess.current();
        uiAccess.getScheduler().schedule(() -> uiAccess.give(() -> {
            if (stamp == myDetailUpdateStamp) {
                doUpdateDetailView();
            }
        }), DETAIL_UPDATE_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    @RequiredUIAccess
    private void doUpdateDetailView() {
        DetailViewImpl detailView = myDetailView;
        if (detailView == null) {
            return;
        }

        List<BreakpointItem> selected = getSelectedBreakpoints(false);
        if (selected.size() == 1) {
            selected.get(0).updateDetailView(detailView);
        }
        else {
            detailView.clearEditor();
            detailView.setPropertiesPanel(null);
            detailView.setCurrentItem(null);
        }
    }

    @RequiredUIAccess
    private void navigate(boolean requestFocus) {
        List<BreakpointItem> breakpoints = getSelectedBreakpoints(false);
        if (!breakpoints.isEmpty()) {
            breakpoints.get(0).navigate(requestFocus);
        }
    }

    @RequiredUIAccess
    private void showContextMenu(ContextMenuEvent event) {
        CheckBoxTree<Object> tree = myTree;
        if (tree == null) {
            return;
        }

        ActionGroup moveToGroup = new ActionGroup(XDebuggerLocalize.breakpointsDialogMoveToGroup(), true) {
            @Override
            public AnAction[] getChildren(@Nullable AnActionEvent e) {
                Set<String> groups = getBreakpointManager().getAllGroups();
                List<AnAction> actions = new ArrayList<>();
                actions.add(new MoveToGroupAction(null));
                for (String group : groups) {
                    actions.add(new MoveToGroupAction(group));
                }
                actions.add(AnSeparator.create());
                actions.add(new MoveToGroupAction());
                return actions.toArray(AnAction.EMPTY_ARRAY);
            }
        };

        ActionPopupMenu menu = ActionManager.getInstance()
            .createActionPopupMenu(ActionPlaces.UNKNOWN, ActionGroup.newImmutableBuilder().add(moveToGroup).build());
        menu.setTargetComponent(tree);
        menu.show(event.getComponent(), event.getInputDetails().getX(), event.getInputDetails().getY());
    }

    private void collectGroupingRules() {
        for (BreakpointPanelProvider provider : myBreakpointsPanelProviders) {
            provider.createBreakpointsGroupingRules(myRulesAvailable);
        }
        myRulesAvailable.sort(XBreakpointGroupingRule.PRIORITY_COMPARATOR);

        myRulesEnabled.clear();
        XBreakpointsDialogState settings = getBreakpointManager().getBreakpointsDialogSettings();

        for (XBreakpointGroupingRule rule : myRulesAvailable) {
            if (rule.isAlwaysEnabled() || (settings != null && settings.getSelectedGroupingRules().contains(rule.getId()))) {
                myRulesEnabled.add(rule);
            }
        }

        for (XBreakpointGroupingRule rule : myRulesAvailable) {
            if (!rule.isAlwaysEnabled()) {
                myToggleRuleActions.add(new ToggleBreakpointGroupingRuleEnabledAction(rule));
            }
        }
    }

    private void saveBreakpointsDialogState() {
        XBreakpointsDialogState dialogState = new XBreakpointsDialogState();
        List<XBreakpointGroupingRule> rulesEnabled = ContainerUtil.filter(myRulesEnabled, rule -> !rule.isAlwaysEnabled());

        dialogState.setSelectedGroupingRules(new HashSet<>(ContainerUtil.map(rulesEnabled, XBreakpointGroupingRule::getId)));
        getBreakpointManager().setBreakpointsDialogSettings(dialogState);
    }

    @RequiredUIAccess
    private void dispose() {
        myBuilt = false;
        saveCurrentItem();
        saveBreakpointsDialogState();
        disposeItems();

        DetailViewImpl detailView = myDetailView;
        if (detailView != null) {
            detailView.clearEditor();
        }
        myDetailView = null;
        myTree = null;

        myOnDispose.run();
    }

    private void disposeItems() {
        for (BreakpointItem item : myBreakpointItems) {
            item.dispose();
        }
    }

    private void saveCurrentItem() {
        DetailViewImpl detailView = myDetailView;
        ItemWrapper current = detailView == null ? null : detailView.getCurrentItem();
        if (current instanceof BreakpointItem breakpointItem) {
            breakpointItem.saveState();
        }
    }

    private class NavigateAction extends AnAction {
        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            navigate(true);
        }
    }

    private class ToggleBreakpointGroupingRuleEnabledAction extends ToggleAction {
        private final XBreakpointGroupingRule myRule;

        public ToggleBreakpointGroupingRuleEnabledAction(XBreakpointGroupingRule rule) {
            super(rule.getPresentableName(), LocalizeValue.empty(), rule.getIcon());
            myRule = rule;
        }

        @Override
        public boolean isSelected(AnActionEvent e) {
            return myRulesEnabled.contains(myRule);
        }

        @Override
        @RequiredUIAccess
        public void setSelected(AnActionEvent e, boolean state) {
            if (state) {
                myRulesEnabled.add(myRule);
            }
            else {
                myRulesEnabled.remove(myRule);
            }
            rebuildTree(true, false);
        }
    }

    private class AddBreakpointAction extends AddAction<Object> {
        @RequiredUIAccess
        @Override
        protected void doAdd(AnActionEvent e) {
            ActionGroup.Builder breakpointTypes = ActionGroup.newImmutableBuilder();
            for (XBreakpointType<?, ?> type : XBreakpointUtil.getBreakpointTypes()) {
                if (type.isAddBreakpointButtonVisible()) {
                    breakpointTypes.add(new AddXBreakpointAction(type));
                }
            }

            JBPopupFactory.getInstance()
                .createActionGroupPopup(null, breakpointTypes.build(), e.getDataContext(), JBPopupFactory.ActionSelectionAid.NUMBERING, false)
                .showUnderneathOf(e);
        }
    }

    private class RemoveBreakpointAction extends RemoveAction<Object> {
        @RequiredUIAccess
        @Override
        public void updateAtUI(AnActionEvent e) {
            boolean enabled = false;
            for (BreakpointItem item : getSelectedBreakpoints(false)) {
                if (item.allowedToRemove()) {
                    enabled = true;
                    break;
                }
            }
            e.getPresentation().setEnabled(enabled);
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            List<BreakpointItem> breakpoints = new ArrayList<>(getSelectedBreakpoints(true));
            breakpoints.removeIf(item -> !item.allowedToRemove());
            for (BreakpointItem breakpoint : breakpoints) {
                breakpoint.removed(myProject);
            }
        }
    }

    private class AddXBreakpointAction extends AnAction {
        private final XBreakpointType<?, ?> myType;

        public AddXBreakpointAction(XBreakpointType<?, ?> type) {
            super(LocalizeValue.of(type.getTitle()), LocalizeValue.empty(), type.getEnabledIcon());
            myType = type;
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            saveCurrentItem();
            XBreakpoint<?> breakpoint = myType.addBreakpoint(myProject, null);
            if (breakpoint != null) {
                collectItems(getCurrentItem());
                rebuildTree(false, false).whenComplete((result, error) -> UIAccess.current().giveIfNeed(() -> selectBreakpoint(breakpoint)));
            }
        }
    }

    private class MoveToGroupAction extends AnAction {
        private final @Nullable String myGroup;
        private final boolean myNewGroup;

        private MoveToGroupAction(@Nullable String group) {
            super(LocalizeValue.ofNullable(group).orIfEmpty(XDebuggerLocalize.breakpointsDialogNoGroup()));
            myGroup = group;
            myNewGroup = false;
        }

        private MoveToGroupAction() {
            super(XDebuggerLocalize.breakpointsDialogCreateNewGroup());
            myNewGroup = true;
            myGroup = null;
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            String groupName = myGroup;
            if (myNewGroup) {
                groupName = Messages.showInputDialog(
                    XDebuggerLocalize.breakpointsDialogNewGroupName().get(),
                    XDebuggerLocalize.breakpointsDialogNewGroup().get(),
                    PlatformIconGroup.actionsNewfolder()
                );
                if (groupName == null) {
                    return;
                }
            }
            for (BreakpointItem item : getSelectedBreakpoints(true)) {
                if (item.allowedToRemove() && item.getBreakpoint() instanceof XBreakpointBase breakpointBase) {
                    breakpointBase.setGroup(groupName);
                }
            }
            rebuildTree(true, false);
        }
    }
}
