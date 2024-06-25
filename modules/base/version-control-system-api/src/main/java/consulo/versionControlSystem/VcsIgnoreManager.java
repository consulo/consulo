// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.versionControlSystem;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

@ServiceAPI(ComponentScope.PROJECT)
public interface VcsIgnoreManager {

  boolean isDirectoryVcsIgnored(@Nonnull String dirPath);

  boolean isRunConfigurationVcsIgnored(@Nonnull String configurationName);

  void removeRunConfigurationFromVcsIgnore(@Nonnull String configurationName);

  /**
   * Check if the file could be potentially ignored. However, this doesn't mean that the file is ignored in VCS.
   * To check if the file ignored use {@link ChangeListManager#isIgnoredFile(VirtualFile)}
   *
   * @param file to check
   * @return true if the file is potentially ignored
   */
  boolean isPotentiallyIgnoredFile(@Nonnull VirtualFile file);

  boolean isPotentiallyIgnoredFile(@Nonnull FilePath filePath);

  static VcsIgnoreManager getInstance(@Nonnull Project project) {
    return project.getInstance(VcsIgnoreManager.class);
  }
}
