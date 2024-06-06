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
package consulo.ide.impl.idea.diff.tools.util;

import consulo.application.ApplicationManager;
import consulo.application.util.function.Computable;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.FoldRegion;
import consulo.codeEditor.FoldingModelEx;
import consulo.codeEditor.event.FoldingListener;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.disposer.Disposable;
import consulo.document.Document;
import consulo.document.event.DocumentEvent;
import consulo.ide.impl.idea.diff.util.DiffDividerDrawUtil;
import consulo.ide.impl.idea.diff.util.DiffDrawUtil;
import consulo.ide.impl.idea.diff.util.DiffUtil;
import consulo.ide.impl.idea.diff.util.LineRange;
import consulo.ide.impl.idea.openapi.util.BooleanGetter;
import consulo.util.lang.StringUtil;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolder;
import org.jetbrains.annotations.Contract;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;

/*
 * This class allows to add custom foldings to hide unchanged regions in diff.
 * EditorSettings#isAutoCodeFoldingEnabled() should be true, to avoid collisions with language-specific foldings
 *    (as it's impossible to create partially overlapped folding regions)
 * @See DiffUtil.setFoldingModelSupport()
 *
 */
public class FoldingModelSupport {
  public static final String PLACEHOLDER = "     ";

  private static final Key<FoldingCache> CACHE_KEY = Key.create("Diff.FoldingUtil.Cache");

  protected final int myCount;
  @Nonnull
  protected final EditorEx[] myEditors;

  @Nonnull
  protected final List<FoldedBlock[]> myFoldings = new ArrayList<>();

  private boolean myDuringSynchronize;
  private final boolean[] myShouldUpdateLineNumbers;

  public FoldingModelSupport(@Nonnull EditorEx[] editors, @Nonnull Disposable disposable) {
    myEditors = editors;
    myCount = myEditors.length;
    myShouldUpdateLineNumbers = new boolean[myCount];

    if (myCount > 1) {
      for (int i = 0; i < myCount; i++) {
        myEditors[i].getFoldingModel().addListener(new MyFoldingListener(i), disposable);
        myEditors[i].getGutterComponentEx().setLineNumberConvertor(getLineConvertor(i));
      }
    }
  }

  //
  // Init
  //

  /*
   * Iterator returns ranges of changed lines: start1, end1, start2, end2, ...
   */
  protected void install(@Nullable final Iterator<int[]> changedLines,
                         @Nonnull final UserDataHolder context,
                         @Nonnull final Settings settings) {
    ApplicationManager.getApplication().assertIsDispatchThread();

    if (changedLines == null) return;
    if (settings.range == -1) return;

    runBatchOperation(new Runnable() {
      @Override
      public void run() {
        FoldingBuilder builder = new FoldingBuilder(context, settings);
        builder.build(changedLines);
      }
    });

    updateLineNumbers(true);
  }

  private class FoldingBuilder {
    @Nonnull
    private final Settings mySettings;
    @Nonnull
    private final ExpandSuggester myExpandSuggester;

    @Nonnull
    private final int[] myLineCount;

    public FoldingBuilder(@Nonnull UserDataHolder context,
                          @Nonnull Settings settings) {
      myExpandSuggester = new ExpandSuggester(context.getUserData(CACHE_KEY), settings.defaultExpanded);
      mySettings = settings;

      myLineCount = new int[myCount];
      for (int i = 0; i < myCount; i++) {
        myLineCount[i] = myEditors[i].getDocument().getLineCount();
      }
    }

    private void build(@Nonnull final Iterator<int[]> changedLines) {
      int[] starts = new int[myCount];
      int[] ends = new int[myCount];

      int[] last = new int[myCount];
      for (int i = 0; i < myCount; i++) {
        last[i] = Integer.MIN_VALUE;
      }

      while (changedLines.hasNext()) {
        int[] offsets = changedLines.next();

        for (int i = 0; i < myCount; i++) {
          starts[i] = last[i];
          ends[i] = offsets[i * 2];
          last[i] = offsets[i * 2 + 1];
        }
        addRange(starts, ends);
      }

      for (int i = 0; i < myCount; i++) {
        starts[i] = last[i];
        ends[i] = Integer.MAX_VALUE;
      }
      addRange(starts, ends);
    }

