// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.scratch;

import consulo.ide.IdeBundle;
import consulo.language.scratch.RootType;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.content.scope.AbstractPackageSet;
import consulo.content.scope.NamedScope;
import consulo.content.scope.NamedScopesHolder;
import consulo.platform.base.icon.PlatformIconGroup;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

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
