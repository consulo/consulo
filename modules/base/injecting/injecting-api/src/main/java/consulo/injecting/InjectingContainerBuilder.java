/*
 * Copyright 2013-2018 consulo.io
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
package consulo.injecting;

import consulo.injecting.key.InjectingKey;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2018-08-23
 */
public interface InjectingContainerBuilder {
  @Nonnull
  default <T> InjectingPoint<T> bind(@Nonnull Class<T> key) {
    return bind(InjectingKey.of(key));
  }

  @Nonnull
  <T> InjectingPoint<T> bind(@Nonnull InjectingKey<T> key);

  @Nonnull
  InjectingContainer build();
}
