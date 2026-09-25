// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.index.impl.internal.forward;

import consulo.index.io.ByteSequenceDataExternalizer;
import consulo.index.io.EnumeratorIntegerDescriptor;
import consulo.index.io.PersistentHashMap;
import consulo.index.io.PersistentHashMapValueStorage;
import consulo.index.io.StorageLockContext;
import consulo.index.io.data.IOUtil;
import consulo.index.io.forward.ForwardIndex;
import consulo.logging.Logger;
import consulo.util.io.ByteArraySequence;

import org.jspecify.annotations.Nullable;
import java.io.IOException;
import java.nio.file.Path;

public class PersistentMapBasedForwardIndex implements ForwardIndex {
  private static final Logger LOG = Logger.getInstance(PersistentMapBasedForwardIndex.class);
  
  private volatile PersistentHashMap<Integer, ByteArraySequence> myPersistentMap;
  
  private final Path myMapFile;
  private final boolean myUseChunks;
  private final @Nullable StorageLockContext myStorageLockContext;

  public PersistentMapBasedForwardIndex(Path mapFile) throws IOException {
    this(mapFile, true);
  }

  public PersistentMapBasedForwardIndex(Path mapFile, boolean useChunks) throws IOException {
    this(mapFile, useChunks, null);
  }

  public PersistentMapBasedForwardIndex(Path mapFile, boolean useChunks, @Nullable StorageLockContext storageLockContext) throws IOException {
    myPersistentMap = createMap(mapFile, useChunks, storageLockContext);
    myMapFile = mapFile;
    myUseChunks = useChunks;
    myStorageLockContext = storageLockContext;
  }

  private static PersistentHashMap<Integer, ByteArraySequence> createMap(Path file,
                                                                         boolean useChunks,
                                                                         @Nullable StorageLockContext storageLockContext) throws IOException {
    Boolean oldHasNoChunksValue = PersistentHashMapValueStorage.CreationTimeOptions.HAS_NO_CHUNKS.get();
    PersistentHashMapValueStorage.CreationTimeOptions.HAS_NO_CHUNKS.set(!useChunks);
    try {
      return new PersistentHashMap<>(file, EnumeratorIntegerDescriptor.INSTANCE, ByteSequenceDataExternalizer.INSTANCE, storageLockContext);
    }
    finally {
      PersistentHashMapValueStorage.CreationTimeOptions.HAS_NO_CHUNKS.set(oldHasNoChunksValue);
    }
  }

  @Override
  public @Nullable ByteArraySequence get(Integer key) throws IOException {
    return myPersistentMap.get(key);
  }

  @Override
  public void put(Integer key, @Nullable ByteArraySequence value) throws IOException {
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
    Path baseFile = myPersistentMap.getBaseFile();
    try {
      myPersistentMap.close();
    }
    catch (IOException e) {
      LOG.info(e);
    }
    IOUtil.deleteAllFilesStartingWith(baseFile);
    myPersistentMap = createMap(myMapFile, myUseChunks, myStorageLockContext);
  }

  @Override
  public void close() throws IOException {
    myPersistentMap.close();
  }

  public boolean containsMapping(int key) throws IOException {
    return myPersistentMap.containsMapping(key);
  }
}
