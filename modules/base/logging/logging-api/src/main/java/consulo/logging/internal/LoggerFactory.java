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
package consulo.logging.internal;

import consulo.logging.Logger;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2019-08-10
 */
public interface LoggerFactory {
  static int HIGHT_PRIORITY = 100;
  static int DEFAULT_PRIORITY = 1;
  static int DISABLE_PRIORITY = -100;

  @Nonnull
  Logger getLoggerInstance(@Nonnull String category);

  @Nonnull
  default Logger getLoggerInstance(@Nonnull Class<?> clazz) {
    return getLoggerInstance(clazz.getName());
  }

  default int getPriority() {
    return DEFAULT_PRIORITY;
  }

  default void shutdown() {
  }
}
