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
package consulo.test.light.impl;

import consulo.platform.impl.PlatformBase;
import jakarta.annotation.Nonnull;

import java.io.File;
import java.net.URL;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2023-11-06
 */
public class LightPlatform extends PlatformBase {
  public LightPlatform() {
    super(LOCAL, LOCAL, Map.of("os.name", "light", "os.version", "1", "user.home", "", "user.name", "light"));
  }

  public LightPlatform(@Nonnull String id, @Nonnull String name, @Nonnull Map<String, String> jvmProperties) {
    super(id, name, jvmProperties);
  }

  @Override
  public void openInBrowser(@Nonnull URL url) {

  }

  @Override
  public void openFileInFileManager(@Nonnull File file) {

  }

  @Override
  public void openDirectoryInFileManager(@Nonnull File file) {

  }
}
