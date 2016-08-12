/*
 * Copyright 2013-2015 must-be.org
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
package com.intellij.openapi.roots.impl;

import com.google.common.base.Predicate;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModuleRootModel;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Processor;
import gnu.trove.TObjectIntHashMap;
import org.jetbrains.annotations.NotNull;
import consulo.roots.ContentFolderTypeProvider;

/**
 * @author VISTALL
 * @since 02.04.2015
 */
public class NullModuleDirModuleRootsProcessor extends ModuleRootsProcessor {
  @Override
  public boolean canHandle(@NotNull ModuleRootModel moduleRootModel) {
    return moduleRootModel.getModule().getModuleDirUrl() == null;
  }

  @Override
  public boolean containsFile(@NotNull TObjectIntHashMap<VirtualFile> roots, @NotNull VirtualFile virtualFile) {
    return roots.contains(virtualFile);
  }

  @Override
  public void processFiles(@NotNull final ModuleRootModel moduleRootModel,
                           @NotNull final Predicate<ContentFolderTypeProvider> predicate,
                           @NotNull final Processor<VirtualFile> processor) {
    moduleRootModel.iterateContentEntries(new Processor<ContentEntry>() {
      @Override
      public boolean process(ContentEntry contentEntry) {
        VirtualFile file = contentEntry.getFile();
        return file == null || processor.process(file);
      }
    });
  }

  @Override
  public void processFileUrls(@NotNull final ModuleRootModel moduleRootModel,
                              @NotNull final Predicate<ContentFolderTypeProvider> predicate,
                              @NotNull final Processor<String> processor) {
    moduleRootModel.iterateContentEntries(new Processor<ContentEntry>() {
      @Override
      public boolean process(ContentEntry contentEntry) {
        return processor.process(contentEntry.getUrl());
      }
    });
  }
}
