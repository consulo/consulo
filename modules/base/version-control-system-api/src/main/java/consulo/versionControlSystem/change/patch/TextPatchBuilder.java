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
package consulo.versionControlSystem.change.patch;

import consulo.application.util.diff.FilesTooBigForDiffException;
import consulo.diff.old.*;
import consulo.util.io.FileUtil;
import consulo.util.lang.BeforeAfter;
import consulo.versionControlSystem.VcsException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NonNls;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * TODO rewrite to new diff impl
 * 
 * @author yole
 */
public class TextPatchBuilder {
  private static final int CONTEXT_LINES = 3;
  @NonNls private static final String REVISION_NAME_TEMPLATE = "(revision {0})";
  @NonNls private static final String DATE_NAME_TEMPLATE = "(date {0})";

  private final String myBasePath;
  private final boolean myIsReversePath;
  private final boolean myIsCaseSensitive;
  @Nullable
  private final Runnable myCancelChecker;
  private final boolean myIncludeBaseText;

  private TextPatchBuilder(String basePath, boolean isReversePath, boolean isCaseSensitive,
                           @Nullable Runnable cancelChecker, boolean includeBaseText) {
    myBasePath = basePath;
    myIsReversePath = isReversePath;
    myIsCaseSensitive = isCaseSensitive;
    myCancelChecker = cancelChecker;
    myIncludeBaseText = includeBaseText;
  }

  private void checkCanceled() {
    if (myCancelChecker != null) {
      myCancelChecker.run();
    }
  }

  public static List<FilePatch> buildPatch(Collection<BeforeAfter<AirContentRevision>> changes, String basePath,
                                           boolean reversePatch,
                                           boolean isCaseSensitive,
                                           @Nullable Runnable cancelChecker,
                                           boolean includeBaseText) throws VcsException {
    TextPatchBuilder builder = new TextPatchBuilder(basePath, reversePatch, isCaseSensitive, cancelChecker, includeBaseText);
    return builder.build(changes);
  }

