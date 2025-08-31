/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.versionControlSystem.history;

import consulo.versionControlSystem.FilePath;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.Serializable;
import java.util.List;

/**
 * @author irengrig
 * @since 2011-03-15
 */
public interface VcsCacheableHistorySessionFactory<Cacheable extends Serializable, T extends VcsAbstractHistorySession> {
  /**
   * define if path should be changed for session construction (file can be moved)
   */
  @Nullable
  FilePath getUsedFilePath(T session);

  @Nullable
  Cacheable getAddinionallyCachedData(T session);

  T createFromCachedData(@Nullable Cacheable cacheable, @Nonnull List<VcsFileRevision> revisions, @Nonnull FilePath filePath, @Nullable VcsRevisionNumber currentRevision);
}
