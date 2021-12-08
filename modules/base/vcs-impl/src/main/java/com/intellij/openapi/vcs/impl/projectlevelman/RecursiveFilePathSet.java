// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.vcs.impl.projectlevelman;

import com.intellij.openapi.vcs.FilePath;
import javax.annotation.Nonnull;

import java.util.Collection;

public class RecursiveFilePathSet {
  private final FilePathMapping<FilePath> myMapping;

  public RecursiveFilePathSet(boolean caseSensitive) {
    myMapping = new FilePathMapping<>(caseSensitive);
  }

  public void add(@Nonnull FilePath filePath) {
    myMapping.add(filePath.getPath(), filePath);
  }

  public void addAll(@Nonnull Collection<? extends FilePath> filePath) {
    for (FilePath path : filePath) {
      add(path);
    }
  }

  public void remove(@Nonnull FilePath filePath) {
    myMapping.remove(filePath.getPath());
  }

  public void clear() {
    myMapping.clear();
  }

  public boolean contains(@Nonnull FilePath filePath) {
    return myMapping.containsKey(filePath.getPath());
  }

  public boolean hasAncestor(@Nonnull FilePath filePath) {
    return myMapping.getMappingFor(filePath.getPath()) != null;
  }

  @Nonnull
  public Collection<FilePath> filePaths() {
    return myMapping.values();
  }
}
