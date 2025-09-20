// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.fileEditor.internal.largeFileEditor;

import consulo.codeEditor.event.CaretEvent;
import consulo.fileEditor.LargeFileEditor;
import consulo.fileEditor.internal.SearchReplaceComponent;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.annotation.Nonnull;

import java.util.List;

public interface LfeSearchManager {

    void updateSearchReplaceComponentActions();

    SearchReplaceComponent getSearchReplaceComponent();

    Object getLastExecutedCloseSearchTask();

    void onSearchActionHandlerExecuted();

    @Nonnull
    LargeFileEditor getLargeFileEditor();

    void launchNewRangeSearch(long fromPageNumber, long toPageNumber, boolean forwardDirection);

    void gotoNextOccurrence(boolean directionForward);

    void onEscapePressed();

    String getStatusText();

    void updateStatusText();

    @RequiredUIAccess
    void onSearchParametersChanged();

    void onCaretPositionChanged(CaretEvent e);

    void dispose();

    List<SearchResult> getSearchResultsInPage(Page page);

    boolean isSearchWorkingNow();

    boolean canShowRegexSearchWarning();
}
