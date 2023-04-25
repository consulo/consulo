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
package consulo.platform.impl;

import consulo.platform.Platform;
import consulo.platform.PlatformFileSystem;
import consulo.platform.PlatformOperatingSystem;

import java.util.Map;

/**
 * @author VISTALL
 * @since 25/04/2023
 */
public class PlatformFileSystemImpl implements PlatformFileSystem {
  private boolean isFileSystemCaseSensitive;
  private boolean areSymLinksSupported;

  public PlatformFileSystemImpl(Platform platform, Map<String, String> jvmProperties) {
    PlatformOperatingSystem os = platform.os();
    isFileSystemCaseSensitive = os.isUnix() && !os.isMac() ||
      "true".equalsIgnoreCase(jvmProperties.get("idea.case.sensitive.fs")) ||
      "true".equalsIgnoreCase(jvmProperties.get("consulo.case.sensitive.fs"));

    areSymLinksSupported = os.isUnix() || os.isWindows() && os.asWindows().isWindows7OrNewer();
  }

  @Override
  public boolean isCaseSensitive() {
    return isFileSystemCaseSensitive;
  }

  @Override
  public boolean areSymLinksSupported() {
    return areSymLinksSupported;
  }
}
