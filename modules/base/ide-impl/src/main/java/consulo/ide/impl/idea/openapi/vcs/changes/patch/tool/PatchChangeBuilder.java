/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.vcs.changes.patch.tool;

import consulo.diff.impl.internal.fragment.LineNumberConvertor;
import consulo.diff.impl.internal.util.DiffImplUtil;
import consulo.diff.util.LineRange;
import consulo.document.impl.DocumentImpl;
import consulo.ide.impl.idea.openapi.vcs.changes.patch.AppliedTextPatch;
import consulo.ide.impl.idea.openapi.vcs.changes.patch.AppliedTextPatch.AppliedSplitPatchHunk;
import consulo.ide.impl.idea.openapi.vcs.changes.patch.AppliedTextPatch.HunkStatus;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.util.collection.primitive.ints.IntList;
import consulo.util.collection.primitive.ints.IntLists;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PatchChangeBuilder {
  @Nonnull
  private final StringBuilder myBuilder = new StringBuilder();
  @Nonnull
  private final List<Hunk> myHunks = new ArrayList<>();
  @Nonnull
  private final LineNumberConvertor.Builder myConvertor = new LineNumberConvertor.Builder();
  @Nonnull
  private final IntList myChangedLines = IntLists.newArrayList();

  private int totalLines = 0;

  @Nonnull
  public static CharSequence getPatchedContent(@Nonnull AppliedTextPatch patch, @Nonnull String localContent) {
    PatchChangeBuilder builder = new PatchChangeBuilder();
    builder.exec(patch.getHunks());

    DocumentImpl document = new DocumentImpl(localContent, true);
    List<Hunk> appliedHunks = ContainerUtil.filter(builder.getHunks(), (h) -> h.getStatus() == HunkStatus.EXACTLY_APPLIED);
    ContainerUtil.sort(appliedHunks, Comparator.comparingInt(h -> h.getAppliedToLines().start));

    for (int i = appliedHunks.size() - 1; i >= 0; i--) {
      Hunk hunk = appliedHunks.get(i);
      LineRange appliedTo = hunk.getAppliedToLines();
      List<String> inserted = hunk.getInsertedLines();

      DiffImplUtil.applyModification(document, appliedTo.start, appliedTo.end, inserted);
    }

    return document.getText();
  }

  public void exec(@Nonnull List<AppliedSplitPatchHunk> splitHunks) {
    int lastBeforeLine = -1;
    for (AppliedSplitPatchHunk hunk : splitHunks) {
      List<String> contextBefore = hunk.getContextBefore();
      List<String> contextAfter = hunk.getContextAfter();

      LineRange beforeRange = hunk.getLineRangeBefore();
      LineRange afterRange = hunk.getLineRangeAfter();

      int overlappedContext = 0;
      if (lastBeforeLine != -1) {
        if (lastBeforeLine >= beforeRange.start) {
          overlappedContext = lastBeforeLine - beforeRange.start + 1;
        }
        else if (lastBeforeLine < beforeRange.start - 1) {
          appendSeparator();
        }
      }

      List<String> trimContext = contextBefore.subList(overlappedContext, contextBefore.size());
      addContext(trimContext, beforeRange.start + overlappedContext, afterRange.start + overlappedContext);


      int deletion = totalLines;
      appendLines(hunk.getDeletedLines());
      int insertion = totalLines;
      appendLines(hunk.getInsertedLines());
      int hunkEnd = totalLines;

      myConvertor.put1(deletion, beforeRange.start + contextBefore.size(), insertion - deletion);
      myConvertor.put2(insertion, afterRange.start + contextBefore.size(), hunkEnd - insertion);


      addContext(contextAfter, beforeRange.end - contextAfter.size(), afterRange.end - contextAfter.size());
      lastBeforeLine = beforeRange.end - 1;


      LineRange deletionRange = new LineRange(deletion, insertion);
      LineRange insertionRange = new LineRange(insertion, hunkEnd);

      myHunks.add(new Hunk(hunk.getInsertedLines(), deletionRange, insertionRange, hunk.getAppliedTo(), hunk.getStatus()));
    }
  }

  private void addContext(@Nonnull List<String> context, int beforeLineNumber, int afterLineNumber) {
    myConvertor.put1(totalLines, beforeLineNumber, context.size());
    myConvertor.put2(totalLines, afterLineNumber, context.size());
    appendLines(context);
  }

  private void appendLines(@Nonnull List<String> lines) {
    for (String line : lines) {
      myBuilder.append(line).append("\n");
    }
    totalLines += lines.size();
  }

  private void appendSeparator() {
    myChangedLines.add(totalLines);
    myBuilder.append("\n");
    totalLines++;
  }

  //
  // Result
  //

  @Nonnull
  public CharSequence getPatchContent() {
    return myBuilder;
  }

  @Nonnull
  public List<Hunk> getHunks() {
    return myHunks;
  }

  @Nonnull
  public LineNumberConvertor getLineConvertor() {
    return myConvertor.build();
  }

  @Nonnull
  public IntList getSeparatorLines() {
    return myChangedLines;
  }


  public static class Hunk {
    @Nonnull
    private final List<String> myInsertedLines;
    @Nonnull
    private final LineRange myPatchDeletionRange;
    @Nonnull
    private final LineRange myPatchInsertionRange;

    @jakarta.annotation.Nullable
    private final LineRange myAppliedToLines;
    @Nonnull
    private final HunkStatus myStatus;

    public Hunk(@Nonnull List<String> insertedLines,
                @Nonnull LineRange patchDeletionRange,
                @Nonnull LineRange patchInsertionRange,
                @jakarta.annotation.Nullable LineRange appliedToLines,
                @Nonnull HunkStatus status) {
      myInsertedLines = insertedLines;
      myPatchDeletionRange = patchDeletionRange;
      myPatchInsertionRange = patchInsertionRange;
      myAppliedToLines = appliedToLines;
      myStatus = status;
    }

    @Nonnull
    public LineRange getPatchDeletionRange() {
      return myPatchDeletionRange;
    }

    @Nonnull
    public LineRange getPatchInsertionRange() {
      return myPatchInsertionRange;
    }

    @Nonnull
    public HunkStatus getStatus() {
      return myStatus;
    }

    public LineRange getAppliedToLines() {
      return myAppliedToLines;
    }

    @Nonnull
    private List<String> getInsertedLines() {
      return myInsertedLines;
    }
  }
}
