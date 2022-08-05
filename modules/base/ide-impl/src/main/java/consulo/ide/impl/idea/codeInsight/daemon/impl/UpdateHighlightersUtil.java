// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.ide.impl.idea.codeInsight.daemon.impl;

import consulo.application.ApplicationManager;
import consulo.application.util.function.Processor;
import consulo.codeEditor.DocumentMarkupModel;
import consulo.codeEditor.impl.SweepProcessor;
import consulo.codeEditor.markup.*;
import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.TextAttributes;
import consulo.document.Document;
import consulo.document.RangeMarker;
import consulo.document.event.DocumentEvent;
import consulo.document.impl.RedBlackTree;
import consulo.document.internal.DocumentEx;
import consulo.document.util.ProperTextRange;
import consulo.document.util.TextRange;
import consulo.ide.impl.idea.openapi.util.Comparing;
import consulo.ide.impl.idea.util.Consumer;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.ide.impl.language.editor.rawHighlight.HighlightInfoImpl;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.impl.internal.daemon.DaemonCodeAnalyzerEx;
import consulo.language.editor.rawHighlight.HighlightInfo;
import consulo.language.editor.rawHighlight.HighlightInfoType;
import consulo.language.editor.rawHighlight.SeverityRegistrar;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.ui.color.ColorValue;
import consulo.util.dataholder.Key;
import consulo.util.lang.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class UpdateHighlightersUtil {
  private static final Comparator<HighlightInfo> BY_START_OFFSET_NODUPS = (t1, t2) -> {
    HighlightInfoImpl o1 = (HighlightInfoImpl)t1;
    HighlightInfoImpl o2 = (HighlightInfoImpl)t2;

    int d = o1.getActualStartOffset() - o2.getActualStartOffset();
    if (d != 0) return d;
    d = o1.getActualEndOffset() - o2.getActualEndOffset();
    if (d != 0) return d;

    d = Comparing.compare(o1.getSeverity(), o2.getSeverity());
    if (d != 0) return -d; // higher severity first, to prevent warnings overlap errors

    if (!Comparing.equal(o1.type, o2.type)) {
      return String.valueOf(o1.type).compareTo(String.valueOf(o2.type));
    }

    if (!Comparing.equal(o1.getGutterIconRenderer(), o2.getGutterIconRenderer())) {
      return String.valueOf(o1.getGutterIconRenderer()).compareTo(String.valueOf(o2.getGutterIconRenderer()));
    }

    if (!Comparing.equal(o1.forcedTextAttributes, o2.forcedTextAttributes)) {
      return String.valueOf(o1.getGutterIconRenderer()).compareTo(String.valueOf(o2.getGutterIconRenderer()));
    }

    if (!Comparing.equal(o1.forcedTextAttributesKey, o2.forcedTextAttributesKey)) {
      return String.valueOf(o1.getGutterIconRenderer()).compareTo(String.valueOf(o2.getGutterIconRenderer()));
    }

    return Comparing.compare(o1.getDescription(), o2.getDescription());
  };

  private static boolean isCoveredByOffsets(HighlightInfo info, HighlightInfo coveredBy) {
    return ((HighlightInfoImpl)coveredBy).startOffset <= ((HighlightInfoImpl)info).startOffset &&
           ((HighlightInfoImpl)info).endOffset <= ((HighlightInfoImpl)coveredBy).endOffset &&
           ((HighlightInfoImpl)info).getGutterIconRenderer() == null;
  }

  static void addHighlighterToEditorIncrementally(@Nonnull Project project,
                                                  @Nonnull Document document,
                                                  @Nonnull PsiFile file,
                                                  int startOffset,
                                                  int endOffset,
                                                  @Nonnull final HighlightInfoImpl info,
                                                  @Nullable final EditorColorsScheme colorsScheme,
                                                  // if null global scheme will be used
                                                  final int group,
                                                  @Nonnull Map<TextRange, RangeMarker> ranges2markersCache) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    if (isFileLevelOrGutterAnnotation(info)) return;
    if (info.getStartOffset() < startOffset || info.getEndOffset() > endOffset) return;

    MarkupModel markup = DocumentMarkupModel.forDocument(document, project, true);
    final SeverityRegistrar severityRegistrar = SeverityRegistrarImpl.getSeverityRegistrar(project);
    final boolean myInfoIsError = isSevere(info, severityRegistrar);
    Processor<HighlightInfo> otherHighlightInTheWayProcessor = oldInfo -> {
      if (!myInfoIsError && isCovered(info, severityRegistrar, oldInfo)) {
        return false;
      }

      return ((HighlightInfoImpl)oldInfo).getGroup() != group || !((HighlightInfoImpl)oldInfo).equalsByActualOffset((HighlightInfoImpl)info);
    };
    boolean allIsClear = DaemonCodeAnalyzerEx.processHighlights(document, project, null, info.getActualStartOffset(), info.getActualEndOffset(), otherHighlightInTheWayProcessor);
    if (allIsClear) {
      createOrReuseHighlighterFor(info, colorsScheme, document, group, file, (MarkupModelEx)markup, null, ranges2markersCache, severityRegistrar);

      clearWhiteSpaceOptimizationFlag(document);
      assertMarkupConsistent(markup, project);
    }
  }

  public static boolean isFileLevelOrGutterAnnotation(HighlightInfoImpl info) {
    return info.isFileLevelAnnotation() || info.getGutterIconRenderer() != null;
  }

  public static void setHighlightersToEditor(@Nonnull Project project,
                                             @Nonnull Document document,
                                             int startOffset,
                                             int endOffset,
                                             @Nonnull Collection<HighlightInfo> highlights,
                                             @Nullable final EditorColorsScheme colorsScheme,
                                             // if null global scheme will be used
                                             int group) {
    TextRange range = new TextRange(startOffset, endOffset);
    ApplicationManager.getApplication().assertIsDispatchThread();

    PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document);
    final DaemonCodeAnalyzerEx codeAnalyzer = DaemonCodeAnalyzerEx.getInstanceEx(project);
    codeAnalyzer.cleanFileLevelHighlights(project, group, psiFile);

    MarkupModel markup = DocumentMarkupModel.forDocument(document, project, true);
    assertMarkupConsistent(markup, project);

    setHighlightersInRange(project, document, range, colorsScheme, new ArrayList<>(highlights), (MarkupModelEx)markup, group);
  }

  @Deprecated //for teamcity
  public static void setHighlightersToEditor(@Nonnull Project project, @Nonnull Document document, int startOffset, int endOffset, @Nonnull Collection<HighlightInfo> highlights, int group) {
    setHighlightersToEditor(project, document, startOffset, endOffset, highlights, null, group);
  }

  // set highlights inside startOffset,endOffset but outside priorityRange
  static void setHighlightersOutsideRange(@Nonnull final Project project,
                                          @Nonnull final Document document,
                                          @Nonnull final PsiFile psiFile,
                                          @Nonnull final List<? extends HighlightInfo> infos,
                                          @Nullable final EditorColorsScheme colorsScheme,
                                          // if null global scheme will be used
                                          final int startOffset,
                                          final int endOffset,
                                          @Nonnull final ProperTextRange priorityRange,
                                          final int group) {
    ApplicationManager.getApplication().assertIsDispatchThread();

    final DaemonCodeAnalyzerEx codeAnalyzer = DaemonCodeAnalyzerEx.getInstanceEx(project);
    if (startOffset == 0 && endOffset == document.getTextLength()) {
      codeAnalyzer.cleanFileLevelHighlights(project, group, psiFile);
    }

    final MarkupModel markup = DocumentMarkupModel.forDocument(document, project, true);
    assertMarkupConsistent(markup, project);

    final SeverityRegistrar severityRegistrar = SeverityRegistrarImpl.getSeverityRegistrar(project);
    final HighlightersRecycler infosToRemove = new HighlightersRecycler();
    ContainerUtil.quickSort(infos, BY_START_OFFSET_NODUPS);
    Set<HighlightInfo> infoSet = new HashSet<>(infos);

    Processor<HighlightInfo> processor = info -> {
      HighlightInfoImpl highlightInfo = (HighlightInfoImpl)info;
      if (highlightInfo.getGroup() == group) {
        RangeHighlighter highlighter = info.getHighlighter();
        int hiStart = highlighter.getStartOffset();
        int hiEnd = highlighter.getEndOffset();
        if (!highlightInfo.isFromInjection() && hiEnd < document.getTextLength() && (hiEnd <= startOffset || hiStart >= endOffset)) {
          return true; // injections are oblivious to restricting range
        }
        boolean toRemove = infoSet.contains(info) || !priorityRange.containsRange(hiStart, hiEnd) && (hiEnd != document.getTextLength() || priorityRange.getEndOffset() != document.getTextLength());
        if (toRemove) {
          infosToRemove.recycleHighlighter(highlighter);
          highlightInfo.setHighlighter(null);
        }
      }
      return true;
    };
    DaemonCodeAnalyzerEx.processHighlightsOverlappingOutside(document, project, null, priorityRange.getStartOffset(), priorityRange.getEndOffset(), processor);

    final Map<TextRange, RangeMarker> ranges2markersCache = new HashMap<>(10);
    final boolean[] changed = {false};
    SweepProcessor.Generator<HighlightInfo> generator = proc -> ContainerUtil.process(infos, proc);
    SweepProcessor.sweep(generator, (offset, i, atStart, overlappingIntervals) -> {
      if (!atStart) return true;
      HighlightInfoImpl info = (HighlightInfoImpl)i;

      if (!info.isFromInjection() && info.getEndOffset() < document.getTextLength() && (info.getEndOffset() <= startOffset || info.getStartOffset() >= endOffset)) {
        return true; // injections are oblivious to restricting range
      }

      if (info.isFileLevelAnnotation()) {
        codeAnalyzer.addFileLevelHighlight(project, group, info, psiFile);
        changed[0] = true;
        return true;
      }
      if (isWarningCoveredByError(info, overlappingIntervals, severityRegistrar)) {
        return true;
      }
      if (info.getStartOffset() < priorityRange.getStartOffset() || info.getEndOffset() > priorityRange.getEndOffset()) {
        createOrReuseHighlighterFor(info, colorsScheme, document, group, psiFile, (MarkupModelEx)markup, infosToRemove, ranges2markersCache, severityRegistrar);
        changed[0] = true;
      }
      return true;
    });
    for (RangeHighlighter highlighter : infosToRemove.forAllInGarbageBin()) {
      highlighter.dispose();
      changed[0] = true;
    }

    if (changed[0]) {
      clearWhiteSpaceOptimizationFlag(document);
    }
    assertMarkupConsistent(markup, project);
  }

  static void setHighlightersInRange(@Nonnull final Project project, @Nonnull final Document document, @Nonnull final TextRange range, @Nullable final EditorColorsScheme colorsScheme,
                                     // if null global scheme will be used
                                     @Nonnull final List<? extends HighlightInfo> infos, @Nonnull final MarkupModelEx markup, final int group) {
    ApplicationManager.getApplication().assertIsDispatchThread();

    final SeverityRegistrar severityRegistrar = SeverityRegistrarImpl.getSeverityRegistrar(project);
    final HighlightersRecycler infosToRemove = new HighlightersRecycler();
    DaemonCodeAnalyzerEx.processHighlights(document, project, null, range.getStartOffset(), range.getEndOffset(), i -> {
      HighlightInfoImpl info = (HighlightInfoImpl)i;
      if (info.getGroup() == group) {
        RangeHighlighter highlighter = info.getHighlighter();
        int hiStart = highlighter.getStartOffset();
        int hiEnd = highlighter.getEndOffset();
        boolean willBeRemoved = hiEnd == document.getTextLength() && range.getEndOffset() == document.getTextLength()
                /*|| range.intersectsStrict(hiStart, hiEnd)*/ || range.containsRange(hiStart, hiEnd) /*|| hiStart <= range.getStartOffset() && hiEnd >= range.getEndOffset()*/;
        if (willBeRemoved) {
          infosToRemove.recycleHighlighter(highlighter);
          info.setHighlighter(null);
        }
      }
      return true;
    });

    ContainerUtil.quickSort(infos, BY_START_OFFSET_NODUPS);
    final Map<TextRange, RangeMarker> ranges2markersCache = new HashMap<>(10);
    final PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document);
    final DaemonCodeAnalyzerEx codeAnalyzer = DaemonCodeAnalyzerEx.getInstanceEx(project);
    final boolean[] changed = {false};
    SweepProcessor.Generator<HighlightInfo> generator = (Processor<HighlightInfo> processor) -> ContainerUtil.process(infos, processor);
    SweepProcessor.sweep(generator, (offset, i, atStart, overlappingIntervals) -> {
      if (!atStart) {
        return true;
      }
      HighlightInfoImpl info = (HighlightInfoImpl)i;
      if (info.isFileLevelAnnotation() && psiFile != null && psiFile.getViewProvider().isPhysical()) {
        codeAnalyzer.addFileLevelHighlight(project, group, info, psiFile);
        changed[0] = true;
        return true;
      }
      if (isWarningCoveredByError(info, overlappingIntervals, severityRegistrar)) {
        return true;
      }
      if (info.getStartOffset() >= range.getStartOffset() && info.getEndOffset() <= range.getEndOffset() && psiFile != null) {
        createOrReuseHighlighterFor(info, colorsScheme, document, group, psiFile, markup, infosToRemove, ranges2markersCache, severityRegistrar);
        changed[0] = true;
      }
      return true;
    });
    for (RangeHighlighter highlighter : infosToRemove.forAllInGarbageBin()) {
      highlighter.dispose();
      changed[0] = true;
    }

    if (changed[0]) {
      clearWhiteSpaceOptimizationFlag(document);
    }
    assertMarkupConsistent(markup, project);
  }

  private static boolean isWarningCoveredByError(@Nonnull HighlightInfo info, @Nonnull Collection<HighlightInfo> overlappingIntervals, @Nonnull SeverityRegistrar severityRegistrar) {
    if (!isSevere(info, severityRegistrar)) {
      for (HighlightInfo overlapping : overlappingIntervals) {
        if (isCovered(info, severityRegistrar, overlapping)) return true;
      }
    }
    return false;
  }

  private static boolean isCovered(@Nonnull HighlightInfo warning, @Nonnull SeverityRegistrar severityRegistrar, @Nonnull HighlightInfo candidate) {
    if (!isCoveredByOffsets(warning, candidate)) return false;
    HighlightSeverity severity = candidate.getSeverity();
    if (severity == HighlightInfoType.SYMBOL_TYPE_SEVERITY) return false; // syntax should not interfere with warnings
    return isSevere(candidate, severityRegistrar);
  }

  private static boolean isSevere(@Nonnull HighlightInfo info, @Nonnull SeverityRegistrar severityRegistrar) {
    HighlightSeverity severity = info.getSeverity();
    return severityRegistrar.compare(HighlightSeverity.ERROR, severity) <= 0 || severity == HighlightInfoType.SYMBOL_TYPE_SEVERITY;
  }

  private static void createOrReuseHighlighterFor(@Nonnull final HighlightInfoImpl info,
                                                  @Nullable final EditorColorsScheme colorsScheme,
                                                  // if null global scheme will be used
                                                  @Nonnull final Document document,
                                                  final int group,
                                                  @Nonnull final PsiFile psiFile,
                                                  @Nonnull MarkupModelEx markup,
                                                  @Nullable HighlightersRecycler infosToRemove,
                                                  @Nonnull final Map<TextRange, RangeMarker> ranges2markersCache,
                                                  @Nonnull SeverityRegistrar severityRegistrar) {
    int infoStartOffset = info.startOffset;
    int infoEndOffset = info.endOffset;

    final int docLength = document.getTextLength();
    if (infoEndOffset > docLength) {
      infoEndOffset = docLength;
      infoStartOffset = Math.min(infoStartOffset, infoEndOffset);
    }
    if (infoEndOffset == infoStartOffset && !info.isAfterEndOfLine()) {
      if (infoEndOffset == docLength) return;  // empty highlighter beyond file boundaries
      infoEndOffset++; //show something in case of empty highlightinfo
    }

    info.setGroup(group);

    int layer = getLayer(info, severityRegistrar);
    RangeHighlighterEx highlighter = infosToRemove == null ? null : (RangeHighlighterEx)infosToRemove.pickupHighlighterFromGarbageBin(infoStartOffset, infoEndOffset, layer);

    final TextRange finalInfoRange = new TextRange(infoStartOffset, infoEndOffset);
    final TextAttributes infoAttributes = info.getTextAttributes(psiFile, colorsScheme);
    Consumer<RangeHighlighterEx> changeAttributes = finalHighlighter -> {
      if (infoAttributes != null) {
        finalHighlighter.setTextAttributes(infoAttributes);
      }

      info.setHighlighter(finalHighlighter);
      finalHighlighter.setAfterEndOfLine(info.isAfterEndOfLine());

      ColorValue color = info.getErrorStripeMarkColor(psiFile, colorsScheme);
      finalHighlighter.setErrorStripeMarkColor(color);
      if (info != finalHighlighter.getErrorStripeTooltip()) {
        finalHighlighter.setErrorStripeTooltip(info);
      }
      GutterMark renderer = info.getGutterIconRenderer();
      finalHighlighter.setGutterIconRenderer((GutterIconRenderer)renderer);

      ranges2markersCache.put(finalInfoRange, info.getHighlighter());
      if (info.quickFixActionRanges != null) {
        List<Pair<HighlightInfoImpl.IntentionActionDescriptor, RangeMarker>> list = new ArrayList<>(info.quickFixActionRanges.size());
        for (Pair<HighlightInfoImpl.IntentionActionDescriptor, TextRange> pair : info.quickFixActionRanges) {
          TextRange textRange = pair.second;
          RangeMarker marker = getOrCreate(document, ranges2markersCache, textRange);
          list.add(Pair.create(pair.first, marker));
        }
        info.quickFixActionMarkers = ContainerUtil.createLockFreeCopyOnWriteList(list);
      }
      ProperTextRange fixRange = info.getFixTextRange();
      if (finalInfoRange.equals(fixRange)) {
        info.fixMarker = null; // null means it the same as highlighter'
      }
      else {
        info.fixMarker = getOrCreate(document, ranges2markersCache, fixRange);
      }
    };

    if (highlighter == null) {
      highlighter = markup.addRangeHighlighterAndChangeAttributes(infoStartOffset, infoEndOffset, layer, null, HighlighterTargetArea.EXACT_RANGE, false, changeAttributes);
      if (HighlightInfoType.VISIBLE_IF_FOLDED.contains(info.type)) {
        highlighter.setVisibleIfFolded(true);
      }
    }
    else {
      markup.changeAttributesInBatch(highlighter, changeAttributes);
    }

    if (infoAttributes != null) {
      boolean attributesSet = Comparing.equal(infoAttributes, highlighter.getTextAttributes());
      assert attributesSet : "Info: " + infoAttributes + "; colorsScheme: " + (colorsScheme == null ? "[global]" : colorsScheme.getName()) + "; highlighter:" + highlighter.getTextAttributes();
    }
  }

  private static int getLayer(@Nonnull HighlightInfoImpl info, @Nonnull SeverityRegistrar severityRegistrar) {
    final HighlightSeverity severity = info.getSeverity();
    int layer;
    if (severity == HighlightSeverity.WARNING) {
      layer = HighlighterLayer.WARNING;
    }
    else if (severity == HighlightSeverity.WEAK_WARNING) {
      layer = HighlighterLayer.WEAK_WARNING;
    }
    else if (severityRegistrar.compare(severity, HighlightSeverity.ERROR) >= 0) {
      layer = HighlighterLayer.ERROR;
    }
    else if (severity == HighlightInfoType.INJECTED_FRAGMENT_SEVERITY) {
      layer = HighlighterLayer.CARET_ROW - 1;
    }
    else if (severity == HighlightInfoType.ELEMENT_UNDER_CARET_SEVERITY) {
      layer = HighlighterLayer.ELEMENT_UNDER_CARET;
    }
    else {
      layer = HighlighterLayer.ADDITIONAL_SYNTAX;
    }
    return layer;
  }

  @Nonnull
  private static RangeMarker getOrCreate(@Nonnull Document document, @Nonnull Map<TextRange, RangeMarker> ranges2markersCache, @Nonnull TextRange textRange) {
    return ranges2markersCache.computeIfAbsent(textRange, __ -> document.createRangeMarker(textRange));
  }

  private static final Key<Boolean> TYPING_INSIDE_HIGHLIGHTER_OCCURRED = Key.create("TYPING_INSIDE_HIGHLIGHTER_OCCURRED");

  static boolean isWhitespaceOptimizationAllowed(@Nonnull Document document) {
    return document.getUserData(TYPING_INSIDE_HIGHLIGHTER_OCCURRED) == null;
  }

  private static void disableWhiteSpaceOptimization(@Nonnull Document document) {
    document.putUserData(TYPING_INSIDE_HIGHLIGHTER_OCCURRED, Boolean.TRUE);
  }

  private static void clearWhiteSpaceOptimizationFlag(@Nonnull Document document) {
    document.putUserData(TYPING_INSIDE_HIGHLIGHTER_OCCURRED, null);
  }

  static void updateHighlightersByTyping(@Nonnull Project project, @Nonnull DocumentEvent e) {
    ApplicationManager.getApplication().assertIsDispatchThread();

    final Document document = e.getDocument();
    if (document instanceof DocumentEx && ((DocumentEx)document).isInBulkUpdate()) return;

    final MarkupModel markup = DocumentMarkupModel.forDocument(document, project, true);
    assertMarkupConsistent(markup, project);

    final int start = e.getOffset() - 1;
    final int end = start + e.getOldLength();

    final List<HighlightInfoImpl> toRemove = new ArrayList<>();
    DaemonCodeAnalyzerEx.processHighlights(document, project, null, start, end, i -> {
      HighlightInfoImpl info = (HighlightInfoImpl)i;

      if (!info.needUpdateOnTyping()) return true;

      RangeHighlighter highlighter = info.getHighlighter();
      int highlighterStart = highlighter.getStartOffset();
      int highlighterEnd = highlighter.getEndOffset();
      if (info.isAfterEndOfLine()) {
        if (highlighterStart < document.getTextLength()) {
          highlighterStart += 1;
        }
        if (highlighterEnd < document.getTextLength()) {
          highlighterEnd += 1;
        }
      }
      if (!highlighter.isValid() || start < highlighterEnd && highlighterStart <= end) {
        toRemove.add(info);
      }
      return true;
    });

    for (HighlightInfoImpl info : toRemove) {
      if (!info.getHighlighter().isValid() || info.type.equals(HighlightInfoType.WRONG_REF)) {
        info.getHighlighter().dispose();
      }
    }

    assertMarkupConsistent(markup, project);

    if (!toRemove.isEmpty()) {
      disableWhiteSpaceOptimization(document);
    }
  }

  private static void assertMarkupConsistent(@Nonnull final MarkupModel markup, @Nonnull Project project) {
    if (!RedBlackTree.VERIFY) {
      return;
    }
    Document document = markup.getDocument();
    DaemonCodeAnalyzerEx.processHighlights(document, project, null, 0, document.getTextLength(), info -> {
      assert ((MarkupModelEx)markup).containsHighlighter(info.getHighlighter());
      return true;
    });
    RangeHighlighter[] allHighlighters = markup.getAllHighlighters();
    for (RangeHighlighter highlighter : allHighlighters) {
      if (!highlighter.isValid()) continue;
      HighlightInfoImpl info = HighlightInfoImpl.fromRangeHighlighter(highlighter);
      if (info == null) continue;
      boolean contains = !DaemonCodeAnalyzerEx
              .processHighlights(document, project, null, info.getActualStartOffset(), info.getActualEndOffset(), highlightInfo -> BY_START_OFFSET_NODUPS.compare(highlightInfo, info) != 0);
      assert contains : info;
    }
  }
}
