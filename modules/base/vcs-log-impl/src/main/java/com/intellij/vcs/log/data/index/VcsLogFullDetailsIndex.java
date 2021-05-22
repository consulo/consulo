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

import com.intellij.openapi.progress.ProgressManager;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import com.intellij.openapi.util.Pair;
import com.intellij.util.Consumer;
import com.intellij.util.indexing.*;
import com.intellij.util.indexing.impl.IndexStorage;
import com.intellij.util.indexing.impl.MapIndexStorage;
import com.intellij.util.indexing.impl.MapReduceIndex;
import com.intellij.util.indexing.impl.forward.ForwardIndex;
import com.intellij.util.indexing.impl.forward.ForwardIndexAccessor;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorIntegerDescriptor;
import com.intellij.util.io.KeyDescriptor;
import com.intellij.vcs.log.VcsFullCommitDetails;
import com.intellij.vcs.log.impl.FatalErrorHandler;
import com.intellij.vcs.log.util.PersistentUtil;
import consulo.util.collection.primitive.ints.IntSet;
import consulo.util.collection.primitive.ints.IntSets;
import gnu.trove.TIntHashSet;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import java.util.function.ObjIntConsumer;

import static com.intellij.vcs.log.data.index.VcsLogPersistentIndex.getVersion;

public class VcsLogFullDetailsIndex<T> implements Disposable {
  protected static final String INDEX = "index";
  @Nonnull
  protected final MyMapReduceIndex myMapReduceIndex;
  @Nonnull
  private final ID<Integer, T> myID;
  @Nonnull
  private final String myLogId;
  @Nonnull
  private final String myName;
  @Nonnull
  protected final DataIndexer<Integer, T, VcsFullCommitDetails> myIndexer;
  @Nonnull
  private final FatalErrorHandler myFatalErrorHandler;

  public VcsLogFullDetailsIndex(@Nonnull String logId,
                                @Nonnull String name,
                                final int version,
                                @Nonnull DataIndexer<Integer, T, VcsFullCommitDetails> indexer,
                                @Nonnull DataExternalizer<T> externalizer,
                                @Nonnull FatalErrorHandler fatalErrorHandler,
                                @Nonnull Disposable disposableParent) throws IOException {
    myID = ID.create(name);
    myName = name;
    myLogId = logId;
    myIndexer = indexer;
    myFatalErrorHandler = fatalErrorHandler;

    myMapReduceIndex = createMapReduceIndex(externalizer, version);

    Disposer.register(disposableParent, this);
  }

  @Nonnull
  private MyMapReduceIndex createMapReduceIndex(@Nonnull DataExternalizer<T> dataExternalizer, int version) throws IOException {
    MyIndexExtension extension = new MyIndexExtension(myIndexer, dataExternalizer, getVersion());
    Pair<ForwardIndex, ForwardIndexAccessor<Integer, T>> pair = createdForwardIndex();
    ForwardIndex forwardIndex = pair != null ? pair.getFirst() : null;
    ForwardIndexAccessor<Integer, T> forwardIndexAccessor = pair != null ? pair.getSecond() : null;

    MyMapIndexStorage myMapIndexStorage = new MyMapIndexStorage(getStorageFile(myName, myLogId), EnumeratorIntegerDescriptor.INSTANCE, dataExternalizer, 5000, false);

    return new MyMapReduceIndex(extension, myMapIndexStorage, forwardIndex, forwardIndexAccessor);
  }

  @Nonnull
  public IntSet getCommitsWithAnyKey(@Nonnull Set<Integer> keys) throws StorageException {
    IntSet result = IntSets.newHashSet();

    for (Integer key : keys) {
      iterateCommitIds(key, result::add);
    }

    return result;
  }

  @Nonnull
  public IntSet getCommitsWithAllKeys(@Nonnull Collection<Integer> keys) throws StorageException {
    return InvertedIndexUtil.collectInputIdsContainingAllKeys(myMapReduceIndex, keys, (k) -> {
      ProgressManager.checkCanceled();
      return true;
    }, null, null);
  }

