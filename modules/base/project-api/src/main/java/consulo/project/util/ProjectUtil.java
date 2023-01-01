/*
 * Copyright 2013-2022 consulo.io
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
package consulo.project.util;

import consulo.application.util.UserHomeFileUtil;
import consulo.container.boot.ContainerPathManager;
import consulo.project.Project;
import consulo.util.io.FileUtil;
import consulo.util.io.PathKt;
import consulo.util.io.PathUtil;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.StringUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.nio.file.Path;
import java.util.Locale;

/**
 * @author VISTALL
 * @since 17-Sep-22
 */
public class ProjectUtil {
  public static boolean isSameProject(@Nullable String projectFilePath, @Nonnull Project project) {
    if (projectFilePath == null) return false;

    String existingBaseDirPath = project.getBasePath();

    File projectFile = new File(projectFilePath);
    if (projectFile.isDirectory()) {
      return FileUtil.pathsEqual(projectFilePath, existingBaseDirPath);
    }

    File parent = projectFile.getParentFile();
    if (parent.getName().equals(Project.DIRECTORY_STORE_FOLDER)) {
      parent = parent.getParentFile();
      return parent != null && FileUtil.pathsEqual(parent.getPath(), existingBaseDirPath);
    }
    return false;
  }

  public static Path getProjectCachePath(Project project, String cacheName) {
    return getProjectCachePath(project, cacheName, false);
  }

  public static Path getProjectCachePath(Project project, String cacheName, boolean forceNameUse) {
    return getProjectCachePath(project, ContainerPathManager.get().getSystemDir().resolve(cacheName), forceNameUse);
  }

  public static Path getProjectCachePath(Project project, Path baseDir, boolean forceNameUse) {
    return getProjectCachePath(project, baseDir, forceNameUse, ".");
  }

  public static Path getProjectCachePath(Project project, Path baseDir, boolean forceNameUse, String hashSeparator) {
    return baseDir.resolve(getProjectCacheFileName(project, forceNameUse, hashSeparator));
  }

  private static String getProjectCacheFileName(Project project, boolean forceNameUse, String hashSeparator) {
    String presentableUrl = project.getPresentableUrl();
    String name = forceNameUse || presentableUrl == null ? project.getName() : PathUtil.getFileName(presentableUrl).toLowerCase(Locale.US);

    name = PathKt.sanitizeFileName(name, false);

    String locationHash = Integer.toHexString(ObjectUtil.notNull(presentableUrl, name).hashCode());

    name = StringUtil.trimMiddle(name, Math.min(name.length(), 255 - hashSeparator.length() - locationHash.length()), false);

    return name + hashSeparator + locationHash;
  }

  @Nullable
  public static String getProjectLocationString(@Nonnull final Project project) {
    return UserHomeFileUtil.getLocationRelativeToUserHome(project.getBasePath());
  }
}
