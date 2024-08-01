// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.impl.internal.change;

import consulo.util.collection.HashingStrategy;
import consulo.util.collection.Sets;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.root.VcsRoot;
import jakarta.annotation.Nonnull;

import java.util.*;

import static consulo.versionControlSystem.impl.internal.change.VcsDirtyScopeManagerImpl.getDirtyScopeHashingStrategy;

public final class VcsDirtyScopeMap {
  private final Map<VcsRoot, Set<FilePath>> myMap = new HashMap<>();

  @Nonnull
  public Map<VcsRoot, Set<FilePath>> asMap() {
    return myMap;
  }

  public void add(@Nonnull VcsRoot vcs, @Nonnull FilePath filePath) {
    Set<FilePath> set = getVcsPathsSet(vcs);
    set.add(filePath);
  }

  public void addAll(@Nonnull VcsDirtyScopeMap map) {
    for (Map.Entry<VcsRoot, Set<FilePath>> entry : map.myMap.entrySet()) {
      Set<FilePath> set = getVcsPathsSet(entry.getKey());
      set.addAll(entry.getValue());
    }
  }

  @Nonnull
  private Set<FilePath> getVcsPathsSet(@Nonnull VcsRoot vcsRoot) {
    return myMap.computeIfAbsent(vcsRoot, key -> {
      HashingStrategy<FilePath> strategy = getDirtyScopeHashingStrategy(Objects.requireNonNull(key.getVcs()));
      return strategy == null ? new HashSet<>() : Sets.newHashSet(strategy);
    });
  }

  public boolean isEmpty() {
    return myMap.isEmpty();
  }

  public void clear() {
    myMap.clear();
  }
}
