/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.virtualFileSystem.impl.internal.windows;

import consulo.component.util.NativeLibraryLoader;
import consulo.logging.Logger;
import consulo.platform.Platform;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Do not use this class directly.
 *
 * @author Dmitry Avdeev
 * @since 12.0
 */
public class WindowsFileSystemHelper {
  private static final Logger LOG = Logger.getInstance(WindowsFileSystemHelper.class);
  private static final boolean DEBUG_ENABLED = LOG.isDebugEnabled();

  private static final WindowsFileSystemHelper ourInstance;

  static {
    WindowsFileSystemHelper instance = null;
    if (Platform.current().os().isWindows7OrNewer() && Boolean.parseBoolean(System.getProperty("consulo.use.native.fs.for.win", "true"))) {
      try {
        NativeLibraryLoader.loadLibrary("windows-file-system-helper", System::load);
        instance = new WindowsFileSystemHelper();
        LOG.info("Native filesystem for Windows is operational");
      }
      catch (Throwable t) {
        LOG.error("Failed to load native filesystem for Windows", t);
      }
    }
    ourInstance = instance;
  }

  public static boolean isAvailable() {
    return ourInstance != null;
  }

  @Nonnull
  public static WindowsFileSystemHelper getInstance() {
    if (!isAvailable()) {
      throw new IllegalStateException("Native filesystem for Windows is not loaded");
    }
    return ourInstance;
  }

  private WindowsFileSystemHelper() {
    initIDs();
  }

  private static native void initIDs();

  @Nullable
  public FileInfo getInfo(@Nonnull String path) {
    path = path.replace('/', '\\');
    if (DEBUG_ENABLED) {
      long t = System.nanoTime();
      final FileInfo result = getInfo0(path);
      t = (System.nanoTime() - t) / 1000;
      LOG.debug("getInfo(" + path + "): " + t + " mks");
      return result;
    }
    else {
      return getInfo0(path);
    }
  }

  @Nullable
  public String resolveSymLink(@Nonnull String path) {
    path = path.replace('/', '\\');
    if (DEBUG_ENABLED) {
      long t = System.nanoTime();
      final String result = resolveSymLink0(path);
      t = (System.nanoTime() - t) / 1000;
      LOG.debug("resolveSymLink(" + path + "): " + t + " mks");
      return result;
    }
    else {
      return resolveSymLink0(path);
    }
  }

  @Nullable
  public FileInfo[] listChildren(@Nonnull String path) {
    path = path.replace('/', '\\');
    if (DEBUG_ENABLED) {
      long t = System.nanoTime();
      FileInfo[] children = listChildren0(path);
      t = (System.nanoTime() - t) / 1000;
      LOG.debug("list(" + path + "): " + children.length + " children, " + t + " mks");
      return children;
    }
    else {
      return listChildren0(path);
    }
  }

  private native FileInfo getInfo0(String path);

  private native String resolveSymLink0(String path);

  private native FileInfo[] listChildren0(String path);
}
