// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.ui;

import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.dataholder.Key;

import java.util.Set;

/**
 * The groupings a changes tree offers, as {@link VcsChangesBrowserBuilder#grouping} takes and reports their keys. A tree
 * publishes its grouping under {@link #KEY}, so the grouping actions of a context menu or a toolbar change it.
 */
public abstract class ChangesGroupingSupport {
    public static final Key<ChangesGroupingSupport> KEY = Key.create("ChangesTree.GroupingSupport");

    public static final String DIRECTORY_GROUPING = "directory";
    public static final String MODULE_GROUPING = "module";
    public static final String REPOSITORY_GROUPING = "repository";
    public static final String NONE_GROUPING = "none";

    protected ChangesGroupingSupport() {
    }

    public abstract Set<String> getGroupingKeys();

    public boolean get(String groupingKey) {
        return isAvailable(groupingKey) && getGroupingKeys().contains(groupingKey);
    }

    @RequiredUIAccess
    public abstract void set(String groupingKey, boolean state);

    public abstract boolean isAvailable(String groupingKey);
}
