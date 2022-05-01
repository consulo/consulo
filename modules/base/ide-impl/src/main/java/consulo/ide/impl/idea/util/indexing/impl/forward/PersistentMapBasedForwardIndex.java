// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.util.indexing.impl.forward;

import consulo.ide.impl.idea.util.io.ByteSequenceDataExternalizer;
import consulo.ide.impl.idea.util.io.PersistentHashMap;
import consulo.ide.impl.idea.util.io.PersistentHashMapValueStorage;
import consulo.index.io.EnumeratorIntegerDescriptor;
import consulo.logging.Logger;
import consulo.util.io.ByteArraySequence;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;

public class PersistentMapBasedForwardIndex implements ForwardIndex {
  private static final Logger LOG = Logger.getInstance(PersistentMapBasedForwardIndex.class);
  @Nonnull
  private volatile PersistentHashMap<Integer, ByteArraySequence> myPersistentMap;
  @Nonnull
  private final File myMapFile;
  private final boolean myUseChunks;

  public PersistentMapBasedForwardIndex(@Nonnull File mapFile) throws IOException {
    this(mapFile, true);
  }

  public PersistentMapBasedForwardIndex(@Nonnull File mapFile, boolean useChunks) throws IOException {
    myPersistentMap = createMap(mapFile);
    myMapFile = mapFile;
    myUseChunks = useChunks;
  }

  @Nonnull
  protected PersistentHashMap<Integer, ByteArraySequence> createMap(File file) throws IOException {
    Boolean oldHasNoChunksValue = PersistentHashMapValueStorage.CreationTimeOptions.HAS_NO_CHUNKS.get();
    PersistentHashMapValueStorage.CreationTimeOptions.HAS_NO_CHUNKS.set(!myUseChunks);
    try {
      return new PersistentHashMap<>(file, EnumeratorIntegerDescriptor.INSTANCE, ByteSequenceDataExternalizer.INSTANCE);
    }
    finally {
      PersistentHashMapValueStorage.CreationTimeOptions.HAS_NO_CHUNKS.set(oldHasNoChunksValue);
    }
  }

  @Nullable
  @Override
  public ByteArraySequence get(@Nonnull Integer key) throws IOException {
    return myPersistentMap.get(key);
  }

  @Override
  public void put(@Nonnull Integer key, @Nullable ByteArraySequence value) throws IOException {
    if (value == null) {
      myPersistentMap.remove(key);
    }
    else {
      myPersistentMap.put(key, value);
    }
  }

  @Override
  public void force() {
    myPersistentMap.force();
  }

  @Override
  public void clear() throws IOException {
    File baseFile = myPersistentMap.getBaseFile();
    try {
      myPersistentMap.close();
    }
    catch (IOException e) {
      LOG.info(e);
    }
    PersistentHashMap.deleteFilesStartingWith(baseFile);
    myPersistentMap = createMap(myMapFile);
  }

  @Override
  public void close() throws IOException {
    myPersistentMap.close();
  }

  public boolean containsMapping(int key) throws IOException {
    return myPersistentMap.containsMapping(key);
  }
}
