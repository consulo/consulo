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
package consulo.localize;

import consulo.annotation.DeprecationInfo;
import consulo.localize.internal.DefaultMapFunctions;
import consulo.localize.internal.JoinLocalizeValue;
import consulo.localize.internal.MapLocalizeValue;
import consulo.localize.internal.SingleLocalizeValue;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2019-04-11
 */
public interface LocalizeValue extends Supplier<String>, Comparable<LocalizeValue> {
    @Nonnull
    static LocalizeValue empty() {
        return SingleLocalizeValue.ourEmpty;
    }

    @Nonnull
    static LocalizeValue space() {
        return SingleLocalizeValue.ourSpace;
    }

    @Nonnull
    static LocalizeValue colon() {
        return SingleLocalizeValue.ourColon;
    }

    @Nonnull
    static LocalizeValue dot() {
        return SingleLocalizeValue.ourDot;
    }

    @Nonnull
    static LocalizeValue questionMark() {
        return SingleLocalizeValue.ourQuestionMark;
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
        return text.isEmpty() ? of() : new SingleLocalizeValue(text);
    }

    @Nonnull
    static LocalizeValue ofNullable(@Nullable String text) {
        return text == null ? of() : of(text);
    }

    @Nonnull
    static LocalizeValue join(@Nonnull LocalizeValue... values) {
        return values.length == 0 ? of() : new JoinLocalizeValue(values);
    }

    @Override
    @Nonnull
    default String get() {
        return getValue();
    }

    @Nonnull
    String getValue();

    long getModificationCount();

    @Nonnull
    default Optional<LocalizeKey> getKey() {
        return Optional.empty();
    }

    @Nonnull
    default LocalizeValue map(@Nonnull BiFunction<LocalizeManager, String, String> mapper) {
        return new MapLocalizeValue(this, mapper);
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

    @Deprecated
    @DeprecationInfo("Use proper English word #capitalize()")
    @Nonnull
    default LocalizeValue captilize() {
        return capitalize();
    }

    int compareIgnoreCase(@Nonnull LocalizeValue other);
}
