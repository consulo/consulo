// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.impl.internal;

import consulo.project.Project;
import consulo.versionControlSystem.VcsRootChecker;
import consulo.virtualFileSystem.VirtualFile;

import java.util.Collection;
import java.util.Set;

final class VcsDefaultMappingUtils {
  private VcsDefaultMappingUtils() {
  }

  /**
   * Provides VCS roots where {@code projectRoots} are located using {@code rootChecker}.
   * <p>
   * {@code mappedDirs} are took into account during detection, so that direct mappings shouldn't be overwritten by &lt;Project&gt; mappings.
   * E.g., if directory {@code /project} has direct mapping for Hg and there is {@code /project/.git} folder
   * (that is going to be detected as &lt;Project&gt; mapping),
   * {@code /project} shouldn't be returned as &lt;Project&gt; mapping. It should stay as Hg direct mapping.
   * So, already mapped files and directories from {@code mappedDirs} are excluded from the result.
   * <p>
   * NB: if {@code rootChecker}'s {@link VcsRootChecker#areChildrenValidMappings()}
   * is true then a result may contain directories under {@code project} but not "pure" VCS roots.
   * Example: for {@code root/svn_root_dir/project_root_dir} if {@code svn_root_dir} is not under project
   * {@code project_dir} will be returned as VCS root, not {@code svn_root_dir}.
   * It is necessary to detect changes only under {@code project_root_dir}, not under {@code svn_root_dir/unrelated_project}
   *
   * @param projectRoots files and directories that are a part of the {@code project}
   * @param mappedDirs   directories that were already mapped with VCS
   */
  static Set<VirtualFile> detectProjectMappings(Project project,
                                                VcsRootChecker rootChecker,
                                                Collection<VirtualFile> projectRoots,
                                                Set<VirtualFile> mappedDirs) {
    return new VcsDefaultMappingDetector(project, rootChecker).detectProjectMappings(projectRoots, mappedDirs);
  }
}
