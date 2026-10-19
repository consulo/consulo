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
package consulo.searchEverywhere;

import consulo.application.util.matcher.MatcherTextRange;
import consulo.application.util.matcher.MinusculeMatcher;
import consulo.localize.LocalizeValue;
import consulo.ui.TextAttribute;
import consulo.ui.TextEffect;
import consulo.ui.TextItemPresentation;
import consulo.ui.color.ColorValue;
import consulo.ui.image.Image;
import consulo.ui.style.ComponentColors;
import consulo.util.collection.FList;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Everything a row of the search everywhere list shows, built once when the item is found, on the search thread.
 * A row only replays it, so nothing reads the model while the list is drawn.
 *
 * @author VISTALL
 * @since 2026-10-19
 */
public final class SearchEverywhereItemPresentation {
    public static final class Builder {
        private @Nullable Image myIcon;
        private final List<LocalizeValue> myTexts = new ArrayList<>();
        private final List<TextAttribute> myAttributes = new ArrayList<>();
        private LocalizeValue mySuffixText = LocalizeValue.empty();
        private @Nullable Image mySuffixIcon;
        private @Nullable ColorValue myBackgroundColor;
        private @Nullable Boolean mySwitchState;
        private LocalizeValue myTooltip = LocalizeValue.empty();

        private Builder() {
        }

        public Builder icon(@Nullable Image icon) {
            myIcon = icon;
            return this;
        }

        public Builder append(String text) {
            return append(LocalizeValue.of(text), TextAttribute.REGULAR);
        }

        public Builder append(String text, TextAttribute attribute) {
            return append(LocalizeValue.of(text), attribute);
        }

        public Builder append(LocalizeValue text, TextAttribute attribute) {
            if (text != LocalizeValue.empty()) {
                myTexts.add(text);
                myAttributes.add(attribute);
            }
            return this;
        }

        /**
         * Appends the text with the fragments the matcher matches in it highlighted as a search match.
         */
        public Builder appendMatched(String text, @Nullable MinusculeMatcher matcher, TextAttribute attribute) {
            FList<MatcherTextRange> fragments = matcher == null || text.isEmpty() ? null : matcher.matchingFragments(text);
            if (fragments == null || fragments.isEmpty()) {
                return append(text, attribute);
            }

            TextAttribute matched = matchedAttribute(attribute);
            int offset = 0;
            for (MatcherTextRange fragment : fragments) {
                int start = Math.max(offset, Math.min(fragment.getStartOffset(), text.length()));
                int end = Math.max(start, Math.min(fragment.getEndOffset(), text.length()));
                if (start > offset) {
                    append(text.substring(offset, start), attribute);
                }
                if (end > start) {
                    append(text.substring(start, end), matched);
                }
                offset = end;
            }
            if (offset < text.length()) {
                append(text.substring(offset), attribute);
            }
            return this;
        }

        public Builder suffix(LocalizeValue text, @Nullable Image icon) {
            mySuffixText = text;
            mySuffixIcon = icon;
            return this;
        }

        public Builder backgroundColor(@Nullable ColorValue color) {
            myBackgroundColor = color;
            return this;
        }

        /**
         * The state of the switch drawn at the trailing edge of the row, for an item which toggles something;
         * {@code null} for an item with no switch.
         */
        public Builder switchState(@Nullable Boolean state) {
            mySwitchState = state;
            return this;
        }

        public Builder tooltip(LocalizeValue tooltip) {
            myTooltip = tooltip;
            return this;
        }

        public SearchEverywhereItemPresentation build() {
            return new SearchEverywhereItemPresentation(this);
        }

        private static TextAttribute matchedAttribute(TextAttribute attribute) {
            TextAttribute matched =
                new TextAttribute(attribute.getStyle(), ComponentColors.SEARCH_MATCH_FOREGROUND, ComponentColors.SEARCH_MATCH_BACKGROUND);
            for (TextEffect effect : attribute.getEffects()) {
                matched = matched.withEffect(effect);
            }
            return matched;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static SearchEverywhereItemPresentation of(String text) {
        return builder().append(text).build();
    }

    private final @Nullable Image myIcon;
    private final List<LocalizeValue> myTexts;
    private final List<TextAttribute> myAttributes;
    private final LocalizeValue mySuffixText;
    private final @Nullable Image mySuffixIcon;
    private final @Nullable ColorValue myBackgroundColor;
    private final @Nullable Boolean mySwitchState;
    private final LocalizeValue myTooltip;

    private SearchEverywhereItemPresentation(Builder builder) {
        this(
            builder.myIcon,
            List.copyOf(builder.myTexts),
            List.copyOf(builder.myAttributes),
            builder.mySuffixText,
            builder.mySuffixIcon,
            builder.myBackgroundColor,
            builder.mySwitchState,
            builder.myTooltip
        );
    }

    private SearchEverywhereItemPresentation(
        @Nullable Image icon,
        List<LocalizeValue> texts,
        List<TextAttribute> attributes,
        LocalizeValue suffixText,
        @Nullable Image suffixIcon,
        @Nullable ColorValue backgroundColor,
        @Nullable Boolean switchState,
        LocalizeValue tooltip
    ) {
        myIcon = icon;
        myTexts = texts;
        myAttributes = attributes;
        mySuffixText = suffixText;
        mySuffixIcon = suffixIcon;
        myBackgroundColor = backgroundColor;
        mySwitchState = switchState;
        myTooltip = tooltip;
    }

    public SearchEverywhereItemPresentation withSwitchState(@Nullable Boolean switchState) {
        return new SearchEverywhereItemPresentation(
            myIcon,
            myTexts,
            myAttributes,
            mySuffixText,
            mySuffixIcon,
            myBackgroundColor,
            switchState,
            myTooltip
        );
    }

    public void render(TextItemPresentation presentation) {
        presentation.clearText();
        presentation.withIcon(myIcon);
        for (int i = 0; i < myTexts.size(); i++) {
            presentation.append(myTexts.get(i), myAttributes.get(i));
        }
    }

    public @Nullable Image getIcon() {
        return myIcon;
    }

    public LocalizeValue getText() {
        return switch (myTexts.size()) {
            case 0 -> LocalizeValue.empty();
            case 1 -> myTexts.getFirst();
            default -> LocalizeValue.join(myTexts.toArray(LocalizeValue[]::new));
        };
    }

    public LocalizeValue getSuffixText() {
        return mySuffixText;
    }

    public @Nullable Image getSuffixIcon() {
        return mySuffixIcon;
    }

    public @Nullable ColorValue getBackgroundColor() {
        return myBackgroundColor;
    }

    public @Nullable Boolean getSwitchState() {
        return mySwitchState;
    }

    public LocalizeValue getTooltip() {
        return myTooltip;
    }
}
