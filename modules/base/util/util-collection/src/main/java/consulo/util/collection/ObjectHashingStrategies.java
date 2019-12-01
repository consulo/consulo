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
package consulo.util.collection;

import gnu.trove.TObjectHashingStrategy;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2019-12-01
 */
public final class ObjectHashingStrategies {
  @Nonnull
  @Contract(pure = true)
  @SuppressWarnings("unchecked")
  public static <T> TObjectHashingStrategy<T> canonicalStrategy() {
    return TObjectHashingStrategy.CANONICAL;
  }

  @Nonnull
  @Contract(pure = true)
  @SuppressWarnings("unchecked")
  public static <T> TObjectHashingStrategy<T> identityStrategy() {
    return TObjectHashingStrategy.IDENTITY;
  }
}
