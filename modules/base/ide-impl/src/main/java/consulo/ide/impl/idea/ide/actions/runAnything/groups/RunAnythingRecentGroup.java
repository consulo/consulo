// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions.runAnything.groups;

import consulo.component.extension.ExtensionPoint;
import consulo.dataContext.DataContext;
import consulo.ide.impl.idea.ide.actions.runAnything.RunAnythingCacheImpl;
import consulo.ide.internal.RunAnythingCache;
import consulo.ide.localize.IdeLocalize;
import consulo.ide.runAnything.RunAnythingItem;
import consulo.ide.runAnything.RunAnythingProvider;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Collection;

public class RunAnythingRecentGroup extends RunAnythingGroupBase {
    public static final RunAnythingRecentGroup INSTANCE = new RunAnythingRecentGroup();

    private RunAnythingRecentGroup() {
    }

    @Nonnull
    @Override
    public LocalizeValue getTitle() {
        return IdeLocalize.runAnythingRecentGroupTitle();
    }

    @Nonnull
    @Override
    public Collection<RunAnythingItem> getGroupItems(@Nonnull DataContext dataContext, @Nonnull String pattern) {
        Project project = dataContext.getData(Project.KEY);
        assert project != null;

        ExtensionPoint<RunAnythingProvider> extensionPoint = project.getApplication().getExtensionPoint(RunAnythingProvider.class);
        Collection<RunAnythingItem> collector = new ArrayList<>();
        for (String command : ((RunAnythingCacheImpl) RunAnythingCache.getInstance(project)).getState().getCommands().reversed()) {
            collector.add(extensionPoint.computeSafeIfAny(provider -> {
                Object matchingValue = provider.findMatchingValue(dataContext, command);
                //noinspection unchecked
                return matchingValue != null ? provider.getMainListItem(dataContext, matchingValue) : null;
            }));
        }

        return collector;
    }

    @Override
    protected int getMaxInitialItems() {
        return 10;
    }
}