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
package consulo.ide.impl.idea.codeInsight.actions;

import consulo.annotation.component.ServiceImpl;
import consulo.application.util.diff.FilesTooBigForDiffException;
import consulo.codeEditor.EditorFactory;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.language.editor.internal.EditorFactoryImpl;
import consulo.ide.impl.idea.openapi.vcs.changes.ChangeListManagerImpl;
import consulo.ide.impl.idea.openapi.vcs.ex.LineStatusTracker;
import consulo.ide.impl.idea.openapi.vcs.ex.Range;
import consulo.ide.impl.idea.openapi.vcs.ex.RangesBuilder;
import consulo.ide.impl.idea.openapi.vcs.impl.LineStatusTrackerManager;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.ide.impl.idea.util.containers.ContainerUtilRt;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.versionControlSystem.VcsException;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.ChangeListManager;
import consulo.versionControlSystem.change.ContentRevision;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.List;

@Singleton
@ServiceImpl
public class VcsAwareFormatChangedTextUtil extends FormatChangedTextUtil {
  @Override
  @Nonnull
  public List<TextRange> getChangedTextRanges(@Nonnull Project project, @Nonnull PsiFile file) throws FilesTooBigForDiffException {
    Document document = PsiDocumentManager.getInstance(project).getDocument(file);
    if (document == null) return ContainerUtil.emptyList();

    List<TextRange> cachedChangedLines = getCachedChangedLines(project, document);
    if (cachedChangedLines != null) {
      return cachedChangedLines;
    }

    if (project.getApplication().isUnitTestMode()) {
      CharSequence testContent = file.getUserData(TEST_REVISION_CONTENT);
      if (testContent != null) {
        return calculateChangedTextRanges(document, testContent);
      }
    }

    Change change = ChangeListManager.getInstance(project).getChange(file.getVirtualFile());
    if (change == null) {
      return ContainerUtilRt.emptyList();
    }
    if (change.getType() == Change.Type.NEW) {
      return ContainerUtil.newArrayList(file.getTextRange());
    }

    String contentFromVcs = getRevisionedContentFrom(change);
    return contentFromVcs != null
      ? calculateChangedTextRanges(document, contentFromVcs)
      : ContainerUtil.<TextRange>emptyList();
  }

  @Nullable
  private static String getRevisionedContentFrom(@Nonnull Change change) {
    ContentRevision revision = change.getBeforeRevision();
    if (revision == null) {
      return null;
    }

    try {
      return revision.getContent();
    }
    catch (VcsException e) {
      LOG.error("Can't get content for: " + change.getVirtualFile(), e);
      return null;
    }
  }

  @Nullable
  private static List<TextRange> getCachedChangedLines(@Nonnull Project project, @Nonnull Document document) {
    LineStatusTracker tracker = LineStatusTrackerManager.getInstance(project).getLineStatusTracker(document);
    if (tracker != null && tracker.isValid()) {
      List<Range> ranges = tracker.getRanges();
      return getChangedTextRanges(document, ranges);
    }

    return null;
  }

  @Nonnull
  protected static List<TextRange> calculateChangedTextRanges(@Nonnull Document document,
                                                              @Nonnull CharSequence contentFromVcs) throws FilesTooBigForDiffException
  {
    return getChangedTextRanges(document, getRanges(document, contentFromVcs));
  }

  @Nonnull
  private static List<Range> getRanges(@Nonnull Document document,
                                       @Nonnull CharSequence contentFromVcs) throws FilesTooBigForDiffException
  {
    Document documentFromVcs = ((EditorFactoryImpl)EditorFactory.getInstance()).createDocument(contentFromVcs, true, false);
    return RangesBuilder.createRanges(document, documentFromVcs);
  }

  @Override
  public int calculateChangedLinesNumber(@Nonnull Document document, @Nonnull CharSequence contentFromVcs) {
    try {
      List<Range> changedRanges = getRanges(document, contentFromVcs);
      int linesChanges = 0;
      for (Range range : changedRanges) {
        linesChanges += countLines(range);
      }
      return linesChanges;
    } catch (FilesTooBigForDiffException e) {
      LOG.info("File too big, can not calculate changed lines number");
      return -1;
    }
  }

  private static int countLines(Range range) {
    byte rangeType = range.getType();
    if (rangeType == Range.MODIFIED) {
      int currentChangedLines = range.getLine2() - range.getLine1();
      int revisionLinesChanged = range.getVcsLine2() - range.getVcsLine1();
      return Math.max(currentChangedLines, revisionLinesChanged);
    }
    else if (rangeType == Range.DELETED) {
      return range.getVcsLine2() - range.getVcsLine1();
    }
    else if (rangeType == Range.INSERTED) {
      return range.getLine2() - range.getLine1();
    }

    return 0;
  }

  @Nonnull
  private static List<TextRange> getChangedTextRanges(@Nonnull Document document, @Nonnull List<Range> changedRanges) {
    List<TextRange> ranges = new ArrayList<>();
    for (Range range : changedRanges) {
      if (range.getType() != Range.DELETED) {
        int changeStartLine = range.getLine1();
        int changeEndLine = range.getLine2();

        int lineStartOffset = document.getLineStartOffset(changeStartLine);
        int lineEndOffset = document.getLineEndOffset(changeEndLine - 1);

        ranges.add(new TextRange(lineStartOffset, lineEndOffset));
      }
    }
    return ranges;
  }

  @Override
  public boolean isChangeNotTrackedForFile(@Nonnull Project project, @Nonnull PsiFile file) {
    boolean isUnderVcs = VcsUtil.isFileUnderVcs(project, VcsUtil.getFilePath(file.getVirtualFile()));
    if (!isUnderVcs) return true;

    ChangeListManagerImpl changeListManager = ChangeListManagerImpl.getInstanceImpl(project);
    List<VirtualFile> unversionedFiles = changeListManager.getUnversionedFiles();
    return unversionedFiles.contains(file.getVirtualFile());
  }
}
