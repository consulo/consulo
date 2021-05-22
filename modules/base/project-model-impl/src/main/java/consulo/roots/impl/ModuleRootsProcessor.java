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
import consulo.annotation.DeprecationInfo;
import consulo.roots.ContentFolderTypeProvider;
import consulo.util.collection.primitive.objects.ObjectIntMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 08.12.14
 */
public abstract class ModuleRootsProcessor {
  public static final ExtensionPointName<ModuleRootsProcessor> EP_NAME = ExtensionPointName.create("com.intellij.moduleRootsProcessor");

  @Nullable
  public static ModuleRootsProcessor findRootsProcessor(@Nonnull ModuleRootModel moduleRootModel) {
    for (ModuleRootsProcessor moduleRootsProcessor : EP_NAME.getExtensionList()) {
      if (moduleRootsProcessor.canHandle(moduleRootModel)) {
        return moduleRootsProcessor;
      }
    }
    return null;
  }

  public abstract boolean canHandle(@Nonnull ModuleRootModel moduleRootModel);

  public abstract boolean containsFile(@Nonnull ObjectIntMap<VirtualFile> roots, @Nonnull VirtualFile virtualFile);

  public void processFiles(@Nonnull ModuleRootModel moduleRootModel, @Nonnull Predicate<ContentFolderTypeProvider> predicate,
                           @Nonnull Processor<VirtualFile> processor) {
    VirtualFile[] files = getFiles(moduleRootModel, predicate);
    ContainerUtil.process(files, processor);
  }

  public void processFileUrls(@Nonnull ModuleRootModel moduleRootModel, @Nonnull Predicate<ContentFolderTypeProvider> predicate,
                              @Nonnull Processor<String> processor) {
    String[] files = getUrls(moduleRootModel, predicate);
    ContainerUtil.process(files, processor);
  }

  @Nonnull
  @Deprecated
  @DeprecationInfo(value = "Override #processFiles()")
  public VirtualFile[] getFiles(@Nonnull ModuleRootModel moduleRootModel, @Nonnull Predicate<ContentFolderTypeProvider> predicate) {
    return VirtualFile.EMPTY_ARRAY;
  }

  @Nonnull
  @Deprecated
  @DeprecationInfo(value = "Override #processFileUrls()")
  public String[] getUrls(@Nonnull ModuleRootModel moduleRootModel, @Nonnull Predicate<ContentFolderTypeProvider> predicate) {
    return ArrayUtil.EMPTY_STRING_ARRAY;
  }
}
