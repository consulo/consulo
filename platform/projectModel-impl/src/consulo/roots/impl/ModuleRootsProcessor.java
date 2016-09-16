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

import com.google.common.base.Predicate;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.roots.ModuleRootModel;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Processor;
import com.intellij.util.containers.ContainerUtil;
import gnu.trove.TObjectIntHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import consulo.annotations.DeprecationInfo;
import consulo.roots.ContentFolderTypeProvider;

/**
 * @author VISTALL
 * @since 08.12.14
 */
public abstract class ModuleRootsProcessor {
  public static final ExtensionPointName<ModuleRootsProcessor> EP_NAME = ExtensionPointName.create("com.intellij.moduleRootsProcessor");

  @Nullable
  public static ModuleRootsProcessor findRootsProcessor(@NotNull ModuleRootModel moduleRootModel) {
    for (ModuleRootsProcessor moduleRootsProcessor : EP_NAME.getExtensions()) {
      if (moduleRootsProcessor.canHandle(moduleRootModel)) {
        return moduleRootsProcessor;
      }
    }
    return null;
  }

  public abstract boolean canHandle(@NotNull ModuleRootModel moduleRootModel);

  public abstract boolean containsFile(@NotNull TObjectIntHashMap<VirtualFile> roots, @NotNull VirtualFile virtualFile);

  public void processFiles(@NotNull ModuleRootModel moduleRootModel, @NotNull Predicate<ContentFolderTypeProvider> predicate,
                           @NotNull Processor<VirtualFile> processor) {
    VirtualFile[] files = getFiles(moduleRootModel, predicate);
    ContainerUtil.process(files, processor);
  }

  public void processFileUrls(@NotNull ModuleRootModel moduleRootModel, @NotNull Predicate<ContentFolderTypeProvider> predicate,
                           @NotNull Processor<String> processor) {
    String[] files = getUrls(moduleRootModel, predicate);
    ContainerUtil.process(files, processor);
  }

  @NotNull
  @Deprecated
  @DeprecationInfo(value = "Override #processFiles()")
  public VirtualFile[] getFiles(@NotNull ModuleRootModel moduleRootModel, @NotNull Predicate<ContentFolderTypeProvider> predicate) {
    return VirtualFile.EMPTY_ARRAY;
  }

  @NotNull
  @Deprecated
  @DeprecationInfo(value = "Override #processFileUrls()")
  public String[] getUrls(@NotNull ModuleRootModel moduleRootModel, @NotNull Predicate<ContentFolderTypeProvider> predicate) {
    return ArrayUtil.EMPTY_STRING_ARRAY;
  }
}
