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
package com.intellij.openapi.project;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.PathManagerEx;
import com.intellij.openapi.fileEditor.UniqueVFilePathBuilder;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.roots.ModuleExtensionWithSdkOrderEntry;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.libraries.LibraryUtil;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFilePathWrapper;
import com.intellij.util.ObjectUtil;
import com.intellij.util.PathUtilRt;
import com.intellij.util.io.PathKt;
import consulo.vfs.ArchiveFileSystem;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.nio.file.Path;
import java.util.Locale;

/**
 * @author max
 */
public class ProjectUtil {
  private ProjectUtil() {
  }

  public static Path getProjectCachePath(Project project, String cacheName) {
    return getProjectCachePath(project, cacheName, false);
  }

  public static Path getProjectCachePath(Project project, String cacheName, boolean forceNameUse) {
    return getProjectCachePath(project, PathManagerEx.getAppSystemDir().resolve(cacheName), forceNameUse);
  }

  public static Path getProjectCachePath(Project project, Path baseDir, boolean forceNameUse) {
    return getProjectCachePath(project, baseDir, forceNameUse, ".");
  }

  public static Path getProjectCachePath(Project project, Path baseDir, boolean forceNameUse, String hashSeparator) {
    return baseDir.resolve(getProjectCacheFileName(project, forceNameUse, hashSeparator));
  }

  private static String getProjectCacheFileName(Project project, boolean forceNameUse, String hashSeparator) {
    String presentableUrl = project.getPresentableUrl();
    String name = forceNameUse || presentableUrl == null ? project.getName() : PathUtilRt.getFileName(presentableUrl).toLowerCase(Locale.US);

    name = PathKt.sanitizeFileName(name, false);

    String locationHash = Integer.toHexString(ObjectUtil.notNull(presentableUrl, name).hashCode());

    name = StringUtil.trimMiddle(name, Math.min(name.length(), 255 - hashSeparator.length() - locationHash.length()), false);

    return name + hashSeparator + locationHash;
  }

  @Nullable
  public static String getProjectLocationString(@Nonnull final Project project) {
    return FileUtil.getLocationRelativeToUserHome(project.getBasePath());
  }

  @Nonnull
  public static String calcRelativeToProjectPath(@Nonnull final VirtualFile file, @Nullable final Project project, final boolean includeFilePath) {
    return calcRelativeToProjectPath(file, project, includeFilePath, false, false);
  }

  @Nonnull
  public static String calcRelativeToProjectPath(@Nonnull final VirtualFile file,
                                                 @Nullable final Project project,
                                                 final boolean includeFilePath,
                                                 final boolean includeUniqueFilePath,
                                                 final boolean keepModuleAlwaysOnTheLeft) {
    if (file instanceof VirtualFilePathWrapper) {
      return includeFilePath ? ((VirtualFilePathWrapper)file).getPresentablePath() : file.getName();
    }
    String url;
    if (includeFilePath) {
      url = file.getPresentableUrl();
    }
    else if (includeUniqueFilePath) {
      url = UniqueVFilePathBuilder.getInstance().getUniqueVirtualFilePath(project, file);
    }
    else {
      url = file.getName();
    }
    if (project == null) {
      return url;
    }
    else {
      final VirtualFile baseDir = project.getBaseDir();
      if (baseDir != null && includeFilePath) {
        //noinspection ConstantConditions
        final String projectHomeUrl = baseDir.getPresentableUrl();
        if (url.startsWith(projectHomeUrl)) {
          url = "..." + url.substring(projectHomeUrl.length());
        }
      }

      if (SystemInfo.isMac && file.getFileSystem() instanceof ArchiveFileSystem) {
        final VirtualFile fileForJar = ((ArchiveFileSystem)file.getFileSystem()).getLocalVirtualFileFor(file);
        if (fileForJar != null) {
          final OrderEntry libraryEntry = LibraryUtil.findLibraryEntry(file, project);
          if (libraryEntry != null) {
            if (libraryEntry instanceof ModuleExtensionWithSdkOrderEntry) {
              url = url + " - [" + ((ModuleExtensionWithSdkOrderEntry)libraryEntry).getSdkName() + "]";
            }
            else {
              url = url + " - [" + libraryEntry.getPresentableName() + "]";
            }
          }
          else {
            url = url + " - [" + fileForJar.getName() + "]";
          }
        }
      }

      final Module module = ModuleUtil.findModuleForFile(file, project);
      if (module == null) return url;
      return !keepModuleAlwaysOnTheLeft && SystemInfo.isMac ? url + " - [" + module.getName() + "]" : "[" + module.getName() + "] - " + url;
    }
  }

  public static String calcRelativeToProjectPath(final VirtualFile file, final Project project) {
    return calcRelativeToProjectPath(file, project, true);
  }

  @Nullable
  public static Project guessProjectForFile(VirtualFile file) {
    return ProjectLocator.getInstance().guessProjectForFile(file);
  }

  @Nonnull
  public static Project guessCurrentProject(JComponent component) {
    Project project = null;
    Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
    if (openProjects.length > 0) project = openProjects[0];
    if (project == null) {
      DataContext dataContext = component == null ? DataManager.getInstance().getDataContext() : DataManager.getInstance().getDataContext(component);
      project = dataContext.getData(CommonDataKeys.PROJECT);
    }
    if (project == null) {
      project = ProjectManager.getInstance().getDefaultProject();
    }
    return project;
  }
}
