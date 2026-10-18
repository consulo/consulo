// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.review.toolwindow;

import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * Represents the state of review toolwindow tabs
 *
 * @param <T>   tab type
 * @param <TVM> tab view model
 */
public record ReviewToolwindowTabs<T extends ReviewTab, TVM extends ReviewTabViewModel>(Map<T, TVM> tabs, @Nullable T selectedTab) {
}
