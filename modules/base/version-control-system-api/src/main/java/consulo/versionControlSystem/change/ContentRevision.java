/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package consulo.versionControlSystem.change;

import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.VcsException;
import consulo.versionControlSystem.history.VcsRevisionNumber;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author max
 */
public interface ContentRevision {
  /**
   * Content of the revision. Implementers are encouraged to lazy implement this especially when it requires connection to the
   * version control server or something.
   * Might return null in case if file path denotes a directory or content is impossible to retreive.
   *
   * @return content of the revision
   * @throws VcsException in case when content retrieval fails
   */
  @Nullable
  String getContent() throws VcsException;

  /**
   * @return file path of the revision
   */
  @Nonnull
  FilePath getFile();

  /**
   * Revision ID. Content revisions with same file path and revision number are considered to be equal and must have same content unless
   * {@link VcsRevisionNumber#NULL} is returned. Use {@link VcsRevisionNumber#NULL} when revision number is not applicable like for
   * the currently uncommited revision.
   * @return revision ID in terms of version control
   */
  @Nonnull
  VcsRevisionNumber getRevisionNumber();
}
