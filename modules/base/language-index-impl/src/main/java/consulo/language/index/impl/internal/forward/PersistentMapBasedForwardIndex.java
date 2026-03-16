// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.index.impl.internal.forward;

import consulo.index.io.ByteSequenceDataExternalizer;
import consulo.index.io.PersistentHashMap;
import consulo.index.io.PersistentHashMapValueStorage;
import consulo.index.io.EnumeratorIntegerDescriptor;
import consulo.index.io.forward.ForwardIndex;
import consulo.logging.Logger;
import consulo.util.io.ByteArraySequence;

import org.jspecify.annotations.Nullable;
import java.io.File;
import java.io.IOException;

public class PersistentMapBasedForwardIndex implements ForwardIndex {
  private static final Logger LOG = Logger.getInstance(PersistentMapBasedForwardIndex.class);
  
  private volatile PersistentHashMap<Integer, ByteArraySequence> myPersistentMap;
  
  private final File myMapFile;
  private final boolean myUseChunks;

  public PersistentMapBasedForwardIndex(File mapFile) throws IOException {
    this(mapFile, true);
  }

  public PersistentMapBasedForwardIndex(File mapFile, boolean useChunks) throws IOException {
    myPersistentMap = createMap(mapFile);
    myMapFile = mapFile;
    myUseChunks = useChunks;
  }

  
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
  public ByteArraySequence get(Integer key) throws IOException {
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
