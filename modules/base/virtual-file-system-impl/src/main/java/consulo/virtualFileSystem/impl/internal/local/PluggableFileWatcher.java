/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.virtualFileSystem.impl.internal.local;

import consulo.virtualFileSystem.ManagingFS;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.TestOnly;

import java.io.IOException;
import java.util.List;

/**
 * @author dslomov
 */
public abstract class PluggableFileWatcher {
  public abstract void initialize(@Nonnull ManagingFS managingFS, @Nonnull FileWatcherNotificationSink notificationSink);

  public abstract void dispose();

  public abstract boolean isOperational();

  public abstract boolean isSettingRoots();

  /**
   * The inputs to this method must be absolute and free of symbolic links.
   */
  public abstract void setWatchRoots(@Nonnull List<String> recursive, @Nonnull List<String> flat);

  public void resetChangedPaths() {
  }

  @TestOnly
  public abstract void startup() throws IOException;

  @TestOnly
  public abstract void shutdown() throws InterruptedException;
}