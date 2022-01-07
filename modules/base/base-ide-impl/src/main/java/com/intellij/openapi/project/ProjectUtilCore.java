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
package com.intellij.openapi.project;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.roots.ModuleExtensionWithSdkOrderEntry;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.libraries.LibraryUtil;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileProvider;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.annotation.access.RequiredReadAction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ProjectUtilCore {
  @Nonnull
  @RequiredReadAction
  public static String appendModuleName(@Nonnull VirtualFile file, @Nonnull Project project, @Nonnull String result, boolean moduleOnTheLeft) {
    Module module = ModuleUtilCore.findModuleForFile(file, project);
    if (module == null || ModuleManager.getInstance(project).getModules().length == 1) {
      return result;
    }

    if (moduleOnTheLeft) {
      return "[" + module.getName() + "] " + result;
    }
    return result + " [" + module.getName() + "]";
  }

  @Nullable
  public static String decorateWithLibraryName(@Nonnull VirtualFile file, @Nonnull Project project, @Nonnull String result) {
    if (file.getFileSystem() instanceof LocalFileProvider localFileProvider) {
      VirtualFile localFile = localFileProvider.getLocalVirtualFileFor(file);
      if (localFile != null) {
        OrderEntry libraryEntry = LibraryUtil.findLibraryEntry(file, project);
        if (libraryEntry instanceof ModuleExtensionWithSdkOrderEntry sdkOrderEntry) {
          return result + " [" + sdkOrderEntry.getSdkName() + "]";
        }
        else if (libraryEntry != null) {
          return result + " [" + libraryEntry.getPresentableName() + "]";
        }
      }
    }

    return null;
  }

  @RequiredReadAction
  public static String displayUrlRelativeToProject(@Nonnull VirtualFile file, @Nonnull String result, @Nonnull Project project, boolean includeFilePath, boolean moduleOnTheLeft) {
    if (includeFilePath) {
      //noinspection ConstantConditions
      final String projectHomeUrl = FileUtil.toSystemDependentName(project.getBasePath());
      if (result.startsWith(projectHomeUrl)) {
        result = "..." + result.substring(projectHomeUrl.length());
      }
    }

    String urlWithLibraryName = decorateWithLibraryName(file, project, result);
    if (urlWithLibraryName != null) {
      return urlWithLibraryName;
    }

    return appendModuleName(file, project, result, moduleOnTheLeft);
  }
}
