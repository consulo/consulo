/*
 * Copyright 2013-2023 consulo.io
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
package consulo.platform;

import javax.annotation.Nonnull;
import java.nio.file.Path;

/**
 * @author VISTALL
 * @since 25/04/2023
 */
public interface PlatformUser extends Platform.User {
  @Override
  boolean superUser();

  @Override
  @Nonnull
  String name();

  @Override
  @Nonnull
  Path homePath();

  @Override
  default boolean darkTheme() {
    return false;
  }
}
