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
package consulo.execution.debug.impl.internal.bookmark;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.bookmark.ui.view.AbstractFavoritesListProvider;
import consulo.bookmark.ui.view.FavoritesManager;
import consulo.execution.debug.XDebuggerManager;
import consulo.execution.debug.breakpoint.ui.XBreakpointGroup;
import consulo.execution.debug.breakpoint.ui.XBreakpointGroupingRule;
import consulo.execution.debug.icon.ExecutionDebugIconGroup;
import consulo.execution.debug.impl.internal.action.handler.XBreakpointsDialogState;
import consulo.execution.debug.impl.internal.action.handler.XDebuggerEditBreakpointActionHandler;
import consulo.execution.debug.impl.internal.breakpoint.XBreakpointManagerImpl;
import consulo.execution.debug.impl.internal.breakpoint.XBreakpointUtil;
import consulo.execution.debug.impl.internal.breakpoint.ui.BreakpointItem;
import consulo.execution.debug.impl.internal.breakpoint.ui.BreakpointPanelProvider;
import consulo.execution.debug.impl.internal.breakpoint.ui.tree.BreakpointItemsTreeController;
import consulo.navigation.Navigatable;
import consulo.project.Project;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.ui.ex.awt.CommonActionsPanel;
import consulo.ui.ex.awt.tree.CheckedTreeNode;
import consulo.ui.ex.awt.tree.ColoredTreeCellRenderer;
import consulo.ui.ex.tree.PresentationData;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * @author Vassiliy.Kudryashov
 */
