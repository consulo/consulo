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
package consulo.compiler.impl.internal;

import consulo.compiler.impl.internal.state.PathKeyDescriptor;
import consulo.index.io.PersistentHashMap;
import consulo.index.io.data.DataExternalizer;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

public abstract class StateCache<T> {
    private PersistentHashMap<String, T> myMap;
    private final File myBaseFile;

    public StateCache(File storePath) throws IOException {
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

    public void update(String path, T state) throws IOException {
        if (state != null) {
            myMap.put(path, state);
        }
        else {
            remove(path);
        }
    }

    public void remove(String path) throws IOException {
        myMap.remove(path);
    }

    public T getState(String path) throws IOException {
        return myMap.get(path);
    }

    public Collection<String> getPaths() throws IOException {
        return myMap.getAllKeysWithExistingMapping();
    }

    public Iterator<String> getPathsIterator() throws IOException {
        return myMap.getAllKeysWithExistingMapping().iterator();
    }

    private PersistentHashMap<String, T> createMap(File file) throws IOException {
        return new PersistentHashMap<>(file, PathKeyDescriptor.INSTANCE, new DataExternalizer<T>() {
            @Override
            public void save(DataOutput out, T value) throws IOException {
                StateCache.this.write(value, out);
            }

            @Override
            public T read(DataInput in) throws IOException {
                return StateCache.this.read(in);
            }
        });
    }
}
