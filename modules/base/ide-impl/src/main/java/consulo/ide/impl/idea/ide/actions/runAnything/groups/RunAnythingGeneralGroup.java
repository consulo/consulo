// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions.runAnything.groups;

import consulo.ide.impl.idea.ide.actions.runAnything.activity.RunAnythingProvider;
import consulo.ide.impl.idea.ide.actions.runAnything.items.RunAnythingItem;
import consulo.dataContext.DataContext;
import consulo.ide.localize.IdeLocalize;
import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Collection;

public class RunAnythingGeneralGroup extends RunAnythingGroupBase {
    public static final RunAnythingGeneralGroup INSTANCE = new RunAnythingGeneralGroup();
    public static final String GENERAL_GROUP_TITLE = IdeLocalize.runAnythingGeneralGroupTitle().get();

    private RunAnythingGeneralGroup() {
    }

    @Nonnull
    @Override
    public LocalizeValue getTitle() {
        return IdeLocalize.runAnythingGeneralGroupTitle();
    }

    @Nonnull
    @Override
    public Collection<RunAnythingItem> getGroupItems(@Nonnull DataContext dataContext, @Nonnull String pattern) {
        Collection<RunAnythingItem> collector = new ArrayList<>();

        for (RunAnythingProvider provider : RunAnythingProvider.EP_NAME.getExtensions()) {
            if (IdeLocalize.runAnythingGeneralGroupTitle().equals(provider.getCompletionGroupTitle())) {
                Collection values = provider.getValues(dataContext, pattern);
                for (Object value : values) {
                    //noinspection unchecked
                    collector.add(provider.getMainListItem(dataContext, value));
                }
            }
        }

        return collector;
    }

    @Override
    protected int getMaxInitialItems() {
        return 15;
    }
}
