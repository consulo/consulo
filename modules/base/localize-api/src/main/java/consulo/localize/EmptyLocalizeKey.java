/*
 * Copyright 2013-2020 consulo.io
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

/**
 * @author VISTALL
 * @since 2020-05-20
 */
final class EmptyLocalizeKey implements LocalizeKey {
  static final EmptyLocalizeKey INSTANCE = new EmptyLocalizeKey();

  @Nonnull
  @Override
  public String getLocalizeId() {
    return "";
  }

  @Nonnull
  @Override
  public String getKey() {
    return "";
  }

  @Nonnull
  @Override
  public LocalizeValue getValue() {
    return LocalizeValue.empty();
  }

  @Nonnull
  @Override
  public LocalizeValue getValue(Object arg) {
    return LocalizeValue.empty();
  }

  @Nonnull
  @Override
  public LocalizeValue getValue(Object arg0, Object arg1) {
    return LocalizeValue.empty();
  }

  @Nonnull
  @Override
  public LocalizeValue getValue(Object arg0, Object arg1, Object arg2) {
    return LocalizeValue.empty();
  }

  @Nonnull
  @Override
  public LocalizeValue getValue(Object arg0, Object arg1, Object arg2, Object arg3) {
    return LocalizeValue.empty();
  }

  @Nonnull
  @Override
  public LocalizeValue getValue(Object arg0, Object arg1, Object arg2, Object arg3, Object arg4) {
    return LocalizeValue.empty();
  }
}
