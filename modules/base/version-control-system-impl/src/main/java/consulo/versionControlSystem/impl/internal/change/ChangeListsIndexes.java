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
package consulo.versionControlSystem.impl.internal.change;

import consulo.logging.Logger;
import consulo.util.lang.BeforeAfter;
import consulo.util.lang.Comparing;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.change.BaseRevision;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.ContentRevision;
import consulo.versionControlSystem.history.VcsRevisionNumber;
import consulo.virtualFileSystem.status.FileStatus;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;

public class ChangeListsIndexes {
  private static final Logger LOG = Logger.getInstance(ChangeListsIndexes.class);

  private Map<FilePath, Data> myMap;
  private Set<Change> myChanges;

  public ChangeListsIndexes() {
    myMap = new HashMap<>();
    myChanges = new HashSet<>();
  }

  public void copyFrom(@Nonnull ChangeListsIndexes idx) {
    myMap = new HashMap<>(idx.myMap);
    myChanges = new HashSet<>(idx.myChanges);
  }

  public void clear() {
    myMap = new HashMap<>();
    myChanges = new HashSet<>();
  }


  private void add(@Nonnull FilePath file,
                   @Nonnull Change change,
                   @Nonnull FileStatus status,
                   @Nullable AbstractVcs key,
                   @Nonnull VcsRevisionNumber number) {
    myMap.put(file, new Data(status, change, key, number));
    if (LOG.isDebugEnabled()) {
      LOG.debug("Set status " + status + " for " + file);
    }
  }

  private void remove(@Nonnull FilePath file) {
    myMap.remove(file);
    if (LOG.isDebugEnabled()) {
      LOG.debug("Clear status for " + file);
    }
  }

  @Nullable
  public Change getChange(@Nonnull FilePath file) {
    Data data = myMap.get(file);
    return data != null ? data.change : null;
  }

  @Nullable
  public FileStatus getStatus(@Nonnull FilePath file) {
    Data data = myMap.get(file);
    return data != null ? data.status : null;
  }

  public void changeAdded(@Nonnull Change change, @Nullable AbstractVcs key) {
    myChanges.add(change);

    ContentRevision afterRevision = change.getAfterRevision();
    ContentRevision beforeRevision = change.getBeforeRevision();

    if (beforeRevision != null && afterRevision != null) {
      add(afterRevision.getFile(), change, change.getFileStatus(), key, beforeRevision.getRevisionNumber());

      if (!Objects.equals(beforeRevision.getFile(), afterRevision.getFile())) {
        add(beforeRevision.getFile(), change, FileStatus.DELETED, key, beforeRevision.getRevisionNumber());
      }
    }
    else if (afterRevision != null) {
      add(afterRevision.getFile(), change, change.getFileStatus(), key, VcsRevisionNumber.NULL);
    }
    else if (beforeRevision != null) {
      add(beforeRevision.getFile(), change, change.getFileStatus(), key, beforeRevision.getRevisionNumber());
    }
  }

  public void changeRemoved(@Nonnull Change change) {
    myChanges.remove(change);

    ContentRevision afterRevision = change.getAfterRevision();
    ContentRevision beforeRevision = change.getBeforeRevision();

    if (afterRevision != null) {
      remove(afterRevision.getFile());
    }
    if (beforeRevision != null) {
      remove(beforeRevision.getFile());
    }
  }

  @Nonnull
  public Set<Change> getChanges() {
    return myChanges;
  }

  @Nullable
  public AbstractVcs getVcsFor(@Nonnull Change change) {
    AbstractVcs vcs = getVcsForRevision(change.getAfterRevision());
    if (vcs != null) return vcs;
    return getVcsForRevision(change.getBeforeRevision());
  }

  @Nullable
  private AbstractVcs getVcsForRevision(@Nullable ContentRevision revision) {
    if (revision != null) {
      Data data = myMap.get(revision.getFile());
      return data != null ? data.vcs : null;
    }
    return null;
  }

  /**
   * this method is called after each local changes refresh and collects all:
   * - paths that are new in local changes
   * - paths that are no more changed locally
   * - paths that were and are changed, but base revision has changed (ex. external update)
   * (for RemoteRevisionsCache and annotation listener)
   */
  public void getDelta(ChangeListsIndexes newIndexes,
                       Set<? super BaseRevision> toRemove,
                       Set<? super BaseRevision> toAdd,
                       Set<? super BeforeAfter<BaseRevision>> toModify) {
    Map<FilePath, Data> oldMap = myMap;
    Map<FilePath, Data> newMap = newIndexes.myMap;

    for (Map.Entry<FilePath, Data> entry : oldMap.entrySet()) {
      FilePath s = entry.getKey();
      Data oldData = entry.getValue();
      Data newData = newMap.get(s);

      if (newData != null) {
        if (!oldData.sameRevisions(newData)) {
          toModify.add(new BeforeAfter<>(createBaseRevision(s, oldData), createBaseRevision(s, newData)));
        }
      }
      else {
        toRemove.add(createBaseRevision(s, oldData));
      }
    }

    for (Map.Entry<FilePath, Data> entry : newMap.entrySet()) {
      FilePath s = entry.getKey();
      Data newData = entry.getValue();

      if (!oldMap.containsKey(s)) {
        toAdd.add(createBaseRevision(s, newData));
      }
    }
  }

  private static BaseRevision createBaseRevision(@Nonnull FilePath path, @Nonnull Data data) {
    return new BaseRevision(data.vcs, data.revision, path);
  }

  @Nonnull
  public Set<FilePath> getAffectedPaths() {
    return myMap.keySet();
  }

  private static class Data {
    @Nonnull
    public final FileStatus status;
    @Nonnull
    public final Change change;
    @Nullable
    public final AbstractVcs vcs;
    @Nonnull
    public final VcsRevisionNumber revision;

    Data(@Nonnull FileStatus status,
         @Nonnull Change change,
         @Nullable AbstractVcs vcs,
         @Nonnull VcsRevisionNumber revision) {
      this.status = status;
      this.change = change;
      this.vcs = vcs;
      this.revision = revision;
    }

    public boolean sameRevisions(@Nonnull Data data) {
      return Comparing.equal(vcs, data.vcs) && Comparing.equal(revision, data.revision);
    }
  }
}
