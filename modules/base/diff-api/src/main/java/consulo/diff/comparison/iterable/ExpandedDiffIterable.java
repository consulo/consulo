/*
 * Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */
package consulo.diff.comparison.iterable;

import consulo.diff.util.Range;

import java.util.Iterator;

class ExpandedDiffIterable extends ChangeDiffIterableBase {
    private final DiffIterable myIterable;
    private final int myOffset1;
    private final int myOffset2;

    ExpandedDiffIterable(DiffIterable iterable, int offset1, int offset2, int length1, int length2) {
        super(length1, length2);
        myIterable = iterable;
        myOffset1 = offset1;
        myOffset2 = offset2;
    }

    @Override
    protected ChangeIterable createChangeIterable() {
        return new ShiftedChangeIterable(myIterable, myOffset1, myOffset2);
    }

    private static class ShiftedChangeIterable implements ChangeIterable {
        private final Iterator<Range> myIterator;
        private final int myOffset1;
        private final int myOffset2;

        private Range myLast;

        ShiftedChangeIterable(DiffIterable iterable, int offset1, int offset2) {
            myIterator = iterable.changes();
            myOffset1 = offset1;
            myOffset2 = offset2;
            next();
        }

        @Override
        public boolean valid() {
            return myLast != null;
        }

        @Override
        public void next() {
            myLast = myIterator.hasNext() ? myIterator.next() : null;
        }

        @Override
        public int getStart1() {
            return myLast.start1 + myOffset1;
        }

        @Override
        public int getStart2() {
            return myLast.start2 + myOffset2;
        }

        @Override
        public int getEnd1() {
            return myLast.end1 + myOffset1;
        }

        @Override
        public int getEnd2() {
            return myLast.end2 + myOffset2;
        }
    }
}
