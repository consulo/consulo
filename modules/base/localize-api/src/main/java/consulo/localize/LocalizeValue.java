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
package consulo.localize;

import consulo.localize.internal.*;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2019-04-11
 */
public interface LocalizeValue extends Supplier<String>, Comparable<LocalizeValue> {
    static LocalizeValue empty() {
        return EmptyLocalizeValue.VALUE;
    }
    static LocalizeValue space() {
        return ConstantLocalizeValue.SPACE;
    }
    static LocalizeValue colon() {
        return ConstantLocalizeValue.COLON;
    }
    static LocalizeValue dot() {
        return ConstantLocalizeValue.DOT;
    }
    static LocalizeValue questionMark() {
        return ConstantLocalizeValue.QUESTION_MARK;
    }
    static LocalizeValue localizeTODO(String text) {
        return of(text);
    }
    static LocalizeValue of(String text) {
        return text.isEmpty() ? empty() : new ConstantLocalizeValue(text);
    }
    static LocalizeValue of(char c) {
        return new ConstantLocalizeValue(String.valueOf(c));
    }
    static LocalizeValue ofNullable(@Nullable String text) {
        return text == null ? empty() : of(text);
    }
    static LocalizeValue join(LocalizeValue... values) {
        return values.length == 0 ? empty() : new JoinedLocalizeValue(values);
    }
    static LocalizeValue join(String separator, LocalizeValue... values) {
        return values.length == 0 ? empty() : new SeparatorJoinedLocalizeValue(separator, values);
    }
    static LocalizeValue joinWithSeparator(LocalizeValue separator, LocalizeValue... values) {
        return values.length == 0 ? empty() : new SeparatorJoinedLocalizeValue2(separator, values);
    }

    static Comparator<LocalizeValue> comparator() {
        return DefaultLocalizeValue.CASE_INSENSITIVE_ORDER;
    }

    default boolean isEmpty() {
        return false;
    }

    default boolean isNotEmpty() {
        return !isEmpty();
    }
    @Override
    default String get() {
        return getValue();
    }

    @Nullable
    default String getNullIfEmpty() {
        return getValue();
    }
    default LocalizeValue orIfEmpty(LocalizeValue defaultValue) {
        return this;
    }
    String getId();
    String getValue();

    byte getModificationCount();
    default Optional<LocalizeKey> getKey() {
        return Optional.empty();
    }
    default LocalizeValue map(Function<String, String> mapper) {
        return new MappedLocalizeValue(this, mapper);
    }
    default LocalizeValue map(BiFunction<LocalizeManager, String, String> mapper) {
        return new MappedLocalizeValue2(this, mapper);
    }
    default LocalizeValue toUpperCase() {
        return map(DefaultMapFunctions.TO_UPPER_CASE);
    }
    default LocalizeValue toLowerCase() {
        return map(DefaultMapFunctions.TO_LOWER_CASE);
    }
    default LocalizeValue capitalize() {
        return map(DefaultMapFunctions.CAPITALIZE);
    }

    @Override
    default public int compareTo(LocalizeValue that) {
        return comparator().compare(this, that);
    }
}
