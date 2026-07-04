// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.desktop.awt.navbar.ui;

import consulo.application.Application;
import consulo.dataContext.DataContext;
import consulo.ide.impl.idea.ide.ui.customization.CustomActionsSchemaImpl;
import consulo.language.ui.navigationBar.NavBarModelExtension;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.IdeActions;
import org.jspecify.annotations.Nullable;

final class NavBarContextMenuActionGroup extends ActionGroup {
    @Override
    public AnAction[] getChildren(@Nullable AnActionEvent e) {
        if (e == null) {
            return EMPTY_ARRAY;
        }
        DataContext dataContext = e.getDataContext();
        String popupGroupId = contextMenuActionGroupId(dataContext);
        ActionGroup group = (ActionGroup) CustomActionsSchemaImpl.getInstance().getCorrectedAction(popupGroupId);
        if (group == null) {
            return EMPTY_ARRAY;
        }
        return group.getChildren(e);
    }

    private static String contextMenuActionGroupId(DataContext dataContext) {
        String groupId = Application.get().getExtensionPoint(NavBarModelExtension.class)
            .computeSafeIfAny(ext -> ext.getPopupMenuGroup(dataContext::getData));
        return groupId != null ? groupId : IdeActions.GROUP_NAVBAR_POPUP;
    }
}
