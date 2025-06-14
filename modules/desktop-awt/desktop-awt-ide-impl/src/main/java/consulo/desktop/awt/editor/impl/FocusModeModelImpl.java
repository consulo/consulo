// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.editor.impl;

import consulo.application.Application;
import consulo.application.util.registry.Registry;
import consulo.codeEditor.Caret;
import consulo.codeEditor.FocusModeModel;
import consulo.codeEditor.event.CaretEvent;
import consulo.codeEditor.event.CaretListener;
import consulo.codeEditor.event.SelectionEvent;
import consulo.codeEditor.event.SelectionListener;
import consulo.codeEditor.markup.MarkupModel;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.TextAttributes;
import consulo.desktop.awt.ui.IdeEventQueue;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.RangeMarker;
import consulo.document.impl.RangeMarkerTree;
import consulo.document.internal.DocumentEx;
import consulo.document.internal.RangeMarkerEx;
import consulo.document.util.DocumentUtil;
import consulo.document.util.Segment;
import consulo.document.util.TextRange;
import consulo.ide.impl.idea.openapi.editor.ex.util.EditorUtil;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.util.ColorValueUtil;
import consulo.util.collection.SmartList;
import consulo.util.lang.ObjectUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;

import static consulo.codeEditor.markup.HighlighterTargetArea.EXACT_RANGE;
import static consulo.colorScheme.EffectType.LINE_UNDERSCORE;

public class FocusModeModelImpl implements FocusModeModel, Disposable {
  public static final int LAYER = 10_000;

  private final List<RangeHighlighter> myFocusModeMarkup = new SmartList<>();
  @Nonnull
  private final DesktopEditorImpl myEditor;
  private RangeMarker myFocusModeRange;

  private final List<FocusModeModelListener> mySegmentListeners = new SmartList<>();
  private final RangeMarkerTree<FocusRegion> myFocusMarkerTree;

  public FocusModeModelImpl(@Nonnull DesktopEditorImpl editor) {
    myEditor = editor;
    myFocusMarkerTree = new RangeMarkerTree<>(editor.getDocument());

    myEditor.getScrollingModel().addVisibleAreaListener(e -> {
      AWTEvent event = IdeEventQueue.getInstance().getTrueCurrentEvent();
      if (event instanceof MouseEvent && !EditorUtil.isPrimaryCaretVisible(myEditor)) {
        clearFocusMode(); // clear when scrolling with touchpad or mouse and primary caret is out the visible area
      }
      else {
        myEditor.applyFocusMode(); // apply the focus mode when jumping to the next line, e.g. Cmd+G
      }
    });

    DesktopCaretModelImpl caretModel = myEditor.getCaretModel();
    caretModel.addCaretListener(new CaretListener() {
      @Override
      public void caretAdded(@Nonnull CaretEvent event) {
        process(event);
      }

      @Override
      public void caretPositionChanged(@Nonnull CaretEvent event) {
        process(event);
      }

      @Override
      public void caretRemoved(@Nonnull CaretEvent event) {
        process(event);
      }

      private void process(@Nonnull CaretEvent event) {
        Caret caret = event.getCaret();
        if (caret == caretModel.getPrimaryCaret()) {
          applyFocusMode(caret);
        }
      }
    });

    myEditor.getSelectionModel().addSelectionListener(new SelectionListener() {
      @Override
      public void selectionChanged(@Nonnull SelectionEvent e) {
        myEditor.applyFocusMode();
      }
    });
  }

  public RangeMarker getFocusModeRange() {
    return myFocusModeRange;
  }

  public void applyFocusMode(@Nonnull Caret caret) {
    // Focus mode should not be applied when idea is used as rd server (for example, centaur mode).
    Application application = Application.get();
    if (application.isHeadlessEnvironment() && !application.isUnitTestMode()) {
      return;
    }

    RangeMarkerEx[] startRange = new RangeMarkerEx[1];
    RangeMarkerEx[] endRange = new RangeMarkerEx[1];
    myFocusMarkerTree.processContaining(caret.getSelectionStart(), startMarker -> {
      if (startRange[0] == null || startRange[0].getStartOffset() < startMarker.getStartOffset()) {
        startRange[0] = startMarker;
      }
      return true;
    });
    myFocusMarkerTree.processContaining(caret.getSelectionEnd(), endMarker -> {
      if (endRange[0] == null || endRange[0].getEndOffset() > endMarker.getEndOffset()) {
        endRange[0] = endMarker;
      }
      return true;
    });

    clearFocusMode();
    if (startRange[0] != null && endRange[0] != null) {
      applyFocusMode(enlargeFocusRangeIfNeeded(new TextRange(startRange[0].getStartOffset(), endRange[0].getEndOffset())));
    }
  }

