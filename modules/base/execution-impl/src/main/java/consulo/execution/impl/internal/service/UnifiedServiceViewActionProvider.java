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
package consulo.execution.impl.internal.service;

import consulo.ui.Component;
import consulo.ui.Tree;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.TreeExpander;
import consulo.ui.ex.action.*;

/**
 * The toolbars and the popup of a unified services view - the counterpart of the swing parts of
 * {@link ServiceViewActionProvider}, which keeps what both views share.
 *
 * @author VISTALL
 * @since 2026-09-24
 */
final class UnifiedServiceViewActionProvider {
    private static final UnifiedServiceViewActionProvider ourInstance = new UnifiedServiceViewActionProvider();

    static UnifiedServiceViewActionProvider getInstance() {
        return ourInstance;
    }

    @RequiredUIAccess
    ActionToolbar createServiceToolbar(Component component, boolean horizontal) {
        ActionGroup actions = (ActionGroup) ActionManager.getInstance().getAction(ServiceViewActionProvider.SERVICE_VIEW_ITEM_TOOLBAR);
        ActionToolbar toolbar = ActionToolbarFactory.getInstance().createActionToolbar(
            ActionPlaces.SERVICES_TOOLBAR,
            actions,
            horizontal ? ActionToolbar.Style.HORIZONTAL : ActionToolbar.Style.VERTICAL
        );
        toolbar.setTargetUIComponent(component);
        return toolbar;
    }

    /**
     * The unified toolbar keeps its size when it has no actions, so it needs no spacer to be wrapped with.
     */
    Component wrapServiceToolbar(ActionToolbar toolbar, boolean horizontal) {
        return toolbar.getUIComponent();
    }

    /**
     * The menu of the selected items on a right click of the tree - the web tree selects the row under the pointer
     * first.
     */
    void installPopupHandler(Component component) {
        component.addContextMenuListener(event -> {
            ActionManager actionManager = ActionManager.getInstance();
            if (!(actionManager.getAction(ServiceViewActionProvider.SERVICE_VIEW_ITEM_POPUP) instanceof ActionGroup group)) {
                return;
            }

            ActionPopupMenu menu = actionManager.createActionPopupMenu(ActionPlaces.SERVICES_POPUP, group);
            menu.setTargetComponent(component);
            menu.show(event.getComponent(), event.getInputDetails().getX(), event.getInputDetails().getY());
        });
    }

    @RequiredUIAccess
    ActionToolbar createMasterComponentToolbar(Component component) {
        DefaultActionGroup group = new DefaultActionGroup();

        if (component instanceof Tree<?> tree && tree.isExpandCollapseAllSupported()) {
            TreeExpander treeExpander = new ServiceViewTreeExpander(tree);
            group.add(CommonActionsManager.getInstance().createExpandAllAction(treeExpander));
            group.add(CommonActionsManager.getInstance().createCollapseAllAction(treeExpander));
            group.addSeparator();
        }

        group.addSeparator();
        group.add(ActionManager.getInstance().getAction(ServiceViewActionProvider.SERVICE_VIEW_TREE_TOOLBAR));

        ActionToolbar treeActionsToolBar =
            ActionToolbarFactory.getInstance().createActionToolbar(ActionPlaces.SERVICES_TOOLBAR, group, ActionToolbar.Style.HORIZONTAL);
        treeActionsToolBar.setTargetUIComponent(component);
        return treeActionsToolBar;
    }

    private record ServiceViewTreeExpander(Tree<?> tree) implements TreeExpander {
        @Override
        public void expandAll() {
            tree.expandAll();
        }

        @Override
        public boolean canExpand() {
            return true;
        }

        @Override
        public void collapseAll() {
            tree.collapseAll();
        }

        @Override
        public boolean canCollapse() {
            return true;
        }
    }
}
