// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.runAnything;

import consulo.application.util.matcher.Matcher;
import consulo.dataContext.DataContext;
import consulo.ui.ex.awt.CollectionListModel;
import org.jspecify.annotations.Nullable;

import java.util.Collection;

public abstract class RunAnythingGroupBase extends RunAnythingGroup {
    
    public abstract Collection<RunAnythingItem> getGroupItems(DataContext dataContext, String pattern);

    protected @Nullable Matcher getMatcher(DataContext dataContext, String pattern) {
        return null;
    }

    @Override
    public SearchResult getItems(
        DataContext dataContext,
        CollectionListModel<Object> model,
        String pattern,
        boolean isInsertionMode,
        Runnable cancellationChecker
    ) {
        cancellationChecker.run();
        SearchResult result = new SearchResult();
        for (RunAnythingItem runConfigurationItem : getGroupItems(dataContext, pattern)) {
            Matcher matcher = getMatcher(dataContext, pattern);
            if (matcher == null) {
                matcher = RUN_ANYTHING_MATCHER_BUILDER.apply(pattern).build();
            }
            if (addToList(model, result, runConfigurationItem.getCommand(), isInsertionMode, runConfigurationItem, matcher)) {
                break;
            }
            cancellationChecker.run();
        }

        return result;
    }

    /**
     * Adds limited number of matched items into the list.
     *
     * @param model           needed to avoid adding duplicates into the list
     * @param textToMatch     an item presentation text to be matched with
     * @param isInsertionMode if true gets {@link #getMaxItemsToInsert()} group items, else limits to {@link #getMaxInitialItems()}
     * @param item            a new item that is conditionally added into the model
     * @param matcher         uses for group items filtering
     * @return true if limit exceeded
     */
    private boolean addToList(
        CollectionListModel<Object> model,
        SearchResult result,
        String textToMatch,
        boolean isInsertionMode,
        RunAnythingItem item,
        Matcher matcher
    ) {
        if (!model.contains(item) && matcher.matches(textToMatch)) {
            if (result.size() == (isInsertionMode ? getMaxItemsToInsert() : getMaxInitialItems())) {
                result.setNeedMore(true);
                return true;
            }
            result.add(item);
        }
        return false;
    }
}