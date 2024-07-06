/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.diff;

import consulo.diff.comparison.ByLine;
import consulo.diff.comparison.ComparisonPolicy;
import consulo.diff.comparison.DiffTooBigException;
import consulo.diff.comparison.iterable.DiffIterableUtil;
import consulo.diff.comparison.iterable.FairDiffIterable;
import consulo.diff.util.Range;
import consulo.logging.Logger;
import consulo.application.progress.DumbProgressIndicator;
import consulo.util.lang.Pair;
import consulo.application.util.LineTokenizer;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;

import java.util.Arrays;
import java.util.List;

/**
 * author: lesya
 */
public class Block {
  private static final Logger LOG = Logger.getInstance(Block.class);

  @Nonnull
  private final String[] mySource;
  private final int myStart;
  private final int myEnd;

  public Block(@Nonnull String source, int start, int end) {
    this(tokenize(source), start, end);
  }

  public Block(@Nonnull String[] source, int start, int end) {
    mySource = source;
    myStart = Math.min(Math.max(0, start), source.length);
    myEnd = Math.min(Math.max(myStart, end), source.length);
  }

  @Nonnull
  public static String[] tokenize(@Nonnull String text) {
    return LineTokenizer.tokenize(text, false, false);
  }

  @Nonnull
  public Block createPreviousBlock(@Nonnull String prevContent) {
    return createPreviousBlock(tokenize(prevContent));
  }

  @Nonnull
  public Block createPreviousBlock(@Nonnull String[] prevContent) {
    int start = -1;
    int end = -1;
    int shift = 0;

    try {
      FairDiffIterable iterable = ByLine.compare(Arrays.asList(prevContent), Arrays.asList(mySource),
                                                 ComparisonPolicy.IGNORE_WHITESPACES, DumbProgressIndicator.INSTANCE);

      for (Pair<Range, Boolean> pair : DiffIterableUtil.iterateAll(iterable)) {
        Boolean equals = pair.second;
        Range range = pair.first;
        if (!equals) {
          if (Math.max(myStart, range.start2) < Math.min(myEnd, range.end2)) {
            // ranges intersect
            if (range.start2 <= myStart) start = range.start1;
            if (range.end2 > myEnd) end = range.end1;
          }
          if (range.start2 > myStart) {
            if (start == -1) start = myStart - shift;
            if (end == -1 && range.start2 >= myEnd) end = myEnd - shift;
          }

          shift += (range.end2 - range.start2) - (range.end1 - range.start1);
        }
        else {
          // intern strings, reducing memory usage
          int count = range.end1 - range.start1;
          for (int i = 0; i < count; i++) {
            int prevIndex = range.start1 + i;
            int sourceIndex = range.start2 + i;
            if (prevContent[prevIndex].equals(mySource[sourceIndex])) {
              prevContent[prevIndex] = mySource[sourceIndex];
            }
          }
        }
      }
      if (start == -1) start = myStart - shift;
      if (end == -1) end = myEnd - shift;

      if (start < 0 || end > prevContent.length || end < start) {
        LOG.error("Invalid block range: [" + start + ", " + end + "); length - " + prevContent.length);
      }

      return new Block(prevContent, start, end);
    }
    catch (DiffTooBigException e) {
      return new Block(prevContent, 0, 0);
    }
  }

  @Nonnull
  public String getBlockContent() {
    return StringUtil.join(getLines(), "\n");
  }

  @Nonnull
  public List<String> getLines() {
    return Arrays.asList(mySource).subList(myStart, myEnd);
  }

  public int hashCode() {
    return Arrays.hashCode(mySource) ^ myStart ^ myEnd;
  }

  public boolean equals(Object object) {
    if (!(object instanceof Block)) return false;
    Block other = (Block)object;
    return Arrays.equals(mySource, other.mySource)
           && myStart == other.myStart
           && myEnd == other.myEnd;
  }

  public int getStart() {
    return myStart;
  }

  public int getEnd() {
    return myEnd;
  }

  public String toString() {
    StringBuilder result = new StringBuilder();

    appendLines(result, 0, myStart);

    result.append("<-----------------------------\n");

    appendLines(result, myStart, myEnd);

    result.append("----------------------------->\n");

    appendLines(result, myEnd, mySource.length);

    return result.toString();
  }

  private void appendLines(@Nonnull StringBuilder result, int from, int to) {
    for (int i = from; i < to; i++) {
      result.append(mySource[i]);
      result.append("\n");
    }
  }
}