  private void iterateCommitIds(int key, @Nonnull Consumer<Integer> consumer) throws StorageException {
    ValueContainer<T> data = myMapReduceIndex.getData(key);
    data.forEach((id, value) -> {
      consumer.consume(id);
      return true;
    });
  }

  protected void iterateCommitIdsAndValues(int key, @Nonnull ObjIntConsumer<T> consumer) throws StorageException {
    myMapReduceIndex.getData(key).forEach((id, value) -> {
      consumer.accept(value, id);
      return true;
    });
  }

  public void update(int commitId, @Nonnull VcsFullCommitDetails details) throws IOException {
    myMapReduceIndex.update(commitId, details).compute();
  }

  public void flush() throws StorageException {
    myMapReduceIndex.flush();
  }

  @Override
  public void dispose() {
    myMapReduceIndex.dispose();
  }

  @Nonnull
  public static File getStorageFile(@Nonnull String kind, @Nonnull String id) {
    return PersistentUtil.getStorageFile(INDEX, kind, id, getVersion(), false);
  }

  @Nullable
  protected Pair<ForwardIndex, ForwardIndexAccessor<Integer, T>> createdForwardIndex() throws IOException {
    return null;
  }

  private class MyMapReduceIndex extends MapReduceIndex<Integer, T, VcsFullCommitDetails> {
    public MyMapReduceIndex(@Nonnull IndexExtension<Integer, T, VcsFullCommitDetails> indexer,
                            @Nonnull IndexStorage<Integer, T> indexStorage,
                            @Nullable ForwardIndex forwardIndex,
                            @Nullable ForwardIndexAccessor<Integer, T> forwardIndexAccessor) throws IOException {
      super(indexer, indexStorage, forwardIndex, forwardIndexAccessor);
    }

    @Override
    public void checkCanceled() {
      ProgressManager.checkCanceled();
    }

    @Override
    protected void requestRebuild(@Nonnull Throwable e) {
      myFatalErrorHandler.consume(this, e);
    }
  }

  private class MyMapIndexStorage extends MapIndexStorage<Integer, T> {

    protected MyMapIndexStorage(@Nonnull File storageFile,
                                @Nonnull KeyDescriptor<Integer> keyDescriptor,
                                @Nonnull DataExternalizer<T> valueExternalizer,
                                int cacheSize,
                                boolean keyIsUniqueForIndexedFile) throws IOException {
      super(storageFile, keyDescriptor, valueExternalizer, cacheSize, keyIsUniqueForIndexedFile);
    }

    @Override
    protected void checkCanceled() {
      ProgressManager.checkCanceled();
    }
  }

  private class MyIndexExtension extends IndexExtension<Integer, T, VcsFullCommitDetails> {
    @Nonnull
    private final DataIndexer<Integer, T, VcsFullCommitDetails> myIndexer;
    @Nonnull
    private final DataExternalizer<T> myExternalizer;
    private final int myVersion;

    public MyIndexExtension(@Nonnull DataIndexer<Integer, T, VcsFullCommitDetails> indexer, @Nonnull DataExternalizer<T> externalizer, int version) {
      myIndexer = indexer;
      myExternalizer = externalizer;
      myVersion = version;
    }

    @Nonnull
    @Override
    public ID<Integer, T> getName() {
      return myID;
    }

    @Nonnull
    @Override
    public DataIndexer<Integer, T, VcsFullCommitDetails> getIndexer() {
      return myIndexer;
    }

    @Nonnull
    @Override
    public KeyDescriptor<Integer> getKeyDescriptor() {
      return EnumeratorIntegerDescriptor.INSTANCE;
    }

    @Nonnull
    @Override
    public DataExternalizer<T> getValueExternalizer() {
      return myExternalizer;
    }

    @Override
    public int getVersion() {
      return myVersion;
    }
  }
}
