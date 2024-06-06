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

import consulo.application.util.SystemInfo;
import consulo.logging.Logger;
import consulo.platform.Platform;
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
      try {
        if ((Platform.current().os().isLinux() || Platform.current().os().isMac() || SystemInfo.isSolaris || SystemInfo.isFreeBSD) && JnaLoader.isLoaded()) {
          return check(new JnaUnixMediatorImpl());
        }
      }
      catch (Throwable t) {
        LOG.warn("Failed to load filesystem access layer: " + Platform.current().os().name() + ", " + SystemInfo.JAVA_VERSION, t);
      }
    }

    return null;
  }

  private static FileSystemMediator check(final FileSystemMediator mediator) throws Exception {
    final String quickTestPath = Platform.current().os().isWindows() ? "C:\\" : "/";
    mediator.getAttributes(quickTestPath);
    return mediator;
  }
}
