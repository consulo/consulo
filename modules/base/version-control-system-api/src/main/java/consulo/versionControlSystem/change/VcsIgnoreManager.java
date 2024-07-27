// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.versionControlSystem.change;

import consulo.annotation.DeprecationInfo;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.project.Project;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

@ServiceAPI(ComponentScope.PROJECT)
public interface VcsIgnoreManager {
  static VcsIgnoreManager getInstance(@Nonnull Project project) {
    return project.getInstance(VcsIgnoreManager.class);
  }

  @Deprecated
  @DeprecationInfo("Not Implemented")
  default boolean isDirectoryVcsIgnored(@Nonnull String dirPath) {
    return false;
  }

  @Deprecated
  @DeprecationInfo("Not Implemented")
  default boolean isRunConfigurationVcsIgnored(@Nonnull String configurationName) {
    return false;
  }

  @Deprecated
  @DeprecationInfo("Not Implemented")
  default void removeRunConfigurationFromVcsIgnore(@Nonnull String configurationName) {
  }

  /**
   * Check if the file could be potentially ignored. However, this doesn't mean that the file is ignored in VCS.
   * To check if the file ignored use {@link ChangeListManager#isIgnoredFile(VirtualFile)}
   *
   * @param file to check
   * @return true if the file is potentially ignored
   */
  default boolean isPotentiallyIgnoredFile(@Nonnull VirtualFile file) {
    return isPotentiallyIgnoredFile(VcsUtil.getFilePath(file));
  }

  boolean isPotentiallyIgnoredFile(@Nonnull FilePath filePath);
}
