/*
 * Copyright 2013-2026 consulo.io
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
package consulo.language.editor.impl.internal.completion.lookup;

import consulo.application.util.matcher.MatcherTextRange;
import consulo.application.util.matcher.MinusculeMatcher;
import consulo.application.util.matcher.NameUtil;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementPresentation;
import consulo.localize.LocalizeValue;
import consulo.ui.AdvancedLabel;
import consulo.ui.Component;
import consulo.ui.ComponentItemRender;
import consulo.ui.RenderItem;
import consulo.ui.TextAttribute;
import consulo.ui.TextItemPresentation;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.font.Font;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.style.ComponentColors;
import consulo.ui.style.StandardColors;
import consulo.util.collection.FList;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * One row of a lookup: the icon and the name of the item, the tail which says more about it, and the type it carries
 * on the right. Three runs of text rather than one, because they are aligned apart - the name against the left and
 * the type against the right, with the tail taking what is left between them.
 * <p/>
 * A row is a component rather than a single run of text because {@link TextItemPresentation} lays its fragments out
 * in one line, and a right aligned column cannot be expressed that way.
 *
 * @author VISTALL
 */
public class LookupItemRender {
    /**
     * The part of the name the typed prefix matched, told apart from the rest of it.
     */
    private static final ColorValue MATCHED_FOREGROUND_COLOR = StandardColors.BLUE;

    private final Function<LookupElement, LookupElementPresentation> myPresentations;
    private final Function<LookupElement, String> myPatterns;

    /**
     * Every row of one refresh is matched against the same prefix, and building a matcher compiles it - so the last
     * one is kept rather than built again per row. Drawing a row must only replay what was worked out elsewhere.
     */
    private @Nullable String myMatcherPattern;
    private @Nullable MinusculeMatcher myMatcher;

    /**
     * @param presentations how an element renders, taken from wherever the lookup caches it rather than computed here
     * @param patterns      the prefix an element was matched with, for telling the matched part of the name apart
     */
    public LookupItemRender(
        Function<LookupElement, LookupElementPresentation> presentations,
        Function<LookupElement, String> patterns
    ) {
        myPresentations = presentations;
        myPatterns = patterns;
    }

    /**
     * A row of the list, as a render which is built once per row on screen and rebound as the items under it change.
     * <p/>
     * Typing changes every item at once, and a row rebuilt from nothing is a tree of components back over the wire
     * per visible line - the three runs are made here and only their text is replaced afterwards.
     */
    public ComponentItemRender<LookupElement> asRender() {
        return ComponentItemRender.reusable(this::createRow, this::bindRow);
    }

    /**
     * The runs of each row, by the layout they were put in - a render hands the backend a component and is handed
     * the same one back to rebind, and the layout is the only thing that travels between the two.
     */
    private final Map<HorizontalLayout, Row> myRows = new IdentityHashMap<>();

    private HorizontalLayout createRow() {
        Row row = new Row();
        myRows.put(row.myLayout, row);
        return row.myLayout;
    }

    /**
     * Sets every run, always - the row it is given is whatever the list last showed there, never a clean one.
     */
    private void bindRow(HorizontalLayout layout, RenderItem<LookupElement> item) {
        Row row = myRows.get(layout);
        if (row == null) {
            return;
        }

        LookupElement value = item.getValue();
        if (value == null) {
            row.clear();
            return;
        }

        LookupElementPresentation presentation = myPresentations.apply(value);

        row.myName.updatePresentation(target -> renderName(target, value, presentation));
        row.myTail.updatePresentation(target -> renderTail(target, presentation));
        row.myType.updatePresentation(target -> renderType(target, presentation));
    }

    /**
     * The three runs a row is made of, kept together so a rebind can reach each of them.
     */
    private static class Row {
        private final HorizontalLayout myLayout = HorizontalLayout.create();
        private final AdvancedLabel myName = AdvancedLabel.create();
        private final AdvancedLabel myTail = AdvancedLabel.create();
        private final AdvancedLabel myType = AdvancedLabel.create();

