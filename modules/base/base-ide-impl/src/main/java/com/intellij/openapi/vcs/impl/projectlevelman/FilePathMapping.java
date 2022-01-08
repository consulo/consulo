// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.vcs.impl.projectlevelman;

import com.intellij.openapi.util.text.StringUtil;
import consulo.util.collection.HashingStrategy;
import consulo.util.collection.Maps;
import consulo.util.collection.primitive.ints.IntList;
import consulo.util.collection.primitive.ints.IntLists;
import consulo.util.collection.primitive.ints.IntSet;
import consulo.util.collection.primitive.ints.IntSets;
import javax.annotation.Nonnull;

import javax.annotation.Nullable;
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

  public void add(@Nonnull String filePath, @Nonnull T value) {
    String path = StringUtil.trimTrailing(filePath, '/');
    myPathMap.put(path, value);
    myPathHashSet.add(pathHashCode(myCaseSensitive, path));
  }

  public void remove(@Nonnull String filePath) {
    String path = StringUtil.trimTrailing(filePath, '/');
    myPathMap.remove(path);
    // We do not update myPathHashSet, so hash collisions might become worse over time.
  }

  public void clear() {
    myPathMap.clear();
    myPathHashSet.clear();
  }

  @Nonnull
  public Collection<T> values() {
    return myPathMap.values();
  }

  public boolean containsKey(@Nonnull String filePath) {
    String path = StringUtil.trimTrailing(filePath, '/');
    return myPathMap.containsKey(path);
  }

  @Nullable
  public T getMappingFor(@Nonnull String filePath) {
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

      prefixHash = pathHashCode(myCaseSensitive, path, index, nextIndex, prefixHash);

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

  private static int pathHashCode(boolean caseSensitive, @Nonnull String path) {
    return pathHashCode(caseSensitive, path, 0, path.length(), 0);
  }

  private static int pathHashCode(boolean caseSensitive, @Nonnull String path, int offset1, int offset2, int prefixHash) {
    if (caseSensitive) {
      return StringUtil.stringHashCode(path, offset1, offset2, prefixHash);
    }
    else {
      return StringUtil.stringHashCodeInsensitive(path, offset1, offset2, prefixHash);
    }
  }
}
