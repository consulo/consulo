/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.ui.ex.awt;

import consulo.application.util.matcher.MatcherTextRange;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.speedSearch.SpeedSearchSupply;
import consulo.ui.ex.awt.tree.ColoredTreeCellRenderer;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.Pair;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * User: spLeaner
 */
public class SpeedSearchUtilBase {

  private SpeedSearchUtilBase() {
  }

  public static void applySpeedSearchHighlighting(@Nonnull JComponent speedSearchEnabledComponent, @Nonnull SimpleColoredComponent coloredComponent, boolean mainTextOnly, boolean selected) {
    SpeedSearchSupply speedSearch = SpeedSearchSupply.getSupply(speedSearchEnabledComponent);
    // The bad thing is that SpeedSearch model is decoupled from UI presentation so we don't know the real matched text.
    // Our best guess is to get strgin from the ColoredComponent. We can only provide main-text-only option.
    Iterable<MatcherTextRange> ranges = speedSearch == null ? null : speedSearch.matchingFragments(coloredComponent.getCharSequence(mainTextOnly).toString());
    Iterator<MatcherTextRange> rangesIterator = ranges != null ? ranges.iterator() : null;
    if (rangesIterator == null || !rangesIterator.hasNext()) return;
    Color bg = selected ? UIUtil.getTreeSelectionBackground() : UIUtil.getTreeTextBackground();

    SimpleColoredComponent.ColoredIterator coloredIterator = coloredComponent.iterator();
    MatcherTextRange range = rangesIterator.next();
    main:
    while (coloredIterator.hasNext()) {
      coloredIterator.next();
      int offset = coloredIterator.getOffset();
      int endOffset = coloredIterator.getEndOffset();
      if (!range.intersectsStrict(offset, endOffset)) continue;
      SimpleTextAttributes attributes = coloredIterator.getTextAttributes();
      SimpleTextAttributes highlighted = new SimpleTextAttributes(bg, attributes.getFgColor(), null, attributes.getStyle() | SimpleTextAttributes.STYLE_SEARCH_MATCH);
      if (range.getStartOffset() > offset) {
        offset = coloredIterator.split(range.getStartOffset() - offset, attributes);
      }
      do {
        if (range.getEndOffset() <= endOffset) {
          offset = coloredIterator.split(range.getEndOffset() - offset, highlighted);
          if (rangesIterator.hasNext()) {
            range = rangesIterator.next();
          }
          else {
            break main;
          }
        }
        else {
          coloredIterator.split(endOffset - offset, highlighted);
          continue main;
        }
      }
      while (range.intersectsStrict(offset, endOffset));
    }
  }

  public static void appendFragmentsForSpeedSearch(@Nonnull JComponent speedSearchEnabledComponent,
                                                   @Nonnull String text,
                                                   @Nonnull SimpleTextAttributes attributes,
                                                   boolean selected,
                                                   @Nonnull SimpleColoredComponent simpleColoredComponent) {
    final SpeedSearchSupply speedSearch = SpeedSearchSupply.getSupply(speedSearchEnabledComponent);
    if (speedSearch != null) {
      final Iterable<MatcherTextRange> fragments = speedSearch.matchingFragments(text);
      if (fragments != null) {
        final Color fg = attributes.getFgColor();
        final Color bg = selected ? UIUtil.getTreeSelectionBackground() : UIUtil.getTreeTextBackground();
        final int style = attributes.getStyle();
        final SimpleTextAttributes plain = new SimpleTextAttributes(style, fg);
        final SimpleTextAttributes highlighted = new SimpleTextAttributes(bg, fg, null, style | SimpleTextAttributes.STYLE_SEARCH_MATCH);
        appendColoredFragments(simpleColoredComponent, text, fragments, plain, highlighted);
        return;
      }
    }
    simpleColoredComponent.append(text, attributes);
  }

  public static void appendColoredFragments(final SimpleColoredComponent simpleColoredComponent,
                                            final String text,
                                            Iterable<MatcherTextRange> colored,
                                            final SimpleTextAttributes plain,
                                            final SimpleTextAttributes highlighted) {
    final List<Pair<String, Integer>> searchTerms = new ArrayList<Pair<String, Integer>>();
    for (MatcherTextRange fragment : colored) {
      searchTerms.add(Pair.create(text.substring(fragment.getStartOffset(), fragment.getEndOffset()), fragment.getStartOffset()));
    }

    final int[] lastOffset = {0};
    ContainerUtil.process(searchTerms, pair -> {
      if (pair.second > lastOffset[0]) {
        simpleColoredComponent.append(text.substring(lastOffset[0], pair.second), plain);
      }

      simpleColoredComponent.append(text.substring(pair.second, pair.second + pair.first.length()), highlighted);
      lastOffset[0] = pair.second + pair.first.length();
      return true;
    });

    if (lastOffset[0] < text.length()) {
      simpleColoredComponent.append(text.substring(lastOffset[0]), plain);
    }
  }

  public static void applySpeedSearchHighlightingFiltered(JTree tree, Object value, ColoredTreeCellRenderer coloredTreeCellRenderer, boolean b, boolean selected) {
    SpeedSearchSupply speedSearch = SpeedSearchSupply.getSupply(tree);
    if (speedSearch != null && !speedSearch.isObjectFilteredOut(value)) {
      applySpeedSearchHighlighting(tree, coloredTreeCellRenderer, b, selected);
    }
  }
}
