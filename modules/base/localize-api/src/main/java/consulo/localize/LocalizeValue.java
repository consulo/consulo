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
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

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
    @Nonnull
    static LocalizeValue empty() {
        return EmptyLocalizeValue.VALUE;
    }

    @Nonnull
    static LocalizeValue space() {
        return ConstantLocalizeValue.SPACE;
    }

    @Nonnull
    static LocalizeValue colon() {
        return ConstantLocalizeValue.COLON;
    }

    @Nonnull
    static LocalizeValue dot() {
        return ConstantLocalizeValue.DOT;
    }

    @Nonnull
    static LocalizeValue questionMark() {
        return ConstantLocalizeValue.QUESTION_MARK;
    }

    @Nonnull
    static LocalizeValue localizeTODO(@Nonnull String text) {
        return of(text);
    }

    @Nonnull
    static LocalizeValue of(@Nonnull String text) {
        return text.isEmpty() ? empty() : new ConstantLocalizeValue(text);
    }

    @Nonnull
    static LocalizeValue of(char c) {
        return new ConstantLocalizeValue(String.valueOf(c));
    }

    @Nonnull
    static LocalizeValue ofNullable(@Nullable String text) {
        return text == null ? empty() : of(text);
    }

    @Nonnull
    static LocalizeValue join(@Nonnull LocalizeValue... values) {
        return values.length == 0 ? empty() : new JoinedLocalizeValue(values);
    }

    @Nonnull
    static LocalizeValue join(@Nonnull String separator, @Nonnull LocalizeValue... values) {
        return values.length == 0 ? empty() : new SeparatorJoinedLocalizeValue(separator, values);
    }

    @Nonnull
    static LocalizeValue joinWithSeparator(@Nonnull LocalizeValue separator, @Nonnull LocalizeValue... values) {
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

    @Nonnull
    @Override
    default String get() {
        return getValue();
    }

    @Nullable
    default String getNullIfEmpty() {
        return getValue();
    }

    @Nonnull
    default LocalizeValue orIfEmpty(LocalizeValue defaultValue) {
        return this;
    }

    @Nonnull
    String getValue();

    byte getModificationCount();

    @Nonnull
    default Optional<LocalizeKey> getKey() {
        return Optional.empty();
    }

    @Nonnull
    default LocalizeValue map(@Nonnull Function<String, String> mapper) {
        return new MappedLocalizeValue(this, mapper);
    }

    @Nonnull
    default LocalizeValue map(@Nonnull BiFunction<LocalizeManager, String, String> mapper) {
        return new MappedLocalizeValue2(this, mapper);
    }

    @Nonnull
    default LocalizeValue toUpperCase() {
        return map(DefaultMapFunctions.TO_UPPER_CASE);
    }

    @Nonnull
    default LocalizeValue toLowerCase() {
        return map(DefaultMapFunctions.TO_LOWER_CASE);
    }

    @Nonnull
    default LocalizeValue capitalize() {
        return map(DefaultMapFunctions.CAPITALIZE);
    }

    @Override
    default public int compareTo(@Nonnull LocalizeValue that) {
        return comparator().compare(this, that);
    }
}
