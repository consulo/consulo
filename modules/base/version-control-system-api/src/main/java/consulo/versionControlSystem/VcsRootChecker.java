/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.project.Project;
import consulo.versionControlSystem.root.VcsRootDetector;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Provides methods to check if the given directory is a root of the given VCS. This is used e.g., by the {@link VcsRootDetector}
 * to detect invalid roots (registered in the settings, but not related to real VCS roots on disk)
 * and unregistered roots (real roots on disk not registered in the settings).
 *
 * @author Kirill Likhodedov
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class VcsRootChecker {

  public static final ExtensionPointName<VcsRootChecker> EXTENSION_POINT_NAME = ExtensionPointName.create(VcsRootChecker.class);

  /**
   * Checks if the given path represents a root of the supported VCS.
   */
  public boolean isRoot(VirtualFile file) {
    return isRoot(file.getPath());
  }

  /**
   * @param path path to check if it is vcs root directory
   * @return true if it is vcs root
   */
  public boolean isRoot(String path) {
    return false;
  }

  /**
   * Checks if registered mapping can be used to perform VCS operations.
   * The difference with {@link #isRoot} is that this method should return {@code true} if unsure.
   */
  public boolean validateRoot(VirtualFile file) {
    return validateRoot(file.getPath());
  }

  /**
   * Checks if a registered mapping can be used to perform VCS operations. Unlike {@link #isRoot(String)}, it should
   * return {@code true} when unsure: a VCS whose roots are not marked on disk (e.g. Perforce workspaces) accepts any
   * mapped directory here.
   *
   * @param path path of the mapped directory
   * @return false if the mapping is certainly invalid
   */
  public boolean validateRoot(String path) {
    return isRoot(path);
  }

  /**
   * @return - return vcs for current checker
   */
  public abstract VcsKey getSupportedVcs();

  /**
   * Check if the "dot" directory changed during scan
   *
   * @param path - path to check
   * @return true if it is a DOT_DIR
   */
  public boolean isVcsDir(@Nullable String path) {
    return false;
  }

  /**
   * Check if the given directory is ignored in the given VCS root.
   * Such situation can happen, when we detect a VCS root above the directory: in that case we should detect the root only if the directory
   * is not ignored from that root (e.g. the root is the home directory, and the VCS is used for storing configs, ignoring everything else).
   */
  public boolean isIgnored(Project project, VirtualFile root, VirtualFile checkForIgnore) {
    return false;
  }

  /**
   * Check if a given VCS root has dependent directories, that should be checked even if not a part of the Project.
   */
  public List<VirtualFile> suggestDependentRoots(VirtualFile vcsRoot) {
    return Collections.emptyList();
  }

  /**
   * @return Whether any descendant of VCS root can be registered as valid VCS mapping.
   */
  public boolean areChildrenValidMappings() {
    return false;
  }

  /**
   * @param projectRoots - directories with project files
   * @param mappedDirs   - roots that have an explicit mappings, and should not be included into detection
   * @return Detected vcs root mappings for the project
   * or null if default logic should be used instead (relying on {@link #isRoot} calls).
   * @throws VcsException - when detection of project mappings failed (e.g., no internet connection for VCSs that require it).
   */
  public @Nullable Collection<VirtualFile> detectProjectMappings(Project project,
                                                                 Collection<VirtualFile> projectRoots,
                                                                 Set<VirtualFile> mappedDirs) throws VcsException {
    return null;
  }

  /**
   * Return true if the checker should be run even if initial mapping detection already passed for the project.
   * <p/>
   * This is useful for VCSs that require always mapping detection, e.g., Perforce.
   * <p/>
   *
   * @return true if the checker should be run even if initial mapping detection already passed for the project.
   */
  public boolean shouldAlwaysRunInitialDetection() {
    return false;
  }
}