    private void addRange(int[] starts, int[] ends) {
      List<FoldedBlock> result = new ArrayList<>(3);
      int[] rangeStarts = new int[myCount];
      int[] rangeEnds = new int[myCount];

      for (int number = 0; ; number++) {
        int shift = getRangeShift(mySettings.range, number);
        if (shift == -1) break;

        for (int i = 0; i < myCount; i++) {
          rangeStarts[i] = bound(starts[i] + shift, i);
          rangeEnds[i] = bound(ends[i] - shift, i);
        }
        ContainerUtil.addAllNotNull(result, createRange(rangeStarts, rangeEnds, myExpandSuggester.isExpanded(rangeStarts, rangeEnds)));
      }

      if (result.size() > 0) {
        FoldedBlock[] block = ContainerUtil.toArray(result, new FoldedBlock[result.size()]);
        for (FoldedBlock folding : block) {
          folding.installHighlighter(block);
        }
        myFoldings.add(block);
      }
    }

    @Nullable
    private FoldedBlock createRange(int[] starts, int[] ends, boolean expanded) {
      boolean hasFolding = false;
      FoldRegion[] regions = new FoldRegion[myCount];
      boolean hasExpanded = false; // do not desync on runBatchFoldingOperationDoNotCollapseCaret

      for (int i = 0; i < myCount; i++) {
        if (ends[i] - starts[i] < 2) continue;
        regions[i] = addFolding(myEditors[i], starts[i], ends[i], expanded);
        hasFolding |= regions[i] != null;
        hasExpanded |= regions[i] != null && regions[i].isExpanded();
      }
      if (hasExpanded && !expanded) {
        for (FoldRegion region : regions) {
          if (region != null) region.setExpanded(true);
        }
      }
      return hasFolding ? new FoldedBlock(regions) : null;
    }

    private int bound(int value, int index) {
      return Math.min(Math.max(value, 0), myLineCount[index]);
    }
  }

  @Nullable
  private static FoldRegion addFolding(@Nonnull EditorEx editor, int start, int end, boolean expanded) {
    Document document = editor.getDocument();
    final int startOffset = document.getLineStartOffset(start);
    final int endOffset = document.getLineEndOffset(end - 1);

    FoldRegion value = editor.getFoldingModel().addFoldRegion(startOffset, endOffset, PLACEHOLDER);
    if (value != null) {
      value.setExpanded(expanded);
      value.setInnerHighlightersMuted(true);
    }
    return value;
  }

  private void runBatchOperation(@Nonnull Runnable runnable) {
    Runnable lastRunnable = runnable;

    for (int i = 0; i < myCount; i++) {
      final Editor editor = myEditors[i];
      final Runnable finalRunnable = lastRunnable;
      lastRunnable = new Runnable() {
        @Override
        public void run() {
          Runnable operation = new Runnable() {
            @Override
            public void run() {
              finalRunnable.run();
            }
          };
          if (DiffUtil.isFocusedComponent(editor.getComponent())) {
            editor.getFoldingModel().runBatchFoldingOperationDoNotCollapseCaret(operation);
          }
          else {
            editor.getFoldingModel().runBatchFoldingOperation(operation);
          }
        }
      };
    }

    myDuringSynchronize = true;
    try {
      lastRunnable.run();
    }
    finally {
      myDuringSynchronize = false;
    }
  }

  public void destroy() {
    for (int i = 0; i < myCount; i++) {
      destroyFoldings(i);
    }

    for (FoldedBlock folding : getFoldedBlocks()) {
      folding.destroyHighlighter();
    }
    myFoldings.clear();
  }

  private void destroyFoldings(final int index) {
    final FoldingModelEx model = myEditors[index].getFoldingModel();
    model.runBatchFoldingOperation(new Runnable() {
      @Override
      public void run() {
        for (FoldedBlock folding : getFoldedBlocks()) {
          FoldRegion region = folding.getRegion(index);
          if (region != null) model.removeFoldRegion(region);
        }
      }
    });
  }

  //
  // Line numbers
  //

  public void onDocumentChanged(@Nonnull DocumentEvent e) {
    if (StringUtil.indexOf(e.getOldFragment(), '\n') != -1 ||
        StringUtil.indexOf(e.getNewFragment(), '\n') != -1) {
      for (int i = 0; i < myCount; i++) {
        if (myEditors[i].getDocument() == e.getDocument()) {
          myShouldUpdateLineNumbers[i] = true;
        }
      }
    }
  }

