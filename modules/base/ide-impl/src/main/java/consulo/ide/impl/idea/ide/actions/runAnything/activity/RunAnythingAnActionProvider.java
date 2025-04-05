// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions.runAnything.activity;

import consulo.application.Application;
import consulo.dataContext.DataContext;
import consulo.ide.impl.idea.ide.actions.runAnything.RunAnythingUtil;
import consulo.ide.impl.idea.ide.actions.runAnything.items.RunAnythingActionItem;
import consulo.ide.impl.idea.ide.actions.runAnything.items.RunAnythingItem;
import consulo.ide.impl.idea.openapi.actionSystem.ex.ActionUtil;
import consulo.ide.localize.IdeLocalize;
import consulo.project.ui.internal.ProjectIdeFocusManager;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public abstract class RunAnythingAnActionProvider<V extends AnAction> extends RunAnythingProviderBase<V> {
    @Nonnull
    @Override
    public RunAnythingItem getMainListItem(@Nonnull DataContext dataContext, @Nonnull V value) {
        return new RunAnythingActionItem<>(value, getCommand(value), value.getTemplatePresentation().getIcon());
    }

    @Override
    public void execute(@Nonnull DataContext dataContext, @Nonnull V value) {
        performRunAnythingAction(value, dataContext);
    }

    @Nullable
    @Override
    public Image getIcon(@Nonnull V value) {
        return value.getTemplatePresentation().getIcon();
    }

    private static void performRunAnythingAction(@Nonnull AnAction action, @Nonnull DataContext dataContext) {
        Application.get().invokeLater(
            () -> ProjectIdeFocusManager.getInstance(RunAnythingUtil.fetchProject(dataContext))
                .doWhenFocusSettlesDown(() -> performAction(action, dataContext))
        );
    }

    private static void performAction(@Nonnull AnAction action, @Nonnull DataContext dataContext) {
        AnActionEvent event = AnActionEvent.createFromAnAction(action, null, ActionPlaces.UNKNOWN, dataContext);

        ActionUtil.performActionDumbAwareWithCallbacks(action, event, dataContext);
    }

    @Nullable
    @Override
    public String getAdText() {
        return IdeLocalize.runAnythingAdRunActionWithDefaultSettings(RunAnythingUtil.SHIFT_SHORTCUT_TEXT).get();
    }
}
