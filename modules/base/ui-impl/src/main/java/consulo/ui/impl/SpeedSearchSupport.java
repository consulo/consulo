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
package consulo.ui.impl;

import consulo.application.util.matcher.MinusculeMatcher;
import consulo.application.util.matcher.NameUtil;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2026-10-20
 */
public final class SpeedSearchSupport<Item> {
    private final boolean myCamelCaseWords;

    private @Nullable Function<Item, String> myConverter;
    private @Nullable String myText;

    private @Nullable String myMatcherPattern;
    private @Nullable MinusculeMatcher myMatcher;

    public SpeedSearchSupport(boolean camelCaseWords) {
        myCamelCaseWords = camelCaseWords;
    }

    public void setConverter(@Nullable Function<Item, String> converter) {
        myConverter = converter;
        if (converter == null) {
            myText = null;
        }
    }

    public boolean isEnabled() {
        return myConverter != null;
    }

    public @Nullable String getText() {
        return myText;
    }

    public void setText(@Nullable String text) {
        myText = myConverter == null ? null : text;
    }

    public boolean matches(Item item) {
        Function<Item, String> converter = myConverter;
        String text = myText;
        if (converter == null || text == null) {
            return false;
        }

        String pattern = text.trim();
        if (pattern.isEmpty()) {
            return true;
        }

        String itemText = converter.apply(item);
        return itemText != null && matcher(pattern).matchingFragments(itemText) != null;
    }

    public @Nullable Item find(List<? extends Item> items, int selectedIndex) {
        int size = items.size();
        int start = Math.min(Math.max(selectedIndex, 0), size);
        for (int i = start; i < size; i++) {
            if (matches(items.get(i))) {
                return items.get(i);
            }
        }
        for (int i = 0; i < start; i++) {
            if (matches(items.get(i))) {
                return items.get(i);
            }
        }
        return null;
    }

    public @Nullable Item findNext(List<? extends Item> items, int selectedIndex) {
        int size = items.size();
        int current = Math.min(selectedIndex, size - 1);
        for (int i = current + 1; i < size; i++) {
            if (matches(items.get(i))) {
                return items.get(i);
            }
        }
        for (int i = 0; i <= current; i++) {
            if (matches(items.get(i))) {
                return items.get(i);
            }
        }
        return null;
    }

    public @Nullable Item findPrevious(List<? extends Item> items, int selectedIndex) {
        int size = items.size();
        if (selectedIndex < 0 || selectedIndex >= size) {
            return null;
        }

        for (int i = selectedIndex - 1; i >= 0; i--) {
            if (matches(items.get(i))) {
                return items.get(i);
            }
        }
        for (int i = size - 1; i >= selectedIndex; i--) {
            if (matches(items.get(i))) {
                return items.get(i);
            }
        }
        return null;
    }

    public @Nullable Item findFirst(List<? extends Item> items) {
        for (Item item : items) {
            if (matches(item)) {
                return item;
            }
        }
        return null;
    }

    public @Nullable Item findLast(List<? extends Item> items) {
        for (int i = items.size() - 1; i >= 0; i--) {
            if (matches(items.get(i))) {
                return items.get(i);
            }
        }
        return null;
    }

    public static <Item> int indexOf(List<? extends Item> items, @Nullable Item item) {
        if (item == null) {
            return -1;
        }

        for (int i = 0; i < items.size(); i++) {
            if (Objects.equals(items.get(i), item)) {
                return i;
            }
        }
        return -1;
    }

    private MinusculeMatcher matcher(String pattern) {
        MinusculeMatcher matcher = myMatcher;
        if (matcher != null && pattern.equals(myMatcherPattern)) {
            return matcher;
        }

        String effective = myCamelCaseWords ? String.join("*", NameUtil.nameToWords(pattern)) : pattern;
        if (!effective.startsWith("*")) {
            effective = "*" + effective;
        }

        matcher = NameUtil.buildMatcher(effective).build();
        myMatcherPattern = pattern;
        myMatcher = matcher;
        return matcher;
    }
}
