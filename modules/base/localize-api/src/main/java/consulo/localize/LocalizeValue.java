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

import javax.annotation.Nonnull;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2019-04-11
 */
public interface LocalizeValue extends Supplier<String> {
  @Nonnull
  static LocalizeValue empty() {
    return SingleLocalizeValue.ourEmpty;
  }

  @Nonnull
  static LocalizeValue space() {
    return SingleLocalizeValue.ourSpace;
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
    if(text.length() == 0) {
      return of();
    }
    return new SingleLocalizeValue(text);
  }

  static LocalizeValue join(@Nonnull LocalizeValue... values) {
    if(values.length == 0) {
      return of();
    }

    return new JoinLocalizeValue(values);
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
}
