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
package consulo.versionControlSystem.log.base;

import consulo.versionControlSystem.log.Hash;
import consulo.versionControlSystem.log.VcsCommitMetadata;
import consulo.versionControlSystem.log.VcsUser;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import java.util.List;

public class VcsCommitMetadataImpl extends VcsShortCommitDetailsImpl implements VcsCommitMetadata {

  @Nonnull
  private final String myFullMessage;

  public VcsCommitMetadataImpl(@Nonnull Hash hash, @Nonnull List<Hash> parents, long commitTime, @Nonnull VirtualFile root,
                               @Nonnull String subject, @Nonnull VcsUser author, @Nonnull String message,
                               @Nonnull VcsUser committer, long authorTime) {
    super(hash, parents, commitTime, root, subject, author, committer, authorTime);
    myFullMessage = message.equals(getSubject()) ? getSubject() : message;
  }

  @Override
  @Nonnull
  public String getFullMessage() {
    return myFullMessage;
  }
}
