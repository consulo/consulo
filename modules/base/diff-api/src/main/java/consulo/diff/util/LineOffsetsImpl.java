/*
 * Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */
package consulo.diff.util;

import consulo.util.collection.primitive.ints.IntList;
import consulo.util.collection.primitive.ints.IntLists;

import java.util.Arrays;

public class LineOffsetsImpl implements LineOffsets {
    private final int[] myLineEnds;
    private final int myTextLength;

    private LineOffsetsImpl(int[] lineEnds, int textLength) {
        myLineEnds = lineEnds;
        myTextLength = textLength;
    }

    @Override
    public int getLineStart(int line) {
        checkLineIndex(line);
        if (line == 0) return 0;
        return myLineEnds[line - 1] + 1;
    }

    @Override
    public int getLineEnd(int line) {
        checkLineIndex(line);
        return myLineEnds[line];
    }

    @Override
    public int getLineEnd(int line, boolean includeNewline) {
        checkLineIndex(line);
        return myLineEnds[line] + (includeNewline && line != myLineEnds.length - 1 ? 1 : 0);
    }

    @Override
    public int getLineNumber(int offset) {
        if (offset < 0 || offset > myTextLength) {
            throw new IndexOutOfBoundsException("Wrong offset: " + offset + ". Available text length: " + myTextLength);
        }
        if (offset == 0) return 0;
        if (offset == myTextLength) return getLineCount() - 1;

        int bsResult = Arrays.binarySearch(myLineEnds, offset);
        return bsResult >= 0 ? bsResult : -bsResult - 1;
    }

    @Override
    public int getLineCount() {
        return myLineEnds.length;
    }

    @Override
    public int getTextLength() {
        return myTextLength;
    }

    private void checkLineIndex(int index) {
        if (index < 0 || index >= getLineCount()) {
            throw new IndexOutOfBoundsException("Wrong line: " + index + ". Available lines count: " + getLineCount());
        }
    }

    public static LineOffsets create(CharSequence text) {
        IntList ends = IntLists.newArrayList();

        int index = 0;
        while (true) {
            int lineEnd = indexOf(text, '\n', index);
            if (lineEnd != -1) {
                ends.add(lineEnd);
                index = lineEnd + 1;
            }
            else {
                ends.add(text.length());
                break;
            }
        }

        return new LineOffsetsImpl(ends.toArray(), text.length());
    }

    private static int indexOf(CharSequence text, char c, int fromIndex) {
        for (int i = fromIndex; i < text.length(); i++) {
            if (text.charAt(i) == c) return i;
        }
        return -1;
    }
}
