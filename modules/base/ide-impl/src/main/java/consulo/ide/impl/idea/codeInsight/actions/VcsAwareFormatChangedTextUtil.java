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
import consulo.ide.impl.idea.openapi.vcs.ex.RangesBuilder;
import consulo.language.editor.internal.EditorFactoryImpl;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.versionControlSystem.VcsException;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.ChangeListManager;
import consulo.versionControlSystem.change.ContentRevision;
import consulo.versionControlSystem.impl.internal.change.ChangeListManagerImpl;
import consulo.versionControlSystem.internal.LineStatusTrackerI;
import consulo.versionControlSystem.internal.LineStatusTrackerManagerI;
import consulo.versionControlSystem.internal.VcsRange;
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
        if (document == null) {
            return List.of();
        }

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
            return List.of();
        }
        if (change.getType() == Change.Type.NEW) {
            return List.of(file.getTextRange());
        }

        String contentFromVcs = getRevisionedContentFrom(change);
        return contentFromVcs != null
            ? calculateChangedTextRanges(document, contentFromVcs)
            : List.of();
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
        LineStatusTrackerI tracker = LineStatusTrackerManagerI.getInstance(project).getLineStatusTracker(document);
        if (tracker != null && tracker.isValid()) {
            List<VcsRange> ranges = tracker.getRanges();
            return getChangedTextRanges(document, ranges);
        }

        return null;
    }

    @Nonnull
    protected static List<TextRange> calculateChangedTextRanges(@Nonnull Document document,
                                                                @Nonnull CharSequence contentFromVcs) throws FilesTooBigForDiffException {
        return getChangedTextRanges(document, getRanges(document, contentFromVcs));
    }

    @Nonnull
    private static List<VcsRange> getRanges(@Nonnull Document document,
                                            @Nonnull CharSequence contentFromVcs) throws FilesTooBigForDiffException {
        Document documentFromVcs = ((EditorFactoryImpl) EditorFactory.getInstance()).createDocument(contentFromVcs, true, false);
        return RangesBuilder.createRanges(document, documentFromVcs);
    }

    @Override
    public int calculateChangedLinesNumber(@Nonnull Document document, @Nonnull CharSequence contentFromVcs) {
        try {
            List<VcsRange> changedRanges = getRanges(document, contentFromVcs);
            int linesChanges = 0;
            for (VcsRange range : changedRanges) {
                linesChanges += countLines(range);
            }
            return linesChanges;
        }
        catch (FilesTooBigForDiffException e) {
            LOG.info("File too big, can not calculate changed lines number");
            return -1;
        }
    }

    private static int countLines(VcsRange range) {
        byte rangeType = range.getType();
        if (rangeType == VcsRange.MODIFIED) {
            int currentChangedLines = range.getLine2() - range.getLine1();
            int revisionLinesChanged = range.getVcsLine2() - range.getVcsLine1();
            return Math.max(currentChangedLines, revisionLinesChanged);
        }
        else if (rangeType == VcsRange.DELETED) {
            return range.getVcsLine2() - range.getVcsLine1();
        }
        else if (rangeType == VcsRange.INSERTED) {
            return range.getLine2() - range.getLine1();
        }

        return 0;
    }

    @Nonnull
    private static List<TextRange> getChangedTextRanges(@Nonnull Document document, @Nonnull List<VcsRange> changedRanges) {
        List<TextRange> ranges = new ArrayList<>();
        for (VcsRange range : changedRanges) {
            if (range.getType() != VcsRange.DELETED) {
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
        if (!isUnderVcs) {
            return true;
        }

        ChangeListManagerImpl changeListManager = ChangeListManagerImpl.getInstanceImpl(project);
        List<VirtualFile> unversionedFiles = changeListManager.getUnversionedFiles();
        return unversionedFiles.contains(file.getVirtualFile());
    }
}
