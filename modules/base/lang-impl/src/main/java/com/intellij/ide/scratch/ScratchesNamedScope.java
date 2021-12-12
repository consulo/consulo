// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.scratch;

import com.intellij.ide.IdeBundle;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.scope.packageSet.AbstractPackageSet;
import com.intellij.psi.search.scope.packageSet.NamedScope;
import com.intellij.psi.search.scope.packageSet.NamedScopesHolder;
import consulo.platform.base.icon.PlatformIconGroup;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ScratchesNamedScope extends NamedScope {
  public static String scratchesAndConsoles() {
    return IdeBundle.message("scratches.and.consoles");
  }

  public ScratchesNamedScope() {
    super(scratchesAndConsoles(), PlatformIconGroup.scopeScratches(), new AbstractPackageSet(scratchesAndConsoles()) {
      @Override
      public boolean contains(@Nonnull VirtualFile file, @Nonnull Project project, @Nullable NamedScopesHolder holder) {
        return ScratchesNamedScope.contains(project, file);
      }
    });
  }

  public static boolean contains(@Nonnull Project project, @Nonnull VirtualFile file) {
    RootType rootType = RootType.forFile(file);
    return rootType != null && !(rootType.isHidden() || rootType.isIgnored(project, file));
  }
}
