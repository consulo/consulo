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
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @author UNV
 * @since 2019-04-11
 */
public interface LocalizedValue extends Supplier<String>, Comparable<LocalizedValue> {
    static LocalizedValue empty() {
        return EmptyLocalizedValue.INSTANCE;
    }
    static LocalizedValue space() {
        return ConstantLocalizedValue.SPACE;
    }
    static LocalizedValue colon() {
        return ConstantLocalizedValue.COLON;
    }
    static LocalizedValue dot() {
        return ConstantLocalizedValue.DOT;
    }
    static LocalizedValue questionMark() {
        return ConstantLocalizedValue.QUESTION_MARK;
    }
    static LocalizedValue localizeTODO(String text) {
        return of(text);
    }
    static LocalizedValue of(String text) {
        return text.isEmpty() ? empty() : new ConstantLocalizedValue(text);
    }
    static LocalizedValue of(char c) {
        return new ConstantLocalizedValue(String.valueOf(c));
    }
    static LocalizedValue ofNullable(@Nullable String text) {
        return text == null ? empty() : of(text);
    }
    static LocalizedValue join(LocalizedValue... values) {
        return values.length == 0 ? empty() : new JoinedLocalizedValue(LocalizationManager.get(), values);
    }
    static LocalizedValue join(String separator, LocalizedValue... values) {
        return values.length == 0 ? empty() : new SeparatorJoinedLocalizedValue(LocalizationManager.get(), separator, values);
    }
    static LocalizedValue joinWithSeparator(LocalizedValue separator, LocalizedValue... values) {
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
    String getId();
    String getValue();
    @Override
    default String get() {
        return getValue();
    }

    default @Nullable String getNullIfEmpty() {
        return getValue();
    }
    default LocalizedValue orIfEmpty(LocalizedValue defaultValue) {
        return this;
    }

    byte getModificationCount();
    default Optional<LocalizationKey> getKey() {
        return Optional.empty();
    }
    default LocalizedValue map(Function<String, String> mapper) {
        return new MappedLocalizedValue(LocalizationManager.get(), this, mapper);
    }
    default LocalizedValue map(BiFunction<LocalizationManager, String, String> mapper) {
        return new MappedLocalizedValue2(LocalizationManager.get(), this, mapper);
    }
    default LocalizedValue toUpperCase() {
        return map(DefaultMapFunctions.TO_UPPER_CASE);
    }
    default LocalizedValue toLowerCase() {
        return map(DefaultMapFunctions.TO_LOWER_CASE);
    }
    default LocalizedValue capitalize() {
        return map(DefaultMapFunctions.CAPITALIZE);
    }

    @Override
    default int compareTo(LocalizedValue that) {
        return comparator().compare(this, that);
    }
}
