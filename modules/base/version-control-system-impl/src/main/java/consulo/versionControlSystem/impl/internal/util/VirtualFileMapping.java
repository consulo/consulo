// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.impl.internal.util;

import consulo.util.collection.ContainerUtil;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public final class VirtualFileMapping<T> {
  private final Map<VirtualFile, T> myVFMap = new HashMap<>();

  public void add(VirtualFile file, T value) {
    myVFMap.put(file, value);
  }

  public void remove(VirtualFile file) {
    myVFMap.remove(file);
  }

  public void clear() {
    myVFMap.clear();
  }

  public Collection<T> values() {
    return myVFMap.values();
  }

  public Collection<Pair<VirtualFile, T>> entries() {
    return ContainerUtil.map(myVFMap.entrySet(), entry -> Pair.create(entry.getKey(), entry.getValue()));
  }

  public boolean containsKey(VirtualFile file) {
    return myVFMap.containsKey(file);
  }

  public @Nullable T getMappingFor(VirtualFile file) {
    Pair<VirtualFile, T> pair = getMappingAndRootFor(file);
    return pair != null ? pair.getSecond() : null;
  }


  public @Nullable Pair<VirtualFile, T> getMappingAndRootFor(VirtualFile file) {
    VirtualFile current = file;
    while (current != null) {
      T value = myVFMap.get(current);
      if (value != null) return Pair.create(current, value);
      current = current.getParent();
    }
    return null;
  }
}
