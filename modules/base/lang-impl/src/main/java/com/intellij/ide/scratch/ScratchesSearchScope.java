// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.scratch;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NotNullLazyKey;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import javax.annotation.Nonnull;

import java.util.Objects;

/**
 * @author gregsh
 */
public class ScratchesSearchScope extends GlobalSearchScope {

  private static final NotNullLazyKey<GlobalSearchScope, Project> SCRATCHES_SCOPE_KEY = NotNullLazyKey.create("SCRATCHES_SCOPE_KEY", project -> new ScratchesSearchScope(project));

  @Nonnull
  public static GlobalSearchScope getScratchesScope(@Nonnull Project project) {
    return SCRATCHES_SCOPE_KEY.getValue(project);
  }

  private ScratchesSearchScope(@Nonnull Project project) {
    super(project);
  }

  @Nonnull
  @Override
  public String getDisplayName() {
    return ScratchesNamedScope.scratchesAndConsoles();
  }

  @Override
  public boolean contains(@Nonnull VirtualFile file) {
    return ScratchesNamedScope.contains(Objects.requireNonNull(getProject()), file);
  }

  @Override
  public boolean isSearchInModuleContent(@Nonnull Module aModule) {
    return false;
  }

  @Override
  public boolean isSearchInLibraries() {
    return false;
  }

  @Override
  public String toString() {
    return getDisplayName();
  }
}
