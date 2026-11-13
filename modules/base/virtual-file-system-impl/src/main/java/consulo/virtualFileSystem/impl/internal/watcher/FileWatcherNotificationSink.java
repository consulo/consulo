/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.virtualFileSystem.impl.internal.watcher;

import consulo.localize.LocalizeValue;
import consulo.util.lang.Pair;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.Collection;

/**
 * Where a {@link PluggableFileWatcher} reports - the paths are of the file systems the watcher is applicable to.
 */
public interface FileWatcherNotificationSink {
  void notifyManualWatchRoots(Collection<Path> roots);

  void notifyMapping(Collection<? extends Pair<Path, Path>> mapping);

  void notifyDirtyPath(Path path);

  void notifyPathCreatedOrDeleted(Path path);

  void notifyDirtyDirectory(Path path);

  void notifyDirtyPathRecursive(Path path);

  /**
   * Changes under the path were lost - or under every root of the watcher, when the path is null.
   */
  void notifyReset(@Nullable Path path);

  void notifyUserOnFailure(LocalizeValue cause);
}