        Row() {
            myLayout.add(myName);
            myLayout.add(myTail);
            myLayout.add(myType);
        }

        void clear() {
            myName.updatePresentation(target -> {
            });
            myTail.updatePresentation(target -> {
            });
            myType.updatePresentation(target -> {
            });
        }
    }

    private void renderName(TextItemPresentation target, LookupElement item, LookupElementPresentation presentation) {
        target.withIcon(presentation.getIcon());

        String text = StringUtil.notNullize(presentation.getItemText());
        int style = styleOf(presentation);

        TextAttribute plain = new TextAttribute(style, presentation.getItemTextForeground());

        String pattern = item instanceof EmptyLookupItem ? "" : StringUtil.notNullize(myPatterns.apply(item));
        FList<MatcherTextRange> matched = pattern.isEmpty() ? null : matcherFor(pattern).matchingFragments(text);
        if (matched == null || matched.isEmpty()) {
            target.append(LocalizeValue.of(text), plain);
            return;
        }

        TextAttribute highlighted = new TextAttribute(style, MATCHED_FOREGROUND_COLOR);

        int last = 0;
        for (MatcherTextRange range : matched) {
            if (range.getStartOffset() > last) {
                target.append(LocalizeValue.of(text.substring(last, range.getStartOffset())), plain);
            }
            target.append(LocalizeValue.of(text.substring(range.getStartOffset(), range.getEndOffset())), highlighted);
            last = range.getEndOffset();
        }
        if (last < text.length()) {
            target.append(LocalizeValue.of(text.substring(last)), plain);
        }
    }

    private static void renderTail(TextItemPresentation target, LookupElementPresentation presentation) {
        List<LookupElementPresentation.TextFragment> fragments = presentation.getTailFragments();
        if (fragments.isEmpty()) {
            return;
        }

        int style = styleOf(presentation);
        for (LookupElementPresentation.TextFragment fragment : fragments) {
            int fragmentStyle = fragment.isItalic() ? style | Font.ITALIC : style;
            ColorValue foreground = fragment.getForegroundColor();
            if (foreground == null && fragment.isGrayed()) {
                foreground = ComponentColors.DISABLED_TEXT;
            }

            target.append(LocalizeValue.of(fragment.text), new TextAttribute(fragmentStyle, foreground));
        }
    }

    private static void renderType(TextItemPresentation target, LookupElementPresentation presentation) {
        target.withIcon(presentation.getTypeIcon());

        String text = presentation.getTypeText();
        if (StringUtil.isEmpty(text)) {
            return;
        }

        target.append(
            LocalizeValue.of(text),
            new TextAttribute(Font.PLAIN, presentation.isTypeGrayed() ? ComponentColors.DISABLED_TEXT : null)
        );
    }

    private static int styleOf(LookupElementPresentation presentation) {
        int style = presentation.isItemTextBold() ? Font.BOLD : Font.PLAIN;
        if (presentation.isStrikeout()) {
            style |= SimpleTextAttributes.STYLE_STRIKEOUT;
        }
        if (presentation.isItemTextUnderlined()) {
            style |= SimpleTextAttributes.STYLE_UNDERLINE;
        }
        if (presentation.isItemTextItalic()) {
            style |= SimpleTextAttributes.STYLE_ITALIC;
        }
        return style;
    }

    /**
     * Which parts of a name the typed prefix accounts for. The leading star makes it a match anywhere rather than
     * only at the start, the way a lookup matches.
     */
    private MinusculeMatcher matcherFor(String pattern) {
        MinusculeMatcher matcher = myMatcher;
        if (matcher == null || !pattern.equals(myMatcherPattern)) {
            matcher = NameUtil.buildMatcher("*" + pattern).build();
            myMatcher = matcher;
            myMatcherPattern = pattern;
        }
        return matcher;
    }
}
