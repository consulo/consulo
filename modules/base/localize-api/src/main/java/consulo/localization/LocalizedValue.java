/*
 * Copyright 2013-2019 consulo.io
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
import consulo.localize.LocalizeKey;
import consulo.localize.LocalizeManager;
import consulo.localize.LocalizeValue;
import consulo.localize.internal.DefaultMapFunctions;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Comparator;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @author NYUrchenko
 * @since 2019-04-11
 */
public interface LocalizedValue extends Supplier<String>, Comparable<LocalizeValue> {
    public static final Comparator<LocalizeValue> CASE_INSENSITIVE_ORDER = LocalizeValue::compareToIgnoreCase;

    @Nonnull
    static LocalizeValue empty() {
        return ConstantLocalizedValue.EMPTY;
    }

    @Nonnull
    static LocalizeValue space() {
        return ConstantLocalizedValue.SPACE;
    }

    @Nonnull
    static LocalizeValue colon() {
        return ConstantLocalizedValue.COLON;
    }

    @Nonnull
    static LocalizeValue dot() {
        return ConstantLocalizedValue.DOT;
    }

    @Nonnull
    static LocalizeValue questionMark() {
        return ConstantLocalizedValue.QUESTION_MARK;
    }

    @Nonnull
    static LocalizeValue of() {
        return empty();
    }

    @Nonnull
    static LocalizeValue localizeTODO(@Nonnull String text) {
        return of(text);
    }

    @Nonnull
    static LocalizeValue of(@Nonnull String text) {
        return text.isEmpty() ? of() : new ConstantLocalizedValue(text);
    }

    @Nonnull
    static LocalizeValue of(char c) {
        return new ConstantLocalizedValue(String.valueOf(c));
    }

    @Nonnull
    static LocalizeValue ofNullable(@Nullable String text) {
        return text == null ? of() : of(text);
    }

    @Nonnull
    static LocalizeValue join(@Nonnull LocalizedValue... values) {
        return values.length == 0 ? of() : new JoinedLocalizedValue(LocalizationManager.get(), values);
    }

    @Nonnull
    static LocalizeValue join(@Nonnull String separator, @Nonnull LocalizedValue... values) {
        return values.length == 0 ? of() : new SeparatorJoinedLocalizedValue(LocalizationManager.get(), separator, values);
    }

    @Nonnull
    static LocalizeValue joinWithSeparator(@Nonnull LocalizeValue separator, @Nonnull LocalizedValue... values) {
        return values.length == 0 ? of() : new SeparatorJoinedLocalizedValue2(LocalizationManager.get(), separator, values);
    }

    @Nonnull
    @Override
    default String get() {
        return getValue();
    }

    @Nonnull
    String getValue();

    byte getModificationCount();

    @Nonnull
    default Optional<LocalizeKey> getKey() {
        return Optional.empty();
    }

    @Nonnull
    default LocalizeValue map(@Nonnull BiFunction<LocalizeManager, String, String> mapper) {
        return new MappedLocalizedValue(LocalizationManager.get(), this, mapper);
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
    default int compareTo(@Nonnull LocalizeValue o) {
        return getValue().compareTo(o.getValue());
    }

    default int compareToIgnoreCase(@Nonnull LocalizeValue other) {
        return getValue().compareToIgnoreCase(other.getValue());
    }
}
