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
package consulo.ide.impl.idea.vcs.log.impl;

import consulo.logging.Logger;
import consulo.util.lang.Couple;
import consulo.application.util.function.ThrowableComputable;
import consulo.ide.impl.idea.openapi.vcs.LocalFilePath;
import consulo.vcs.change.Change;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.ide.impl.idea.vcs.log.Hash;
import consulo.ide.impl.idea.vcs.log.VcsFullCommitDetails;
import consulo.ide.impl.idea.vcs.log.VcsUser;
import javax.annotation.Nonnull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Allows to postpone changes parsing, which might take long for a large amount of commits,
 * because {@link Change} holds {@link LocalFilePath} which makes costly refreshes and type detections.
 */
public class VcsChangesLazilyParsedDetails extends VcsCommitMetadataImpl implements VcsFullCommitDetails {

  private static final Logger LOG = Logger.getInstance(VcsChangesLazilyParsedDetails.class);

  @Nonnull
  protected final ThrowableComputable<Collection<Change>, ? extends Exception> myChangesGetter;

  public VcsChangesLazilyParsedDetails(@Nonnull Hash hash, @Nonnull List<Hash> parents, long commitTime, @Nonnull VirtualFile root,
                                       @Nonnull String subject, @Nonnull VcsUser author, @Nonnull String message,
                                       @Nonnull VcsUser committer, long authorTime,
                                       @Nonnull ThrowableComputable<Collection<Change>, ? extends Exception> changesGetter) {
    super(hash, parents, commitTime, root, subject, author, message, committer, authorTime);
    myChangesGetter = changesGetter;
  }

  @Nonnull
  @Override
  public Collection<Change> getChanges() {
    try {
      return myChangesGetter.compute();
    }
    catch (Exception e) {
      LOG.error("Error happened when parsing changes", e);
      return Collections.emptyList();
    }
  }

  @Nonnull
  public Collection<String> getModifiedPaths() {
    Set<String> changedPaths = ContainerUtil.newHashSet();
    for (Change change : getChanges()) {
      if (change.getAfterRevision() != null) changedPaths.add(change.getAfterRevision().getFile().getPath());
      if (change.getBeforeRevision() != null) changedPaths.add(change.getBeforeRevision().getFile().getPath());
    }
    return changedPaths;
  }

  @Nonnull
  public Collection<Couple<String>> getRenamedPaths() {
    Set<Couple<String>> renames = ContainerUtil.newHashSet();
    for (Change change : getChanges()) {
      if (change.getType().equals(Change.Type.MOVED)) {
        if (change.getAfterRevision() != null && change.getBeforeRevision() != null) {
          renames.add(Couple.of(change.getBeforeRevision().getFile().getPath(), change.getAfterRevision().getFile().getPath()));
        }
      }
    }
    return renames;
  }
}