@ExtensionImpl
public class BreakpointsFavoriteListProvider extends AbstractFavoritesListProvider<Object>
    implements BreakpointPanelProvider.BreakpointsListener {

    private final List<BreakpointPanelProvider> myBreakpointPanelProviders;
    private final BreakpointItemsTreeController myTreeController;
    private final List<XBreakpointGroupingRule> myRulesAvailable = new ArrayList<XBreakpointGroupingRule>();
    private final BreakpointsSimpleTree myTree;

    private Set<XBreakpointGroupingRule> myRulesEnabled = new TreeSet<XBreakpointGroupingRule>(new Comparator<XBreakpointGroupingRule>() {
        @Override
        public int compare(XBreakpointGroupingRule o1, XBreakpointGroupingRule o2) {
            final int res = o2.getPriority() - o1.getPriority();
            return res != 0 ? res : (o1.getId().compareTo(o2.getId()));
        }
    });

    @Inject
    public BreakpointsFavoriteListProvider(Project project) {
        super(project, "Breakpoints");
        myBreakpointPanelProviders = XBreakpointUtil.collectPanelProviders();
        myTreeController = new BreakpointItemsTreeController(myRulesAvailable);
        myTree = new BreakpointsSimpleTree(myProject, myTreeController);
        myTreeController.setTreeView(myTree);
        updateChildren();
        for (final BreakpointPanelProvider provider : myBreakpointPanelProviders) {
            provider.addListener(this, myProject, myProject);
            provider.createBreakpointsGroupingRules(myRulesAvailable);
        }
    }

    @Override
    public void breakpointsChanged() {
        updateChildren();
    }

    private void getEnabledGroupingRules(Collection<XBreakpointGroupingRule> rules) {
        rules.clear();
        XBreakpointsDialogState settings = ((XBreakpointManagerImpl) XDebuggerManager.getInstance(myProject).getBreakpointManager()).getBreakpointsDialogSettings();

        for (XBreakpointGroupingRule rule : myRulesAvailable) {
            if (rule.isAlwaysEnabled() || (settings != null && settings.getSelectedGroupingRules().contains(rule.getId()))) {
                rules.add(rule);
            }
        }
    }

    private void updateChildren() {
        if (myProject.isDisposed()) {
            return;
        }
        myChildren.clear();
        List<BreakpointItem> items = new ArrayList<BreakpointItem>();
        for (final BreakpointPanelProvider provider : myBreakpointPanelProviders) {
            provider.provideBreakpointItems(myProject, items);
        }
        getEnabledGroupingRules(myRulesEnabled);
        myTreeController.setGroupingRules(myRulesEnabled);
        myTreeController.rebuildTree(items);


        CheckedTreeNode root = myTreeController.getRoot();
        for (int i = 0; i < root.getChildCount(); i++) {
            TreeNode child = root.getChildAt(i);
            if (child instanceof DefaultMutableTreeNode) {
                replicate((DefaultMutableTreeNode) child, myNode, myChildren);
            }
        }

        FavoritesManager.getInstance(myProject).fireListeners(getListName(myProject));
    }

    private void replicate(DefaultMutableTreeNode source, AbstractTreeNode destination, final List<AbstractTreeNode<Object>> destinationChildren) {
        final ArrayList<AbstractTreeNode<Object>> copyChildren = new ArrayList<AbstractTreeNode<Object>>();
        AbstractTreeNode<Object> copy = new AbstractTreeNode<Object>(myProject, source.getUserObject()) {
            @RequiredReadAction
            @Nonnull
            @Override
            public Collection<? extends AbstractTreeNode> getChildren() {
                return copyChildren;
            }

            @Override
            protected void update(PresentationData presentation) {
            }
        };

        for (int i = 0; i < source.getChildCount(); i++) {
            final TreeNode treeNode = source.getChildAt(i);
            if (treeNode instanceof DefaultMutableTreeNode) {
                final DefaultMutableTreeNode sourceChild = (DefaultMutableTreeNode) treeNode;
                replicate(sourceChild, copy, copyChildren);
            }
        }
        if (checkNavigatable(copy)) {
            destinationChildren.add(copy);
            copy.setParent(destination);
        }
    }

    private static boolean checkNavigatable(AbstractTreeNode<?> node) {
        if (node.getValue() instanceof Navigatable && ((Navigatable) node.getValue()).canNavigate()) {
            return true;
        }
        Collection<? extends AbstractTreeNode> children = node.getChildren();
        for (AbstractTreeNode child : children) {
            if (checkNavigatable(child)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    @Override
    public String getCustomName(@Nonnull CommonActionsPanel.Buttons type) {
        switch (type) {
            case EDIT:
                return "Edit breakpoint";
            case REMOVE:
                return "Remove breakpoint";
            default:
                return null;
        }
    }

    @Override
    public boolean willHandle(@Nonnull CommonActionsPanel.Buttons type, Project project, @Nonnull Set<Object> selectedObjects) {
        return (selectedObjects.size() == 1 && (type == CommonActionsPanel.Buttons.EDIT || type == CommonActionsPanel.Buttons.REMOVE)) &&
            ((AbstractTreeNode) selectedObjects.iterator().next()).getValue() instanceof BreakpointItem;
    }

    @Override
    public void handle(@Nonnull CommonActionsPanel.Buttons type, Project project, @Nonnull Set<Object> selectedObjects, JComponent component) {
        Rectangle bounds = component.getBounds();
        if (component instanceof JTree) {
            JTree tree = (JTree) component;
            bounds = tree.getRowBounds(tree.getLeadSelectionRow());
            bounds.y += bounds.height / 2;
            bounds = tree.getVisibleRect().intersection(bounds);
        }
        Point whereToShow = new Point((int) bounds.getCenterX(), (int) bounds.getCenterY());
        BreakpointItem breakpointItem = (BreakpointItem) ((AbstractTreeNode) selectedObjects.iterator().next()).getValue();
        switch (type) {
            case EDIT:
                XDebuggerEditBreakpointActionHandler.INSTANCE.editBreakpoint(myProject, component, whereToShow, breakpointItem);
                break;
            case REMOVE:
                breakpointItem.removed(myProject);
                break;
            default:
                break;
        }
    }

    @Override
    public int getWeight() {
        return AbstractFavoritesListProvider.BREAKPOINTS_WEIGHT;
    }

    @Override
    public void customizeRenderer(ColoredTreeCellRenderer renderer,
                                  JTree tree,
                                  @Nonnull Object value,
                                  boolean selected,
                                  boolean expanded,
                                  boolean leaf,
                                  int row,
                                  boolean hasFocus) {
        renderer.clear();
        renderer.setIcon(ExecutionDebugIconGroup.breakpointBreakpoint());
        if (value instanceof BreakpointItem) {
            BreakpointItem breakpointItem = (BreakpointItem) value;
            breakpointItem.setupGenericRenderer(renderer, true);
        }
        else if (value instanceof XBreakpointGroup) {
            renderer.append(((XBreakpointGroup) value).getName());
            renderer.setIcon(((XBreakpointGroup) value).getIcon(expanded));
        }
        else if (value instanceof XBreakpointGroupingRule) {
            renderer.append(((XBreakpointGroupingRule) value).getPresentableName());
        }
        else {
            renderer.append(String.valueOf(value));
        }
    }
}
