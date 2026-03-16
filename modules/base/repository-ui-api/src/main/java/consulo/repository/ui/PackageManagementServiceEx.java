/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package consulo.repository.ui;

import consulo.util.concurrent.AsyncResult;

import org.jspecify.annotations.Nullable;
import java.io.File;

public abstract class PackageManagementServiceEx extends PackageManagementService {

  public abstract void updatePackage(InstalledPackage installedPackage, @Nullable String version, Listener listener);

  public boolean shouldFetchLatestVersionsForOnlyInstalledPackages() {
    return true;
  }

  public AsyncResult<String> fetchLatestVersion(InstalledPackage pkg) {
    return AsyncResult.resolved();
  }

  public void installPackage(RepoPackage repoPackage, @Nullable String version, @Nullable String extraOptions, Listener listener, File workingDir) {
  }
}
