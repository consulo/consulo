// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.desktop.awt.navbar.ui;

import consulo.application.Application;
import consulo.dataContext.DataContext;
import consulo.language.ui.navigationBar.NavBarModelExtension;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AsyncActionGroup;
import consulo.ui.ex.action.CustomActionsSchema;
import consulo.ui.ex.action.IdeActions;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.step.CodeExecution;
import consulo.util.concurrent.coroutine.step.CompletableFutureStep;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

final class NavBarContextMenuActionGroup extends AsyncActionGroup {
    @Override
    public Coroutine<?, List<AnAction>> getChildrenAsync(@Nullable AnActionEvent e) {
        if (e == null) {
            return Coroutine.first(CodeExecution.<Object, List<AnAction>>supply(() -> List.of()));
        }

        String popupGroupId = contextMenuActionGroupId(e.getDataContext());
        return Coroutine.first(CompletableFutureStep.<Object, List<AnAction>>await((input, continuation) ->
            CustomActionsSchema.getCorrectedGroupAsync(popupGroupId).thenCompose(group -> group == null
                ? CompletableFuture.completedFuture(List.<AnAction>of())
                : group.getChildrenAsync(e).runAsync(continuation.scope(), null).toFuture())));
    }

    private static String contextMenuActionGroupId(DataContext dataContext) {
        String groupId = Application.get().getExtensionPoint(NavBarModelExtension.class)
            .computeSafeIfAny(ext -> ext.getPopupMenuGroup(dataContext::getData));
        return groupId != null ? groupId : IdeActions.GROUP_NAVBAR_POPUP;
    }
}
