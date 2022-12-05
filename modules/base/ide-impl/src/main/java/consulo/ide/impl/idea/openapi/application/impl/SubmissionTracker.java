// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.application.impl;

import com.google.common.annotations.VisibleForTesting;
import consulo.logging.Logger;
import consulo.logging.attachment.Attachment;
import consulo.logging.attachment.AttachmentFactory;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

final class SubmissionTracker {
  private static final Logger LOG = Logger.getInstance(SubmissionTracker.class);
  private static final String TOO_MANY_NON_BLOCKING_READ_ACTIONS_SUBMITTED_AT_ONCE = "Too many non-blocking read actions submitted at once";
  private static final String SUGGESTIONS = "Please use coalesceBy, BoundedTaskExecutor or another way of limiting the number of concurrently running threads.";
  private static final String TOO_MANY_SUBMISSIONS = TOO_MANY_NON_BLOCKING_READ_ACTIONS_SUBMITTED_AT_ONCE + ". " + SUGGESTIONS;
  @VisibleForTesting
  static final String ARE_CURRENTLY_ACTIVE = " with similar stack traces are currently active";

  private final AtomicInteger myCount = new AtomicInteger();

  /**
   * Not-null if we're tracking submissions to provide diagnostics
   */
  private volatile
  @Nullable
  Map<String, Integer> myTraces;
  private volatile boolean myStoppedTracing;

  @Nullable
  String preventTooManySubmissions() {
    int currentCount = myCount.incrementAndGet();
    if (myStoppedTracing) return null;

    Map<String, Integer> traces = myTraces;
    if (currentCount > 100) {
      if (traces == null) {
        myTraces = new ConcurrentHashMap<>();
      }
      else {
        Integer count = traces.get(callerTrace());
        if (count != null && count > 10) {
          stopTracing();
          LOG.error(TOO_MANY_SUBMISSIONS + ": " + count + ARE_CURRENTLY_ACTIVE);
        }
        else if (currentCount % 127 == 0) {
          stopTracing();
          reportTooManyUnidentifiedSubmissions(traces);
        }
      }
    }
    if (traces != null) {
      String trace = callerTrace();
      traces.merge(trace, 1, Integer::sum);
      return trace;
    }
    return null;
  }

  private void stopTracing() {
    myStoppedTracing = true;
    myTraces = null;
  }

  private String callerTrace() {
    return StackWalker.getInstance().walk(stream -> {
      return stream.dropWhile(ste -> ste.getClassName().contains("NonBlockingReadAction") || ste.getClassName().equals(getClass().getName()))
              .limit(10)
              .map(it -> it.toStackTraceElement().toString())
              .collect(Collectors.joining("\n"));
    });
  }

  void unregisterSubmission(@Nullable String startTrace) {
    myCount.decrementAndGet();
    Map<String, Integer> traces = myTraces;
    if (startTrace != null && traces != null) {
      traces.compute(startTrace, (__, i) -> i == null || i.intValue() == 1 ? null : i - 1);
    }
  }

  private static void reportTooManyUnidentifiedSubmissions(Map<String, Integer> traces) {
    String mostFrequentTraces =
            traces.entrySet()
                  .stream()
                  .sorted((o1, o2) -> Integer.compare(o2.getValue(), o1.getValue()))
                  .map(e -> e.getValue() + " occurrences of " + e.getKey())
                  .limit(10)
                  .collect(Collectors.joining("\n\n"));

    if (LOG.isDebugEnabled()) {
      LOG.debug(mostFrequentTraces);
    }
    Attachment attachment = AttachmentFactory.get().create("diagnostic.txt", mostFrequentTraces);
    attachment.setIncluded(true);
    LOG.error(TOO_MANY_NON_BLOCKING_READ_ACTIONS_SUBMITTED_AT_ONCE + ". " + SUGGESTIONS, attachment);
  }

}
