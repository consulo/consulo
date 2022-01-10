// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.daemon.impl;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.openapi.application.ApplicationManager;
import consulo.awt.TargetAWT;
import consulo.logging.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.ex.MarkupModelEx;
import com.intellij.openapi.editor.ex.RangeHighlighterEx;
import com.intellij.openapi.editor.impl.DocumentMarkupModel;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.MarkupEditorFilter;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import consulo.util.dataholder.Key;
import com.intellij.openapi.util.Segment;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.util.Processor;
import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

class LineMarkersUtil {
  private static final Logger LOG = Logger.getInstance(LineMarkersUtil.class);

  static boolean processLineMarkers(@Nonnull Project project, @Nonnull Document document, @Nonnull Segment bounds, int group, // -1 for all
                                    @Nonnull Processor<? super LineMarkerInfo<?>> processor) {
    MarkupModelEx markupModel = (MarkupModelEx)DocumentMarkupModel.forDocument(document, project, true);
    return markupModel.processRangeHighlightersOverlappingWith(bounds.getStartOffset(), bounds.getEndOffset(), highlighter -> {
      LineMarkerInfo<?> info = getLineMarkerInfo(highlighter);
      return info == null || group != -1 && info.updatePass != group || processor.process(info);
    });
  }

  static void setLineMarkersToEditor(@Nonnull Project project, @Nonnull Document document, @Nonnull Segment bounds, @Nonnull Collection<? extends LineMarkerInfo<PsiElement>> markers, int group) {
    ApplicationManager.getApplication().assertIsDispatchThread();

    MarkupModelEx markupModel = (MarkupModelEx)DocumentMarkupModel.forDocument(document, project, true);
    HighlightersRecycler toReuse = new HighlightersRecycler();
    processLineMarkers(project, document, bounds, group, info -> {
      toReuse.recycleHighlighter(info.highlighter);
      return true;
    });

    if (LOG.isDebugEnabled()) {
      List<LineMarkerInfo<?>> oldMarkers = DaemonCodeAnalyzerImpl.getLineMarkers(document, project);
      LOG.debug("LineMarkersUtil.setLineMarkersToEditor(markers: " + markers + ", group: " + group + "); oldMarkers: " + oldMarkers + "; reused: " + toReuse.forAllInGarbageBin().size());
    }

    for (final LineMarkerInfo<?> info : markers) {
      PsiElement element = info.getElement();
      if (element == null) {
        continue;
      }

      TextRange textRange = element.getTextRange();
      if (textRange == null) continue;
      TextRange elementRange = InjectedLanguageManager.getInstance(project).injectedToHost(element, textRange);
      if (!TextRange.containsRange(bounds, elementRange)) {
        continue;
      }
      createOrReuseLineMarker(info, markupModel, toReuse);
    }

    for (RangeHighlighter highlighter : toReuse.forAllInGarbageBin()) {
      highlighter.dispose();
    }
  }

  @Nonnull
  private static RangeHighlighter createOrReuseLineMarker(@Nonnull LineMarkerInfo<?> info, @Nonnull MarkupModelEx markupModel, @Nullable HighlightersRecycler toReuse) {
    LineMarkerInfo.LineMarkerGutterIconRenderer<?> newRenderer = (LineMarkerInfo.LineMarkerGutterIconRenderer<?>)info.createGutterRenderer();

    RangeHighlighter highlighter = toReuse == null ? null : toReuse.pickupHighlighterFromGarbageBin(info.startOffset, info.endOffset, HighlighterLayer.ADDITIONAL_SYNTAX);
    boolean newHighlighter = false;
    if (highlighter == null) {
      newHighlighter = true;
      highlighter = markupModel.addRangeHighlighterAndChangeAttributes(info.startOffset, info.endOffset, HighlighterLayer.ADDITIONAL_SYNTAX, null, HighlighterTargetArea.LINES_IN_RANGE, false, markerEx -> {
                markerEx.setGutterIconRenderer(newRenderer);
                markerEx.setLineSeparatorColor(TargetAWT.to(info.separatorColor));
                markerEx.setLineSeparatorPlacement(info.separatorPlacement);

                markerEx.putUserData(LINE_MARKER_INFO, info);
              });

      MarkupEditorFilter editorFilter = info.getEditorFilter();
      if (editorFilter != MarkupEditorFilter.EMPTY) {
        highlighter.setEditorFilter(editorFilter);
      }
    }

    if (!newHighlighter) {
      highlighter.putUserData(LINE_MARKER_INFO, info);

      LineMarkerInfo.LineMarkerGutterIconRenderer<?> oldRenderer =
              highlighter.getGutterIconRenderer() instanceof LineMarkerInfo.LineMarkerGutterIconRenderer ? (LineMarkerInfo.LineMarkerGutterIconRenderer<?>)highlighter.getGutterIconRenderer() : null;
      boolean rendererChanged = newRenderer == null || !newRenderer.equals(oldRenderer);
      boolean lineSeparatorColorChanged = !Comparing.equal(highlighter.getLineSeparatorColor(), info.separatorColor);
      boolean lineSeparatorPlacementChanged = !Comparing.equal(highlighter.getLineSeparatorPlacement(), info.separatorPlacement);

      if (rendererChanged || lineSeparatorColorChanged || lineSeparatorPlacementChanged) {
        markupModel.changeAttributesInBatch((RangeHighlighterEx)highlighter, markerEx -> {
          if (rendererChanged) {
            markerEx.setGutterIconRenderer(newRenderer);
          }
          if (lineSeparatorColorChanged) {
            markerEx.setLineSeparatorColor(TargetAWT.to(info.separatorColor));
          }
          if (lineSeparatorPlacementChanged) {
            markerEx.setLineSeparatorPlacement(info.separatorPlacement);
          }
        });
      }
    }
    info.highlighter = highlighter;
    return highlighter;
  }

  static void addLineMarkerToEditorIncrementally(@Nonnull Project project, @Nonnull Document document, @Nonnull LineMarkerInfo<?> marker) {
    ApplicationManager.getApplication().assertIsDispatchThread();

    MarkupModelEx markupModel = (MarkupModelEx)DocumentMarkupModel.forDocument(document, project, true);
    LineMarkerInfo<?>[] markerInTheWay = {null};
    boolean allIsClear = markupModel.processRangeHighlightersOverlappingWith(marker.startOffset, marker.endOffset, highlighter -> (markerInTheWay[0] = getLineMarkerInfo(highlighter)) == null);
    if (allIsClear) {
      createOrReuseLineMarker(marker, markupModel, null);
    }
    if (LOG.isDebugEnabled()) {
      LOG.debug("LineMarkersUtil.addLineMarkerToEditorIncrementally: " + marker + " " + (allIsClear ? "created" : " (was not added because " + markerInTheWay[0] + " was in the way)"));
    }
  }

  private static LineMarkerInfo<?> getLineMarkerInfo(@Nonnull RangeHighlighter highlighter) {
    return highlighter.getUserData(LINE_MARKER_INFO);
  }

  private static final Key<LineMarkerInfo<?>> LINE_MARKER_INFO = Key.create("LINE_MARKER_INFO");
}
