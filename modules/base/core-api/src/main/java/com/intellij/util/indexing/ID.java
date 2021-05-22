/*
 * Copyright 2000-2017 JetBrains s.r.o.
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

package com.intellij.util.indexing;

import consulo.container.boot.ContainerPathManager;
import consulo.util.collection.primitive.ints.IntMaps;
import consulo.util.collection.primitive.ints.IntObjectMap;
import consulo.util.collection.primitive.objects.ObjectIntMap;
import consulo.util.collection.primitive.objects.ObjectMaps;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;

/**
 * @author Eugene Zhuravlev
 */
public class ID<K, V> extends IndexId<K, V> {
  private static final IntObjectMap<ID> ourRegistry = IntMaps.newConcurrentIntObjectHashMap();
  private static final ObjectIntMap<String> ourNameToIdRegistry = ObjectMaps.newObjectIntHashMap();
  static final int MAX_NUMBER_OF_INDICES = Short.MAX_VALUE;

  private final short myUniqueId;

  static {
    final File indices = getEnumFile();
    try {
      ObjectIntMap<String> nameToIdRegistry = ObjectMaps.newObjectIntHashMap();
      try (BufferedReader reader = new BufferedReader(new FileReader(indices))) {
        for (int cnt = 1; ; cnt++) {
          final String name = reader.readLine();
          if (name == null) break;
          nameToIdRegistry.putInt(name, cnt);
        }
      }

      synchronized (ourNameToIdRegistry) {
        ourNameToIdRegistry.putAll(nameToIdRegistry);
      }
    }
    catch (IOException e) {
      synchronized (ourNameToIdRegistry) {
        ourNameToIdRegistry.clear();
        writeEnumFile();
      }
    }
  }

  @Nonnull
  private static File getEnumFile() {
    final File indexFolder = ContainerPathManager.get().getIndexRoot();
    return new File(indexFolder, "indices.enum");
  }

  protected ID(@Nonnull String name) {
    super(name);
    myUniqueId = stringToId(name);

    final ID old = ourRegistry.put(myUniqueId, this);
    assert old == null : "ID with name '" + name + "' is already registered";
  }

  private static short stringToId(@Nonnull String name) {
    synchronized (ourNameToIdRegistry) {
      if (ourNameToIdRegistry.containsKey(name)) {
        return (short)ourNameToIdRegistry.getInt(name);
      }

      int n = ourNameToIdRegistry.size() + 1;
      assert n <= MAX_NUMBER_OF_INDICES : "Number of indices exceeded: " + n;

      ourNameToIdRegistry.putInt(name, n);
      writeEnumFile();
      return (short)n;
    }
  }

  static void reinitializeDiskStorage() {
    synchronized (ourNameToIdRegistry) {
      writeEnumFile();
    }
  }

  private static void writeEnumFile() {
    try {
      final File f = getEnumFile();
      try (BufferedWriter w = new BufferedWriter(new FileWriter(f))) {
        final String[] names = new String[ourNameToIdRegistry.size()];

        ourNameToIdRegistry.forEach((key, value) -> names[value - 1] = key);

        for (String name : names) {
          w.write(name);
          w.newLine();
        }
      }
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Nonnull
  public static <K, V> ID<K, V> create(@Nonnull String name) {
    final ID<K, V> found = findByName(name);
    return found == null ? new ID<>(name) : found;
  }

  @Nullable
  @SuppressWarnings("unchecked")
  public static <K, V> ID<K, V> findByName(@Nonnull String name) {
    return (ID<K, V>)findById(stringToId(name));
  }

  @Override
  public int hashCode() {
    return myUniqueId;
  }

  public int getUniqueId() {
    return myUniqueId;
  }

  public static ID<?, ?> findById(int id) {
    return ourRegistry.get(id);
  }
}
