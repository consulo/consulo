/*
 * Copyright 2013-2014 must-be.org
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

import com.google.common.base.Predicate;
import com.intellij.openapi.roots.ModuleRootModel;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ArrayUtil;
import consulo.roots.ContentFolderTypeProvider;
import gnu.trove.TObjectIntHashMap;
import gnu.trove.TObjectProcedure;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 08.12.14
 */
@Deprecated
public abstract class ModuleRootsProcessorFromModuleDir extends ModuleRootsProcessor {
  @Override
  public boolean containsFile(@NotNull TObjectIntHashMap<VirtualFile> roots, @NotNull final VirtualFile virtualFile) {
    return !roots.forEachKey(new TObjectProcedure<VirtualFile>() {
      @Override
      public boolean execute(VirtualFile object) {
        return !VfsUtilCore.isAncestor(object, virtualFile, false);
      }
    });
  }

  @NotNull
  @Override
  public VirtualFile[] getFiles(@NotNull ModuleRootModel moduleRootModel, @NotNull Predicate<ContentFolderTypeProvider> predicate) {
    if (predicate.apply(ProductionContentFolderTypeProvider.getInstance())) {
      VirtualFile moduleDir = moduleRootModel.getModule().getModuleDir();
      if (moduleDir == null) {
        return VirtualFile.EMPTY_ARRAY;
      }
      return new VirtualFile[]{moduleDir};
    }
    return VirtualFile.EMPTY_ARRAY;
  }

  @NotNull
  @Override
  public String[] getUrls(@NotNull ModuleRootModel moduleRootModel, @NotNull Predicate<ContentFolderTypeProvider> predicate) {
    if (predicate.apply(ProductionContentFolderTypeProvider.getInstance())) {
      return new String[]{moduleRootModel.getModule().getModuleDirUrl()};
    }
    return ArrayUtil.EMPTY_STRING_ARRAY;
  }
}
