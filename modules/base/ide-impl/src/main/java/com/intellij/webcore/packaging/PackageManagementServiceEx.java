/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.webcore.packaging;

import com.intellij.util.CatchingConsumer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.File;

public abstract class PackageManagementServiceEx extends PackageManagementService {

  public abstract void updatePackage(@Nonnull InstalledPackage installedPackage, @Nullable String version, @Nonnull Listener listener);

  public boolean shouldFetchLatestVersionsForOnlyInstalledPackages() {
    return true;
  }

  public abstract void fetchLatestVersion(@Nonnull InstalledPackage pkg, @Nonnull final CatchingConsumer<String, Exception> consumer);

  public void installPackage(final RepoPackage repoPackage, @Nullable final String version, @Nullable String extraOptions, final Listener listener, @Nonnull final File workingDir) {
  }
}
