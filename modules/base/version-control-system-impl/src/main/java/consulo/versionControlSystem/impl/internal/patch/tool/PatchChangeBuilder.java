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
package consulo.versionControlSystem.impl.internal.patch.tool;

import consulo.diff.internal.DiffImplUtil;
import consulo.diff.internal.LineNumberConvertor;
import consulo.diff.util.LineRange;
import consulo.document.Document;
import consulo.document.internal.DocumentFactory;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.primitive.ints.IntList;
import consulo.util.collection.primitive.ints.IntLists;
import consulo.versionControlSystem.impl.internal.patch.apply.AppliedTextPatch;
import consulo.versionControlSystem.impl.internal.patch.apply.AppliedTextPatch.AppliedSplitPatchHunk;
import consulo.versionControlSystem.impl.internal.patch.apply.AppliedTextPatch.HunkStatus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.jspecify.annotations.Nullable;

public class PatchChangeBuilder {
  
  private final StringBuilder myBuilder = new StringBuilder();
  
  private final List<Hunk> myHunks = new ArrayList<>();
  
  private final LineNumberConvertor.Builder myConvertor = new LineNumberConvertor.Builder();
  
  private final IntList myChangedLines = IntLists.newArrayList();

  private int totalLines = 0;

  
  public static CharSequence getPatchedContent(AppliedTextPatch patch, String localContent) {
    PatchChangeBuilder builder = new PatchChangeBuilder();
    builder.exec(patch.getHunks());

    Document document = DocumentFactory.getInstance().createDocument(localContent, true);
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

  public void exec(List<AppliedSplitPatchHunk> splitHunks) {
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

  private void addContext(List<String> context, int beforeLineNumber, int afterLineNumber) {
    myConvertor.put1(totalLines, beforeLineNumber, context.size());
    myConvertor.put2(totalLines, afterLineNumber, context.size());
    appendLines(context);
  }

  private void appendLines(List<String> lines) {
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

  
  public CharSequence getPatchContent() {
    return myBuilder;
  }

  
  public List<Hunk> getHunks() {
    return myHunks;
  }

  
  public LineNumberConvertor getLineConvertor() {
    return myConvertor.build();
  }

  
  public IntList getSeparatorLines() {
    return myChangedLines;
  }


  public static class Hunk {
    
    private final List<String> myInsertedLines;
    
    private final LineRange myPatchDeletionRange;
    
    private final LineRange myPatchInsertionRange;

    private final @Nullable LineRange myAppliedToLines;
    
    private final HunkStatus myStatus;

    public Hunk(List<String> insertedLines,
                LineRange patchDeletionRange,
                LineRange patchInsertionRange,
                @Nullable LineRange appliedToLines,
                HunkStatus status) {
      myInsertedLines = insertedLines;
      myPatchDeletionRange = patchDeletionRange;
      myPatchInsertionRange = patchInsertionRange;
      myAppliedToLines = appliedToLines;
      myStatus = status;
    }

    
    public LineRange getPatchDeletionRange() {
      return myPatchDeletionRange;
    }

    
    public LineRange getPatchInsertionRange() {
      return myPatchInsertionRange;
    }

    
    public HunkStatus getStatus() {
      return myStatus;
    }

    public LineRange getAppliedToLines() {
      return myAppliedToLines;
    }

    
    private List<String> getInsertedLines() {
      return myInsertedLines;
    }
  }
}