  private List<FilePatch> build(Collection<BeforeAfter<AirContentRevision>> changes) throws VcsException {
    List<FilePatch> result = new ArrayList<FilePatch>();
    for(BeforeAfter<AirContentRevision> c: changes) {
      checkCanceled();

      AirContentRevision beforeRevision;
      AirContentRevision afterRevision;
      if (myIsReversePath) {
        beforeRevision = c.getAfter();
        afterRevision = c.getBefore();
      }
      else {
        beforeRevision = c.getBefore();
        afterRevision = c.getAfter();
      }
      if (beforeRevision != null && beforeRevision.getPath().isDirectory()) {
        continue;
      }
      if (afterRevision != null && afterRevision.getPath().isDirectory()) {
        continue;
      }

      if ((beforeRevision != null) && beforeRevision.isBinary() || (afterRevision != null) && afterRevision.isBinary()) {
        result.add(buildBinaryPatch(myBasePath, beforeRevision, afterRevision));
        continue;
      }

      if (beforeRevision == null) {
        result.add(buildAddedFile(myBasePath, afterRevision));
        continue;
      }
      if (afterRevision == null) {
        result.add(buildDeletedFile(myBasePath, beforeRevision));
        continue;
      }

      DiffString beforeContent = DiffString.createNullable(beforeRevision.getContentAsString());
      if (beforeContent == null) {
        throw new VcsException("Failed to fetch old content for changed file " + beforeRevision.getPath().getPath());
      }
      DiffString afterContent = DiffString.createNullable(afterRevision.getContentAsString());
      if (afterContent == null) {
        throw new VcsException("Failed to fetch new content for changed file " + afterRevision.getPath().getPath());
      }
      DiffString[] beforeLines = tokenize(beforeContent);
      DiffString[] afterLines = tokenize(afterContent);

      DiffFragmentOld[] woFormattingBlocks;
        DiffFragmentOld[] step1lineFragments;
      try {
        woFormattingBlocks = DiffPolicyOld.LINES_WO_FORMATTING.buildFragments(beforeContent, afterContent);
        step1lineFragments = new DiffCorrectionOld.TrueLineBlocks(ComparisonPolicyOld.DEFAULT).correctAndNormalize(woFormattingBlocks);
      }
      catch (FilesTooBigForDiffException e) {
        throw new VcsException("File '" + myBasePath + "' is too big and there are too many changes to build diff", e);
      }
      List<LineFragment> fragments = new DiffFragmentsProcessorOld().process(step1lineFragments);

      if (fragments.size() > 1 || (fragments.size() == 1 && fragments.get(0).getType() != null && fragments.get(0).getType() != TextDiffTypeEnum.NONE)) {
        TextFilePatch patch = buildPatchHeading(myBasePath, beforeRevision, afterRevision);
        result.add(patch);

        int lastLine1 = 0;
        int lastLine2 = 0;

        while(fragments.size() > 0) {
          checkCanceled();

          List<LineFragment> adjacentFragments = getAdjacentFragments(fragments);
          if (adjacentFragments.size() > 0) {
            LineFragment first = adjacentFragments.get(0);
            LineFragment last = adjacentFragments.get(adjacentFragments.size()-1);

            int start1 = first.getStartingLine1();
            int start2 = first.getStartingLine2();
            int end1 = last.getStartingLine1() + last.getModifiedLines1();
            int end2 = last.getStartingLine2() + last.getModifiedLines2();
            int contextStart1 = Math.max(start1 - CONTEXT_LINES, lastLine1);
            int contextStart2 = Math.max(start2 - CONTEXT_LINES, lastLine2);
            int contextEnd1 = Math.min(end1 + CONTEXT_LINES, beforeLines.length);
            int contextEnd2 = Math.min(end2 + CONTEXT_LINES, afterLines.length);

            PatchHunk hunk = new PatchHunk(contextStart1, contextEnd1, contextStart2, contextEnd2);
            patch.addHunk(hunk);

            for(LineFragment fragment: adjacentFragments) {
              checkCanceled();

              for(int i=contextStart1; i<fragment.getStartingLine1(); i++) {
                addLineToHunk(hunk, beforeLines[i], PatchLine.Type.CONTEXT);
              }
              for(int i=fragment.getStartingLine1(); i<fragment.getStartingLine1()+fragment.getModifiedLines1(); i++) {
                addLineToHunk(hunk, beforeLines[i], PatchLine.Type.REMOVE);
              }
              for(int i=fragment.getStartingLine2(); i<fragment.getStartingLine2()+fragment.getModifiedLines2(); i++) {
                addLineToHunk(hunk, afterLines[i], PatchLine.Type.ADD);
              }
              contextStart1 = fragment.getStartingLine1()+fragment.getModifiedLines1();
            }
            for(int i=contextStart1; i<contextEnd1; i++) {
              addLineToHunk(hunk, beforeLines[i], PatchLine.Type.CONTEXT);
            }
          }
        }

        checkPathEndLine(patch, c.getAfter());
      } else if (! beforeRevision.getPath().equals(afterRevision.getPath())) {
        TextFilePatch movedPatch = buildMovedFile(myBasePath, beforeRevision, afterRevision);
        checkPathEndLine(movedPatch, c.getAfter());
        result.add(movedPatch);
      }
    }
    return result;
  }

  private void checkPathEndLine(TextFilePatch filePatch, AirContentRevision cr) throws VcsException {
    if (cr == null) return;
    if (filePatch.isDeletedFile() || filePatch.getAfterName() == null) return;
    List<PatchHunk> hunks = filePatch.getHunks();
    if (hunks.isEmpty()) return;
    PatchHunk hunk = hunks.get(hunks.size() - 1);
    List<PatchLine> lines = hunk.getLines();
    if (lines.isEmpty()) return;
    String contentAsString = cr.getContentAsString();
    if (contentAsString == null) return;
    if (! contentAsString.endsWith("\n")) {
      lines.get(lines.size() - 1).setSuppressNewLine(true);
    }
  }

  @Nonnull
  private static DiffString[] tokenize(@Nonnull DiffString text) {
    return text.length() == 0 ? new DiffString[]{text} : text.tokenize();
  }

  private FilePatch buildBinaryPatch(String basePath,
                                     AirContentRevision beforeRevision,
                                     AirContentRevision afterRevision) throws VcsException {
    AirContentRevision headingBeforeRevision = beforeRevision != null ? beforeRevision : afterRevision;
    AirContentRevision headingAfterRevision = afterRevision != null ? afterRevision : beforeRevision;
    byte[] beforeContent = beforeRevision != null ? beforeRevision.getContentAsBytes() : null;
    byte[] afterContent = afterRevision != null ? afterRevision.getContentAsBytes() : null;
    BinaryFilePatch patch = new BinaryFilePatch(beforeContent, afterContent);
    setPatchHeading(patch, basePath, headingBeforeRevision, headingAfterRevision);
    return patch;
  }

