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
package com.intellij.compiler.impl;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.IOUtil;
import com.intellij.util.io.KeyDescriptor;
import com.intellij.util.io.PersistentHashMap;
import org.jetbrains.annotations.NonNls;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

public abstract class StateCache<T> {
  private static class FileKeyDescriptor implements KeyDescriptor<File> {
    public static final FileKeyDescriptor INSTANCE = new FileKeyDescriptor();

    @Override
    public int hashCode(File value) {
      return FileUtil.fileHashCode(value);
    }

    @Override
    public boolean equals(File val1, File val2) {
      return FileUtil.filesEqual(val1, val2);
    }

    @Override
    public void save(DataOutput out, File value) throws IOException {
      IOUtil.writeUTF(out, value.getPath());
    }

    @Override
    public File read(DataInput in) throws IOException {
      return new File(IOUtil.readUTF(in));
    }
  }

  private PersistentHashMap<File, T> myMap;
  private final File myBaseFile;

  public StateCache(@NonNls File storePath) throws IOException {
    myBaseFile = storePath;
    myMap = createMap(storePath);
  }

  protected abstract T read(DataInput stream) throws IOException;

  protected abstract void write(T t, DataOutput out) throws IOException;

  public void force() {
    myMap.force();
  }

  public void close() throws IOException {
    myMap.close();
  }

  public boolean wipe() {
    try {
      myMap.close();
    }
    catch (IOException ignored) {
    }
    PersistentHashMap.deleteFilesStartingWith(myBaseFile);
    try {
      myMap = createMap(myBaseFile);
    }
    catch (IOException ignored) {
      return false;
    }
    return true;
  }

  public void update(@NonNls File file, T state) throws IOException {
    if (state != null) {
      myMap.put(file, state);
    }
    else {
      remove(file);
    }
  }

  public void remove(File file) throws IOException {
    myMap.remove(file);
  }

  public T getState(File file) throws IOException {
    return myMap.get(file);
  }

  public Collection<File> getFiles() throws IOException {
    return myMap.getAllKeysWithExistingMapping();
  }

  public Iterator<File> getFilesIterator() throws IOException {
    return myMap.getAllKeysWithExistingMapping().iterator();
  }


  private PersistentHashMap<File, T> createMap(final File file) throws IOException {
    return new PersistentHashMap<>(file, FileKeyDescriptor.INSTANCE, new DataExternalizer<T>() {
      @Override
      public void save(final DataOutput out, final T value) throws IOException {
        StateCache.this.write(value, out);
      }

      @Override
      public T read(final DataInput in) throws IOException {
        return StateCache.this.read(in);
      }
    });
  }

}
