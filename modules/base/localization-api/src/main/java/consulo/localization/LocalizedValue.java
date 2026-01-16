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
package consulo.localization;

import consulo.localization.internal.*;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Comparator;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @author NYUrchenko
 * @since 2019-04-11
 */
public interface LocalizedValue extends Supplier<String>, Comparable<LocalizedValue> {
    @Nonnull
    static LocalizedValue empty() {
        return EmptyLocalizedValue.VALUE;
    }

    @Nonnull
    static LocalizedValue space() {
        return ConstantLocalizedValue.SPACE;
    }

    @Nonnull
    static LocalizedValue colon() {
        return ConstantLocalizedValue.COLON;
    }

    @Nonnull
    static LocalizedValue dot() {
        return ConstantLocalizedValue.DOT;
    }

    @Nonnull
    static LocalizedValue questionMark() {
        return ConstantLocalizedValue.QUESTION_MARK;
    }

    @Nonnull
    static LocalizedValue localizeTODO(@Nonnull String text) {
        return of(text);
    }

    @Nonnull
    static LocalizedValue of(@Nonnull String text) {
        return text.isEmpty() ? empty() : new ConstantLocalizedValue(text);
    }

    @Nonnull
    static LocalizedValue of(char c) {
        return new ConstantLocalizedValue(String.valueOf(c));
    }

    @Nonnull
    static LocalizedValue ofNullable(@Nullable String text) {
        return text == null ? empty() : of(text);
    }

    @Nonnull
    static LocalizedValue join(@Nonnull LocalizedValue... values) {
        return values.length == 0 ? empty() : new JoinedLocalizedValue(LocalizationManager.get(), values);
    }

    @Nonnull
    static LocalizedValue join(@Nonnull String separator, @Nonnull LocalizedValue... values) {
        return values.length == 0 ? empty() : new SeparatorJoinedLocalizedValue(LocalizationManager.get(), separator, values);
    }

    @Nonnull
    static LocalizedValue joinWithSeparator(@Nonnull LocalizedValue separator, @Nonnull LocalizedValue... values) {
        return values.length == 0 ? empty() : new SeparatorJoinedLocalizedValue2(LocalizationManager.get(), separator, values);
    }

    static Comparator<LocalizedValue> comparator() {
        return AbstractLocalizedValue.CASE_INSENSITIVE_ORDER;
    }

    default boolean isEmpty() {
        return false;
    }

    default boolean isNotEmpty() {
        return !isEmpty();
    }

    @Nonnull
    String getValue();

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
    default LocalizedValue orIfEmpty(LocalizedValue defaultValue) {
        return this;
    }

    byte getModificationCount();

    @Nonnull
    default Optional<LocalizationKey> getKey() {
        return Optional.empty();
    }

    @Nonnull
    default LocalizedValue map(@Nonnull Function<String, String> mapper) {
        return new MappedLocalizedValue(LocalizationManager.get(), this, mapper);
    }

    @Nonnull
    default LocalizedValue map(@Nonnull BiFunction<LocalizationManager, String, String> mapper) {
        return new MappedLocalizedValue2(LocalizationManager.get(), this, mapper);
    }

    @Nonnull
    default LocalizedValue toUpperCase() {
        return map(DefaultMapFunctions.TO_UPPER_CASE);
    }

    @Nonnull
    default LocalizedValue toLowerCase() {
        return map(DefaultMapFunctions.TO_LOWER_CASE);
    }

    @Nonnull
    default LocalizedValue capitalize() {
        return map(DefaultMapFunctions.CAPITALIZE);
    }

    @Override
    default int compareTo(@Nonnull LocalizedValue that) {
        return comparator().compare(this, that);
    }
}