  @Nonnull
  protected IntUnaryOperator getLineConvertor(final int index) {
    return value -> {
      updateLineNumbers(false);
      for (FoldedBlock folding : getFoldedBlocks()) { // TODO: avoid full scan - it could slowdown painting
        int line = folding.getLine(index);
        if (line == -1) continue;
        if (line > value) break;
        FoldRegion region = folding.getRegion(index);
        if (line == value && region != null && !region.isExpanded()) return -1;
      }
      return value;
    };
  }

  private void updateLineNumbers(boolean force) {
    for (int i = 0; i < myCount; i++) {
      if (!myShouldUpdateLineNumbers[i] && !force) continue;
      myShouldUpdateLineNumbers[i] = false;

      ApplicationManager.getApplication().assertReadAccessAllowed();
      for (FoldedBlock folding : getFoldedBlocks()) {
        folding.updateLineNumber(i);
      }
    }
  }

  //
  // Synchronized toggling of ranges
  //

  public void expandAll(final boolean expanded) {
    if (myDuringSynchronize) return;
    myDuringSynchronize = true;
    try {
      for (int i = 0; i < myCount; i++) {
        final int index = i;
        final FoldingModelEx model = myEditors[index].getFoldingModel();
        model.runBatchFoldingOperation(new Runnable() {
          @Override
          public void run() {
            for (FoldedBlock folding : getFoldedBlocks()) {
              FoldRegion region = folding.getRegion(index);
              if (region != null) region.setExpanded(expanded);
            }
          }
        });
      }
    }
    finally {
      myDuringSynchronize = false;
    }
  }

  private class MyFoldingListener implements FoldingListener {
    private final int myIndex;
    @Nonnull
    Set<FoldRegion> myModifiedRegions = new HashSet<>();

    public MyFoldingListener(int index) {
      myIndex = index;
    }

    @Override
    public void onFoldRegionStateChange(@Nonnull FoldRegion region) {
      if (myDuringSynchronize) return;
      myModifiedRegions.add(region);
    }

    @Override
    public void onFoldProcessingEnd() {
      if (myModifiedRegions.isEmpty()) return;
      myDuringSynchronize = true;
      try {
        for (int i = 0; i < myCount; i++) {
          if (i == myIndex) continue;
          final int pairedIndex = i;
          myEditors[pairedIndex].getFoldingModel().runBatchFoldingOperation(new Runnable() {
            @Override
            public void run() {
              for (FoldedBlock folding : getFoldedBlocks()) {
                FoldRegion region = folding.getRegion(myIndex);
                if (region == null || !region.isValid()) continue;
                if (myModifiedRegions.contains(region)) {
                  FoldRegion pairedRegion = folding.getRegion(pairedIndex);
                  if (pairedRegion == null || !pairedRegion.isValid()) continue;
                  pairedRegion.setExpanded(region.isExpanded());
                }
              }
            }
          });
        }

        myModifiedRegions.clear();
      }
      finally {
        myDuringSynchronize = false;
      }
    }
  }

  //
  // Highlighting
  //

  protected class MyPaintable implements DiffDividerDrawUtil.DividerSeparatorPaintable {
    private final int myLeft;
    private final int myRight;

    public MyPaintable(int left, int right) {
      myLeft = left;
      myRight = right;
    }

    @Override
    public void process(@Nonnull Handler handler) {
      for (FoldedBlock[] block : myFoldings) {
        for (FoldedBlock folding : block) {
          FoldRegion region1 = folding.getRegion(myLeft);
          FoldRegion region2 = folding.getRegion(myRight);
          if (region1 == null || !region1.isValid() || region1.isExpanded()) continue;
          if (region2 == null || !region2.isValid() || region2.isExpanded()) continue;
          int line1 = myEditors[myLeft].getDocument().getLineNumber(region1.getStartOffset());
          int line2 = myEditors[myRight].getDocument().getLineNumber(region2.getStartOffset());
          if (!handler.process(line1, line2)) return;
          break;
        }
      }
    }

    public void paintOnDivider(@Nonnull Graphics2D gg, @Nonnull Component divider) {
      DiffDividerDrawUtil.paintSeparators(gg, divider.getWidth(), myEditors[myLeft], myEditors[myRight], this);
    }

    public void paintOnScrollbar(@Nonnull Graphics2D gg, int width) {
      DiffDividerDrawUtil.paintSeparatorsOnScrollbar(gg, width, myEditors[myLeft], myEditors[myRight], this);
    }
  }

  //
  // Cache
  //

