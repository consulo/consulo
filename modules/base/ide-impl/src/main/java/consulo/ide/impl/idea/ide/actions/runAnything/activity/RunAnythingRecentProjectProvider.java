// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions.runAnything.activity;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.dataContext.DataContext;
import consulo.ide.impl.idea.ide.ReopenProjectAction;
import consulo.ide.impl.idea.ide.actions.runAnything.RunAnythingContext;
import consulo.ide.impl.idea.ide.actions.runAnything.items.RunAnythingItem;
import consulo.ide.impl.idea.ide.actions.runAnything.items.RunAnythingItemBase;
import consulo.ide.localize.IdeLocalize;
import consulo.localize.LocalizeValue;
import consulo.project.internal.RecentProjectsManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.image.Image;
import consulo.util.lang.ObjectUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@ExtensionImpl
public class RunAnythingRecentProjectProvider extends RunAnythingAnActionProvider<AnAction> {
    @Nonnull
    @Override
    public Collection<AnAction> getValues(@Nonnull DataContext dataContext, @Nonnull String pattern) {
        return Arrays.stream(RecentProjectsManager.getInstance().getRecentProjectsActions(false)).collect(Collectors.toList());
    }

    @Nonnull
    @Override
    public RunAnythingItem getMainListItem(@Nonnull DataContext dataContext, @Nonnull AnAction value) {
        if (value instanceof ReopenProjectAction reopenProjectAction) {
            return new RecentProjectElement(
                reopenProjectAction,
                getCommand(value),
                reopenProjectAction.getExtensionIcon()
            );
        }
        return super.getMainListItem(dataContext, value);
    }

    @Override
    @Nonnull
    public LocalizeValue getCompletionGroupTitle() {
        return IdeLocalize.runAnythingRecentProjectCompletionGroupTitle();
    }

    @Nonnull
    @Override
    public String getHelpCommandPlaceholder() {
        return IdeLocalize.runAnythingRecentProjectCommandPlaceholder().get();
    }

    @Nonnull
    @Override
    public String getHelpCommand() {
        return "open";
    }

    @Nullable
    @Override
    public String getHelpGroupTitle() {
        return IdeLocalize.runAnythingRecentProjectHelpGroupTitle().get();
    }

    @Nonnull
    @Override
    public String getCommand(@Nonnull AnAction value) {
        return getHelpCommand() + " " + ObjectUtil.notNull(
            value.getTemplatePresentation().getText(),
            IdeLocalize.runAnythingActionsUndefined().get()
        );
    }

    static class RecentProjectElement extends RunAnythingItemBase {
        @Nonnull
        private final ReopenProjectAction myValue;

        RecentProjectElement(@Nonnull ReopenProjectAction value, @Nonnull String command, @Nullable Image icon) {
            super(command, icon);
            myValue = value;
        }

        @Nullable
        @Override
        public String getDescription() {
            return myValue.getProjectPath();
        }
    }

    @Nonnull
    @Override
    @RequiredReadAction
    public List<RunAnythingContext> getExecutionContexts(@Nonnull DataContext dataContext) {
        return Collections.emptyList();
    }
}