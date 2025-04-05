// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions.runAnything.groups;

import consulo.application.util.matcher.Matcher;
import consulo.dataContext.DataContext;
import consulo.ide.impl.idea.ide.actions.runAnything.activity.RunAnythingProvider;
import consulo.ide.impl.idea.ide.actions.runAnything.items.RunAnythingItem;
import consulo.localize.LocalizeValue;
import consulo.util.collection.ContainerUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

public class RunAnythingCompletionGroup<V, P extends RunAnythingProvider<V>> extends RunAnythingGroupBase {
    public static final Collection<RunAnythingGroup> MAIN_GROUPS = createCompletionGroups();

    @Nonnull
    private final P myProvider;
    @Nonnull
    private final LocalizeValue myTitle;

    public RunAnythingCompletionGroup(@Nonnull P provider, @Nonnull LocalizeValue title) {
        myProvider = provider;
        myTitle = title;
    }

    @Nonnull
    protected P getProvider() {
        return myProvider;
    }

    @Nonnull
    @Override
    public LocalizeValue getTitle() {
        return myTitle;
    }

    @Nonnull
    @Override
    public Collection<RunAnythingItem> getGroupItems(@Nonnull DataContext dataContext, @Nonnull String pattern) {
        P provider = getProvider();
        return ContainerUtil.map(provider.getValues(dataContext, pattern), value -> provider.getMainListItem(dataContext, value));
    }

    @Nullable
    @Override
    protected Matcher getMatcher(@Nonnull DataContext dataContext, @Nonnull String pattern) {
        return getProvider().getMatcher(dataContext, pattern);
    }

    public static Collection<RunAnythingGroup> createCompletionGroups() {
        return RunAnythingProvider.EP_NAME.getExtensionList()
            .stream()
            .map(RunAnythingProvider::getCompletionGroup)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
    }
}