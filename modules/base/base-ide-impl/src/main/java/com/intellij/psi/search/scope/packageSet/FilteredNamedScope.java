// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.search.scope.packageSet;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileFilter;
import consulo.localize.LocalizeValue;
import consulo.ui.image.Image;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;

public class FilteredNamedScope extends NamedScope {
  public FilteredNamedScope(@Nonnull @NonNls String name, LocalizeValue presentableName, @Nonnull Image icon, int priority, @Nonnull VirtualFileFilter filter) {
    super(name, presentableName, icon, new FilteredPackageSet(name, priority) {
      @Override
      public boolean contains(@Nonnull VirtualFile file, @Nonnull Project project) {
        return filter.accept(file);
      }
    });
  }
}
