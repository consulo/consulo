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
package consulo.module.impl.internal.layer;

import consulo.annotation.DeprecationInfo;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.util.function.Processor;
import consulo.component.extension.ExtensionPointName;
import consulo.content.ContentFolderTypeProvider;
import consulo.module.content.layer.ModuleRootModel;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.primitive.objects.ObjectIntMap;
import consulo.virtualFileSystem.VirtualFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 08.12.14
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class ModuleRootsProcessor {
  public static final ExtensionPointName<ModuleRootsProcessor> EP_NAME = ExtensionPointName.create(ModuleRootsProcessor.class);

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

  public void processFiles(@Nonnull ModuleRootModel moduleRootModel, @Nonnull Predicate<ContentFolderTypeProvider> predicate, @Nonnull Processor<VirtualFile> processor) {
    VirtualFile[] files = getFiles(moduleRootModel, predicate);
    ContainerUtil.process(files, processor);
  }

  public void processFileUrls(@Nonnull ModuleRootModel moduleRootModel, @Nonnull Predicate<ContentFolderTypeProvider> predicate, @Nonnull Processor<String> processor) {
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
