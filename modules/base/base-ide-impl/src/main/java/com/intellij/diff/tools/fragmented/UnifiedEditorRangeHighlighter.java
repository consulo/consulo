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
package com.intellij.diff.tools.fragmented;

import com.intellij.openapi.application.ApplicationManager;
import consulo.logging.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.ex.MarkupModelEx;
import com.intellij.openapi.editor.ex.RangeHighlighterEx;
import com.intellij.openapi.editor.impl.DocumentMarkupModel;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.util.Processor;
import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

class UnifiedEditorRangeHighlighter {
  public static final Logger LOG = UnifiedDiffViewer.LOG;

  @Nonnull
  private final List<Element> myPieces = new ArrayList<Element>();

  public UnifiedEditorRangeHighlighter(@javax.annotation.Nullable Project project, @Nonnull Document document) {
    ApplicationManager.getApplication().assertReadAccessAllowed();

    MarkupModelEx model = (MarkupModelEx)DocumentMarkupModel.forDocument(document, project, false);
    if (model == null) return;

    model.processRangeHighlightersOverlappingWith(0, document.getTextLength(), new Processor<RangeHighlighterEx>() {
      @Override
      public boolean process(RangeHighlighterEx marker) {
        int newStart = marker.getStartOffset();
        int newEnd = marker.getEndOffset();

        myPieces.add(new Element(marker, newStart, newEnd));

        return true;
      }
    });
  }

  public UnifiedEditorRangeHighlighter(@javax.annotation.Nullable Project project,
                                       @Nonnull Document document1,
                                       @Nonnull Document document2,
                                       @Nonnull List<HighlightRange> ranges) {
    ApplicationManager.getApplication().assertReadAccessAllowed();

    MarkupModelEx model1 = (MarkupModelEx)DocumentMarkupModel.forDocument(document1, project, false);
    MarkupModelEx model2 = (MarkupModelEx)DocumentMarkupModel.forDocument(document2, project, false);
    init(model1, model2, ranges);
  }

  private void init(@javax.annotation.Nullable MarkupModelEx model1,
                    @javax.annotation.Nullable MarkupModelEx model2,
                    @Nonnull List<HighlightRange> ranges) {
    for (HighlightRange range : ranges) {
      if (range.getSide().isLeft()) {
        if (model1 != null) processRange(model1, range);
      }
      else {
        if (model2 != null) processRange(model2, range);
      }
    }
  }

  private void processRange(@Nonnull MarkupModelEx model, @Nonnull HighlightRange range) {
    final TextRange base = range.getBase();
    final TextRange changed = range.getChanged();
    final int changedLength = changed.getEndOffset() - changed.getStartOffset();

    model.processRangeHighlightersOverlappingWith(changed.getStartOffset(), changed.getEndOffset(), new Processor<RangeHighlighterEx>() {
      @Override
      public boolean process(RangeHighlighterEx marker) {
        int relativeStart = Math.max(marker.getStartOffset() - changed.getStartOffset(), 0);
        int relativeEnd = Math.min(marker.getEndOffset() - changed.getStartOffset(), changedLength);

        int newStart = base.getStartOffset() + relativeStart;
        int newEnd = base.getStartOffset() + relativeEnd;

        if (newEnd - newStart <= 0) return true;

        myPieces.add(new Element(marker, newStart, newEnd));

        return true;
      }
    });
  }

  public static void erase(@javax.annotation.Nullable Project project, @Nonnull Document document) {
    MarkupModel model = DocumentMarkupModel.forDocument(document, project, true);
    model.removeAllHighlighters();
  }

  public void apply(@javax.annotation.Nullable Project project, @Nonnull Document document) {
    MarkupModel model = DocumentMarkupModel.forDocument(document, project, true);

    for (Element piece : myPieces) {
      RangeHighlighterEx delegate = piece.getDelegate();
      if (!delegate.isValid()) continue;

      RangeHighlighter highlighter = model
              .addRangeHighlighter(piece.getStart(), piece.getEnd(), delegate.getLayer(), delegate.getTextAttributes(), delegate.getTargetArea());
      highlighter.setEditorFilter(delegate.getEditorFilter());
      highlighter.setCustomRenderer(delegate.getCustomRenderer());
      highlighter.setErrorStripeMarkColor(delegate.getErrorStripeMarkColor());
      highlighter.setErrorStripeTooltip(delegate.getErrorStripeTooltip());
      highlighter.setGutterIconRenderer(delegate.getGutterIconRenderer());
      highlighter.setLineMarkerRenderer(delegate.getLineMarkerRenderer());
      highlighter.setLineSeparatorColor(delegate.getLineSeparatorColor());
      highlighter.setThinErrorStripeMark(delegate.isThinErrorStripeMark());
      highlighter.setLineSeparatorPlacement(delegate.getLineSeparatorPlacement());
      highlighter.setLineSeparatorRenderer(delegate.getLineSeparatorRenderer());
    }
  }

  private static class Element {
    @Nonnull
    private final RangeHighlighterEx myDelegate;

    private final int myStart;
    private final int myEnd;

    public Element(@Nonnull RangeHighlighterEx delegate, int start, int end) {
      myDelegate = delegate;
      myStart = start;
      myEnd = end;
    }

    @Nonnull
    public RangeHighlighterEx getDelegate() {
      return myDelegate;
    }

    public int getStart() {
      return myStart;
    }

    public int getEnd() {
      return myEnd;
    }
  }
}
