/*
 * Copyright 2013-2017 consulo.io
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
package com.intellij.codeInsight.completion;

import com.intellij.codeInsight.lookup.Lookup;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupEvent;
import com.intellij.featureStatistics.FeatureUsageTracker;
import com.intellij.featureStatistics.FeatureUsageTrackerImpl;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.editor.event.DocumentAdapter;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.statistics.StatisticsInfo;
import com.intellij.util.Alarm;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;

import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 02-May-17
 * <p>
 * from kotlin platform\lang-impl\src\com\intellij\codeInsight\completion\StatisticsUpdate.kt
 */
public class StatisticsUpdate implements Disposable {
  static {
    Disposer.register(ApplicationManager.getApplication(), StatisticsUpdate::cancelLastCompletionStatisticsUpdate);
  }

  @Nullable
  private static StatisticsUpdate ourPendingUpdate;
  private static final Alarm ourStatsAlarm = new Alarm(ApplicationManager.getApplication());

  public static StatisticsUpdate collectStatisticChanges(LookupElement item) {
    applyLastCompletionStatisticsUpdate();

    final StatisticsInfo base = StatisticsWeigher.getBaseStatisticsInfo(item, null);
    if (base == StatisticsInfo.EMPTY) {
      return new StatisticsUpdate(StatisticsInfo.EMPTY);
    }

    StatisticsUpdate update = new StatisticsUpdate(base);
    ourPendingUpdate = update;
    Disposer.register(update, () -> ourPendingUpdate = null);

    return update;
  }

  public void trackStatistics(InsertionContext context) {
    if (ourPendingUpdate != this) {
      return;
    }

    if (!context.getOffsetMap().containsOffset(CompletionInitializationContext.START_OFFSET)) {
      return;
    }

    final Document document = context.getDocument();
    int startOffset = context.getStartOffset();
    int tailOffset = context.getEditor().getCaretModel().getOffset();
    if (startOffset < 0 || tailOffset <= startOffset) {
      return;
    }

    final RangeMarker marker = document.createRangeMarker(startOffset, tailOffset);
    final DocumentAdapter listener = new DocumentAdapter() {
      @Override
      public void beforeDocumentChange(DocumentEvent e) {
        if (!marker.isValid() || e.getOffset() > marker.getStartOffset() && e.getOffset() < marker.getEndOffset()) {
          cancelLastCompletionStatisticsUpdate();
        }
      }
    };

    ourStatsAlarm.addRequest(() -> {
      if (ourPendingUpdate == this) {
        applyLastCompletionStatisticsUpdate();
      }
    }, 20 * 1000);

    document.addDocumentListener(listener);
    Disposer.register(this, () -> {
      document.removeDocumentListener(listener);
      marker.dispose();
      ourStatsAlarm.cancelAllRequests();
    });
  }

  public static void cancelLastCompletionStatisticsUpdate() {
    if (ourPendingUpdate != null) {
      Disposer.dispose(ourPendingUpdate);
      assert ourPendingUpdate == null;
    }
  }

  public static void applyLastCompletionStatisticsUpdate() {
    StatisticsUpdate update = ourPendingUpdate;
    if (update != null) {
      update.performUpdate();
      Disposer.dispose(update);
      assert ourPendingUpdate == null;
    }
  }

  private final StatisticsInfo myInfo;
  private int mySpared;

  public StatisticsUpdate(StatisticsInfo info) {
    myInfo = info;
  }

  void performUpdate() {
    myInfo.incUseCount();
    ((FeatureUsageTrackerImpl)FeatureUsageTracker.getInstance()).getCompletionStatistics().registerInvocation(mySpared);
  }

  public void addSparedChars(Lookup lookup, LookupElement item, InsertionContext context) {
    String textInserted;
    if (context.getOffsetMap().containsOffset(CompletionInitializationContext.START_OFFSET) &&
        context.getOffsetMap().containsOffset(InsertionContext.TAIL_OFFSET) &&
        context.getTailOffset() >= context.getStartOffset()) {
      textInserted = context.getDocument().getImmutableCharSequence().subSequence(context.getStartOffset(), context.getTailOffset()).toString();
    }
    else {
      textInserted = item.getLookupString();
    }
    String withoutSpaces = StringUtil.replace(textInserted, new String[]{" ", "\t", "\n"}, new String[]{"", "", ""});
    int spared = withoutSpaces.length() - lookup.itemPattern(item).length();
    char completionChar = context.getCompletionChar();

    if (!LookupEvent.isSpecialCompletionChar(completionChar) && withoutSpaces.contains(String.valueOf(completionChar))) {
      spared--;
    }
    if (spared > 0) {
      mySpared += spared;
    }
  }

  @Override
  public void dispose() {
  }
}
