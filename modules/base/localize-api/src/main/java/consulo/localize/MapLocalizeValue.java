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
import java.util.function.BiFunction;

/**
 * @author VISTALL
 * @since 2020-07-30
 */
final class MapLocalizeValue extends BaseLocalizeValue {
  private final LocalizeValue myDelegate;
  private final BiFunction<LocalizeManager, String, String> myMapper;

  MapLocalizeValue(LocalizeValue delegate, BiFunction<LocalizeManager, String, String> mapper) {
    super(ourEmptyArgs);
    myDelegate = delegate;
    myMapper = mapper;
  }

  @Nonnull
  @Override
  protected String getUnformattedText(@Nonnull LocalizeManager localizeManager) {
    String value = myDelegate.getValue();
    return myMapper.apply(localizeManager, value);
  }
}
