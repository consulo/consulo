// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.versionControlSystem;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

@ExtensionAPI(ComponentScope.APPLICATION)
public interface VcsIgnoreChecker {
  @NotNull
  VcsKey getSupportedVcs();

  @NotNull
  IgnoredCheckResult isIgnored(@NotNull VirtualFile vcsRoot, @NotNull Path file);

  @NotNull
  IgnoredCheckResult isFilePatternIgnored(@NotNull VirtualFile vcsRoot, @NotNull String filePattern);
}
