// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.execution.impl;

import consulo.execution.ui.console.HyperlinkInfoBase;
import consulo.ui.ex.OccurenceNavigator;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.markup.MarkupModelEx;
import consulo.codeEditor.markup.RangeHighlighterEx;
import consulo.ide.impl.idea.openapi.editor.ex.util.EditorUtil;
import consulo.application.util.function.FilteringProcessor;
import consulo.application.util.function.CommonProcessors;
import consulo.codeEditor.CodeInsightColors;
import consulo.codeEditor.Editor;
import consulo.codeEditor.LogicalPosition;
import consulo.codeEditor.event.EditorMouseEvent;
import consulo.codeEditor.event.EditorMouseEventArea;
import consulo.codeEditor.event.EditorMouseListener;
import consulo.codeEditor.event.EditorMouseMotionListener;
import consulo.codeEditor.markup.HighlighterLayer;
import consulo.codeEditor.markup.HighlighterTargetArea;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.TextAttributes;
import consulo.document.Document;
import consulo.execution.ui.console.Filter;
import consulo.execution.ui.console.HyperlinkInfo;
import consulo.project.Project;
import consulo.ui.ex.RelativePoint;
import consulo.util.dataholder.Key;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;

/**
 * @author peter
 */
public class EditorHyperlinkSupport {
  private static final Key<TextAttributes> OLD_HYPERLINK_TEXT_ATTRIBUTES = Key.create("OLD_HYPERLINK_TEXT_ATTRIBUTES");
  private static final Key<HyperlinkInfoTextAttributes> HYPERLINK = Key.create("HYPERLINK");
  private static final Key<EditorHyperlinkSupport> EDITOR_HYPERLINK_SUPPORT_KEY = Key.create("EDITOR_HYPERLINK_SUPPORT_KEY");

  private final EditorEx myEditor;
  @Nonnull
  private final Project myProject;
  private final AsyncFilterRunner myFilterRunner;

