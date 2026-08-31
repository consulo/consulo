/*
 * Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.versionControlSystem.impl.internal;

import consulo.diff.internal.DiffImplUtil;
import consulo.versionControlSystem.internal.VcsRange;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import static consulo.versionControlSystem.UpToDateLineNumberProvider.ABSENT_LINE_NUMBER;

/**
 * Derives {@link VcsRange}s from the tracker blocks on demand, under the tracker lock.
 * There is no cached range list: {@link #getBlocks()} returns {@code null} while the tracker is
 * not valid, and every query then returns {@code null} as well.
 */
public abstract class LineStatusTrackerBlockOperations<B extends BlockI> {
    private final DocumentTracker myLock;

    protected LineStatusTrackerBlockOperations(DocumentTracker lock) {
        myLock = lock;
    }

    protected abstract @Nullable List<B> getBlocks();

    protected abstract VcsRange toRange(B block);

    public @Nullable List<VcsRange> getRanges() {
        return myLock.withRead(() -> {
            List<B> blocks = getNonEmptyBlocks();
            if (blocks == null) {
                return null;
            }
            List<VcsRange> result = new ArrayList<>(blocks.size());
            for (B block : blocks) {
                result.add(toRange(block));
            }
            return result;
        });
    }

    public @Nullable VcsRange findRange(VcsRange range) {
        B block = findBlock(range);
        return block != null ? toRange(block) : null;
    }

    public @Nullable B findBlock(VcsRange range) {
        return myLock.withRead(() -> {
            List<B> blocks = getBlocks();
            if (blocks == null) {
                return null;
            }
            for (B block : blocks) {
                if (matches(block, range)) {
                    return block;
                }
            }
            return null;
        });
    }

    public @Nullable VcsRange getNextRange(int line) {
        return myLock.withRead(() -> {
            List<B> blocks = getBlocks();
            if (blocks == null) {
                return null;
            }
            for (B block : blocks) {
                if (line < block.getEnd() && !isSelectedByLine(block, line)) {
                    return toRange(block);
                }
            }
            return null;
        });
    }

    public @Nullable VcsRange getPrevRange(int line) {
        return myLock.withRead(() -> {
            List<B> blocks = getBlocks();
            if (blocks == null) {
                return null;
            }
            for (int i = blocks.size() - 1; i >= 0; i--) {
                B block = blocks.get(i);
                if (line > block.getStart() && !isSelectedByLine(block, line)) {
                    return toRange(block);
                }
            }
            return null;
        });
    }

    /**
     * Consulo-specific: navigates relative to an already obtained range rather than a line number.
     */
    public @Nullable VcsRange getNextRange(VcsRange range) {
        return myLock.withRead(() -> {
            List<B> blocks = getNonEmptyBlocks();
            if (blocks == null) {
                return null;
            }
            int index = indexOfBlock(blocks, range);
            if (index == -1 || index == blocks.size() - 1) {
                return null;
            }
            return toRange(blocks.get(index + 1));
        });
    }

    /**
     * Consulo-specific: navigates relative to an already obtained range rather than a line number.
     */
    public @Nullable VcsRange getPrevRange(VcsRange range) {
        return myLock.withRead(() -> {
            List<B> blocks = getNonEmptyBlocks();
            if (blocks == null) {
                return null;
            }
            int index = indexOfBlock(blocks, range);
            if (index <= 0) {
                return null;
            }
            return toRange(blocks.get(index - 1));
        });
    }

    public @Nullable List<VcsRange> getRangesForLines(BitSet lines) {
        return myLock.withRead(() -> {
            List<B> blocks = getBlocks();
            if (blocks == null) {
                return null;
            }
            List<VcsRange> result = new ArrayList<>();
            for (B block : blocks) {
                if (isSelectedByLine(block, lines)) {
                    result.add(toRange(block));
                }
            }
            return result;
        });
    }

    public @Nullable VcsRange getRangeForLine(int line) {
        return myLock.withRead(() -> {
            List<B> blocks = getBlocks();
            if (blocks == null) {
                return null;
            }
            for (B block : blocks) {
                if (isSelectedByLine(block, line)) {
                    return toRange(block);
                }
            }
            return null;
        });
    }

    public boolean isLineModified(int line) {
        return isRangeModified(line, line + 1);
    }

    public boolean isRangeModified(int startLine, int endLine) {
        if (startLine == endLine) {
            return false;
        }
        assert startLine < endLine;

        return myLock.withRead(() -> {
            List<B> blocks = getBlocks();
            if (blocks == null) {
                return false;
            }
            for (B block : blocks) {
                if (block.getStart() >= endLine) {
                    return false;
                }
                if (block.getEnd() > startLine) {
                    return true;
                }
            }
            return false;
        });
    }

    public int transferLineFromVcs(int line, boolean approximate) {
        return transferLine(line, approximate, true);
    }

    public int transferLineToVcs(int line, boolean approximate) {
        return transferLine(line, approximate, false);
    }

    private int transferLine(int line, boolean approximate, boolean fromVcs) {
        return myLock.withRead(() -> {
            List<B> blocks = getBlocks();
            if (blocks == null) {
                return approximate ? line : ABSENT_LINE_NUMBER;
            }

            int result = line;

            for (B block : blocks) {
                int startLine1 = fromVcs ? block.getVcsStart() : block.getStart();
                int endLine1 = fromVcs ? block.getVcsEnd() : block.getEnd();
                int startLine2 = fromVcs ? block.getStart() : block.getVcsStart();
                int endLine2 = fromVcs ? block.getEnd() : block.getVcsEnd();

                if (startLine1 <= line && line < endLine1) {
                    return approximate ? startLine2 : ABSENT_LINE_NUMBER;
                }

                if (endLine1 > line) {
                    return result;
                }

                int length1 = endLine1 - startLine1;
                int length2 = endLine2 - startLine2;
                result += length2 - length1;
            }
            return result;
        });
    }

    private @Nullable List<B> getNonEmptyBlocks() {
        List<B> blocks = getBlocks();
        if (blocks == null) {
            return null;
        }
        List<B> result = new ArrayList<>(blocks.size());
        for (B block : blocks) {
            if (!block.isEmpty()) {
                result.add(block);
            }
        }
        return result;
    }

    private int indexOfBlock(List<B> blocks, VcsRange range) {
        for (int i = 0; i < blocks.size(); i++) {
            if (matches(blocks.get(i), range)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean matches(BlockI block, VcsRange range) {
        return block.getStart() == range.getLine1()
            && block.getEnd() == range.getLine2()
            && block.getVcsStart() == range.getVcsLine1()
            && block.getVcsEnd() == range.getVcsLine2();
    }

    public static boolean isSelectedByLine(BlockI block, int line) {
        return DiffImplUtil.isSelectedByLine(line, block.getStart(), block.getEnd());
    }

    public static boolean isSelectedByLine(BlockI block, BitSet lines) {
        return DiffImplUtil.isSelectedByLine(lines, block.getStart(), block.getEnd());
    }
}
