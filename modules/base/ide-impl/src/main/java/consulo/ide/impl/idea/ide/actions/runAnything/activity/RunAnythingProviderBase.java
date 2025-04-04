// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions.runAnything.activity;

import consulo.annotation.access.RequiredReadAction;
import consulo.ide.impl.idea.ide.actions.runAnything.RunAnythingChooseContextAction;
import consulo.ide.impl.idea.ide.actions.runAnything.RunAnythingContext;
import consulo.ide.impl.idea.ide.actions.runAnything.RunAnythingUtil;
import consulo.ide.impl.idea.ide.actions.runAnything.items.RunAnythingHelpItem;
import consulo.ide.impl.idea.ide.actions.runAnything.items.RunAnythingItem;
import consulo.ide.impl.idea.ide.actions.runAnything.items.RunAnythingItemBase;
import consulo.dataContext.DataContext;
import consulo.localize.LocalizeValue;
import consulo.util.lang.StringUtil;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.application.util.matcher.Matcher;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;

import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * This class provides ability to run an arbitrary activity for matched 'Run Anything' input text
 */
public abstract class RunAnythingProviderBase<V> implements RunAnythingProvider<V> {
    @Nonnull
    @Override
    public Collection<V> getValues(@Nonnull DataContext dataContext, @Nonnull String pattern) {
        return ContainerUtil.emptyList();
    }

    @Override
    @Nullable
    public V findMatchingValue(@Nonnull DataContext dataContext, @Nonnull String pattern) {
        return getValues(dataContext, pattern).stream()
            .filter(value -> StringUtil.equals(pattern, getCommand(value)))
            .findFirst()
            .orElse(null);
    }

    @Override
    @Nullable
    public Image getIcon(@Nonnull V value) {
        return null;
    }

    @Override
    @Nullable
    public String getAdText() {
        return null;
    }

    @Nonnull
    @Override
    public RunAnythingItem getMainListItem(@Nonnull DataContext dataContext, @Nonnull V value) {
        return new RunAnythingItemBase(getCommand(value), getIcon(value));
    }

    @Nullable
    @Override
    public RunAnythingHelpItem getHelpItem(@Nonnull DataContext dataContext) {
        String placeholder = getHelpCommandPlaceholder();
        String commandPrefix = getHelpCommand();
        if (placeholder == null || commandPrefix == null) {
            return null;
        }
        return new RunAnythingHelpItem(placeholder, commandPrefix, getHelpDescription(), getHelpIcon());
    }

    @Override
    @Nonnull
    public LocalizeValue getCompletionGroupTitle() {
        return LocalizeValue.empty();
    }

    @Nullable
    @Override
    public Matcher getMatcher(@Nonnull DataContext dataContext, @Nonnull String pattern) {
        return null;
    }

    @Nonnull
    @Override
    @RequiredReadAction
    public List<RunAnythingContext> getExecutionContexts(@Nonnull DataContext dataContext) {
        return RunAnythingChooseContextAction.allContexts(RunAnythingUtil.fetchProject(dataContext));
    }

    @Nullable
    public Image getHelpIcon() {
        return Image.empty(16);
    }

    @Nullable
    public String getHelpDescription() {
        return null;
    }

    @Nullable
    public String getHelpCommandPlaceholder() {
        return getHelpCommand();
    }

    /**
     * Null means no help command
     *
     * @return
     */
    @Nullable
    public String getHelpCommand() {
        return null;
    }
}