/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.openapi.roots.ui.configuration.artifacts.sourceItems;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.packaging.elements.PackagingElement;
import com.intellij.packaging.elements.PackagingElementOutputKind;
import com.intellij.packaging.impl.elements.moduleContent.ModuleOutputElementTypeBase;
import com.intellij.packaging.impl.ui.ModuleElementPresentation;
import com.intellij.packaging.ui.ArtifactEditorContext;
import com.intellij.packaging.ui.PackagingSourceItem;
import com.intellij.packaging.ui.SourceItemPresentation;
import com.intellij.packaging.ui.SourceItemWeights;
import consulo.util.pointers.NamedPointer;
import javax.annotation.Nonnull;

import java.util.Collections;
import java.util.List;

/**
 * @author nik
 */
public class ModuleOutputSourceItem extends PackagingSourceItem {
  private final Module myModule;
  private final ModuleOutputElementTypeBase myModuleOutputType;

  public ModuleOutputSourceItem(@Nonnull Module module, ModuleOutputElementTypeBase moduleOutputType) {
    myModule = module;
    myModuleOutputType = moduleOutputType;
  }

  public Module getModule() {
    return myModule;
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof ModuleOutputSourceItem &&
           myModule.equals(((ModuleOutputSourceItem)obj).myModule) &&
           myModuleOutputType.equals(((ModuleOutputSourceItem)obj).myModuleOutputType);
  }

  @Override
  public int hashCode() {
    return myModule.hashCode();
  }

  @Override
  public SourceItemPresentation createPresentation(@Nonnull ArtifactEditorContext context) {
    final NamedPointer<Module> modulePointer = ModuleUtilCore.createPointer(myModule);
    return new DelegatedSourceItemPresentation(new ModuleElementPresentation(modulePointer, context, myModuleOutputType.getContentFolderType())) {
      @Override
      public int getWeight() {
        return SourceItemWeights.MODULE_OUTPUT_WEIGHT;
      }
    };
  }

  @Override
  @Nonnull
  public List<? extends PackagingElement<?>> createElements(@Nonnull ArtifactEditorContext context) {
    final NamedPointer<Module> modulePointer = ModuleUtilCore.createPointer(myModule);

    return Collections.singletonList(myModuleOutputType.createElement(context.getProject(), modulePointer));
  }

  @Nonnull
  @Override
  public PackagingElementOutputKind getKindOfProducedElements() {
    return PackagingElementOutputKind.DIRECTORIES_WITH_CLASSES;
  }
}
