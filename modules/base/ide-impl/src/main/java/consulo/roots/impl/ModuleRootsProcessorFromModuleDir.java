/*
 * Copyright 2013-2016 consulo.io
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
package consulo.roots.impl;

import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.util.ArrayUtil;
import consulo.content.ContentFolderTypeProvider;
import consulo.language.content.ProductionContentFolderTypeProvider;
import consulo.module.content.layer.ModuleRootModel;
import consulo.util.collection.primitive.objects.ObjectIntMap;
import consulo.virtualFileSystem.VirtualFile;

import javax.annotation.Nonnull;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 08.12.14
 */
@Deprecated
public abstract class ModuleRootsProcessorFromModuleDir extends ModuleRootsProcessor {
  @Override
  public boolean containsFile(@Nonnull ObjectIntMap<VirtualFile> roots, @Nonnull final VirtualFile virtualFile) {
    for (ObjectIntMap.Entry<VirtualFile> next : roots.entrySet()) {
      if (VfsUtilCore.isAncestor(next.getKey(), virtualFile, false)) {
        return true;
      }
    }
    return false;
  }

  @Nonnull
  @Override
  public VirtualFile[] getFiles(@Nonnull ModuleRootModel moduleRootModel, @Nonnull Predicate<ContentFolderTypeProvider> predicate) {
    if (predicate.test(ProductionContentFolderTypeProvider.getInstance())) {
      VirtualFile moduleDir = moduleRootModel.getModule().getModuleDir();
      if (moduleDir == null) {
        return VirtualFile.EMPTY_ARRAY;
      }
      return new VirtualFile[]{moduleDir};
    }
    return VirtualFile.EMPTY_ARRAY;
  }

  @Nonnull
  @Override
  public String[] getUrls(@Nonnull ModuleRootModel moduleRootModel, @Nonnull Predicate<ContentFolderTypeProvider> predicate) {
    if (predicate.test(ProductionContentFolderTypeProvider.getInstance())) {
      return new String[]{moduleRootModel.getModule().getModuleDirUrl()};
    }
    return ArrayUtil.EMPTY_STRING_ARRAY;
  }
}
