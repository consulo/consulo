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
package consulo.execution.ui.console;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.codeEditor.CodeInsightColors;
import consulo.codeEditor.markup.HighlighterLayer;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.TextAttributes;
import consulo.colorScheme.TextAttributesKey;
import consulo.colorScheme.event.EditorColorsListener;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Yura Cangea
 * @version 1.0
 */
public interface Filter {
  Filter[] EMPTY_ARRAY = new Filter[0];

  class Result extends ResultItem {
    private NextAction myNextAction = NextAction.EXIT;
    private final List<? extends ResultItem> myResultItems;

    public Result(int highlightStartOffset, int highlightEndOffset, @Nullable HyperlinkInfo hyperlinkInfo) {
      this(highlightStartOffset, highlightEndOffset, hyperlinkInfo, null);
    }

    public Result(int highlightStartOffset, int highlightEndOffset, @Nullable HyperlinkInfo hyperlinkInfo, @Nullable TextAttributes highlightAttributes) {
      super(highlightStartOffset, highlightEndOffset, hyperlinkInfo, highlightAttributes, null);
      myResultItems = null;
    }

    public Result(int highlightStartOffset,
                  int highlightEndOffset,
                  @Nullable HyperlinkInfo hyperlinkInfo,
                  @Nullable TextAttributes highlightAttributes,
                  @Nullable TextAttributes followedHyperlinkAttributes) {
      super(highlightStartOffset, highlightEndOffset, hyperlinkInfo, highlightAttributes, followedHyperlinkAttributes);
      myResultItems = null;
    }

    public Result(int highlightStartOffset, int highlightEndOffset, @Nullable HyperlinkInfo hyperlinkInfo, boolean grayedHyperlink) {
      super(highlightStartOffset, highlightEndOffset, hyperlinkInfo, grayedHyperlink);
      myResultItems = null;
    }

    public Result(@Nonnull List<? extends ResultItem> resultItems) {
      super(-1, -1, null, null, null);
      myResultItems = resultItems;
    }

    @Nonnull
    public List<ResultItem> getResultItems() {
      List<? extends ResultItem> resultItems = myResultItems;
      if (resultItems == null) {
        resultItems = Collections.singletonList(this);
      }
      return Collections.unmodifiableList(resultItems);
    }

    /**
     * @deprecated This method will be removed. Result may be constructed using ResultItems, in that case this method will return incorrect value. Use {@link #getResultItems()} instead.
     */
    @Deprecated
    @Override
    public int getHighlightStartOffset() {
      return super.getHighlightStartOffset();
    }

    /**
     * @deprecated This method will be removed. Result may be constructed using ResultItems, in that case this method will return incorrect value. Use {@link #getResultItems()} instead.
     */
    @Deprecated
    @Override
    public int getHighlightEndOffset() {
      return super.getHighlightEndOffset();
    }

    /**
     * @deprecated This method will be removed. Result may be constructed using ResultItems, in that case this method will return incorrect value. Use {@link #getResultItems()} instead.
     */
    @Deprecated
    @Nullable
    @Override
    public TextAttributes getHighlightAttributes() {
      return super.getHighlightAttributes();
    }

    /**
     * @deprecated This method will be removed. Result may be constructed using ResultItems, in that case this method will return incorrect value. Use {@link #getResultItems()} or {@link #getFirstHyperlinkInfo()} instead.
     */
    @Deprecated
    @Nullable
    @Override
    public HyperlinkInfo getHyperlinkInfo() {
      return super.getHyperlinkInfo();
    }

    @Nullable
    public HyperlinkInfo getFirstHyperlinkInfo() {
      HyperlinkInfo info = super.getHyperlinkInfo();
      if (info == null && myResultItems != null) {
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < myResultItems.size(); i++) {
          ResultItem resultItem = myResultItems.get(i);
          if (resultItem.getHyperlinkInfo() != null) {
            return resultItem.getHyperlinkInfo();
          }
        }
      }
      return info;
    }

    public NextAction getNextAction() {
      return myNextAction;
    }