  /*
   * To Cache:
   * For each block of foldings (foldings for a single unchanged block in diff) we remember biggest expanded and biggest collapsed range.
   *
   * From Cache:
   * We look into cache while building ranges, trying to find corresponding range in cached state.
   * "Corresponding range" now is just smallest covering range.
   *
   * If document was modified since cache creation, this will lead to strange results. But this is a rare case, and we can't do anything with it.
   */

  private class ExpandSuggester {
    @Nullable private final FoldingCache myCache;
    private final int[] myIndex = new int[myCount];
    private final boolean myDefault;

    public ExpandSuggester(@Nullable FoldingCache cache, boolean defaultValue) {
      myCache = cache;
      myDefault = defaultValue;
    }

    public boolean isExpanded(int[] starts, int[] ends) {
      if (myCache == null || myCache.ranges.length != myCount) return myDefault;
      if (myDefault != myCache.expandByDefault) return myDefault;

      Boolean state = null;
      for (int index = 0; index < myCount; index++) {
        Boolean sideState = getCachedExpanded(starts[index], ends[index], index);
        if (sideState == null) continue;
        if (state == null) {
          state = sideState;
          continue;
        }
        if (state != sideState) return myDefault;
      }
      return state == null ? myDefault : state;
    }

    @Nullable
    private Boolean getCachedExpanded(int start, int end, int index) {
      if (start == end) return null;

      //noinspection ConstantConditions
      List<FoldedRangeState> ranges = myCache.ranges[index];
      for (; myIndex[index] < ranges.size(); myIndex[index]++) {
        FoldedRangeState range = ranges.get(myIndex[index]);
        LineRange lineRange = range.getLineRange();

        if (lineRange.end <= start) continue;
        if (lineRange.contains(start, end)) {
          if (range.collapsed != null && range.collapsed.contains(start, end)) return false;
          if (range.expanded != null && range.expanded.contains(start, end)) return true;
          assert false : "Invalid LineRange" + range.expanded + ", " + range.collapsed + ", " + new LineRange(start, end);
        }
        if (lineRange.start >= start) return null; // we could need current range for enclosing next-level foldings
      }
      return null;
    }
  }

  public void updateContext(@Nonnull UserDataHolder context, @Nonnull final Settings settings) {
    if (myFoldings.isEmpty()) return; // do not rewrite cache by initial state
    context.putUserData(CACHE_KEY, getFoldingCache(settings));
  }

  @Nonnull
  private FoldingCache getFoldingCache(@Nonnull final Settings settings) {
    return ApplicationManager.getApplication().runReadAction(new Computable<FoldingCache>() {
      @Override
      public FoldingCache compute() {
        List<FoldedRangeState>[] result = new List[myCount];
        for (int i = 0; i < myCount; i++) {
          result[i] = getFoldedRanges(i, settings);
        }
        return new FoldingCache(result, settings.defaultExpanded);
      }
    });
  }

  @Nonnull
  private List<FoldedRangeState> getFoldedRanges(int index, @Nonnull Settings settings) {
    ApplicationManager.getApplication().assertReadAccessAllowed();
    List<FoldedRangeState> ranges = new ArrayList<>();
    Document document = myEditors[index].getDocument();

    for (FoldedBlock[] blocks : myFoldings) {
      LineRange expanded = null;
      LineRange collapsed = null;

      for (FoldedBlock folding : blocks) {
        FoldRegion region = folding.getRegion(index);
        if (region == null || !region.isValid()) continue;
        if (region.isExpanded()) {
          if (expanded == null) {
            int line1 = document.getLineNumber(region.getStartOffset());
            int line2 = document.getLineNumber(region.getEndOffset()) + 1;

            expanded = new LineRange(line1, line2);
          }
        }
        else {
          int line1 = document.getLineNumber(region.getStartOffset());
          int line2 = document.getLineNumber(region.getEndOffset()) + 1;
          collapsed = new LineRange(line1, line2);
          break;
        }
      }

      if (expanded != null || collapsed != null) {
        ranges.add(new FoldedRangeState(expanded, collapsed));
      }
    }
    return ranges;
  }

  private static class FoldingCache {
    public final boolean expandByDefault;
    @Nonnull
    public final List<FoldedRangeState>[] ranges;

    public FoldingCache(@Nonnull List<FoldedRangeState>[] ranges, boolean expandByDefault) {
      this.ranges = ranges;
      this.expandByDefault = expandByDefault;
    }
  }

  private static class FoldedRangeState {
    @Nullable public final LineRange expanded;
    @Nullable public final LineRange collapsed;

