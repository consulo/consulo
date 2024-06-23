// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.impl.internal.change;

import consulo.application.util.SystemInfo;
import consulo.util.collection.CharSequenceHashingStrategy;
import consulo.util.collection.Sets;
import consulo.util.io.PathUtil;
import consulo.util.lang.CharSequenceSubSequence;
import consulo.util.lang.ThreeState;
import consulo.versionControlSystem.FilePath;
import jakarta.annotation.Nonnull;

import java.util.Collection;
import java.util.Set;

public class AffectedPathSet {
  private final Set<CharSequence> myDirectParents = Sets.newHashSet(CharSequenceHashingStrategy.of(SystemInfo.isFileSystemCaseSensitive));
  private final Set<CharSequence> myAllPaths = Sets.newHashSet(CharSequenceHashingStrategy.of(SystemInfo.isFileSystemCaseSensitive));

  public AffectedPathSet(@Nonnull Collection<FilePath> paths) {
    for (FilePath path : paths) {
      add(path);
    }
  }

  private void add(@Nonnull FilePath filePath) {
    // do not store 'subString' to reduce memory footprint
    CharSequence parent = PathUtil.getParentPathSequence(new CharSequenceSubSequence(filePath.getPath()));
    if (parent.isEmpty()) return;

    myDirectParents.add(parent);

    while (!parent.isEmpty()) {
      boolean wasAdded = myAllPaths.add(parent);
      if (!wasAdded) break;
      parent = PathUtil.getParentPathSequence(parent);
    }
  }

  public @Nonnull ThreeState haveChangesUnder(@Nonnull FilePath filePath) {
    String path = filePath.getPath();
    if (myDirectParents.contains(path)) return ThreeState.YES;
    if (myAllPaths.contains(path)) return ThreeState.UNSURE;
    return ThreeState.NO;
  }
}