  public void clearFocusMode() {
    myFocusModeMarkup.forEach(myEditor.getMarkupModel()::removeHighlighter);
    myFocusModeMarkup.clear();
    if (myFocusModeRange != null) {
      myFocusModeRange.dispose();
      myFocusModeRange = null;
    }
  }

  public boolean isInFocusMode(@Nonnull RangeMarker region) {
    return myFocusModeRange != null && !intersects(myFocusModeRange, region);
  }

  /**
   * Find or create and get new focus region.
   * <p>
   * Return pair or focus region and found / created status.
   */
  @Nonnull
  public FocusRegion createFocusRegion(int start, int end) {
    FocusRegion marker = new FocusRegion(myEditor, start, end);
    myFocusMarkerTree.addInterval(marker, start, end, false, false, true, 0);
    mySegmentListeners.forEach(l -> l.focusRegionAdded(marker));
    return marker;
  }

  @SuppressWarnings("Duplicates")
  @Nullable
  public FocusRegion findFocusRegion(int start, int end) {
    FocusRegion[] found = new FocusRegion[1];
    myFocusMarkerTree.processOverlappingWith(start, end, range -> {
      if (range.getStartOffset() == start && range.getEndOffset() == end) {
        found[0] = range;
        return false;
      }
      return true;
    });
    return found[0];
  }

  public void removeFocusRegion(FocusRegion marker) {
    boolean removed = myFocusMarkerTree.removeInterval(marker);
    if (removed) mySegmentListeners.forEach(l -> l.focusRegionRemoved(marker));
  }

  public void addFocusSegmentListener(FocusModeModelListener newListener, Disposable disposable) {
    mySegmentListeners.add(newListener);
    Disposer.register(disposable, () -> mySegmentListeners.remove(newListener));
  }

  @Nonnull
  private Segment enlargeFocusRangeIfNeeded(Segment range) {
    int originalStart = range.getStartOffset();
    DocumentEx document = myEditor.getDocument();
    int start = DocumentUtil.getLineStartOffset(originalStart, document);
    if (start < originalStart) {
      range = new TextRange(start, range.getEndOffset());
    }
    int originalEnd = range.getEndOffset();
    int end = DocumentUtil.getLineEndOffset(originalEnd, document);
    if (end >= originalEnd) {
      range = new TextRange(range.getStartOffset(), end < document.getTextLength() ? end + 1 : end);
    }
    return range;
  }

  private void applyFocusMode(@Nonnull Segment focusRange) {
    EditorColorsScheme scheme = ObjectUtil.notNull(myEditor.getColorsScheme(), EditorColorsManager.getInstance().getGlobalScheme());
    ColorValue background = scheme.getDefaultBackground();
    //noinspection UseJBColor
    ColorValue foreground = TargetAWT.from(Registry.getColor(ColorValueUtil.isDark(background) ? "editor.focus.mode.color.dark" : "editor.focus.mode.color.light", Color.GRAY));
    TextAttributes attributes = new TextAttributes(foreground, background, background, LINE_UNDERSCORE, Font.PLAIN);
    myEditor.putUserData(FOCUS_MODE_ATTRIBUTES, attributes);

    MarkupModel markupModel = myEditor.getMarkupModel();
    DocumentEx document = myEditor.getDocument();
    int textLength = document.getTextLength();

    int start = focusRange.getStartOffset();
    int end = focusRange.getEndOffset();

    if (start <= textLength) myFocusModeMarkup.add(markupModel.addRangeHighlighter(0, start, LAYER, attributes, EXACT_RANGE));
    if (end <= textLength) myFocusModeMarkup.add(markupModel.addRangeHighlighter(end, textLength, LAYER, attributes, EXACT_RANGE));

    myFocusModeRange = document.createRangeMarker(start, end);
  }

  @Override
  public void dispose() {
    myFocusMarkerTree.dispose(myEditor.getDocument());
  }

  private static boolean intersects(RangeMarker a, RangeMarker b) {
    return Math.max(a.getStartOffset(), b.getStartOffset()) < Math.min(a.getEndOffset(), b.getEndOffset());
  }

  public interface FocusModeModelListener {
    void focusRegionAdded(FocusRegion newRegion);

    void focusRegionRemoved(FocusRegion oldRegion);
  }
}
