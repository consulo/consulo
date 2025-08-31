/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.versionControlSystem;

import consulo.annotation.DeprecationInfo;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.ApplicationManager;
import consulo.application.util.function.Processor;
import consulo.colorScheme.TextAttributes;
import consulo.component.ProcessCanceledException;
import consulo.execution.ui.console.ConsoleViewContentType;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.versionControlSystem.change.ContentRevisionCache;
import consulo.versionControlSystem.change.VcsAnnotationLocalChangesListener;
import consulo.versionControlSystem.checkout.CheckoutProvider;
import consulo.versionControlSystem.history.VcsHistoryCache;
import consulo.versionControlSystem.root.VcsRoot;
import consulo.versionControlSystem.ui.UpdateInfoTree;
import consulo.versionControlSystem.update.ActionInfo;
import consulo.versionControlSystem.update.UpdatedFiles;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

/**
 * Manages the version control systems used by a specific project.
 */
@ServiceAPI(ComponentScope.PROJECT)
public abstract class ProjectLevelVcsManager {
  @Deprecated
  @DeprecationInfo("Use class as is - inline value")
  public static final Class<VcsMappingListener> VCS_CONFIGURATION_CHANGED = VcsMappingListener.class;
  @Deprecated
  @DeprecationInfo("Use class as is - inline value")
  public static final Class<PluginVcsMappingListener> VCS_CONFIGURATION_CHANGED_IN_PLUGIN = PluginVcsMappingListener.class;

  public abstract void iterateVfUnderVcsRoot(VirtualFile file, Processor<VirtualFile> processor);

  /**
   * Returns the <code>ProjectLevelVcsManager<code> instance for the specified project.
   *
   * @param project the project for which the instance is requested.
   * @return the manager instance.
   */
  public static ProjectLevelVcsManager getInstance(Project project) {
    return project.getComponent(ProjectLevelVcsManager.class);
  }

  /**
   * Gets the instance of the component if the project wasn't disposed. If the project was
   * disposed, throws ProcessCanceledException. Should only be used for calling from background
   * threads (for example, committed changes refresh thread).
   *
   * @param project the project for which the component instance should be retrieved.
   * @return component instance
   */
  public static ProjectLevelVcsManager getInstanceChecked(final Project project) {
    return ApplicationManager.getApplication().runReadAction(new Supplier<ProjectLevelVcsManager>() {
      public ProjectLevelVcsManager get() {
        if (project.isDisposed()) throw new ProcessCanceledException();
        return getInstance(project);
      }
    });
  }

  /**
   * Returns the list of all registered version control systems.
   *
   * @return the list of registered version control systems.
   */
  public abstract VcsDescriptor[] getAllVcss();

  /**
   * Returns the version control system with the specified name.
   *
   * @param name the name of the VCS to find.
   * @return the VCS instance, or null if none is found.
   */
  @Nullable
  public abstract AbstractVcs findVcsByName(@Nonnull String name);

  @Nullable
  public AbstractVcs findVcsByName(@Nonnull VcsKey vcsKey) {
      return findVcsByName(vcsKey.getName());
  }

  @Nullable
  public abstract VcsDescriptor getDescriptor(String name);

  /**
   * Checks if all files in the specified array are managed by the specified VCS.
   *
   * @param abstractVcs the VCS to check.
   * @param files       the files to check.
   * @return true if all files are managed by the VCS, false otherwise.
   */
  public abstract boolean checkAllFilesAreUnder(AbstractVcs abstractVcs, VirtualFile[] files);

  /**
   * Returns the VCS managing the specified file.
   *
   * @param file the file to check.
   * @return the VCS instance, or null if the file does not belong to any module or the module
   * it belongs to is not under version control.
   */
  @Nullable
  public abstract AbstractVcs getVcsFor(@Nonnull VirtualFile file);

  /**
   * Returns the VCS managing the specified file path.
   *
   * @param file the file to check.
   * @return the VCS instance, or null if the file does not belong to any module or the module
   * it belongs to is not under version control.
   */
  @Nullable
  public abstract AbstractVcs getVcsFor(FilePath file);

  /**
   * Return the parent directory of the specified file which is mapped to a VCS.
   *
   * @param file the file for which the root is requested.
   * @return the root, or null if the specified file is not in a VCS-managed directory.
   */
  @Nullable
  public abstract VirtualFile getVcsRootFor(@Nullable VirtualFile file);

  /**
   * Return the parent directory of the specified file path which is mapped to a VCS.
   *
   * @param file the file for which the root is requested.
   * @return the root, or null if the specified file is not in a VCS-managed directory.
   */
  @Nullable
  public abstract VirtualFile getVcsRootFor(FilePath file);

  @Nullable
  public abstract VcsRoot getVcsRootObjectFor(VirtualFile file);

  @Nullable
  public abstract VcsRoot getVcsRootObjectFor(FilePath file);

  /**
   * Checks if the specified VCS is used by any of the modules in the project.
   *
   * @param vcs the VCS to check.
   * @return true if the VCS is used by any of the modules, false otherwise
   */
  public abstract boolean checkVcsIsActive(AbstractVcs vcs);

