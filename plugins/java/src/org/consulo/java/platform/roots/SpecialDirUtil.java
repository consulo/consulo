/*
 * Copyright 2013 Consulo.org
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
package org.consulo.java.platform.roots;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import org.consulo.java.platform.module.extension.JavaModuleExtensionImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author VISTALL
 * @since 19:09/05.07.13
 */
public class SpecialDirUtil {
  public static final String META_INF = "META-INF";

  @Nullable
  public static String getSpecialDirLocation(@NotNull Module module, @NotNull String name) {
    final JavaModuleExtensionImpl extension = ModuleUtilCore.getExtension(module, JavaModuleExtensionImpl.class);
    if(extension == null) {
      return null;
    }

    switch(extension.getSpecialDirLocation()) {
      case MODULE_DIR:
        return module.getModuleDirPath() + File.separator + name;
      case SOURCE_DIR:
        ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
        final VirtualFile[] sourceRoots = moduleRootManager.getSourceRoots(false);
        if(sourceRoots.length == 0) {
          return null;
        }

        for(VirtualFile virtualFile : sourceRoots) {
          final VirtualFile child = virtualFile.findChild(name);
          if(child != null) {
            return child.getPath();
          }
        }

        return sourceRoots[0].getPath() + File.separator + name;
    }
    return null;
  }

  @NotNull
  public static List<VirtualFile> collectSpecialDirs(@NotNull Module module, @NotNull String name) {
    final JavaModuleExtensionImpl extension = ModuleUtilCore.getExtension(module, JavaModuleExtensionImpl.class);
    if(extension == null) {
      return Collections.emptyList();
    }

    switch(extension.getSpecialDirLocation()) {
      case MODULE_DIR:
        final String specialDirLocation = getSpecialDirLocation(module, name);
        assert specialDirLocation != null;
        final VirtualFile virtualFile = VcsUtil.getVirtualFile(specialDirLocation);
        if(virtualFile == null) {
          return Collections.emptyList();
        }
        return Collections.singletonList(virtualFile);
      case SOURCE_DIR:
        ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
        final VirtualFile[] sourceRoots = moduleRootManager.getSourceRoots(false);
        if(sourceRoots.length == 0) {
          return Collections.emptyList();
        }

        List<VirtualFile> list = new ArrayList<VirtualFile>(2);
        for(VirtualFile sourceRoot : sourceRoots) {
          final VirtualFile child = sourceRoot.findChild(name);
          if(child != null) {
            list.add(child);
          }
        }

        return list;
    }
    return Collections.emptyList();
  }
}
