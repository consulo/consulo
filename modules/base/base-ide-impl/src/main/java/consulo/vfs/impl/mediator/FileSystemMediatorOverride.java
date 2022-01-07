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
package consulo.vfs.impl.mediator;

import com.intellij.jna.JnaLoader;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileSystemUtil;
import com.intellij.openapi.util.io.win32.IdeaWin32;
import consulo.logging.Logger;

import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2019-12-07
 */
public class FileSystemMediatorOverride {
  private static final Logger LOG = Logger.getInstance(FileSystemMediatorOverride.class);

  public static final String FORCE_USE_NIO2_KEY = "idea.io.use.nio2";

  public static void replaceIfNeedMediator() {
    FileSystemUtil.setMediatorLock(getMediator());
  }

  @Nullable
  private static FileSystemUtil.Mediator getMediator() {
    if (!Boolean.getBoolean(FORCE_USE_NIO2_KEY)) {
      try {
        if (SystemInfo.isWindows && IdeaWin32.isAvailable()) {
          return check(new IdeaWin32MediatorImpl());
        }
        else if ((SystemInfo.isLinux || SystemInfo.isMac || SystemInfo.isSolaris || SystemInfo.isFreeBSD) && JnaLoader.isLoaded()) {
          return check(new JnaUnixMediatorImpl());
        }
      }
      catch (Throwable t) {
        LOG.warn("Failed to load filesystem access layer: " + SystemInfo.OS_NAME + ", " + SystemInfo.JAVA_VERSION, t);
      }
    }

    return null;
  }

  private static FileSystemUtil.Mediator check(final FileSystemUtil.Mediator mediator) throws Exception {
    final String quickTestPath = SystemInfo.isWindows ? "C:\\" : "/";
    mediator.getAttributes(quickTestPath);
    return mediator;
  }
}
