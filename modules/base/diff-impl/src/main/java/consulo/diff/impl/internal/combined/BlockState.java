// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.diff.impl.internal.combined;

import consulo.diff.PrevNextDifferenceIterable;
import consulo.diff.combined.CombinedBlockId;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class BlockState implements PrevNextDifferenceIterable, BlockOrder {
    private final List<BlockStateListener> myListeners = new CopyOnWriteArrayList<>();

    private final List<CombinedBlockId> myBlocks;

    private final Map<CombinedBlockId, Integer> myBlockByIndex = new HashMap<>();

    private CombinedBlockId myCurrentBlock;

    public BlockState(List<CombinedBlockId> list, CombinedBlockId current) {
        myBlocks = List.copyOf(list);
        for (int index = 0; index < myBlocks.size(); index++) {
            myBlockByIndex.put(myBlocks.get(index), index);
        }
        // todo: find and fix initial problem in Space review integration
        myCurrentBlock = myBlocks.contains(current) ? current : myBlocks.get(0);
    }

    public CombinedBlockId getCurrentBlock() {
        return myCurrentBlock;
    }

    public void setCurrentBlock(CombinedBlockId currentBlock) {
        CombinedBlockId oldValue = myCurrentBlock;
        myCurrentBlock = currentBlock;
        if (!oldValue.equals(currentBlock)) {
            for (BlockStateListener listener : myListeners) {
                listener.onCurrentChanged(oldValue, currentBlock);
            }
        }
    }

    public void addListener(BlockStateListener listener, Disposable disposable) {
        myListeners.add(listener);
        Disposer.register(disposable, () -> myListeners.remove(listener));
    }

    @Override
    public int indexOf(CombinedBlockId blockId) {
        Integer index = myBlockByIndex.get(blockId);
        if (index == null) {
            throw new IllegalArgumentException("Unknown block " + blockId);
        }
        return index;
    }

    @Override
    public @Nullable CombinedBlockId getOrNull(int index) {
        return index >= 0 && index < myBlocks.size() ? myBlocks.get(index) : null;
    }

    @Override
    public int getBlocksCount() {
        return myBlocks.size();
    }

    @Override
    public Iterable<CombinedBlockId> iterateBlocks() {
        return myBlocks;
    }

    @Override
    public boolean canGoPrev() {
        return getCurrentIndex() > 0;
    }

    @Override
    public boolean canGoNext() {
        return getCurrentIndex() < getBlocksCount() - 1;
    }

    @Override
    public void goPrev() {
        setCurrentBlock(myBlocks.get(getCurrentIndex() - 1));
    }

    @Override
    public void goNext() {
        setCurrentBlock(myBlocks.get(getCurrentIndex() + 1));
    }

    private int getCurrentIndex() {
        return indexOf(myCurrentBlock);
    }
}
