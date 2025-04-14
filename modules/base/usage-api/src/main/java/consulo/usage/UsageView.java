/*
 * Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package consulo.usage;

import consulo.content.scope.SearchScope;
import consulo.disposer.Disposable;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author max
 */
public interface UsageView extends Disposable {
    /**
     * Returns {@link UsageTarget} to look usages for
     */
    Key<UsageTarget[]> USAGE_TARGETS_KEY = Key.create("usageTarget");

    /**
     * Returns {@link Usage} which are selected in usage view
     */
    Key<Usage[]> USAGES_KEY = Key.create("usages");

    Key<UsageView> USAGE_VIEW_KEY = Key.create("UsageView.new");

    Key<UsageInfo> USAGE_INFO_KEY = Key.create("UsageInfo");
    Key<SearchScope> USAGE_SCOPE = Key.create("UsageScope");

    Key<List<UsageInfo>> USAGE_INFO_LIST_KEY = Key.create("UsageInfo.List");

    void appendUsage(@Nonnull Usage usage);

    void removeUsage(@Nonnull Usage usage);

    void includeUsages(@Nonnull Usage[] usages);

    void excludeUsages(@Nonnull Usage[] usages);

    void selectUsages(@Nonnull Usage[] usages);

    void close();

    boolean isSearchInProgress();

    default void addButtonToLowerPane(@Nonnull Runnable runnable, @Nonnull String text) {
        addButtonToLowerPane(new AnAction(text) {
            @RequiredUIAccess
            @Override
            public void actionPerformed(@Nonnull AnActionEvent e) {
                runnable.run();
            }
        });
    }

    void addButtonToLowerPane(@Nonnull AnAction action);

    /**
     * @param rerunAction this action is used to provide non-standard search restart. Disabled action makes toolbar button disabled too.
     */
    default void setRerunAction(@Nonnull Action rerunAction) {
    }

    void setAdditionalComponent(@Nullable JComponent component);

    void addPerformOperationAction(
        @Nonnull Runnable processRunnable,
        @Nonnull String commandName,
        String cannotMakeString,
        @Nonnull String shortDescription
    );

    /**
     * @param checkReadOnlyStatus if false, check is performed inside processRunnable
     */
    void addPerformOperationAction(
        @Nonnull Runnable processRunnable,
        @Nonnull String commandName,
        String cannotMakeString,
        @Nonnull String shortDescription,
        boolean checkReadOnlyStatus
    );

    @Nonnull
    UsageViewPresentation getPresentation();

    @Nonnull
    Set<Usage> getExcludedUsages();

    @Nonnull
    Set<Usage> getSelectedUsages();

    @Nonnull
    Set<Usage> getUsages();

    @Nonnull
    List<Usage> getSortedUsages();

    @Nonnull
    JComponent getComponent();

    @Nonnull
    default JComponent getPreferredFocusableComponent() {
        return getComponent();
    }


    int getUsagesCount();

    /**
     * Removes all specified usages from the usage view in one heroic swoop.
     * Reloads the whole tree model once instead of firing individual remove event for each node.
     * Useful for processing huge number of usages faster, e.g. during "find in path/replace all".
     */
    void removeUsagesBulk(@Nonnull Collection<Usage> usages);

    default void addExcludeListener(@Nonnull Disposable disposable, @Nonnull ExcludeListener listener) {
    }

    interface ExcludeListener {
        /**
         * @param usages   unmodifiable set or nodes that were excluded or included
         * @param excluded if <code>true</code> usages were excluded otherwise they were included
         */
        void fireExcluded(@Nonnull Set<? extends Usage> usages, boolean excluded);
    }

    boolean isDisposed();

    void expandAll();

    void select();
}
