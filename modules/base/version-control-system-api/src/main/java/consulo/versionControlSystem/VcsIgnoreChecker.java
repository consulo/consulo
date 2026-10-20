// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.versionControlSystem;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.virtualFileSystem.VirtualFile;

import java.nio.file.Path;

@ExtensionAPI(ComponentScope.APPLICATION)
public interface VcsIgnoreChecker {
    VcsKey getSupportedVcs();

    IgnoredCheckResult isIgnored(VirtualFile vcsRoot, Path file);

    IgnoredCheckResult isFilePatternIgnored(VirtualFile vcsRoot, String filePattern);
}
