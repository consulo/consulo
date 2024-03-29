// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.repository.ui;

import consulo.util.concurrent.AsyncResult;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class PackageManagementService {
  /**
   * Returns the list of URLs for all configured package repositories.
   *
   * @return list of URLs, or null if the repository management is not supported by this package management service.
   */
  @Nullable
  public List<String> getAllRepositories() {
    return null;
  }

  /**
   * An async version of {@link #getAllRepositories()}.
   */
  public AsyncResult<List<String>> fetchAllRepositories() {
    return AsyncResult.resolved(getAllRepositories());
  }

  /**
   * Returns true if the service supports managing repositories.
   */
  public boolean canManageRepositories() {
    return getAllRepositories() != null;
  }

  /**
   * Checks if the user can change the URL of the specified repository or remove it from the list.
   *
   * @param repositoryUrl the URL to check
   * @return true if can be modified, false otherwise.
   */
  public boolean canModifyRepository(String repositoryUrl) {
    return true;
  }

  public void addRepository(String repositoryUrl) {
  }

  public void removeRepository(String repositoryUrl) {
  }

  /**
   * @return a negative integer, if the first version is older than the second,
   * zero, if the versions are equals,
   * a positive integer, if the first version is newer than the second.
   */
  public int compareVersions(@Nonnull String version1, @Nonnull String version2) {
    return PackageVersionComparator.VERSION_COMPARATOR.compare(version1, version2);
  }

  /**
   * Returns the list of all packages in all configured repositories. Called in a background thread
   * and may initiate network connections. May return cached data.
   *
   * @return the list of all packages in all repositories
   * @throws IOException
   */
  @Nonnull
  public abstract List<RepoPackage> getAllPackages() throws IOException;

  /**
   * Reloads the lists of packages for all configured repositories and returns the results. Called in a background thread
   * and may initiate network connections. May not return cached data.
   *
   * @return the list of all packages in all repositories
   * @throws IOException
   */
  @Nonnull
  public List<RepoPackage> reloadAllPackages() throws IOException {
    return getAllPackages();
  }

  /**
   * Returns the cached list of all packages in all configured repositories, or an empty list if there is no cached information available.
   *
   * @return the list of all packages or an empty list.
   */
  @Nonnull
  public List<RepoPackage> getAllPackagesCached() {
    return List.of();
  }

  /**
   * Returns true if the 'install to user' checkbox should be visible.
   */
  public boolean canInstallToUser() {
    return false;
  }

  /**
   * Returns the text of the 'install to user' checkbox.
   *
   * @return the text of the 'install to user' checkbox.
   */
  public String getInstallToUserText() {
    return "";
  }

  /**
   * Returns true if the 'install to user' checkbox should be initially selected.
   */
  public boolean isInstallToUserSelected() {
    return false;
  }

  /**
   * Called when the 'install to user' checkbox is checked or unchecked.
   */
  public void installToUserChanged(boolean newValue) {
  }

  /**
   * Returns the list of packages which are currently installed.
   *
   * @return the collection of currently installed packages.
   */
  @Nonnull
  public List<? extends InstalledPackage> getInstalledPackagesList() throws IOException {
    return new ArrayList<>(getInstalledPackages());
  }

  /**
   * @deprecated Please use {@link #getInstalledPackagesList()} instead.
   */
  @SuppressWarnings("DeprecatedIsStillUsed")
  @Deprecated(since = "2020.2", forRemoval = true)
  public Collection<InstalledPackage> getInstalledPackages() throws IOException {
    throw new AbstractMethodError("The method is deprecated. Please use `getInstalledPackagesList`.");
  }

  /**
   * Installs the specified package. Called in the event dispatch thread; needs to take care of spawning a background task itself.
   *
   * @param repoPackage   the package to install
   * @param version       the version selected by the user, or null if the checkbox to install a specific version is not checked
   * @param forceUpgrade  if true, the latest version of the package is installed even if there is already an installed version
   * @param extraOptions  additional options entered by the user
   * @param listener      the listener that must be called to publish information about the installation progress
   * @param installToUser the state of the "install to user" checkbox (ignore if not applicable)
   */
  public abstract void installPackage(RepoPackage repoPackage, @Nullable String version, boolean forceUpgrade, @Nullable String extraOptions, Listener listener, boolean installToUser);

  public abstract void uninstallPackages(List<InstalledPackage> installedPackages, Listener listener);

  @Nonnull
  public abstract AsyncResult<List<String>> fetchPackageVersions(String packageName);

  @Nonnull
  public abstract AsyncResult<String> fetchPackageDetails(String packageName);

  /**
   * @return identifier of this service for reported usage data (sent for JetBrains implementations only).
   * Return null to avoid reporting any usage data.
   */
  @Nullable
  public String getID() {
    return null;
  }

  public interface Listener {
    /**
     * Fired when the installation of the specified package is started.
     * Called from the caller thread.
     *
     * @param packageName the name of the package being installed.
     */
    void operationStarted(String packageName);

    /**
     * Fired when the installation of the specified package has been completed (successfully or unsuccessfully).
     * Called from the caller thread.
     *
     * @param packageName      the name of the installed package.
     * @param errorDescription null if the package has been installed successfully, error message otherwise.
     */
    void operationFinished(String packageName, @Nullable ErrorDescription errorDescription);
  }

  public static class ErrorDescription {
    @Nonnull
    private final String myMessage;
    @Nullable
    private final String myCommand;
    @Nullable
    private final String myOutput;
    @Nullable
    private final String mySolution;

    @Nullable
    public static ErrorDescription fromMessage(@Nullable String message) {
      return message != null ? new ErrorDescription(message, null, null, null) : null;
    }

    public ErrorDescription(@Nonnull String message, @Nullable String command, @Nullable String output, @Nullable String solution) {
      myMessage = message;
      myCommand = command;
      myOutput = output;
      mySolution = solution;
    }

    /**
     * The reason message that explains why the error has occurred.
     */
    @Nonnull
    public String getMessage() {
      return myMessage;
    }

    /**
     * The packaging command that has been executed, if it is meaningful to the user.
     */
    @Nullable
    public String getCommand() {
      return myCommand;
    }

    /**
     * The output of the packaging command.
     */
    @Nullable
    public String getOutput() {
      return myOutput;
    }

    /**
     * A possible solution of this packaging problem for the user.
     */
    @Nullable
    public String getSolution() {
      return mySolution;
    }
  }
}
