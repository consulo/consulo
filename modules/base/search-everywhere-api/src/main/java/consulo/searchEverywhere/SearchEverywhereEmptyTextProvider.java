// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.searchEverywhere;

import consulo.ui.ex.awt.StatusText;

/**
 * Interface for contributors that provide custom empty state text in Search Everywhere.
 */
public interface SearchEverywhereEmptyTextProvider {
    void updateEmptyStatus(StatusText statusText, Runnable rebuild);
}
