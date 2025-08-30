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
package consulo.versionControlSystem.log.impl.internal.data.index;

import consulo.disposer.Disposable;
import consulo.index.io.*;
import consulo.index.io.data.DataExternalizer;
import consulo.index.io.data.IOUtil;
import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.util.collection.Sets;
import consulo.util.collection.primitive.ints.IntSet;
import consulo.util.collection.primitive.ints.IntSets;
import consulo.util.io.FileUtil;
import consulo.util.io.PathUtil;
import consulo.util.lang.Couple;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.log.VcsFullCommitDetails;
import consulo.versionControlSystem.log.base.VcsChangesLazilyParsedDetails;
import consulo.versionControlSystem.log.impl.internal.FatalErrorHandler;
import consulo.versionControlSystem.log.impl.internal.util.PersistentUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

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
    super(logId, PATHS, VcsLogPersistentIndex.getVersion(), new PathsIndexer(createPathsEnumerator(logId), roots),
          new NullableIntKeyDescriptor(), fatalErrorHandler, disposableParent);

    myPathsIndexer = (PathsIndexer)myIndexer;
    myPathsIndexer.setFatalErrorConsumer(e -> fatalErrorHandler.consume(this, e));
  }

  @Nonnull
  private static PersistentEnumeratorBase<String> createPathsEnumerator(@Nonnull String logId) throws IOException {
    File storageFile = PersistentUtil.getStorageFile(INDEX, INDEX_PATHS_IDS, logId, VcsLogPersistentIndex.getVersion(), true);
    return new PersistentBTreeEnumerator<>(
      storageFile,
      Platform.current().fs().isCaseSensitive() ? EnumeratorStringDescriptor.INSTANCE : new ToLowerCaseStringDescriptor(),
      Page.PAGE_SIZE,
      null,
      VcsLogPersistentIndex.getVersion()
    );
  }

  @Override
  public void flush() throws StorageException {
    super.flush();
    myPathsIndexer.getPathsEnumerator().force();
  }

  public IntSet getCommitsForPaths(@Nonnull Collection<FilePath> paths) throws IOException, StorageException {
    Set<Integer> allPathIds = new HashSet<>();
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
  public Set<Integer> addCommitsAndGetRenames(
    @Nonnull Set<Integer> newPathIds,
    @Nonnull Set<Integer> allPathIds,
    @Nonnull IntSet commits
  ) throws StorageException {
    Set<Integer> renames = new HashSet<>();
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
      myRoots = Sets.newHashSet(FileUtil.PATH_HASHING_STRATEGY);
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
      if (inputData instanceof VcsChangesLazilyParsedDetails vcsChangesLazilyParsedDetails) {
        changedPaths = vcsChangesLazilyParsedDetails.getModifiedPaths();
        moves = vcsChangesLazilyParsedDetails.getRenamedPaths();
      }
      else {
        moves = new HashSet<>();
        changedPaths = new HashSet<>();
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
          myFatalErrorConsumer.accept(e);
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
          myFatalErrorConsumer.accept(e);
        }
      });

      return result;
    }

    @Nonnull
    private Collection<String> getParentPaths(@Nonnull Collection<String> paths) {
      Set<String> result = new HashSet<>();
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
