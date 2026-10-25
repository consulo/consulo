// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.ui;

/**
 * Keys of the groupings a changes tree offers, as {@link VcsChangesBrowserBuilder#grouping} takes and reports them.
 */
public final class ChangesGroupingSupport {
    public static final String DIRECTORY_GROUPING = "directory";
    public static final String MODULE_GROUPING = "module";
    public static final String REPOSITORY_GROUPING = "repository";
    public static final String NONE_GROUPING = "none";

    private ChangesGroupingSupport() {
    }
}
