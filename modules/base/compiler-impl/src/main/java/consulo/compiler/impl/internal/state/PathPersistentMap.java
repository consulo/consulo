/*
 * Copyright 2013-2026 consulo.io
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
package consulo.compiler.impl.internal.state;

import consulo.index.io.PersistentHashMap;
import consulo.index.io.data.DataExternalizer;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 2026-08-30
 */
public class PathPersistentMap<V> {
    private final File myBaseFile;
    private final DataExternalizer<V> myExternalizer;

    private PersistentHashMap<String, V> myMap;

    public PathPersistentMap(File baseFile, DataExternalizer<V> externalizer) throws IOException {
        myBaseFile = baseFile;
        myExternalizer = externalizer;
        myMap = createMap();
    }

    public @Nullable V get(String path) throws IOException {
        return myMap.get(path);
    }

    public void put(String path, V value) throws IOException {
        myMap.put(path, value);
    }

    public void remove(String path) throws IOException {
        myMap.remove(path);
    }

    public boolean processKeys(Predicate<? super String> processor) throws IOException {
        return myMap.processKeysWithExistingMapping(processor);
    }

    public Collection<String> getAllKeys() throws IOException {
        return myMap.getAllKeysWithExistingMapping();
    }

    public void force() {
        myMap.force();
    }

    public void close() throws IOException {
        myMap.close();
    }

    public void wipe() throws IOException {
        try {
            myMap.close();
        }
        catch (IOException ignored) {
        }
        PersistentHashMap.deleteFilesStartingWith(myBaseFile);
        myMap = createMap();
    }

    private PersistentHashMap<String, V> createMap() throws IOException {
        return new PersistentHashMap<>(myBaseFile, PathKeyDescriptor.INSTANCE, myExternalizer);
    }
}
