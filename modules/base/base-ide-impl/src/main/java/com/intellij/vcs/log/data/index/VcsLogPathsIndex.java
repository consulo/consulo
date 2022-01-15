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
package com.intellij.vcs.log.data.index;

import com.intellij.openapi.util.Couple;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Consumer;
import com.intellij.util.PathUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.indexing.DataIndexer;
import com.intellij.util.indexing.StorageException;
import com.intellij.util.io.*;
import com.intellij.vcs.log.VcsFullCommitDetails;
import com.intellij.vcs.log.impl.FatalErrorHandler;
import com.intellij.vcs.log.impl.VcsChangesLazilyParsedDetails;
import com.intellij.vcs.log.util.PersistentUtil;
import consulo.disposer.Disposable;
import consulo.logging.Logger;
import consulo.util.collection.primitive.ints.IntSet;
import consulo.util.collection.primitive.ints.IntSets;

import javax.annotation.Nonnull;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.intellij.util.containers.ContainerUtil.newTroveSet;
import static com.intellij.vcs.log.data.index.VcsLogPersistentIndex.getVersion;

public class VcsLogPathsIndex extends VcsLogFullDetailsIndex<Integer> {
  private static final Logger LOG = Logger.getInstance(VcsLogPathsIndex.class);
  public static final String PATHS = "paths";
  public static final String INDEX_PATHS_IDS = "paths-ids";

  @Nonnull
  private final PathsIndexer myPathsIndexer;

  public VcsLogPathsIndex(@Nonnull String logId,
                          @Nonnull Set<VirtualFile> roots,
                          @Nonnull FatalErrorHandler fatalErrorHandler,
                          @Nonnull Disposable disposableParent) throws IOException {
    super(logId, PATHS, getVersion(), new PathsIndexer(createPathsEnumerator(logId), roots),
          new NullableIntKeyDescriptor(), fatalErrorHandler, disposableParent);

    myPathsIndexer = (PathsIndexer)myIndexer;
    myPathsIndexer.setFatalErrorConsumer(e -> fatalErrorHandler.consume(this, e));
  }

  @Nonnull
  private static PersistentEnumeratorBase<String> createPathsEnumerator(@Nonnull String logId) throws IOException {
    File storageFile = PersistentUtil.getStorageFile(INDEX, INDEX_PATHS_IDS, logId, getVersion(), true);
    return new PersistentBTreeEnumerator<>(storageFile, SystemInfo.isFileSystemCaseSensitive ? EnumeratorStringDescriptor.INSTANCE
                                                                                             : new ToLowerCaseStringDescriptor(),
                                           Page.PAGE_SIZE, null, getVersion());
  }

  @Override
  public void flush() throws StorageException {
    super.flush();
    myPathsIndexer.getPathsEnumerator().force();
  }

  public IntSet getCommitsForPaths(@Nonnull Collection<FilePath> paths) throws IOException, StorageException {
    Set<Integer> allPathIds = ContainerUtil.newHashSet();
    for (FilePath path : paths) {
      allPathIds.add(myPathsIndexer.myPathsEnumerator.enumerate(path.getPath()));
    }

    IntSet result = IntSets.newHashSet();
    Set<Integer> renames = allPathIds;
    while (!renames.isEmpty()) {
      renames = addCommitsAndGetRenames(renames, allPathIds, result);
      allPathIds.addAll(renames);
    }

    return result;
  }

  @Nonnull
  public Set<Integer> addCommitsAndGetRenames(@Nonnull Set<Integer> newPathIds,
                                              @Nonnull Set<Integer> allPathIds,
                                              @Nonnull IntSet commits)
          throws StorageException {
    Set<Integer> renames = ContainerUtil.newHashSet();
    for (Integer key : newPathIds) {
      iterateCommitIdsAndValues(key, (value, commit) -> {
        commits.add(commit);
        if (value != null && !allPathIds.contains(value)) {
          renames.add(value);
        }
      });
    }
    return renames;
  }

  @Override
  public void dispose() {
    super.dispose();
    try {
      myPathsIndexer.getPathsEnumerator().close();
    }
    catch (IOException e) {
      LOG.warn(e);
    }
  }