    public void setNextAction(NextAction nextAction) {
      myNextAction = nextAction;
    }
  }

  enum NextAction {
    EXIT,
    CONTINUE_FILTERING,
  }

  class ResultItem {
    private static final Map<TextAttributesKey, TextAttributes> GRAYED_BY_NORMAL_CACHE = new ConcurrentHashMap<>(2);

    static {
      Application application = ApplicationManager.getApplication();
      if (application != null) {
        application.getMessageBus().connect().subscribe(EditorColorsListener.class, __ -> {
          // invalidate cache on Appearance Theme/Editor Scheme change
          GRAYED_BY_NORMAL_CACHE.clear();
        });
      }
    }

    private final int highlightStartOffset;
    private final int highlightEndOffset;
    /**
     * @deprecated use {@link #getHighlightAttributes()} instead, the visibility of this field will be decreased.
     */
    @Deprecated
    @Nullable
    public final TextAttributes highlightAttributes;
    /**
     * @deprecated use {@link #getHyperlinkInfo()} instead, the visibility of this field will be decreased.
     */
    @Deprecated
    @Nullable
    public final HyperlinkInfo hyperlinkInfo;

    private final TextAttributes myFollowedHyperlinkAttributes;

    public ResultItem(int highlightStartOffset, int highlightEndOffset, @Nullable HyperlinkInfo hyperlinkInfo) {
      this(highlightStartOffset, highlightEndOffset, hyperlinkInfo, null, null);
    }

    public ResultItem(int highlightStartOffset, int highlightEndOffset, @Nullable HyperlinkInfo hyperlinkInfo, @Nullable TextAttributes highlightAttributes) {
      this(highlightStartOffset, highlightEndOffset, hyperlinkInfo, highlightAttributes, null);
    }

    public ResultItem(int highlightStartOffset, int highlightEndOffset, @Nullable HyperlinkInfo hyperlinkInfo, boolean grayedHyperlink) {
      this(highlightStartOffset, highlightEndOffset, hyperlinkInfo, grayedHyperlink ? getGrayedHyperlinkAttributes(CodeInsightColors.HYPERLINK_ATTRIBUTES) : null,
           grayedHyperlink ? getGrayedHyperlinkAttributes(CodeInsightColors.FOLLOWED_HYPERLINK_ATTRIBUTES) : null);
    }

    public ResultItem(int highlightStartOffset,
                      int highlightEndOffset,
                      @Nullable HyperlinkInfo hyperlinkInfo,
                      @Nullable TextAttributes highlightAttributes,
                      @Nullable TextAttributes followedHyperlinkAttributes) {
      this.highlightStartOffset = highlightStartOffset;
      this.highlightEndOffset = highlightEndOffset;
      this.hyperlinkInfo = hyperlinkInfo;
      this.highlightAttributes = highlightAttributes;
      myFollowedHyperlinkAttributes = followedHyperlinkAttributes;
    }

    public int getHighlightStartOffset() {
      return highlightStartOffset;
    }

    public int getHighlightEndOffset() {
      return highlightEndOffset;
    }

    @Nullable
    public TextAttributes getHighlightAttributes() {
      return highlightAttributes;
    }

    @Nullable
    public TextAttributes getFollowedHyperlinkAttributes() {
      return myFollowedHyperlinkAttributes;
    }

    @Nullable
    public HyperlinkInfo getHyperlinkInfo() {
      return hyperlinkInfo;
    }

    /**
     * See {@link HighlighterLayer} for available predefined layers.
     */
    public int getHighlighterLayer() {
      return getHyperlinkInfo() != null ? HighlighterLayer.HYPERLINK : HighlighterLayer.CONSOLE_FILTER;
    }

    @Nullable
    private static TextAttributes getGrayedHyperlinkAttributes(@Nonnull TextAttributesKey normalHyperlinkAttrsKey) {
      EditorColorsScheme globalScheme = EditorColorsManager.getInstance().getGlobalScheme();
      TextAttributes grayedHyperlinkAttrs = GRAYED_BY_NORMAL_CACHE.get(normalHyperlinkAttrsKey);
      if (grayedHyperlinkAttrs == null) {
        TextAttributes normalHyperlinkAttrs = globalScheme.getAttributes(normalHyperlinkAttrsKey);
        if (normalHyperlinkAttrs != null) {
          grayedHyperlinkAttrs = normalHyperlinkAttrs.clone();
          grayedHyperlinkAttrs.setForegroundColor(TargetAWT.from(UIUtil.getInactiveTextColor()));
          grayedHyperlinkAttrs.setEffectColor(TargetAWT.from(UIUtil.getInactiveTextColor()));
          GRAYED_BY_NORMAL_CACHE.put(normalHyperlinkAttrsKey, grayedHyperlinkAttrs);
        }
      }
      return grayedHyperlinkAttrs;
    }
  }

  /**
   * Filters line by creating an instance of {@link Result}.
   *
   * @param line         The line to be filtered. Note that the line must contain a line
   *                     separator at the end.
   * @param entireLength The length of the entire text including the line passed for filtration.
   * @return {@code null} if there was no match. Otherwise, an instance of {@link Result}
   */
  @Nullable
  Result applyFilter(@Nonnull String line, int entireLength);
}
