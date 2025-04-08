// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions.runAnything.groups;

import consulo.application.Application;
import consulo.ide.runAnything.RunAnythingProvider;
import consulo.ide.runAnything.RunAnythingItem;
import consulo.dataContext.DataContext;
import consulo.ide.localize.IdeLocalize;
import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Collection;

public class RunAnythingGeneralGroup extends RunAnythingGroupBase {
    public static final RunAnythingGeneralGroup INSTANCE = new RunAnythingGeneralGroup();

    private RunAnythingGeneralGroup() {
    }

    @Nonnull
    @Override
    public LocalizeValue getTitle() {
        return IdeLocalize.runAnythingGeneralGroupTitle();
    }

    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public Collection<RunAnythingItem> getGroupItems(@Nonnull DataContext dataContext, @Nonnull String pattern) {
        Collection<RunAnythingItem> collector = new ArrayList<>();

        Application.get().getExtensionPoint(RunAnythingProvider.class).forEachExtensionSafe(provider -> {
            if (provider.getCompletionGroup() == RunAnythingGeneralGroup.INSTANCE) {
                Collection values = provider.getValues(dataContext, pattern);
                for (Object value : values) {
                    collector.add(provider.getMainListItem(dataContext, value));
                }
            }
        });

        return collector;
    }

    @Override
    protected int getMaxInitialItems() {
        return 15;
    }
}
