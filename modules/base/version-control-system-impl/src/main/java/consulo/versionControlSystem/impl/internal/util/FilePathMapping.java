// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.versionControlSystem.impl.internal.util;

import consulo.util.collection.HashingStrategy;
import consulo.util.collection.Maps;
import consulo.util.collection.primitive.ints.IntList;
import consulo.util.collection.primitive.ints.IntLists;
import consulo.util.collection.primitive.ints.IntSet;
import consulo.util.collection.primitive.ints.IntSets;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.internal.FilePathHashUtil;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public final class FilePathMapping<T> {
  private final boolean myCaseSensitive;

  private final Map<String, T> myPathMap;
  private final IntSet myPathHashSet = IntSets.newHashSet();

  public FilePathMapping(boolean caseSensitive) {
    myCaseSensitive = caseSensitive;
    myPathMap = caseSensitive ? new HashMap<>() : Maps.newHashMap(HashingStrategy.caseInsensitive());
  }

  public void add(String filePath, T value) {
    String path = StringUtil.trimTrailing(filePath, '/');
    myPathMap.put(path, value);
    myPathHashSet.add(FilePathHashUtil.pathHashCode(myCaseSensitive, path));
  }

  public void remove(String filePath) {
    String path = StringUtil.trimTrailing(filePath, '/');
    myPathMap.remove(path);
    // We do not update myPathHashSet, so hash collisions might become worse over time.
  }

  public void clear() {
    myPathMap.clear();
    myPathHashSet.clear();
  }

  
  public Collection<T> values() {
    return myPathMap.values();
  }

  public boolean containsKey(String filePath) {
    String path = StringUtil.trimTrailing(filePath, '/');
    return myPathMap.containsKey(path);
  }

  @Nullable
  public T getMappingFor(String filePath) {
    String path = StringUtil.trimTrailing(filePath, '/');

    int index = 0;
    int prefixHash = 0;
    IntList matches = IntLists.newArrayList();

    // check empty string for FS root
    if (myPathHashSet.contains(prefixHash)) {
      matches.add(index);
    }

    while (index < path.length()) {
      int nextIndex = path.indexOf('/', index + 1);
      if (nextIndex == -1) nextIndex = path.length();

      prefixHash = FilePathHashUtil.pathHashCode(myCaseSensitive, path, index, nextIndex, prefixHash);

      if (myPathHashSet.contains(prefixHash)) {
        matches.add(nextIndex);
      }

      index = nextIndex;
    }

    for (int i = matches.size() - 1; i >= 0; i--) {
      String prefix = path.substring(0, matches.get(i));
      T root = myPathMap.get(prefix);
      if (root != null) return root;
    }

    return null;
  }
}
