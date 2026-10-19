/*
 * Copyright 2013-2026 consulo.io
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
package consulo.versionControlSystem.review.comment;

import org.jspecify.annotations.Nullable;

/**
 * What a suggested change needs of the thread holding it. The line indexes are 0-based; the original ones are the
 * lines of the reviewed revision, the others the lines now - {@code null} once the thread is outdated.
 *
 * @author VISTALL
 * @since 2026-10-19
 */
public final class CodeReviewSuggestionThreadData {
    private final String myId;
    private final boolean myResolved;
    private final boolean myCanResolve;
    private final String myFilePath;
    private final String myDiffHunk;
    private final int myCodeLinesCount;
    private final @Nullable Integer myOriginalStartLineIndex;
    private final @Nullable Integer myOriginalEndLineIndex;
    private final @Nullable Integer myStartLineIndex;
    private final @Nullable Integer myEndLineIndex;

    /**
     * @param diffHunk the hunk of the review diff the thread is on, from its {@code @@} header to the commented line
     * @param codeLinesCount how many lines the thread is on
     */
    public CodeReviewSuggestionThreadData(String id,
                                          boolean resolved,
                                          boolean canResolve,
                                          String filePath,
                                          String diffHunk,
                                          int codeLinesCount,
                                          @Nullable Integer originalStartLineIndex,
                                          @Nullable Integer originalEndLineIndex,
                                          @Nullable Integer startLineIndex,
                                          @Nullable Integer endLineIndex) {
        myId = id;
        myResolved = resolved;
        myCanResolve = canResolve;
        myFilePath = filePath;
        myDiffHunk = diffHunk;
        myCodeLinesCount = codeLinesCount;
        myOriginalStartLineIndex = originalStartLineIndex;
        myOriginalEndLineIndex = originalEndLineIndex;
        myStartLineIndex = startLineIndex;
        myEndLineIndex = endLineIndex;
    }

    public String getId() {
        return myId;
    }

    public boolean isResolved() {
        return myResolved;
    }

    public boolean canResolve() {
        return myCanResolve;
    }

    public String getFilePath() {
        return myFilePath;
    }

    public String getDiffHunk() {
        return myDiffHunk;
    }

    public int getCodeLinesCount() {
        return myCodeLinesCount;
    }

    public @Nullable Integer getOriginalStartLineIndex() {
        return myOriginalStartLineIndex;
    }

    public @Nullable Integer getOriginalEndLineIndex() {
        return myOriginalEndLineIndex;
    }

    public @Nullable Integer getStartLineIndex() {
        return myStartLineIndex;
    }

    public @Nullable Integer getEndLineIndex() {
        return myEndLineIndex;
    }
}
