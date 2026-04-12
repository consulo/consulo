// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.searchEverywhere;

import org.jspecify.annotations.Nullable;

/**
 * Interface for contributors that provide extended footer information in Search Everywhere popup.
 * E.g., it renders description and "Assign/Change" shortcut for a selected action.
 */
public interface SearchEverywhereExtendedInfoProvider {
    default @Nullable ExtendedInfo createExtendedInfo() {
        return null;
    }
}
