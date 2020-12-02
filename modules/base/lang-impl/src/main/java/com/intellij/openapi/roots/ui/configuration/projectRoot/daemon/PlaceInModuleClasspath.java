/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.intellij.openapi.roots.ui.configuration.projectRoot.daemon;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModuleRootModel;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.impl.OrderEntryUtil;
import com.intellij.openapi.roots.ui.configuration.ProjectStructureConfigurable;
import com.intellij.openapi.roots.ui.configuration.projectRoot.StructureConfigurableContext;
import consulo.util.concurrent.AsyncResult;

import javax.annotation.Nonnull;

/**
 * @author nik
 */
public class PlaceInModuleClasspath extends PlaceInProjectStructure {
  private final StructureConfigurableContext myContext;
  private final Module myModule;
  private final ProjectStructureElement myElement;
  private final OrderEntry myOrderEntry;

  public PlaceInModuleClasspath(StructureConfigurableContext context, Module module, ProjectStructureElement element, OrderEntry orderEntry) {
    myContext = context;
    myModule = module;
    myElement = element;
    myOrderEntry = orderEntry;
  }

  public PlaceInModuleClasspath(@Nonnull StructureConfigurableContext context, @Nonnull Module module, ProjectStructureElement element, @Nonnull ProjectStructureElement elementInClasspath) {
    myContext = context;
    myModule = module;
    myElement = element;
    ModuleRootModel rootModel = myContext.getModulesConfigurator().getRootModel(myModule);
    if (elementInClasspath instanceof LibraryProjectStructureElement) {
      myOrderEntry = OrderEntryUtil.findLibraryOrderEntry(rootModel, ((LibraryProjectStructureElement)elementInClasspath).getLibrary());
    }
    else if (elementInClasspath instanceof ModuleProjectStructureElement) {
      myOrderEntry = OrderEntryUtil.findModuleOrderEntry(rootModel, ((ModuleProjectStructureElement)elementInClasspath).getModule());
    }
    else if (elementInClasspath instanceof SdkProjectStructureElement) {
      myOrderEntry = OrderEntryUtil.findJdkOrderEntry(rootModel, ((SdkProjectStructureElement)elementInClasspath).getSdk());
    }
    else {
      myOrderEntry = null;
    }
  }

  @Nonnull
  @Override
  public ProjectStructureElement getContainingElement() {
    return myElement;
  }

  @Override
  public String getPlacePath() {
    return myOrderEntry != null ? myOrderEntry.getPresentableName() : null;
  }

  @Nonnull
  @Override
  public AsyncResult<Void> navigate() {
    return ProjectStructureConfigurable.getInstance(myContext.getProject()).selectOrderEntry(myModule, myOrderEntry);
  }
}
