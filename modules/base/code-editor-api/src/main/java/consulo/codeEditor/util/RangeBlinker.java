/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package consulo.codeEditor.util;

import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.codeEditor.markup.HighlighterLayer;
import consulo.codeEditor.markup.HighlighterTargetArea;
import consulo.codeEditor.markup.MarkupModel;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.colorScheme.TextAttributes;
import consulo.document.util.Segment;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.util.collection.ArrayUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author cdr
 */
public class RangeBlinker {
  private final Editor myEditor;
  private int myTimeToLive;
  private final List<Segment> myMarkers = new ArrayList<>();
  private boolean show = true;
  private final TextAttributes myAttributes;
  private final List<RangeHighlighter> myAddedHighlighters = new ArrayList<>();

  private Future<?> myBlinkingFuture;

  public RangeBlinker(Editor editor, final TextAttributes attributes, int timeToLive) {
    myAttributes = attributes;
    myEditor = editor;
    myTimeToLive = timeToLive;
  }

  public void resetMarkers(final List<Segment> markers) {
    removeHighlights();
    myMarkers.clear();
    stopBlinking();
    myMarkers.addAll(markers);
    show = true;
  }

  private void removeHighlights() {
    MarkupModel markupModel = myEditor.getMarkupModel();
    RangeHighlighter[] allHighlighters = markupModel.getAllHighlighters();

    for (RangeHighlighter highlighter : myAddedHighlighters) {
      if (ArrayUtil.indexOf(allHighlighters, highlighter) != -1) {
        highlighter.dispose();
      }
    }
    myAddedHighlighters.clear();
  }

  public void startBlinking() {
    Project project = myEditor.getProject();
    Application application = Application.get();
    if (application.isDisposed() || myEditor.isDisposed() || project != null && project.isDisposed()) {
      return;
    }

    MarkupModel markupModel = myEditor.getMarkupModel();
    if (show) {
      for (Segment segment : myMarkers) {
        if (segment.getEndOffset() > myEditor.getDocument().getTextLength()) continue;
        RangeHighlighter highlighter =
                markupModel.addRangeHighlighter(segment.getStartOffset(), segment.getEndOffset(), HighlighterLayer.ADDITIONAL_SYNTAX, myAttributes, HighlighterTargetArea.EXACT_RANGE);
        myAddedHighlighters.add(highlighter);
      }
    }
    else {
      removeHighlights();
    }
    stopBlinking();

    UIAccess uiAccess = application.getLastUIAccess();

    myBlinkingFuture = uiAccess.getScheduler().schedule(() -> {
      if (myTimeToLive > 0 || show) {
        myTimeToLive--;
        show = !show;
        startBlinking();
      }
    }, 400, TimeUnit.MILLISECONDS);
  }

  public void stopBlinking() {
    if (myBlinkingFuture != null) {
      myBlinkingFuture.cancel(false);
      myBlinkingFuture = null;
    }
  }
}
