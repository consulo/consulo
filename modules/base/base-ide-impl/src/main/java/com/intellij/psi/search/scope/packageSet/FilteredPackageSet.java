// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.search.scope.packageSet;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class FilteredPackageSet extends AbstractPackageSet {
  public FilteredPackageSet(@Nonnull String text) {
    super(text);
  }

  public FilteredPackageSet(@Nonnull String text, int priority) {
    super(text, priority);
  }

  public abstract boolean contains(@Nonnull VirtualFile file, @Nonnull Project project);

  @Override
  public boolean contains(@Nonnull VirtualFile file, @Nonnull Project project, @Nullable NamedScopesHolder holder) {
    return contains(file, project);
  }
}
