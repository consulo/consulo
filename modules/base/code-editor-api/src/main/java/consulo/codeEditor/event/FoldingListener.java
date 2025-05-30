// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.codeEditor.event;

import consulo.codeEditor.CustomFoldRegion;
import consulo.codeEditor.FoldRegion;
import jakarta.annotation.Nonnull;
import org.intellij.lang.annotations.MagicConstant;

/**
 * Defines common contract for clients interested in folding processing.
 *
 * @author Denis Zhdanov
 */
public interface FoldingListener {
    interface ChangeFlags {
        int WIDTH_CHANGED = 0x1;
        int HEIGHT_CHANGED = 0x2;
        int GUTTER_ICON_PROVIDER_CHANGED = 0x4;
    }

    /**
     * Informs that {@code 'collapsed'} state of given fold region is just changed, or that the given fold region has just been created.
     * <p/>
     * <b>Note:</b> listener should delay fold region state processing until {@link #onFoldProcessingEnd()} is called.
     * I.e. folding model may return inconsistent data between current moment and {@link #onFoldProcessingEnd()}.
     *
     * @param region fold region that is just collapsed or expanded
     */
    default void onFoldRegionStateChange(@Nonnull FoldRegion region) {
    }

    /**
     * Called when properties (size or gutter renderer) are changed for a {@link CustomFoldRegion}. This can happen as a result of explicit
     * {@link CustomFoldRegion#update()} call, or due to an implicit update (e.g. when appearance settings change). The changes can happen
     * outside of batch folding operation.
     */
    default void onCustomFoldRegionPropertiesChange(@Nonnull CustomFoldRegion region,
                                                    @MagicConstant(flagsFromClass = ChangeFlags.class) int flags) {
    }

    /**
     * This method is called when the specified {@link FoldRegion} is about to become invalid. This can happen either due to explicit removal
     * of the region (using {@link FoldingModel#removeFoldRegion(FoldRegion)}, {@link FoldingModelEx#clearFoldRegions()} or
     * {@link FoldRegion#dispose()}), or as a result of document change.
     */
    default void beforeFoldRegionDisposed(@Nonnull FoldRegion region) {
    }

    /**
     * Informs that the given fold region is about to be removed.
     */
    default void beforeFoldRegionRemoved(@Nonnull FoldRegion region) {
    }

    /**
     * Informs that fold processing is done.
     */
    default void onFoldProcessingEnd() {
    }
}
