/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.versionControlSystem.log.impl.internal.data;

import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import java.util.Collection;

/**
 * Prepares the initial {@link DataPack} and handles subsequent VCS Log refreshes.
 */
public interface VcsLogRefresher {

  /**
   * Synchronously loads some recent commits from the VCS, builds the DataPack and queues to refresh everything. <br/>
   * This is called on log initialization and on the full refresh.
   */
  @Nonnull
  DataPack readFirstBlock();

  /**
   * Refreshes the log and builds the actual data pack.
   * Triggered by some event from the VCS which indicates that the log could change (e.g. new commits arrived).
   */
  void refresh(@Nonnull Collection<VirtualFile> rootsToRefresh);
}