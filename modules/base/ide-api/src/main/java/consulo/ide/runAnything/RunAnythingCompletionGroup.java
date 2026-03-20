// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.runAnything;

import consulo.application.Application;
import consulo.application.util.matcher.Matcher;
import consulo.dataContext.DataContext;
import consulo.localize.LocalizeValue;
import consulo.util.collection.ContainerUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;

public class RunAnythingCompletionGroup<V, P extends RunAnythingProvider<V>> extends RunAnythingGroupBase {
    public static final Collection<RunAnythingGroup> MAIN_GROUPS = createCompletionGroups();

    
    private final P myProvider;
    
    private final LocalizeValue myTitle;

    public RunAnythingCompletionGroup(P provider, LocalizeValue title) {
        myProvider = provider;
        myTitle = title;
    }

    
    protected P getProvider() {
        return myProvider;
    }

    
    @Override
    public LocalizeValue getTitle() {
        return myTitle;
    }

    
    @Override
    public Collection<RunAnythingItem> getGroupItems(DataContext dataContext, String pattern) {
        P provider = getProvider();
        return ContainerUtil.map(provider.getValues(dataContext, pattern), value -> provider.getMainListItem(dataContext, value));
    }

    @Override
    protected @Nullable Matcher getMatcher(DataContext dataContext, String pattern) {
        return getProvider().getMatcher(dataContext, pattern);
    }

    public static Collection<RunAnythingGroup> createCompletionGroups() {
        return new ArrayList<>(
            Application.get().getExtensionPoint(RunAnythingProvider.class)
                .collectMapped(new LinkedHashSet<>(), RunAnythingProvider::getCompletionGroup)
        );
    }
}