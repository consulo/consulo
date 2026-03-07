// Copyright 2013-2026 consulo.io. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.wm.navigationToolbar;

import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Pre-computed presentation data for a single NavBar item.
 * Computed on a background thread inside ReadAction.nonBlocking(),
 * then consumed on EDT to create NavBarItem Swing components without ReadAction.
 *
 * @since 2026-03-02
 */
public record NavBarItemData(
    @Nonnull Object element,
    @Nonnull String text,
    @Nullable Image icon,
    @Nonnull SimpleTextAttributes attributes,
    boolean needPaintIcon
) {
}
