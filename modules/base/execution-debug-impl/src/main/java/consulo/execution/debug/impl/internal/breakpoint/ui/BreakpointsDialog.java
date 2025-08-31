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

import consulo.codeEditor.util.popup.DetailController;
import consulo.codeEditor.util.popup.ItemWrapper;
import consulo.codeEditor.util.popup.MasterController;
import consulo.dataContext.DataManager;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.execution.debug.XDebuggerManager;
import consulo.execution.debug.breakpoint.XBreakpoint;
import consulo.execution.debug.breakpoint.XBreakpointType;
import consulo.execution.debug.breakpoint.ui.XBreakpointGroupingRule;
import consulo.execution.debug.impl.internal.action.handler.XBreakpointsDialogState;
import consulo.execution.debug.impl.internal.breakpoint.XBreakpointBase;
import consulo.execution.debug.impl.internal.breakpoint.XBreakpointManagerImpl;
import consulo.execution.debug.impl.internal.breakpoint.XBreakpointUtil;
import consulo.execution.debug.impl.internal.breakpoint.ui.tree.BreakpointItemNode;
import consulo.execution.debug.impl.internal.breakpoint.ui.tree.BreakpointItemsTreeController;
import consulo.execution.debug.impl.internal.breakpoint.ui.tree.BreakpointsCheckboxTree;
import consulo.language.editor.ui.awt.DetailViewImpl;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.Size2D;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.tree.CheckedTreeNode;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.Couple;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.util.*;

public class BreakpointsDialog extends WholeWestDialogWrapper {
    @Nonnull
    private Project myProject;

    private Object myInitialBreakpoint;
    private List<BreakpointPanelProvider> myBreakpointsPanelProviders;

    private BreakpointItemsTreeController myTreeController;

    private JLabel temp = new JLabel();

    private MasterController myMasterController = new MasterController() {
        @Override
        public ItemWrapper[] getSelectedItems() {
            List<BreakpointItem> res = myTreeController.getSelectedBreakpoints(false);
            return res.toArray(new ItemWrapper[res.size()]);
        }

        @Override
        public JLabel getPathLabel() {
            return temp;
        }
    };

    private final DetailController myDetailController = new DetailController(myMasterController);

    private final Collection<BreakpointItem> myBreakpointItems = new ArrayList<>();

    private final List<XBreakpointGroupingRule> myRulesAvailable = new ArrayList<>();

    private final Set<XBreakpointGroupingRule> myRulesEnabled = new TreeSet<>(XBreakpointGroupingRule.PRIORITY_COMPARATOR);
    private final Disposable myListenerDisposable = Disposable.newDisposable();
    private final List<ToggleAction> myToggleRuleActions = new ArrayList<>();

    private XBreakpointManagerImpl getBreakpointManager() {
        return (XBreakpointManagerImpl) XDebuggerManager.getInstance(myProject).getBreakpointManager();
    }

    protected BreakpointsDialog(@Nonnull Project project, Object breakpoint, @Nonnull List<BreakpointPanelProvider> providers) {
        super(project);
        myProject = project;
        myBreakpointsPanelProviders = providers;
        myInitialBreakpoint = breakpoint;

        collectGroupingRules();

        collectItems();

        setTitle("Breakpoints");
        setModal(false);
        init();
        setOKButtonText("Done");

        myTitlelessDecorator.install(getWindow());
    }

    @Nonnull
    @Override
    public String getSplitterKey() {
        return getDimensionServiceKey() + ".splitter";
    }

    @RequiredUIAccess
    @Nonnull
    @Override
    public Couple<JComponent> createSplitterComponents(JPanel rootPanel) {
        return Couple.of(createMasterView(), createDetailView());
    }

    private JComponent createDetailView() {
        DetailViewImpl detailView = new DetailViewImpl(myProject);
        myDetailController.setDetailView(detailView);

        return detailView;
    }

    void collectItems() {
        if (!myBreakpointsPanelProviders.isEmpty()) {
            disposeItems();
            myBreakpointItems.clear();
            for (BreakpointPanelProvider panelProvider : myBreakpointsPanelProviders) {
                panelProvider.provideBreakpointItems(myProject, myBreakpointItems);
            }
        }
    }

    void initSelection(Collection<BreakpointItem> breakpoints) {
        boolean found = selectBreakpoint(myInitialBreakpoint);
        if (!found && !breakpoints.isEmpty()) {
            myTreeController.selectFirstBreakpointItem();
        }
    }

    @Nullable
    @Override
    protected String getDimensionServiceKey() {
        return getClass().getName();
    }

    @Override
    public Size2D getDefaultSize() {
        return new Size2D(910, 400);
    }

