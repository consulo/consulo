// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.index.impl.internal;

public class FileIndexingStateWithExplanation {
    @FunctionalInterface
    public interface Explanation {
        String explain();
    }

    private static final FileIndexingStateWithExplanation NOT_INDEXED_WITHOUT_EXPLANATION =
        new FileIndexingStateWithExplanation(FileIndexingState.NOT_INDEXED, () -> "not indexed");

    private static final FileIndexingStateWithExplanation UP_TO_DATE_WITHOUT_EXPLANATION =
        new FileIndexingStateWithExplanation(FileIndexingState.UP_TO_DATE, () -> "up-to-date");

    private final FileIndexingState myState;
    private final Explanation myExplanation;

    private enum FileIndexingState {
        NOT_INDEXED,
        OUT_DATED,
        UP_TO_DATE
    }

    private FileIndexingStateWithExplanation(FileIndexingState state, Explanation explanation) {
        myState = state;
        myExplanation = explanation;
    }

    public String getExplanationAsString() {
        return myExplanation.explain();
    }

    @Override
    public String toString() {
        return myState + "(" + getExplanationAsString() + ")";
    }

    public boolean isIndexedButOutdated() {
        return myState == FileIndexingState.OUT_DATED;
    }

    public boolean updateRequired() {
        return myState != FileIndexingState.UP_TO_DATE;
    }

    public boolean isUpToDate() {
        return myState == FileIndexingState.UP_TO_DATE;
    }

    public boolean isNotIndexed() {
        return myState == FileIndexingState.NOT_INDEXED;
    }

    public static FileIndexingStateWithExplanation outdated(String reason) {
        return outdated(() -> reason);
    }

    public static FileIndexingStateWithExplanation outdated(Explanation explanation) {
        return new FileIndexingStateWithExplanation(FileIndexingState.OUT_DATED, explanation);
    }

    public static FileIndexingStateWithExplanation notIndexed() {
        return NOT_INDEXED_WITHOUT_EXPLANATION;
    }

    public static FileIndexingStateWithExplanation upToDate() {
        return UP_TO_DATE_WITHOUT_EXPLANATION;
    }
}