  /**
   * If your editor has a project inside, better use {@link #get(Editor)}
   */
  public EditorHyperlinkSupport(@Nonnull Editor editor, @Nonnull Project project) {
    myEditor = (EditorEx)editor;
    myProject = project;
    myFilterRunner = new AsyncFilterRunner(this, myEditor);

    editor.addEditorMouseListener(new EditorMouseListener() {
      private MouseEvent myInitialMouseEvent = null;

      @Override
      public void mousePressed(@Nonnull EditorMouseEvent e) {
        myInitialMouseEvent = e.getMouseEvent();
      }

      @Override
      public void mouseReleased(@Nonnull EditorMouseEvent e) {
        MouseEvent initialMouseEvent = myInitialMouseEvent;
        myInitialMouseEvent = null;
        MouseEvent mouseEvent = e.getMouseEvent();
        if (mouseEvent.getButton() == MouseEvent.BUTTON1 && !mouseEvent.isPopupTrigger()) {
          if (initialMouseEvent != null && (mouseEvent.getComponent() != initialMouseEvent.getComponent() || !mouseEvent.getPoint().equals(initialMouseEvent.getPoint()))) {
            return;
          }

          Runnable runnable = getLinkNavigationRunnable(myEditor.xyToLogicalPosition(e.getMouseEvent().getPoint()));
          if (runnable != null) {
            runnable.run();
          }
        }
      }
    });

    editor.addEditorMouseMotionListener(new EditorMouseMotionListener() {
      @Override
      public void mouseMoved(@Nonnull EditorMouseEvent e) {
        if (e.getArea() != EditorMouseEventArea.EDITING_AREA) return;
        HyperlinkInfo info = getHyperlinkInfoByPoint(e.getMouseEvent().getPoint());
        myEditor.setCustomCursor(EditorHyperlinkSupport.class, info == null ? null : Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      }
    });
  }

  public static EditorHyperlinkSupport get(@Nonnull Editor editor) {
    EditorHyperlinkSupport instance = editor.getUserData(EDITOR_HYPERLINK_SUPPORT_KEY);
    if (instance == null) {
      Project project = editor.getProject();
      assert project != null;
      instance = new EditorHyperlinkSupport(editor, project);
      editor.putUserData(EDITOR_HYPERLINK_SUPPORT_KEY, instance);
    }
    return instance;
  }

  public void clearHyperlinks() {
    for (RangeHighlighter highlighter : getHyperlinks(0, myEditor.getDocument().getTextLength(), myEditor)) {
      removeHyperlink(highlighter);
    }
  }

  @SuppressWarnings("SameParameterValue")
  public void waitForPendingFilters(long timeoutMs) {
    myFilterRunner.waitForPendingFilters(timeoutMs);
  }

  /**
   * @deprecated left for API compatibility
   */
  @Deprecated
  public Map<RangeHighlighter, HyperlinkInfo> getHyperlinks() {
    Map<RangeHighlighter, HyperlinkInfo> result = new LinkedHashMap<>();
    for (RangeHighlighter highlighter : getHyperlinks(0, myEditor.getDocument().getTextLength(), myEditor)) {
      HyperlinkInfo info = getHyperlinkInfo(highlighter);
      if (info != null) {
        result.put(highlighter, info);
      }
    }
    return result;
  }

  @Nullable
  public Runnable getLinkNavigationRunnable(LogicalPosition logical) {
    if (EditorUtil.inVirtualSpace(myEditor, logical)) {
      return null;
    }

    RangeHighlighter range = findLinkRangeAt(myEditor.logicalPositionToOffset(logical));
    if (range != null) {
      HyperlinkInfo hyperlinkInfo = getHyperlinkInfo(range);
      if (hyperlinkInfo != null) {
        return () -> {
          if (hyperlinkInfo instanceof HyperlinkInfoBase) {
            Point point = myEditor.logicalPositionToXY(logical);
            MouseEvent event = new MouseEvent(myEditor.getContentComponent(), 0, 0, 0, point.x, point.y, 1, false);
            ((HyperlinkInfoBase)hyperlinkInfo).navigate(myProject, new RelativePoint(event));
          }
          else {
            hyperlinkInfo.navigate(myProject);
          }
          linkFollowed(myEditor, getHyperlinks(0, myEditor.getDocument().getTextLength(), myEditor), range);
        };
      }
    }
    return null;
  }

  @Nullable
  public static HyperlinkInfo getHyperlinkInfo(@Nonnull RangeHighlighter range) {
    HyperlinkInfoTextAttributes attributes = range.getUserData(HYPERLINK);
    return attributes != null ? attributes.getHyperlinkInfo() : null;
  }

  @Nullable
  private RangeHighlighter findLinkRangeAt(int offset) {
    //noinspection LoopStatementThatDoesntLoop
    for (RangeHighlighter highlighter : getHyperlinks(offset, offset, myEditor)) {
      return highlighter;
    }
    return null;
  }

  @Nullable
  private HyperlinkInfo getHyperlinkAt(int offset) {
    RangeHighlighter range = findLinkRangeAt(offset);
    return range == null ? null : getHyperlinkInfo(range);
  }

  public List<RangeHighlighter> findAllHyperlinksOnLine(int line) {
    int lineStart = myEditor.getDocument().getLineStartOffset(line);
    int lineEnd = myEditor.getDocument().getLineEndOffset(line);
    return getHyperlinks(lineStart, lineEnd, myEditor);
  }

  private static List<RangeHighlighter> getHyperlinks(int startOffset, int endOffset, Editor editor) {
    MarkupModelEx markupModel = (MarkupModelEx)editor.getMarkupModel();
    CommonProcessors.CollectProcessor<RangeHighlighterEx> processor = new CommonProcessors.CollectProcessor<>();
    markupModel.processRangeHighlightersOverlappingWith(startOffset, endOffset,
                                                        new FilteringProcessor<>(rangeHighlighterEx -> rangeHighlighterEx.isValid() && getHyperlinkInfo(rangeHighlighterEx) != null, processor));
    return new ArrayList<>(processor.getResults());
  }

  public void removeHyperlink(@Nonnull RangeHighlighter hyperlink) {
    myEditor.getMarkupModel().removeHighlighter(hyperlink);
  }

  @Nullable
  public HyperlinkInfo getHyperlinkInfoByLineAndCol(int line, int col) {
    return getHyperlinkAt(myEditor.logicalPositionToOffset(new LogicalPosition(line, col)));
  }

  /**
   * @deprecated left for API compatibility, use {@link #createHyperlink(int, int, TextAttributes, HyperlinkInfo)}
   */
  @Deprecated
  public void addHyperlink(int highlightStartOffset, int highlightEndOffset, @Nullable TextAttributes highlightAttributes, @Nonnull HyperlinkInfo hyperlinkInfo) {
    createHyperlink(highlightStartOffset, highlightEndOffset, highlightAttributes, hyperlinkInfo);
  }

  public void createHyperlink(@Nonnull RangeHighlighter highlighter, @Nonnull HyperlinkInfo hyperlinkInfo) {
    associateHyperlink(highlighter, hyperlinkInfo, null);
  }

  @Nonnull
  public RangeHighlighter createHyperlink(int highlightStartOffset, int highlightEndOffset, @Nullable TextAttributes highlightAttributes, @Nonnull HyperlinkInfo hyperlinkInfo) {
    return createHyperlink(highlightStartOffset, highlightEndOffset, highlightAttributes, hyperlinkInfo, null, HighlighterLayer.HYPERLINK);
  }

  @Nonnull
  private RangeHighlighter createHyperlink(int highlightStartOffset,
                                           int highlightEndOffset,
                                           @Nullable TextAttributes highlightAttributes,
                                           @Nonnull HyperlinkInfo hyperlinkInfo,
                                           @Nullable TextAttributes followedHyperlinkAttributes,
                                           int layer) {
    TextAttributes textAttributes = highlightAttributes != null ? highlightAttributes : getHyperlinkAttributes();
    RangeHighlighter highlighter = myEditor.getMarkupModel().addRangeHighlighter(highlightStartOffset, highlightEndOffset, layer, textAttributes, HighlighterTargetArea.EXACT_RANGE);
    associateHyperlink(highlighter, hyperlinkInfo, followedHyperlinkAttributes);
    return highlighter;
  }

  /**
   * @deprecated Use {@link #get(Editor)} and then {@link #createHyperlink(RangeHighlighter, HyperlinkInfo)}
   */
  @Deprecated
  public static void associateHyperlink(@Nonnull RangeHighlighter highlighter, @Nonnull HyperlinkInfo hyperlinkInfo) {
    associateHyperlink(highlighter, hyperlinkInfo, null);
  }

  private static void associateHyperlink(@Nonnull RangeHighlighter highlighter, @Nonnull HyperlinkInfo hyperlinkInfo, @Nullable TextAttributes followedHyperlinkAttributes) {
    highlighter.putUserData(HYPERLINK, new HyperlinkInfoTextAttributes(hyperlinkInfo, followedHyperlinkAttributes));
  }

  @Nullable
  public HyperlinkInfo getHyperlinkInfoByPoint(Point p) {
    LogicalPosition pos = myEditor.xyToLogicalPosition(new Point(p.x, p.y));
    if (EditorUtil.inVirtualSpace(myEditor, pos)) {
      return null;
    }

    return getHyperlinkInfoByLineAndCol(pos.line, pos.column);
  }

  @Deprecated
  public void highlightHyperlinks(@Nonnull Filter customFilter, Filter predefinedMessageFilter, int line1, int endLine) {
    highlightHyperlinks((line, entireLength) -> {
      Filter.Result result = customFilter.applyFilter(line, entireLength);
      return result != null ? result : predefinedMessageFilter.applyFilter(line, entireLength);
    }, line1, endLine);
  }

  public void highlightHyperlinks(@Nonnull Filter customFilter, int line1, int endLine) {
    myFilterRunner.highlightHyperlinks(myProject, customFilter, Math.max(0, line1), endLine);
  }

  void highlightHyperlinks(@Nonnull Filter.Result result, int offsetDelta) {
    Document document = myEditor.getDocument();
    for (Filter.ResultItem resultItem : result.getResultItems()) {
      int start = resultItem.getHighlightStartOffset() + offsetDelta;
      int end = resultItem.getHighlightEndOffset() + offsetDelta;
      if (start < 0 || end < start || end > document.getTextLength()) {
        continue;
      }

      TextAttributes attributes = resultItem.getHighlightAttributes();
      if (resultItem.getHyperlinkInfo() != null) {
        createHyperlink(start, end, attributes, resultItem.getHyperlinkInfo(), resultItem.getFollowedHyperlinkAttributes(), resultItem.getHighlighterLayer());
      }
      else if (attributes != null) {
        addHighlighter(start, end, attributes, resultItem.getHighlighterLayer());
      }
    }
  }

  public void addHighlighter(int highlightStartOffset, int highlightEndOffset, TextAttributes highlightAttributes) {
    addHighlighter(highlightStartOffset, highlightEndOffset, highlightAttributes, HighlighterLayer.CONSOLE_FILTER);

  }

  public void addHighlighter(int highlightStartOffset, int highlightEndOffset, TextAttributes highlightAttributes, int highlighterLayer) {
    myEditor.getMarkupModel().addRangeHighlighter(highlightStartOffset, highlightEndOffset, highlighterLayer, highlightAttributes, HighlighterTargetArea.EXACT_RANGE);
  }

  private static TextAttributes getHyperlinkAttributes() {
    return EditorColorsManager.getInstance().getGlobalScheme().getAttributes(CodeInsightColors.HYPERLINK_ATTRIBUTES);
  }

  @Nonnull
  private static TextAttributes getFollowedHyperlinkAttributes(@Nonnull RangeHighlighter range) {
    HyperlinkInfoTextAttributes attrs = HYPERLINK.get(range);
    TextAttributes result = attrs != null ? attrs.getFollowedHyperlinkAttributes() : null;
    if (result == null) {
      result = EditorColorsManager.getInstance().getGlobalScheme().getAttributes(CodeInsightColors.FOLLOWED_HYPERLINK_ATTRIBUTES);
    }
    return result;
  }

  @Nullable
  public static OccurenceNavigator.OccurenceInfo getNextOccurrence(Editor editor, int delta, Consumer<? super RangeHighlighter> action) {
    List<RangeHighlighter> ranges = getHyperlinks(0, editor.getDocument().getTextLength(), editor);
    if (ranges.isEmpty()) {
      return null;
    }
    int i;
    for (i = 0; i < ranges.size(); i++) {
      RangeHighlighter range = ranges.get(i);
      if (range.getUserData(OLD_HYPERLINK_TEXT_ATTRIBUTES) != null) {
        break;
      }
    }
    i %= ranges.size();
    int newIndex = i;
    while (newIndex < ranges.size() && newIndex >= 0) {
      newIndex = (newIndex + delta + ranges.size()) % ranges.size();
      RangeHighlighter next = ranges.get(newIndex);
      HyperlinkInfo info = getHyperlinkInfo(next);
      assert info != null;
      if (info.includeInOccurenceNavigation()) {
        boolean inCollapsedRegion = editor.getFoldingModel().getCollapsedRegionAtOffset(next.getStartOffset()) != null;
        if (!inCollapsedRegion) {
          return new OccurenceNavigator.OccurenceInfo(requestFocus -> {
            action.accept(next);
            linkFollowed(editor, ranges, next);
          }, newIndex == -1 ? -1 : newIndex + 1, ranges.size());
        }
      }
      if (newIndex == i) {
        break; // cycled through everything, found no next/prev hyperlink
      }
    }
    return null;
  }

  // todo fix link followed here!
  private static void linkFollowed(Editor editor, Collection<? extends RangeHighlighter> ranges, RangeHighlighter link) {
    MarkupModelEx markupModel = (MarkupModelEx)editor.getMarkupModel();
    for (RangeHighlighter range : ranges) {
      TextAttributes oldAttr = range.getUserData(OLD_HYPERLINK_TEXT_ATTRIBUTES);
      if (oldAttr != null) {
        markupModel.setRangeHighlighterAttributes(range, oldAttr);
        range.putUserData(OLD_HYPERLINK_TEXT_ATTRIBUTES, null);
      }
      if (range == link) {
        range.putUserData(OLD_HYPERLINK_TEXT_ATTRIBUTES, range.getTextAttributes(editor.getColorsScheme()));
        markupModel.setRangeHighlighterAttributes(range, getFollowedHyperlinkAttributes(range));
      }
    }
    //refresh highlighter text attributes
    markupModel.addRangeHighlighter(0, 0, link.getLayer(), getHyperlinkAttributes(), HighlighterTargetArea.EXACT_RANGE).dispose();
  }


  @Nonnull
  public static String getLineText(@Nonnull Document document, int lineNumber, boolean includeEol) {
    return getLineSequence(document, lineNumber, includeEol).toString();
  }

  @Nonnull
  private static CharSequence getLineSequence(@Nonnull Document document, int lineNumber, boolean includeEol) {
    int endOffset = document.getLineEndOffset(lineNumber);
    if (includeEol && endOffset < document.getTextLength()) {
      endOffset++;
    }
    return document.getImmutableCharSequence().subSequence(document.getLineStartOffset(lineNumber), endOffset);
  }

  private static class HyperlinkInfoTextAttributes extends TextAttributes {
    private final HyperlinkInfo myHyperlinkInfo;
    private final TextAttributes myFollowedHyperlinkAttributes;

    HyperlinkInfoTextAttributes(@Nonnull HyperlinkInfo hyperlinkInfo, @Nullable TextAttributes followedHyperlinkAttributes) {
      myHyperlinkInfo = hyperlinkInfo;
      myFollowedHyperlinkAttributes = followedHyperlinkAttributes;
    }

    @Nonnull
    HyperlinkInfo getHyperlinkInfo() {
      return myHyperlinkInfo;
    }

    @Nullable
    TextAttributes getFollowedHyperlinkAttributes() {
      return myFollowedHyperlinkAttributes;
    }
  }
}
