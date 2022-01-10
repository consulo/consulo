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
package consulo.extensions;

import com.intellij.util.xmlb.Converter;
import consulo.localize.LocalizeManager;
import consulo.localize.LocalizeValue;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2020-05-29
 */
public final class LocalizeValueConverter extends Converter<LocalizeValue> {
  @Override
  public LocalizeValue fromString(@Nonnull String value) {
    return LocalizeManager.get().fromStringKey(value);
  }

  @Nonnull
  @Override
  public String toString(@Nonnull LocalizeValue localizeValue) {
    throw new UnsupportedOperationException("We can't write localize value");
  }
}