  private static void addLineToHunk(@Nonnull PatchHunk hunk, @Nonnull DiffString line, PatchLine.Type type) {
    PatchLine patchLine;
    if (!line.endsWith('\n')) {
      patchLine = new PatchLine(type, line.toString());
      patchLine.setSuppressNewLine(true);
    }
    else {
      patchLine = new PatchLine(type, line.substring(0, line.length() - 1).toString());
    }
    hunk.addLine(patchLine);
  }

  private TextFilePatch buildMovedFile(String basePath, AirContentRevision beforeRevision,
                                       AirContentRevision afterRevision) throws VcsException {
    TextFilePatch result = buildPatchHeading(basePath, beforeRevision, afterRevision);
    PatchHunk hunk = new PatchHunk(0, 0, 0, 0);
    result.addHunk(hunk);
    return result;
  }

  private TextFilePatch buildAddedFile(String basePath, AirContentRevision afterRevision) throws VcsException {
    DiffString content = DiffString.createNullable(afterRevision.getContentAsString());
    if (content == null) {
      throw new VcsException("Failed to fetch content for added file " + afterRevision.getPath().getPath());
    }
    DiffString[] lines = tokenize(content);
    TextFilePatch result = buildPatchHeading(basePath, afterRevision, afterRevision);
    PatchHunk hunk = new PatchHunk(-1, -1, 0, lines.length);
    for (DiffString line : lines) {
      checkCanceled();
      addLineToHunk(hunk, line, PatchLine.Type.ADD);
    }
    result.addHunk(hunk);
    return result;
  }

  private TextFilePatch buildDeletedFile(String basePath, AirContentRevision beforeRevision) throws VcsException {
    DiffString content = DiffString.createNullable(beforeRevision.getContentAsString());
    if (content == null) {
      throw new VcsException("Failed to fetch old content for deleted file " + beforeRevision.getPath().getPath());
    }
    DiffString[] lines = tokenize(content);
    TextFilePatch result = buildPatchHeading(basePath, beforeRevision, beforeRevision);
    PatchHunk hunk = new PatchHunk(0, lines.length, -1, -1);
    for (DiffString line : lines) {
      checkCanceled();
      addLineToHunk(hunk, line, PatchLine.Type.REMOVE);
    }
    result.addHunk(hunk);
    return result;
  }

  private static List<LineFragment> getAdjacentFragments(ArrayList<LineFragment> fragments) {
    List<LineFragment> result = new ArrayList<LineFragment>();
    int endLine = -1;
    while(!fragments.isEmpty()) {
      LineFragment fragment = fragments.get(0);
      if (fragment.getType() == null || fragment.getType() == TextDiffTypeEnum.NONE) {
        fragments.remove(0);
        continue;
      }

      if (result.isEmpty() || endLine + CONTEXT_LINES >= fragment.getStartingLine1() - CONTEXT_LINES) {
        result.add(fragment);
        fragments.remove(0);
        endLine = fragment.getStartingLine1() + fragment.getModifiedLines1();
      }
      else {
        break;
      }
    }
    return result;
  }

  private String getRelativePath(String basePath, String secondPath) {
    String baseModified = FileUtil.toSystemIndependentName(basePath);
    String secondModified = FileUtil.toSystemIndependentName(secondPath);

    String relPath = FileUtil.getRelativePath(baseModified, secondModified, '/', myIsCaseSensitive);
    if (relPath == null) return secondModified;
    return relPath;
  }

  private static String getRevisionName(AirContentRevision revision) {
    String revisionName = revision.getRevisionNumber();
    if (revisionName != null) {
      return MessageFormat.format(REVISION_NAME_TEMPLATE, revisionName);
    }
    return MessageFormat.format(DATE_NAME_TEMPLATE, Long.toString(revision.getPath().lastModified()));
  }

  private TextFilePatch buildPatchHeading(String basePath, AirContentRevision beforeRevision, AirContentRevision afterRevision) {
    TextFilePatch result = new TextFilePatch(afterRevision == null ? null : afterRevision.getCharset());
    setPatchHeading(result, basePath, beforeRevision, afterRevision);
    return result;
  }

  private void setPatchHeading(FilePatch result, String basePath,
                               @Nonnull AirContentRevision beforeRevision,
                               @Nonnull AirContentRevision afterRevision) {
    result.setBeforeName(getRelativePath(basePath, beforeRevision.getPath().getPath()));
    result.setBeforeVersionId(getRevisionName(beforeRevision));

    result.setAfterName(getRelativePath(basePath, afterRevision.getPath().getPath()));
    result.setAfterVersionId(getRevisionName(afterRevision));
  }
}
