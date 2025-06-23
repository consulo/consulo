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
package consulo.ui.ex.awt.speedSearch;

import consulo.application.util.matcher.Matcher;
import consulo.application.util.matcher.MatcherTextRange;
import consulo.application.util.matcher.MinusculeMatcher;
import consulo.application.util.registry.Registry;
import consulo.ui.ex.ColoredTextContainer;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.SpeedSearchUtilBase;
import consulo.ui.ex.awt.tree.ColoredTreeCellRenderer;

import jakarta.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;

/**
 * @author spLeaner
 */
public final class SpeedSearchUtil {

  private SpeedSearchUtil() {
  }

  public static void applySpeedSearchHighlighting(@Nonnull JComponent speedSearchEnabledComponent, @Nonnull ColoredTextContainer coloredComponent, boolean mainTextOnly, boolean selected) {
    SpeedSearchUtilBase.applySpeedSearchHighlighting(speedSearchEnabledComponent, coloredComponent, mainTextOnly, selected);
  }

  public static void appendFragmentsForSpeedSearch(@Nonnull JComponent speedSearchEnabledComponent,
                                                   @Nonnull String text,
                                                   @Nonnull SimpleTextAttributes attributes,
                                                   boolean selected,
                                                   @Nonnull ColoredTextContainer simpleColoredComponent) {
    SpeedSearchUtilBase.appendFragmentsForSpeedSearch(speedSearchEnabledComponent, text, attributes, selected, simpleColoredComponent);
  }

  public static void appendColoredFragmentForMatcher(@Nonnull String text, ColoredTextContainer component,
                                                     @Nonnull final SimpleTextAttributes attributes,
                                                     Matcher matcher,
                                                     Color selectedBg,
                                                     boolean selected) {
    if (!(matcher instanceof MinusculeMatcher) || (Registry.is("ide.highlight.match.in.selected.only") && !selected)) {
      component.append(text, attributes);
      return;
    }

    final Iterable<MatcherTextRange> iterable = ((MinusculeMatcher)matcher).matchingFragments(text);
    if (iterable != null) {
      final Color fg = attributes.getFgColor();
      final int style = attributes.getStyle();
      final SimpleTextAttributes plain = new SimpleTextAttributes(style, fg);
      final SimpleTextAttributes highlighted = new SimpleTextAttributes(selectedBg, fg, null, style | SimpleTextAttributes.STYLE_SEARCH_MATCH);
      appendColoredFragments(component, text, iterable, plain, highlighted);
    }
    else {
      component.append(text, attributes);
    }
  }

  public static void appendColoredFragments(final ColoredTextContainer simpleColoredComponent,
                                            final String text,
                                            Iterable<MatcherTextRange> colored,
                                            final SimpleTextAttributes plain,
                                            final SimpleTextAttributes highlighted) {
    SpeedSearchUtilBase.appendColoredFragments(simpleColoredComponent, text, colored, plain, highlighted);
  }

  public static void applySpeedSearchHighlightingFiltered(JTree tree, Object value, ColoredTreeCellRenderer coloredTreeCellRenderer, boolean b, boolean selected) {
    SpeedSearchUtilBase.applySpeedSearchHighlightingFiltered(tree, value, coloredTreeCellRenderer, b, selected);
  }
}
