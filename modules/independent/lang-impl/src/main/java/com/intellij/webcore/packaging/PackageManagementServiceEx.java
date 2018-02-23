package com.intellij.webcore.packaging;

import com.intellij.util.CatchingConsumer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.File;

/**
 * @author Sergey Simonchik
 */
public abstract class PackageManagementServiceEx extends PackageManagementService {

  public abstract void updatePackage(@Nonnull InstalledPackage installedPackage, @Nullable String version, @Nonnull Listener listener);

  public abstract boolean shouldFetchLatestVersionsForOnlyInstalledPackages();

  public abstract void fetchLatestVersion(@Nonnull InstalledPackage pkg, @Nonnull final CatchingConsumer<String, Exception> consumer);

  public void installPackage(final RepoPackage repoPackage,
                             @Nullable final String version,
                             @Nullable String extraOptions,
                             final Listener listener,
                             @Nonnull final File workingDir) {
  }
}
