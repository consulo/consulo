/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.vcs.persistent;

import consulo.index.io.Forceable;
import consulo.index.io.KeyDescriptor;
import consulo.index.io.data.DataExternalizer;
import consulo.index.io.data.DataOutputStream;
import consulo.logging.Logger;
import consulo.util.io.BufferExposingByteArrayOutputStream;
import consulo.util.io.FileUtil;
import consulo.util.io.UnsyncByteArrayInputStream;
import consulo.virtualFileSystem.RawFileLoader;
import jakarta.annotation.Nonnull;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author irengrig
 */
public class SmallMapSerializer<K,V> implements Forceable {
  private final Map<KeyWrapper<K>,V> myMap;
  private final File myFile;
  private final KeyDescriptor<K> myKeyDescriptor;
  private final DataExternalizer<V> myValueExternalizer;
  private boolean myDirty;
  private Logger LOG = Logger.getInstance(SmallMapSerializer.class);

  public SmallMapSerializer(File file, KeyDescriptor<K> keyDescriptor, DataExternalizer<V> valueExternalizer) {
    myFile = file;
    myKeyDescriptor = keyDescriptor;
    myValueExternalizer = valueExternalizer;
    myMap = new HashMap<KeyWrapper<K>, V>();
    init();
  }

  private void init() {
    try {
      byte[] bytes = RawFileLoader.getInstance().loadFileBytes(myFile);
      DataInputStream dis = new DataInputStream(new UnsyncByteArrayInputStream(bytes));
      int size = dis.readInt();
      for (int i = 0; i < size; i++) {
        KeyWrapper<K> keyWrapper = new KeyWrapper<K>(myKeyDescriptor, myKeyDescriptor.read(dis));
        V value = myValueExternalizer.read(dis);
        myMap.put(keyWrapper, value);
      }
    } catch (FileNotFoundException ignore) {
    } catch (IOException e) {
      LOG.error(e);
    }
  }

  public void put(K key, V value) {
    myMap.put(new KeyWrapper<K>(myKeyDescriptor, key), value);
    myDirty = true;
  }

  public V get(K key) {
    return myMap.get(new KeyWrapper<K>(myKeyDescriptor, key));
  }

  @Override
  public void force() {
    if (! myDirty) return;
    try{
      BufferExposingByteArrayOutputStream bos = new BufferExposingByteArrayOutputStream();
      DataOutput out = new DataOutputStream(bos);
      out.writeInt(myMap.size());
      for (Map.Entry<KeyWrapper<K>, V> entry : myMap.entrySet()) {
        myKeyDescriptor.save(out, entry.getKey().myKey);
        myValueExternalizer.save(out, entry.getValue());
      }
      FileUtil.writeToFile(myFile, bos.getInternalBuffer(), 0, bos.size());
    } catch (IOException e) {
      LOG.error(e);
    } finally {
      myDirty = false;
    }
  }

  @Override
  public boolean isDirty() {
    return myDirty;
  }

  private static class KeyWrapper<K> {
    private final K myKey;
    private final KeyDescriptor<K> myDescriptor;

    private KeyWrapper(@Nonnull KeyDescriptor<K> descriptor, K key) {
      myDescriptor = descriptor;
      myKey = key;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      KeyWrapper<K> that = (KeyWrapper) o;

      return myDescriptor.equals(this.myKey, that.myKey);
    }

    @Override
    public int hashCode() {
      return myDescriptor.hashCode(myKey);
    }
  }

  public Collection<K> keySet() {
    ArrayList<K> result = new ArrayList<K>(myMap.size());
    for (KeyWrapper<K> keyWrapper : myMap.keySet()) {
      result.add(keyWrapper.myKey);
    }
    return result;
  }
}
