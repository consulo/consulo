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
package com.intellij.execution.impl;

import com.intellij.execution.filters.Filter;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.util.ProgressIndicatorUtils;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.NullableComputable;
import com.intellij.openapi.util.Ref;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author peter
 */
class AsyncFilterRunner {
  private static final Logger LOG = Logger.getInstance("#com.intellij.execution.impl.FilterRunner");
  private static final ExecutorService ourExecutor = AppExecutorUtil.createBoundedApplicationPoolExecutor("console filters", 1);
  private final EditorHyperlinkSupport myHyperlinks;
  private final Editor myEditor;
  private final Map<AtomicBoolean, Future<FilterResults>> myPendingFilterResults = new LinkedHashMap<>();

  AsyncFilterRunner(EditorHyperlinkSupport hyperlinks, Editor editor) {
    myHyperlinks = hyperlinks;
    myEditor = editor;
  }

  void highlightHyperlinks(final Filter customFilter, final int startLine, final int endLine) {
    if (endLine < 0) return;

    Computable<FilterResults> bgComputation = highlightHyperlinksAsync(customFilter, startLine, endLine);
    if (ApplicationManager.getApplication().isWriteAccessAllowed()) {
      bgComputation.compute().applyHighlights(myHyperlinks);
    }
    else {
      runFiltersInBackground(bgComputation);
    }
  }

  private void runFiltersInBackground(Computable<FilterResults> bgComputation) {
    AtomicBoolean handled = new AtomicBoolean();
    Future<FilterResults> future = ourExecutor.submit(() -> {
      FilterResults results = computeWithWritePriority(bgComputation);
      if (!results.myResults.isEmpty()) {
        ApplicationManager.getApplication().invokeLater(() -> {
          results.applyHighlights(myHyperlinks);
          myPendingFilterResults.remove(handled);
        }, ModalityState.any(), o -> handled.get());
      }
      return results;
    });
    myPendingFilterResults.put(handled, future);
    handleSynchronouslyIfQuick(handled, future, 5);
  }

  @NotNull
  private FilterResults computeWithWritePriority(Computable<FilterResults> bgComputation) {
    Ref<FilterResults> applyResults = Ref.create(FilterResults.EMPTY);
    Runnable computeInReadAction = () -> {
      if (myEditor.isDisposed()) return;
      applyResults.set(bgComputation.compute());
    };
    while (!ProgressIndicatorUtils.runInReadActionWithWriteActionPriority(computeInReadAction)) {
      ProgressIndicatorUtils.yieldToPendingWriteActions();
    }
    return applyResults.get();
  }

  private boolean handleSynchronouslyIfQuick(AtomicBoolean handled, Future<FilterResults> future, long timeout) {
    try {
      future.get(timeout, TimeUnit.MILLISECONDS).applyHighlights(myHyperlinks);
      handled.set(true);
      myPendingFilterResults.remove(handled);
      return true;
    }
    catch (TimeoutException ignored) {
      return false;
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public boolean waitForPendingFilters(long timeoutMs) {
    ApplicationManager.getApplication().assertIsDispatchThread();

    long started = System.currentTimeMillis();
    while (!myPendingFilterResults.isEmpty()) {
      Map.Entry<AtomicBoolean, Future<FilterResults>> next = myPendingFilterResults.entrySet().iterator().next();

      timeoutMs -= System.currentTimeMillis() - started;
      if (timeoutMs < 1) return false;

      if (!handleSynchronouslyIfQuick(next.getKey(), next.getValue(), timeoutMs)) return false;
    }

    return true;
  }

  @NotNull
  private Computable<FilterResults> highlightHyperlinksAsync(Filter filter, int startLine, int endLine) {
    Document document = myEditor.getDocument();
    int markerOffset = document.getLineEndOffset(endLine);
    RangeMarker marker = document.createRangeMarker(markerOffset, markerOffset);
    List<LineHighlighter> tasks = new ArrayList<>();
    for (int i = startLine; i <= endLine; i++) {
       tasks.add(processLine(document, filter, i));
    }
    return () -> {
      List<Filter.Result> results = new ArrayList<>();
      for (LineHighlighter task : tasks) {
        ProgressManager.checkCanceled();
        if (!marker.isValid()) return FilterResults.EMPTY;
        ContainerUtil.addIfNotNull(results, task.compute());
      }
      return new FilterResults(markerOffset, marker, results);
    };
  }

  @NotNull
  private static LineHighlighter processLine(Document document, Filter filter, int line) {
    int lineEnd = document.getLineEndOffset(line);
    int endOffset = lineEnd + (lineEnd < document.getTextLength() ? 1 /* for \n */ : 0);
    String text = EditorHyperlinkSupport.getLineText(document, line, true);
    return () -> checkRange(filter, endOffset, filter.applyFilter(text, endOffset));
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

  private interface LineHighlighter extends NullableComputable<Filter.Result> {
  }

  private static class FilterResults {
    static final FilterResults EMPTY = new FilterResults(0, null, Collections.emptyList());
    private int myInitialMarkerOffset;
    private RangeMarker myMarker;
    private List<Filter.Result> myResults;

    FilterResults(int initialMarkerOffset, RangeMarker marker, List<Filter.Result> results) {
      myInitialMarkerOffset = initialMarkerOffset;
      myMarker = marker;
      myResults = results;
    }

    void applyHighlights(EditorHyperlinkSupport hyperlinks) {
      if (myResults.isEmpty() || !myMarker.isValid()) return;

      int delta = myMarker.getStartOffset() - myInitialMarkerOffset;
      for (Filter.Result result : myResults) {
        hyperlinks.highlightHyperlinks(result, delta);
      }
    }
  }

}
