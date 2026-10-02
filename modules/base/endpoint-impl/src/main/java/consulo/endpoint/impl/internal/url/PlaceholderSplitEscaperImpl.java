// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.impl.internal.url;

import consulo.endpoint.url.UrlSpecialSegmentMarker;
import consulo.language.psi.util.SplitEscaper;
import consulo.util.lang.Pair;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import org.jspecify.annotations.Nullable;

import java.util.List;

final class PlaceholderSplitEscaperImpl implements SplitEscaper {
    private final IntSet myToIgnore;

    PlaceholderSplitEscaperImpl(List<UrlSpecialSegmentMarker> braces, CharSequence input, String pattern) {
        IntArrayList toIgnore = new IntArrayList();
        for (UrlSpecialSegmentMarker beginEndPair : braces) {
            List<String> matches = List.of(beginEndPair.getPrefix(), beginEndPair.getSuffix(), pattern);
            int position = 0;
            int openBraces = 0;

            while (true) {
                @Nullable Pair<Integer, String> found = findAnyOf(input, matches, position);
                if (found == null) {
                    break;
                }
                int pos = found.getFirst();
                String str = found.getSecond();
                if (str.equals(beginEndPair.getPrefix())) {
                    openBraces++;
                }
                else if (str.equals(beginEndPair.getSuffix())) {
                    openBraces--;
                }
                else if (str.equals(pattern)) {
                    if (openBraces > 0) {
                        toIgnore.add(pos);
                    }
                }
                if (openBraces < 0) {
                    openBraces = 0;
                }
                position = pos + str.length();
            }
        }

        myToIgnore = toIgnore.isEmpty() ? IntSets.EMPTY_SET : new IntOpenHashSet(toIgnore);
    }

    private static @Nullable Pair<Integer, String> findAnyOf(CharSequence input, List<String> strings, int startIndex) {
        for (int index = Math.max(startIndex, 0); index <= input.length(); index++) {
            for (String string : strings) {
                if (regionMatches(string, input, index)) {
                    return Pair.create(index, string);
                }
            }
        }
        return null;
    }

    private static boolean regionMatches(String string, CharSequence input, int index) {
        int length = string.length();
        if (index + length > input.length()) {
            return false;
        }
        for (int i = 0; i < length; i++) {
            if (string.charAt(i) != input.charAt(index + i)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean filter(int lastSplit, int currentPosition) {
        return !myToIgnore.contains(currentPosition);
    }
}
