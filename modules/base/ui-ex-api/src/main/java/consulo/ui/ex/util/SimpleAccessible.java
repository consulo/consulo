// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ui.ex.util;

import consulo.annotation.DeprecationInfo;
import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Provides a minimal accessible information about the object.
 */
public interface SimpleAccessible {
    /**
     * Returns a human-readable string that designates the purpose of the object.
     */
    //TODO: rename into getAccessibleName() after deprecation deletion
    @Nonnull
    default LocalizeValue getAccessibleNameValue() {
        return LocalizeValue.of(getAccessibleName());
    }

    /**
     * Returns a human-readable string that designates the purpose of the object.
     */
    @Deprecated
    @DeprecationInfo("Use getAccessibleNameValue(int)")
    @Nonnull
    default String getAccessibleName() {
        return getAccessibleNameValue().get();
    }

    /**
     * Returns the tooltip text or null when the tooltip is not available
     */
    //TODO: rename into getAccessibleTooltipText() after deprecation deletion
    @Nonnull
    default LocalizeValue getAccessibleTooltipValue() {
        return LocalizeValue.ofNullable(getAccessibleTooltipText());
    }

    /**
     * Returns the tooltip text or null when the tooltip is not available
     */
    @Deprecated
    @DeprecationInfo("Use getToolTipValue(int)")
    @Nullable
    default String getAccessibleTooltipText() {
        LocalizeValue tooltip = getAccessibleTooltipValue();
        return tooltip == LocalizeValue.empty() ? null : tooltip.get();
    }
}
