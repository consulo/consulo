// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.execution.impl;

import consulo.execution.ui.console.Filter;
import consulo.application.ApplicationManager;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.application.ReadAction;
import consulo.logging.Logger;
import consulo.document.Document;
import consulo.codeEditor.Editor;
import consulo.document.RangeMarker;
import consulo.document.impl.DocumentImpl;
import consulo.application.progress.ProgressManager;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.lang.TimeoutUtil;
import consulo.application.util.concurrent.SequentialTaskExecutor;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import consulo.util.concurrent.Promise;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author peter
 */
class AsyncFilterRunner {
  private static final Logger LOG = Logger.getInstance(AsyncFilterRunner.class);
  private static final ExecutorService ourExecutor = SequentialTaskExecutor.createSequentialApplicationPoolExecutor("Console Filters");
  private final EditorHyperlinkSupport myHyperlinks;
  private final Editor myEditor;
  private final Queue<HighlighterJob> myQueue = new ConcurrentLinkedQueue<>();
  @Nonnull
  private List<FilterResult> myResults = new ArrayList<>();

  AsyncFilterRunner(@Nonnull EditorHyperlinkSupport hyperlinks, @Nonnull Editor editor) {
    myHyperlinks = hyperlinks;
    myEditor = editor;
  }

  void highlightHyperlinks(@Nonnull Project project, @Nonnull Filter customFilter, int startLine, int endLine) {
    if (endLine < 0) return;

    myQueue.offer(new HighlighterJob(project, customFilter, startLine, endLine, myEditor.getDocument()));
    if (ApplicationManager.getApplication().isWriteAccessAllowed()) {
      runTasks();
      highlightAvailableResults();
      return;
    }

    Promise<?> promise = ReadAction.nonBlocking(this::runTasks).submit(ourExecutor);

    if (isQuick(promise)) {
      highlightAvailableResults();
    }
    else {
      promise.onSuccess(__ -> {
        if (hasResults()) {
          ApplicationManager.getApplication().invokeLater(this::highlightAvailableResults, IdeaModalityState.any());
        }
      });
    }
  }

  private static boolean isQuick(Promise<?> future) {
    try {
      future.blockingGet(5, TimeUnit.MILLISECONDS);
      return true;
    }
    catch (TimeoutException ignored) {
      return false;
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void highlightAvailableResults() {
    for (FilterResult result : takeAvailableResults()) {
      result.applyHighlights();
    }
  }

  private boolean hasResults() {
    synchronized (myQueue) {
      return !myResults.isEmpty();
    }
  }

  @Nonnull
  private List<FilterResult> takeAvailableResults() {
    synchronized (myQueue) {
      List<FilterResult> results = myResults;
      myResults = new ArrayList<>();
      return results;
    }
  }

  private void addLineResult(@Nullable FilterResult result) {
    if (result == null) return;

    synchronized (myQueue) {
      myResults.add(result);
    }
  }

  @RequiredUIAccess
  boolean waitForPendingFilters(long timeoutMs) {
    UIAccess.assertIsUIThread();

    long started = System.currentTimeMillis();
    while (true) {
      if (myQueue.isEmpty()) {
        // results are available before queue is emptied, so process the last results, if any, and exit
        highlightAvailableResults();
        return true;
      }

      if (hasResults()) {
        highlightAvailableResults();
        continue;
      }

      if (System.currentTimeMillis() - started > timeoutMs) {
        return false;
      }
      TimeoutUtil.sleep(1);
    }
  }

  private void runTasks() {
    ApplicationManager.getApplication().assertReadAccessAllowed();
    if (myEditor.isDisposed()) return;

    while (!myQueue.isEmpty()) {
      HighlighterJob highlighter = myQueue.peek();
      if (!DumbService.isDumbAware(highlighter.filter) && DumbService.isDumb(highlighter.myProject)) return;
      while (highlighter.hasUnprocessedLines()) {
        ProgressManager.checkCanceled();
        addLineResult(highlighter.analyzeNextLine());
      }
      LOG.assertTrue(highlighter == myQueue.remove());
    }
  }

  private static Filter.Result checkRange(Filter filter, int endOffset, Filter.Result result) {
    if (result != null) {
      for (Filter.ResultItem resultItem : result.getResultItems()) {
        int start = resultItem.getHighlightStartOffset();
        int end = resultItem.getHighlightEndOffset();
        if (end < start || end > endOffset) {
          LOG.error("Filter returned wrong range: start=" + start + "; end=" + end + "; max=" + endOffset + "; filter=" + filter);
        }
      }
    }
    return result;
  }

  /**
   * It's important that FilterResult doesn't reference frozen document from {@link HighlighterJob#snapshot},
   * as the lifetime of FilterResult is longer (until EDT is free to apply events), and there can be many jobs
   * holding many document snapshots all together consuming a lot of memory.
   */
  private class FilterResult {
    private final DeltaTracker myDelta;
    private final Filter.Result myResult;

    FilterResult(DeltaTracker delta, Filter.Result result) {
      myDelta = delta;
      myResult = result;
    }

    void applyHighlights() {
      if (!myDelta.isOutdated()) {
        myHyperlinks.highlightHyperlinks(myResult, myDelta.getOffsetDelta());
      }
    }
  }

  private class HighlighterJob {
    @Nonnull
    private final Project myProject;
    private final AtomicInteger startLine;
    private final int endLine;
    private final DeltaTracker delta;
    @Nonnull
    private final Filter filter;
    @Nonnull
    private final Document snapshot;

    HighlighterJob(@Nonnull Project project, @Nonnull Filter filter, int startLine, int endLine, @Nonnull Document document) {
      myProject = project;
      this.startLine = new AtomicInteger(startLine);
      this.endLine = endLine;
      this.filter = filter;

      delta = new DeltaTracker(document, document.getLineEndOffset(endLine));

      snapshot = ((DocumentImpl)document).freeze();
    }

    boolean hasUnprocessedLines() {
      return !delta.isOutdated() && startLine.get() <= endLine;
    }

    @Nullable
    private AsyncFilterRunner.FilterResult analyzeNextLine() {
      int line = startLine.get();
      Filter.Result result = analyzeLine(line);
      LOG.assertTrue(line == startLine.getAndIncrement());
      return result == null ? null : new FilterResult(delta, result);
    }

    private Filter.Result analyzeLine(int line) {
      int lineStart = snapshot.getLineStartOffset(line);
      if (lineStart + delta.getOffsetDelta() < 0) return null;

      String lineText = EditorHyperlinkSupport.getLineText(snapshot, line, true);
      int endOffset = lineStart + lineText.length();
      return checkRange(filter, endOffset, filter.applyFilter(lineText, endOffset));
    }

  }

  private static class DeltaTracker {
    private final int initialMarkerOffset;
    private final RangeMarker endMarker;

    DeltaTracker(Document document, int offset) {
      initialMarkerOffset = offset;
      endMarker = document.createRangeMarker(initialMarkerOffset, initialMarkerOffset);
    }

    boolean isOutdated() {
      return !endMarker.isValid() || endMarker.getEndOffset() == 0;
    }

    int getOffsetDelta() {
      return endMarker.getStartOffset() - initialMarkerOffset;
    }

  }

}