    public FoldedRangeState(@Nullable LineRange expanded, @Nullable LineRange collapsed) {
      assert expanded != null || collapsed != null;

      this.expanded = expanded;
      this.collapsed = collapsed;
    }

    @Nonnull
    public LineRange getLineRange() {
      //noinspection ConstantConditions
      return expanded != null ? expanded : collapsed;
    }
  }

  //
  // Impl
  //

  @Nonnull
  private Iterable<FoldedBlock> getFoldedBlocks() {
    return new Iterable<>() {
      @Nonnull
      @Override
      public Iterator<FoldedBlock> iterator() {
        return new Iterator<>() {
          private int myGroupIndex = 0;
          private int myBlockIndex = 0;

          @Override
          public boolean hasNext() {
            return myGroupIndex < myFoldings.size();
          }

          @Override
          public FoldedBlock next() {
            FoldedBlock[] group = myFoldings.get(myGroupIndex);
            FoldedBlock folding = group[myBlockIndex];

            if (group.length > myBlockIndex + 1) {
              myBlockIndex++;
            }
            else {
              myGroupIndex++;
              myBlockIndex = 0;
            }

            return folding;
          }

          @Override
          public void remove() {
            throw new UnsupportedOperationException();
          }
        };
      }
    };
  }

  protected class FoldedBlock {
    @Nonnull
    private final FoldRegion[] myRegions;
    @Nonnull
    private final int[] myLines;
    @Nonnull
    private final List<RangeHighlighter> myHighlighters = new ArrayList<>(myCount);

    public FoldedBlock(@Nonnull FoldRegion[] regions) {
      assert regions.length == myCount;
      myRegions = regions;
      myLines = new int[myCount];
    }

    public void installHighlighter(@Nonnull final FoldedBlock[] block) {
      assert myHighlighters.isEmpty();

      for (int i = 0; i < myCount; i++) {
        FoldRegion region = myRegions[i];
        if (region == null || !region.isValid()) continue;
        myHighlighters.addAll(DiffDrawUtil.createLineSeparatorHighlighter(myEditors[i],
                                                                          region.getStartOffset(), region.getEndOffset(),
                                                                          getHighlighterCondition(block, i)));
      }
    }

    public void destroyHighlighter() {
      for (RangeHighlighter highlighter : myHighlighters) {
        highlighter.dispose();
      }
      myHighlighters.clear();
    }

    public void updateLineNumber(int index) {
      FoldRegion region = myRegions[index];
      if (region == null || !region.isValid()) {
        myLines[index] = -1;
      }
      else {
        myLines[index] = myEditors[index].getDocument().getLineNumber(region.getStartOffset());
      }
    }

    @Nullable
    public FoldRegion getRegion(int index) {
      return myRegions[index];
    }

    public int getLine(int index) {
      return myLines[index];
    }

    @Nonnull
    private BooleanGetter getHighlighterCondition(@Nonnull final FoldedBlock[] block, final int index) {
      return new BooleanGetter() {
        @Override
        public boolean get() {
          if (!myEditors[index].getFoldingModel().isFoldingEnabled()) return false;

          for (FoldedBlock folding : block) {
            FoldRegion region = folding.getRegion(index);
            boolean visible = region != null && region.isValid() && !region.isExpanded();
            if (folding == FoldedBlock.this) return visible;
            if (visible) return false; // do not paint separator, if 'parent' folding is collapsed
          }
          return false;
        }
      };
    }
  }

  //
  // Helpers
  //

  /*
   * number - depth of folding insertion (from zero)
   * return: number of context lines. ('-1' - end)
   */
  private static int getRangeShift(int range, int number) {
    switch (number) {
      case 0:
        return range;
      case 1:
        return range * 2;
      case 2:
        return range * 4;
      default:
        return -1;
    }
  }

  @Nullable
  @Contract("null, _ -> null; !null, _ -> !null")
  protected static <T, V> Iterator<V> map(@Nullable final List<T> list, @Nonnull final Function<T, V> mapping) {
    if (list == null) return null;
    final Iterator<T> it = list.iterator();
    return new Iterator<>() {
      @Override
      public boolean hasNext() {
        return it.hasNext();
      }

      @Override
      public V next() {
        return mapping.apply(it.next());
      }

      @Override
      public void remove() {
      }
    };
  }

  public static class Settings {
    public final int range;
    public final boolean defaultExpanded;

    public Settings(int range, boolean defaultExpanded) {
      this.range = range;
      this.defaultExpanded = defaultExpanded;
    }
  }
}
