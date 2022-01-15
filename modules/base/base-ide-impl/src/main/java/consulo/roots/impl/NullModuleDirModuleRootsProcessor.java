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
import com.intellij.openapi.roots.ModuleRootModel;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Processor;
import consulo.roots.ContentFolderTypeProvider;
import consulo.util.collection.primitive.objects.ObjectIntMap;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 02.04.2015
 */
public class NullModuleDirModuleRootsProcessor extends ModuleRootsProcessor {
  @Override
  public boolean canHandle(@Nonnull ModuleRootModel moduleRootModel) {
    return moduleRootModel.getModule().getModuleDirUrl() == null;
  }

  @Override
  public boolean containsFile(@Nonnull ObjectIntMap<VirtualFile> roots, @Nonnull VirtualFile virtualFile) {
    return roots.containsKey(virtualFile);
  }

  @Override
  public void processFiles(@Nonnull final ModuleRootModel moduleRootModel,
                           @Nonnull final Predicate<ContentFolderTypeProvider> predicate,
                           @Nonnull final Processor<VirtualFile> processor) {
    moduleRootModel.iterateContentEntries(contentEntry -> {
      VirtualFile file = contentEntry.getFile();
      return file == null || processor.process(file);
    });
  }

  @Override
  public void processFileUrls(@Nonnull final ModuleRootModel moduleRootModel,
                              @Nonnull final Predicate<ContentFolderTypeProvider> predicate,
                              @Nonnull final Processor<String> processor) {
    moduleRootModel.iterateContentEntries(contentEntry -> processor.process(contentEntry.getUrl()));
  }
}