    @Nonnull
    @Override
    protected Action[] createActions() {
        return new Action[]{getOKAction(), getHelpAction()};
    }

    private class ToggleBreakpointGroupingRuleEnabledAction extends ToggleAction {
        private XBreakpointGroupingRule myRule;

        public ToggleBreakpointGroupingRuleEnabledAction(XBreakpointGroupingRule rule) {
            super(rule.getPresentableName(), null, rule.getIcon());
            myRule = rule;
            getTemplatePresentation().setText(rule.getPresentableName());
        }

        @Override
        public boolean isSelected(AnActionEvent e) {
            return myRulesEnabled.contains(myRule);
        }

        @Override
        public void setSelected(AnActionEvent e, boolean state) {
            if (state) {
                myRulesEnabled.add(myRule);
            }
            else {
                myRulesEnabled.remove(myRule);
            }
            myTreeController.setGroupingRules(myRulesEnabled);
        }
    }

    private JComponent createMasterView() {
        myTreeController = new BreakpointItemsTreeController(myRulesEnabled) {
            @Override
            public void nodeStateWillChangeImpl(CheckedTreeNode node) {
                if (node instanceof BreakpointItemNode) {
                    ((BreakpointItemNode) node).getBreakpointItem().saveState();
                }
                super.nodeStateWillChangeImpl(node);
            }

            @Override
            public void nodeStateDidChangeImpl(CheckedTreeNode node) {
                super.nodeStateDidChangeImpl(node);
                if (node instanceof BreakpointItemNode) {
                    myDetailController.doUpdateDetailView(true);
                }
            }

            @Override
            protected void selectionChangedImpl() {
                super.selectionChangedImpl();
                saveCurrentItem();
                myDetailController.updateDetailView();
            }
        };
        JTree tree = new BreakpointsCheckboxTree(myProject, myTreeController) {
            @Override
            protected void onDoubleClick(CheckedTreeNode node) {
                navigate(false);
            }
        };

        PopupHandler.installPopupHandler(
            tree,
            new ActionGroup() {
                @Nonnull
                @Override
                public AnAction[] getChildren(@Nullable AnActionEvent e) {
                    ActionGroup group = new ActionGroup("Move to group", true) {
                        @Nonnull
                        @Override
                        public AnAction[] getChildren(@Nullable AnActionEvent e) {
                            Set<String> groups = getBreakpointManager().getAllGroups();
                            AnAction[] res = new AnAction[groups.size() + 3];
                            int i = 0;
                            res[i++] = new MoveToGroupAction(null);
                            for (String group : groups) {
                                res[i++] = new MoveToGroupAction(group);
                            }
                            res[i++] = AnSeparator.create();
                            res[i] = new MoveToGroupAction();
                            return res;
                        }
                    };
                    return new AnAction[]{group};
                }
            },
            ActionPlaces.UNKNOWN,
            ActionManager.getInstance()
        );

        new AnAction("BreakpointDialog.GoToSource") {
            @RequiredUIAccess
            @Override
            public void actionPerformed(@Nonnull AnActionEvent e) {
                navigate(true);
                close(OK_EXIT_CODE);
            }
        }.registerCustomShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)), tree);

        new AnAction("BreakpointDialog.ShowSource") {
            @RequiredUIAccess
            @Override
            public void actionPerformed(@Nonnull AnActionEvent e) {
                navigate(true);
                close(OK_EXIT_CODE);
            }
        }.registerCustomShortcutSet(ActionManager.getInstance().getAction(IdeActions.ACTION_EDIT_SOURCE).getShortcutSet(), tree);

        DefaultActionGroup breakpointTypes = new DefaultActionGroup();
        for (XBreakpointType<?, ?> type : XBreakpointUtil.getBreakpointTypes()) {
            if (type.isAddBreakpointButtonVisible()) {
                breakpointTypes.addAll(new AddXBreakpointAction(type));
            }
        }

        ToolbarDecorator decorator = ToolbarDecorator.createDecorator(tree).
            setAddAction(button -> JBPopupFactory.getInstance().createActionGroupPopup(
                null,
                breakpointTypes,
                DataManager.getInstance().getDataContext(button.getContextComponent()),
                JBPopupFactory.ActionSelectionAid.NUMBERING,
                false
            ).show(button.getPreferredPopupPoint())).
            setRemoveAction(button -> myTreeController.removeSelectedBreakpoints(myProject)).
            setRemoveActionUpdater(e -> {
                boolean enabled = false;
                ItemWrapper[] items = myMasterController.getSelectedItems();
                for (ItemWrapper item : items) {
                    if (item.allowedToRemove()) {
                        enabled = true;
                    }
                }
                return enabled;
            }).
            setToolbarPosition(ActionToolbarPosition.TOP).
            setToolbarBackgroundColor(UIUtil.getPanelBackground()).
            setToolbarBorder(IdeBorderFactory.createEmptyBorder());

        tree.setBorder(JBUI.Borders.empty());

        for (ToggleAction action : myToggleRuleActions) {
            decorator.addExtraAction(action);
        }

        JPanel panel = decorator.createPanel();
        panel.setBorder(IdeBorderFactory.createEmptyBorder());

        myTreeController.setTreeView(tree);

        myTreeController.buildTree(myBreakpointItems);

        initSelection(myBreakpointItems);

        BreakpointPanelProvider.BreakpointsListener listener = () -> {
            collectItems();
            myTreeController.rebuildTree(myBreakpointItems);
            myDetailController.doUpdateDetailView(true);
        };

        for (BreakpointPanelProvider provider : myBreakpointsPanelProviders) {
            provider.addListener(listener, myProject, myListenerDisposable);
        }

        return panel;
    }

    private void navigate(boolean requestFocus) {
        List<BreakpointItem> breakpoints = myTreeController.getSelectedBreakpoints(false);
        if (!breakpoints.isEmpty()) {
            breakpoints.get(0).navigate(requestFocus);
        }
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return myTreeController.getTreeView();
    }

    private void collectGroupingRules() {
        for (BreakpointPanelProvider provider : myBreakpointsPanelProviders) {
            provider.createBreakpointsGroupingRules(myRulesAvailable);
        }
        Collections.sort(myRulesAvailable, XBreakpointGroupingRule.PRIORITY_COMPARATOR);

        myRulesEnabled.clear();
        XBreakpointsDialogState settings = (getBreakpointManager()).getBreakpointsDialogSettings();

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

        dialogState.setSelectedGroupingRules(new HashSet<>(ContainerUtil.map(rulesEnabled, rule -> rule.getId())));
        getBreakpointManager().setBreakpointsDialogSettings(dialogState);
    }


    @Override
    protected void dispose() {
        saveCurrentItem();
        Disposer.dispose(myListenerDisposable);
        saveBreakpointsDialogState();
        disposeItems();
        super.dispose();
    }

    private void disposeItems() {
        for (BreakpointItem item : myBreakpointItems) {
            item.dispose();
        }
    }

    @Nullable
    @Override
    protected String getHelpId() {
        return "reference.dialogs.breakpoints";
    }

    private void saveCurrentItem() {
        ItemWrapper item = myDetailController.getSelectedItem();
        if (item instanceof BreakpointItem) {
            ((BreakpointItem) item).saveState();
        }
    }

    private class AddXBreakpointAction extends AnAction {
        private final XBreakpointType<?, ?> myType;

        public AddXBreakpointAction(XBreakpointType<?, ?> type) {
            myType = type;
            getTemplatePresentation().setIcon(type.getEnabledIcon());
            getTemplatePresentation().setText(type.getTitle());
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(@Nonnull AnActionEvent e) {
            saveCurrentItem();
            XBreakpoint<?> breakpoint = myType.addBreakpoint(myProject, null);
            if (breakpoint != null) {
                selectBreakpoint(breakpoint);
            }
        }
    }

    private boolean selectBreakpoint(Object breakpoint) {
        for (BreakpointItem item : myBreakpointItems) {
            if (item.getBreakpoint() == breakpoint) {
                myTreeController.selectBreakpointItem(item, null);
                return true;
            }
        }
        return false;
    }

    private class MoveToGroupAction extends AnAction {
        private final String myGroup;
        private final boolean myNewGroup;

        private MoveToGroupAction(String group) {
            super(group == null ? "<no group>" : group);
            myGroup = group;
            myNewGroup = false;
        }

        private MoveToGroupAction() {
            super("Create new...");
            myNewGroup = true;
            myGroup = null;
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(@Nonnull AnActionEvent e) {
            String groupName = myGroup;
            if (myNewGroup) {
                groupName = Messages.showInputDialog("New group name", "New Group", PlatformIconGroup.actionsNewfolder());
                if (groupName == null) {
                    return;
                }
            }
            for (BreakpointItem item : myTreeController.getSelectedBreakpoints(true)) {
                Object breakpoint = item.getBreakpoint();
                if (item.allowedToRemove() && breakpoint instanceof XBreakpointBase) {
                    ((XBreakpointBase) breakpoint).setGroup(groupName);
                }
            }
            myTreeController.rebuildTree(myBreakpointItems);
        }
    }
}
