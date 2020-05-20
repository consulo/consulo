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

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 09-Nov-17
 */
public interface LocalizeKey {
  @Nonnull
  static LocalizeKey empty() {
    return EmptyLocalizeKey.INSTANCE;
  }

  @Nonnull
  static LocalizeKey of(@Nonnull String localizeId, @Nonnull String key) {
    return new DefaultLocalizeKey(localizeId, key);
  }

  @Nonnull
  String getLocalizeId();

  @Nonnull
  String getKey();

  @Nonnull
  LocalizeValue getValue();

  @Nonnull
  LocalizeValue getValue(Object arg);

  @Nonnull
  LocalizeValue getValue(Object arg0, Object arg1);

  @Nonnull
  LocalizeValue getValue(Object arg0, Object arg1, Object arg2);

  @Nonnull
  LocalizeValue getValue(Object arg0, Object arg1, Object arg2, Object arg3);

  @Nonnull
  LocalizeValue getValue(Object arg0, Object arg1, Object arg2, Object arg3, Object arg4);
}
