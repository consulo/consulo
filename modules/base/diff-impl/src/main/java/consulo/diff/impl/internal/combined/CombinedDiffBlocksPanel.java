// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.diff.impl.internal.combined;

import consulo.diff.combined.CombinedBlockId;
import consulo.ui.Component;
import consulo.ui.Rectangle2D;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.internal.ScrollableLayoutInternal;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.layout.WrappedLayout;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

final class CombinedDiffBlocksPanel {
    private final BlockOrder myBlockOrder;
    private final VerticalLayout myComponent;
    private final List<WrappedLayout> mySlots = new ArrayList<>();
    private final int[] myPlaceholderHeights;
    private final boolean[] myHasContent;

    private @Nullable ScrollableLayoutInternal myScroll;
    private int myLastBlockMinHeight = -1;

    @RequiredUIAccess
    CombinedDiffBlocksPanel(BlockOrder blockOrder) {
        myBlockOrder = blockOrder;
        myComponent = VerticalLayout.create(Space.LARGE);
        myPlaceholderHeights = new int[blockOrder.getBlocksCount()];
        myHasContent = new boolean[blockOrder.getBlocksCount()];

        for (int i = 0; i < blockOrder.getBlocksCount(); i++) {
            myPlaceholderHeights[i] = CombinedDiffLoadingBlock.DEFAULT_LOADING_BLOCK_HEIGHT;
            WrappedLayout slot = WrappedLayout.create(createPlaceholder(myPlaceholderHeights[i]));
            mySlots.add(slot);
            myComponent.add(slot);
        }
    }

    Component getComponent() {
        return myComponent;
    }

    @RequiredUIAccess
    void install(ScrollableLayoutInternal scroll) {
        myScroll = scroll;
        for (WrappedLayout slot : mySlots) {
            scroll.setBoundsTracked(slot, true);
        }
    }

    @RequiredUIAccess
    void setContent(CombinedBlockId blockId, Component component) {
        int blockIndex = myBlockOrder.indexOf(blockId);
        myHasContent[blockIndex] = true;
        mySlots.get(blockIndex).set(component);
    }

    @RequiredUIAccess
    void setPlaceholder(CombinedBlockId blockId, @Nullable Integer height) {
        int blockIndex = myBlockOrder.indexOf(blockId);
        int placeholderHeight = height != null ? height : CombinedDiffLoadingBlock.DEFAULT_LOADING_BLOCK_HEIGHT;
        myPlaceholderHeights[blockIndex] = placeholderHeight;
        myHasContent[blockIndex] = false;
        mySlots.get(blockIndex).set(createPlaceholder(placeholderHeight));
    }

    @RequiredUIAccess
    void setLastBlockMinHeight(int minHeight) {
        if (mySlots.isEmpty() || myLastBlockMinHeight == minHeight) {
            return;
        }
        myLastBlockMinHeight = minHeight;
        mySlots.get(mySlots.size() - 1).setMinHeight(minHeight);
    }

    @RequiredUIAccess
    int getHeightForBlock(CombinedBlockId blockId) {
        BlockBounds bounds = getBoundsForBlock(blockId);
        if (bounds != null) {
            return bounds.height();
        }
        return myPlaceholderHeights[myBlockOrder.indexOf(blockId)];
    }

    @RequiredUIAccess
    @Nullable BlockBounds getBoundsForBlock(CombinedBlockId blockId) {
        return getBounds(blockId, myBlockOrder.indexOf(blockId));
    }

    @RequiredUIAccess
    List<BlockBounds> getBlockBounds() {
        List<BlockBounds> bounds = new ArrayList<>();
        int index = 0;
        for (CombinedBlockId id : myBlockOrder.iterateBlocks()) {
            BlockBounds blockBounds = getBounds(id, index);
            if (blockBounds != null) {
                bounds.add(blockBounds);
            }
            index++;
        }
        return bounds;
    }

    @RequiredUIAccess
    private @Nullable BlockBounds getBounds(CombinedBlockId blockId, int index) {
        ScrollableLayoutInternal scroll = myScroll;
        if (scroll == null) {
            return null;
        }
        Rectangle2D rect = scroll.getBounds(mySlots.get(index));
        if (rect == null) {
            return null;
        }
        return new BlockBounds(blockId, rect.minY(), rect.maxY());
    }

    @RequiredUIAccess
    private static Component createPlaceholder(int height) {
        VerticalLayout placeholder = VerticalLayout.create();
        placeholder.setHeight(height);
        return placeholder;
    }
}
