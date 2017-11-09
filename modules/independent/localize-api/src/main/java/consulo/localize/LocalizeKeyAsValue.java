/*
 * Copyright 2013-2017 consulo.io
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

import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 09-Nov-17
 */
public interface LocalizeKeyAsValue extends LocalizeKey, LocalizeValue {
  @NotNull
  static LocalizeKeyAsValue of(@NotNull String text) {
    return new SingleLocalizeKeyAsValue(text);
  }

  @NotNull
  @Override
  default LocalizeValue getValue(Object arg) {
    return this;
  }

  @NotNull
  @Override
  default LocalizeValue getValue(Object arg0, Object arg1) {
    return this;
  }

  @NotNull
  @Override
  default LocalizeValue getValue(Object arg0, Object arg1, Object arg2) {
    return this;
  }

  @NotNull
  @Override
  default LocalizeValue getValue(Object arg0, Object arg1, Object arg2, Object arg3) {
    return this;
  }

  @NotNull
  @Override
  default LocalizeValue getValue(Object arg0, Object arg1, Object arg2, Object arg3, Object arg4) {
    return this;
  }
}
