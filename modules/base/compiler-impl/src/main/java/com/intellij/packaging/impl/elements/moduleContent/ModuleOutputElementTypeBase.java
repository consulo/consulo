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
package com.intellij.packaging.impl.elements.moduleContent;

import com.google.common.base.Predicates;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.elements.CompositePackagingElement;
import com.intellij.packaging.elements.PackagingElement;
import com.intellij.packaging.elements.PackagingElementType;
import com.intellij.packaging.ui.ArtifactEditorContext;
import consulo.roots.ContentFolderTypeProvider;
import consulo.ui.image.Image;
import consulo.util.pointers.NamedPointer;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * @author nik
 */
public abstract class ModuleOutputElementTypeBase extends PackagingElementType<ModuleOutputPackagingElementImpl> {
  protected final ContentFolderTypeProvider myContentFolderTypeProvider;

  public ModuleOutputElementTypeBase(String id, ContentFolderTypeProvider contentFolderType) {
    super(id, contentFolderType.getName());
    myContentFolderTypeProvider = contentFolderType;
  }

  @Nonnull
  @Override
  public Image getIcon() {
    return myContentFolderTypeProvider.getIcon();
  }

  public boolean isSuitableModule(ModulesProvider modulesProvider, Module module) {
    for (ContentEntry entry : modulesProvider.getRootModel(module).getContentEntries()) {
      if (entry.getFolders(Predicates.equalTo(myContentFolderTypeProvider)).length != 0) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean isAvailableForAdd(@Nonnull ArtifactEditorContext context, @Nonnull Artifact artifact) {
    return !getSuitableModules(context).isEmpty();
  }

  @Override
  @Nonnull
  public List<? extends PackagingElement<?>> chooseAndCreate(@Nonnull ArtifactEditorContext context,
                                                             @Nonnull Artifact artifact,
                                                             @Nonnull CompositePackagingElement<?> parent) {
    List<Module> suitableModules = getSuitableModules(context);
    List<Module> selected = context.chooseModules(suitableModules, ProjectBundle.message("dialog.title.packaging.choose.module"));

    final List<PackagingElement<?>> elements = new ArrayList<PackagingElement<?>>();
    for (Module module : selected) {
      elements.add(createElement(context.getProject(), ModuleUtilCore.createPointer(module)));
    }
    return elements;
  }

  public ContentFolderTypeProvider getContentFolderType() {
    return myContentFolderTypeProvider;
  }

  public ModuleOutputPackagingElementImpl createElement(@Nonnull Project project, @Nonnull NamedPointer<Module> pointer) {
    return new ModuleOutputPackagingElementImpl(this, project, pointer, myContentFolderTypeProvider);
  }

  @Nonnull
  @Override
  public ModuleOutputPackagingElementImpl createEmpty(@Nonnull Project project) {
    return new ModuleOutputPackagingElementImpl(this, project, myContentFolderTypeProvider);
  }

  private List<Module> getSuitableModules(ArtifactEditorContext context) {
    ModulesProvider modulesProvider = context.getModulesProvider();
    ArrayList<Module> modules = new ArrayList<Module>();
    for (Module module : modulesProvider.getModules()) {
      if (isSuitableModule(modulesProvider, module)) {
        modules.add(module);
      }
    }
    return modules;
  }
}
