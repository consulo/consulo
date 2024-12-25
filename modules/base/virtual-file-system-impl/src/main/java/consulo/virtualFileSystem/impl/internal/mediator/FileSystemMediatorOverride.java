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
package consulo.virtualFileSystem.impl.internal.mediator;

import consulo.logging.Logger;
import consulo.platform.CpuArchitecture;
import consulo.platform.Platform;
import consulo.platform.PlatformOperatingSystem;
import consulo.util.jna.JnaLoader;
import consulo.virtualFileSystem.internal.FileSystemMediator;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2019-12-07
 */
public class FileSystemMediatorOverride {
  private static final Logger LOG = Logger.getInstance(FileSystemMediatorOverride.class);

  public static final String FORCE_USE_NIO2_KEY = "consulo.io.use.nio2";

  public static void replaceIfNeedMediator() {
    FileSystemUtil.setMediatorLock(getMediator());
  }

  @Nullable
  private static FileSystemMediator getMediator() {
    if (!Boolean.getBoolean(FORCE_USE_NIO2_KEY)) {
      Platform platform = Platform.current();
      PlatformOperatingSystem os = platform.os();
      try {
        if (!os.isWindows() && platform.jvm().arch() == CpuArchitecture.X86_64 && JnaLoader.isLoaded()) {
          return check(os, new JnaUnixMediatorImpl(os));
        }
      }
      catch (Throwable t) {
        LOG.warn("Failed to load filesystem access layer: " + os.name() + ", " + platform.jvm().name(), t);
      }
    }

    return null;
  }

  private static FileSystemMediator check(PlatformOperatingSystem os, final FileSystemMediator mediator) throws Exception {
    final String quickTestPath = os.isWindows() ? "C:\\" : "/";
    mediator.getAttributes(quickTestPath);
    return mediator;
  }
}