  /**
   * Checks if the VCS with the specified name is used by any of the modules in the project.
   *
   * @param vcsName the name of the VCS to check.
   * @return true if the VCS is used by any of the modules, false otherwise
   */
  public abstract boolean checkVcsIsActive(@Nonnull String vcsId);

  /**
   * Returns the list of VCSes supported by plugins.
   */
  @Nonnull
  public abstract Collection<AbstractVcs> getAllSupportedVcss();

  /**
   * Returns the list of VCSes used by at least one module in the project.
   *
   * @return the list of VCSes used in the project.
   */
  public abstract AbstractVcs[] getAllActiveVcss();

  /**
   * @return VCS configured for the project, if there's only a single one. Return 'null' otherwise.
   */
  @Nullable
  public abstract AbstractVcs getSingleVCS();

  public abstract boolean hasActiveVcss();

  public abstract boolean hasAnyMappings();

  @Deprecated(forRemoval = true)
  public void addMessageToConsoleWindow(String message, TextAttributes attributes) {
    addMessageToConsoleWindow(message, new ConsoleViewContentType("", attributes));
  }

  public void addMessageToConsoleWindow(@Nullable String message, @Nonnull ConsoleViewContentType contentType) {
    addMessageToConsoleWindow(VcsConsoleLine.create(message, contentType));
  }

  public abstract void addMessageToConsoleWindow(@Nullable VcsConsoleLine line);

  @Nonnull
  public abstract VcsShowSettingOption getStandardOption(@Nonnull VcsConfiguration.StandardOption option, @Nonnull AbstractVcs vcs);

  @Nonnull
  public abstract VcsShowConfirmationOption getStandardConfirmation(@Nonnull VcsConfiguration.StandardConfirmation option, AbstractVcs vcs);

  @Nonnull
  public abstract VcsShowSettingOption getOrCreateCustomOption(@Nonnull String vcsActionName, @Nonnull AbstractVcs vcs);


  public abstract void showProjectOperationInfo(UpdatedFiles updatedFiles, String displayActionName);

  /**
   * Adds a listener for receiving notifications about changes in VCS configuration for the project.
   *
   * @param listener the listener instance.
   * @since 6.0
   * @deprecated use {@link VcsMappingListener} instead
   */
  public abstract void addVcsListener(VcsListener listener);

  /**
   * Removes a listener for receiving notifications about changes in VCS configuration for the project.
   *
   * @param listener the listener instance.
   * @since 6.0
   * @deprecated use {@link VcsMappingListener} instead
   */
  public abstract void removeVcsListener(VcsListener listener);

  /**
   * Marks the beginning of a background VCS operation (commit or update).
   *
   * @since 6.0
   */
  public abstract void startBackgroundVcsOperation();

  /**
   * Marks the end of a background VCS operation (commit or update).
   *
   * @since 6.0
   */
  public abstract void stopBackgroundVcsOperation();

  /**
   * Checks if a background VCS operation (commit or update) is currently in progress.
   *
   * @return true if a background operation is in progress, false otherwise.
   * @since 6.0
   */
  public abstract boolean isBackgroundVcsOperationRunning();

  public abstract List<VirtualFile> getRootsUnderVcsWithoutFiltering(AbstractVcs vcs);

  public abstract VirtualFile[] getRootsUnderVcs(@Nonnull AbstractVcs vcs);

  /**
   * Also includes into list all modules under roots
   */
  public abstract List<VirtualFile> getDetailedVcsMappings(AbstractVcs vcs);

  public abstract VirtualFile[] getAllVersionedRoots();

  @Nonnull
  public abstract VcsRoot[] getAllVcsRoots();

  @Nonnull
  public abstract LocalizeValue getConsolidatedVcsName();

  public abstract List<VcsDirectoryMapping> getDirectoryMappings();

  public abstract List<VcsDirectoryMapping> getDirectoryMappings(AbstractVcs vcs);

  @Nullable
  public abstract VcsDirectoryMapping getDirectoryMappingFor(FilePath path);

  /**
   * This method can be used only when initially loading the project configuration!
   */
  public abstract void setDirectoryMapping(String path, String activeVcsName);

  public abstract void setDirectoryMappings(List<VcsDirectoryMapping> items);

  public abstract void iterateVcsRoot(VirtualFile root, Processor<FilePath> iterator);

  public abstract void iterateVcsRoot(VirtualFile root,
                                      Processor<FilePath> iterator,
                                      @Nullable VirtualFileFilter directoryFilter);

  @Nullable
  public abstract AbstractVcs findVersioningVcs(VirtualFile file);

  public abstract CheckoutProvider.Listener getCompositeCheckoutListener();

  public abstract VcsHistoryCache getVcsHistoryCache();

  public abstract ContentRevisionCache getContentRevisionCache();

  public abstract boolean isFileInContent(VirtualFile vf);

  public abstract boolean isIgnored(VirtualFile vf);

  @Nonnull
  public abstract VcsAnnotationLocalChangesListener getAnnotationLocalChangesListener();

  @Nonnull
  public abstract VcsShowSettingOption getOptions(VcsConfiguration.StandardOption option);

  @RequiredUIAccess
  @Nullable
  public abstract UpdateInfoTree showUpdateProjectInfo(UpdatedFiles updatedFiles,
                                                       String displayActionName,
                                                       ActionInfo actionInfo,
                                                       boolean canceled);
}