  private static class PathsIndexer implements DataIndexer<Integer, Integer, VcsFullCommitDetails> {
    @Nonnull
    private final PersistentEnumeratorBase<String> myPathsEnumerator;
    @Nonnull
    private final Set<String> myRoots;
    @Nonnull
    private Consumer<Exception> myFatalErrorConsumer = LOG::error;

    private PathsIndexer(@Nonnull PersistentEnumeratorBase<String> enumerator, @Nonnull Set<VirtualFile> roots) {
      myPathsEnumerator = enumerator;
      myRoots = newTroveSet(FileUtil.PATH_HASHING_STRATEGY);
      for (VirtualFile root : roots) {
        myRoots.add(root.getPath());
      }
    }

    public void setFatalErrorConsumer(@Nonnull Consumer<Exception> fatalErrorConsumer) {
      myFatalErrorConsumer = fatalErrorConsumer;
    }

    @Nonnull
    @Override
    public Map<Integer, Integer> map(@Nonnull VcsFullCommitDetails inputData) {
      Map<Integer, Integer> result = new HashMap<>();


      Collection<Couple<String>> moves;
      Collection<String> changedPaths;
      if (inputData instanceof VcsChangesLazilyParsedDetails) {
        changedPaths = ((VcsChangesLazilyParsedDetails)inputData).getModifiedPaths();
        moves = ((VcsChangesLazilyParsedDetails)inputData).getRenamedPaths();
      }
      else {
        moves = ContainerUtil.newHashSet();
        changedPaths = ContainerUtil.newHashSet();
        for (Change change : inputData.getChanges()) {
          if (change.getAfterRevision() != null) changedPaths.add(change.getAfterRevision().getFile().getPath());
          if (change.getBeforeRevision() != null) changedPaths.add(change.getBeforeRevision().getFile().getPath());
          if (change.getType().equals(Change.Type.MOVED)) {
            moves.add(Couple.of(change.getBeforeRevision().getFile().getPath(), change.getAfterRevision().getFile().getPath()));
          }
        }
      }

      getParentPaths(changedPaths).forEach(changedPath -> {
        try {
          result.put(myPathsEnumerator.enumerate(changedPath), null);
        }
        catch (IOException e) {
          myFatalErrorConsumer.consume(e);
        }
      });
      moves.forEach(renamedPaths -> {
        try {
          int beforeId = myPathsEnumerator.enumerate(renamedPaths.first);
          int afterId = myPathsEnumerator.enumerate(renamedPaths.second);

          result.put(beforeId, afterId);
          result.put(afterId, beforeId);
        }
        catch (IOException e) {
          myFatalErrorConsumer.consume(e);
        }
      });

      return result;
    }

    @Nonnull
    private Collection<String> getParentPaths(@Nonnull Collection<String> paths) {
      Set<String> result = ContainerUtil.newHashSet();
      for (String path : paths) {
        while (!path.isEmpty() && !result.contains(path)) {
          result.add(path);
          if (myRoots.contains(path)) break;

          path = PathUtil.getParentPath(path);
        }
      }
      return result;
    }

    @Nonnull
    public PersistentEnumeratorBase<String> getPathsEnumerator() {
      return myPathsEnumerator;
    }
  }

  private static class NullableIntKeyDescriptor implements DataExternalizer<Integer> {
    @Override
    public void save(@Nonnull DataOutput out, Integer value) throws IOException {
      if (value == null) {
        out.writeBoolean(false);
      }
      else {
        out.writeBoolean(true);
        out.writeInt(value);
      }
    }

    @Override
    public Integer read(@Nonnull DataInput in) throws IOException {
      if (in.readBoolean()) {
        return in.readInt();
      }
      return null;
    }
  }

  private static class ToLowerCaseStringDescriptor implements KeyDescriptor<String> {
    @Override
    public int hashCode(String value) {
      return CaseInsensitiveStringHashingStrategy.INSTANCE.hashCode(value);
    }

    @Override
    public boolean equals(String val1, String val2) {
      return CaseInsensitiveStringHashingStrategy.INSTANCE.equals(val1, val2);
    }

    @Override
    public void save(@Nonnull DataOutput out, String value) throws IOException {
      IOUtil.writeUTF(out, value.toLowerCase());
    }

    @Override
    public String read(@Nonnull DataInput in) throws IOException {
      return IOUtil.readUTF(in);
    }
  }
}
