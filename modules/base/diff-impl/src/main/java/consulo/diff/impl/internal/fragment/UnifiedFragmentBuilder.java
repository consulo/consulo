/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.diff.impl.internal.fragment;

import consulo.diff.fragment.LineFragment;
import consulo.diff.util.LineRange;
import consulo.diff.util.Side;
import consulo.document.Document;
import consulo.document.util.TextRange;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

// This class works incorrectly with non-fair differences (when chunk of matched lines has different length in left/right files)
public class UnifiedFragmentBuilder {
  @Nonnull
  private final List<LineFragment> myFragments;
  @Nonnull
  private final Document myDocument1;
  @Nonnull
  private final Document myDocument2;
  @Nonnull
  private final Side myMasterSide;

  @Nonnull
  private final StringBuilder myBuilder = new StringBuilder();
  @Nonnull
  private final List<ChangedBlock> myBlocks = new ArrayList<ChangedBlock>();
  @Nonnull
  private final List<HighlightRange> myRanges = new ArrayList<HighlightRange>();
  @Nonnull
  private final LineNumberConvertor.Builder myConvertor = new LineNumberConvertor.Builder();
  @Nonnull
  private final List<LineRange> myChangedLines = new ArrayList<LineRange>();

  public UnifiedFragmentBuilder(@Nonnull List<LineFragment> fragments,
                                @Nonnull Document document1,
                                @Nonnull Document document2,
                                @Nonnull Side masterSide) {
    myFragments = fragments;
    myDocument1 = document1;
    myDocument2 = document2;
    myMasterSide = masterSide;
  }

  private boolean myEqual = false;

  private int lastProcessedLine1 = -1;
  private int lastProcessedLine2 = -1;
  private int totalLines = 0;

  public void exec() {
    if (myFragments.isEmpty()) {
      myEqual = true;
      appendTextMaster(0, 0, getLineCount(myDocument1) - 1, getLineCount(myDocument2) - 1);
      return;
    }

    for (LineFragment fragment : myFragments) {
      processEquals(fragment.getStartLine1() - 1, fragment.getStartLine2() - 1);
      processChanged(fragment);
    }
    processEquals(getLineCount(myDocument1) - 1, getLineCount(myDocument2) - 1);
  }

  private void processEquals(int endLine1, int endLine2) {
    int startLine1 = lastProcessedLine1 + 1;
    int startLine2 = lastProcessedLine2 + 1;

    appendTextMaster(startLine1, startLine2, endLine1, endLine2);
  }

  @SuppressWarnings("UnnecessaryLocalVariable")
  private void processChanged(@Nonnull LineFragment fragment) {
    int startLine1 = fragment.getStartLine1();
    int endLine1 = fragment.getEndLine1() - 1;
    int lines1 = endLine1 - startLine1;

    int startLine2 = fragment.getStartLine2();
    int endLine2 = fragment.getEndLine2() - 1;
    int lines2 = endLine2 - startLine2;

    int linesBefore = totalLines;
    int linesAfter;

    if (lines1 >= 0) {
      int startOffset = myDocument1.getLineStartOffset(startLine1);
      int endOffset = myDocument1.getLineEndOffset(endLine1);

      appendTextSide(Side.LEFT, startOffset, endOffset, lines1, startLine1, -1);
    }

    int linesBetween = totalLines;

    if (lines2 >= 0) {
      int startOffset = myDocument2.getLineStartOffset(startLine2);
      int endOffset = myDocument2.getLineEndOffset(endLine2);

      appendTextSide(Side.RIGHT, startOffset, endOffset, lines2, -1, startLine2);
    }

    linesAfter = totalLines;

    int blockStartLine1 = linesBefore;
    int blockEndLine1 = linesBetween;
    int blockStartLine2 = linesBetween;
    int blockEndLine2 = linesAfter;

    myBlocks.add(new ChangedBlock(linesBefore, linesAfter,
                                  new LineRange(blockStartLine1, blockEndLine1),
                                  new LineRange(blockStartLine2, blockEndLine2),
                                  fragment));

    lastProcessedLine1 = endLine1;
    lastProcessedLine2 = endLine2;
  }

  private void appendTextMaster(int startLine1, int startLine2, int endLine1, int endLine2) {
    int lines = myMasterSide.isLeft() ? endLine1 - startLine1 : endLine2 - startLine2;

    if (lines >= 0) {
      int startOffset = myMasterSide.isLeft() ? myDocument1.getLineStartOffset(startLine1) : myDocument2.getLineStartOffset(startLine2);
      int endOffset = myMasterSide.isLeft() ? myDocument1.getLineEndOffset(endLine1) : myDocument2.getLineEndOffset(endLine2);

      appendText(myMasterSide, startOffset, endOffset, lines, startLine1, startLine2);
    }
  }

  private void appendTextSide(@Nonnull Side side, int offset1, int offset2, int lines, int startLine1, int startLine2) {
    int linesBefore = totalLines;
    appendText(side, offset1, offset2, lines, startLine1, startLine2);
    int linesAfter = totalLines;

    myChangedLines.add(new LineRange(linesBefore, linesAfter));
  }

  private void appendText(@Nonnull Side side, int offset1, int offset2, int lines, int startLine1, int startLine2) {
    Document document = side.select(myDocument1, myDocument2);

    int newline = document.getTextLength() > offset2 + 1 ? 1 : 0;
    TextRange base = new TextRange(myBuilder.length(), myBuilder.length() + offset2 - offset1 + newline);
    TextRange changed = new TextRange(offset1, offset2 + newline);
    myRanges.add(new HighlightRange(side, base, changed));

    myBuilder.append(document.getCharsSequence().subSequence(offset1, offset2));
    myBuilder.append('\n');

    if (startLine1 != -1) {
      myConvertor.put1(totalLines, startLine1, lines + 1);
    }
    if (startLine2 != -1) {
      myConvertor.put2(totalLines, startLine2, lines + 1);
    }

    totalLines += lines + 1;
  }

  private static int getLineCount(@Nonnull Document document) {
    return Math.max(document.getLineCount(), 1);
  }

  //
  // Result
  //


  public boolean isEqual() {
    return myEqual;
  }

  @Nonnull
  public CharSequence getText() {
    return myBuilder;
  }

  @Nonnull
  public List<ChangedBlock> getBlocks() {
    return myBlocks;
  }

  @Nonnull
  public List<HighlightRange> getRanges() {
    return myRanges;
  }

  @Nonnull
  public LineNumberConvertor getConvertor() {
    return myConvertor.build();
  }

  @Nonnull
  public List<LineRange> getChangedLines() {
    return myChangedLines;
  }
}
