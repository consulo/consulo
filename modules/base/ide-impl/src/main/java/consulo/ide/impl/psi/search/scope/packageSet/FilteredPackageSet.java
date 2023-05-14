// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.psi.search.scope.packageSet;

import consulo.content.scope.AbstractPackageSet;
import consulo.content.scope.NamedScopesHolder;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

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
