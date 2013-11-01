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

import com.intellij.compiler.ant.BuildProperties;
import com.intellij.compiler.ant.Generator;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ContentFolder;
import com.intellij.openapi.roots.ContentFolderType;
import com.intellij.openapi.roots.ModuleRootModel;
import com.intellij.openapi.roots.ui.configuration.DefaultModulesProvider;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.ArtifactType;
import com.intellij.packaging.elements.*;
import com.intellij.packaging.impl.elements.ModuleOutputPackagingElement;
import com.intellij.packaging.impl.ui.DelegatedPackagingElementPresentation;
import com.intellij.packaging.impl.ui.ModuleElementPresentation;
import com.intellij.packaging.ui.ArtifactEditorContext;
import com.intellij.packaging.ui.PackagingElementPresentation;
import com.intellij.util.ArrayUtil;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xmlb.annotations.Attribute;
import org.consulo.compiler.CompilerPathsManager;
import org.consulo.util.pointers.NamedPointer;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mustbe.consulo.roots.ContentFolderTypeProvider;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author nik
 */
public final class ModuleOutputPackagingElementImpl
  extends PackagingElement<ModuleOutputPackagingElementImpl.ModuleOutputPackagingElementState> implements ModuleOutputPackagingElement {
  @NonNls public static final String MODULE_NAME_ATTRIBUTE = "name";

  private NamedPointer<Module> myModulePointer;
  private final ContentFolderTypeProvider myContentFolderType;
  private final Project myProject;

  public ModuleOutputPackagingElementImpl(PackagingElementType type,
                                          Project project,
                                          NamedPointer<Module> modulePointer,
                                          ContentFolderTypeProvider contentFolderType) {
    super(type);
    myProject = project;
    myModulePointer = modulePointer;
    myContentFolderType = contentFolderType;
  }

  public ModuleOutputPackagingElementImpl(PackagingElementType type, Project project, ContentFolderTypeProvider contentFolderType) {
    super(type);
    myProject = project;
    myContentFolderType = contentFolderType;
  }

  @Override
  public List<? extends Generator> computeAntInstructions(@NotNull PackagingElementResolvingContext resolvingContext,
                                                          @NotNull AntCopyInstructionCreator creator,
                                                          @NotNull ArtifactAntGenerationContext generationContext,
                                                          @NotNull ArtifactType artifactType) {
    if (myModulePointer != null) {
      final String moduleOutput =
        BuildProperties.propertyRef(generationContext.getModuleOutputPath(myModulePointer.getName(), myContentFolderType));
      return Collections.singletonList(creator.createDirectoryContentCopyInstruction(moduleOutput));
    }
    return Collections.emptyList();
  }

  @Override
  public void computeIncrementalCompilerInstructions(@NotNull IncrementalCompilerInstructionCreator creator,
                                                     @NotNull PackagingElementResolvingContext resolvingContext,
                                                     @NotNull ArtifactIncrementalCompilerContext compilerContext,
                                                     @NotNull ArtifactType artifactType) {
    final Module module = findModule(resolvingContext);
    if (module != null) {
      final CompilerPathsManager compilerPathsManager = CompilerPathsManager.getInstance(myProject);
      final VirtualFile output = compilerPathsManager.getCompilerOutput(module, myContentFolderType);
      if (output != null) {
        creator.addDirectoryCopyInstructions(output, null);
      }
    }
  }

  @NotNull
  @Override
  public Collection<VirtualFile> getSourceRoots(PackagingElementResolvingContext context) {
    Module module = myModulePointer.get();
    if (module == null) {
      return Collections.emptyList();
    }

    List<VirtualFile> roots = new SmartList<VirtualFile>();
    ModuleRootModel rootModel = context.getModulesProvider().getRootModel(module);
    for (ContentEntry entry : rootModel.getContentEntries()) {
      for (ContentFolder folder : entry.getFolders(myContentFolderType)) {
        ContainerUtil.addIfNotNull(folder.getFile(), roots);
      }
    }
    return roots;
  }

  @NonNls
  @Override
  public String toString() {
    return "module:" + getModuleName();
  }

  @NotNull
  @Override
  public PackagingElementOutputKind getFilesKind(PackagingElementResolvingContext context) {
    return PackagingElementOutputKind.DIRECTORIES_WITH_CLASSES;
  }

  @Override
  public boolean isEqualTo(@NotNull PackagingElement<?> element) {
    return element.getClass() == getClass() &&
           myModulePointer != null &&
           myModulePointer.equals(((ModuleOutputPackagingElementImpl)element).myModulePointer) &&
           myContentFolderType == ((ModuleOutputPackagingElementImpl)element).getContentFolderType();
  }

  @Override
  public ModuleOutputPackagingElementState getState() {
    final ModuleOutputPackagingElementState state = new ModuleOutputPackagingElementState();
    if (myModulePointer != null) {
      state.setModuleName(myModulePointer.getName());
    }
    return state;
  }

  @Override
  public void loadState(ModuleOutputPackagingElementState state) {
    final String moduleName = state.getModuleName();
    myModulePointer = moduleName != null ? ModuleUtilCore.createPointer(myProject, moduleName) : null;
  }

  @Override
  @Nullable
  public String getModuleName() {
    return myModulePointer != null ? myModulePointer.getName() : null;
  }

  @NotNull
  @Override
  public ContentFolderType getContentFolderType() {
    return myContentFolderType;
  }

  @Override
  public PackagingElementPresentation createPresentation(@NotNull ArtifactEditorContext context) {
    return new DelegatedPackagingElementPresentation(new ModuleElementPresentation(myModulePointer, context, myContentFolderType));
  }

  @Override
  @Nullable
  public Module findModule(PackagingElementResolvingContext context) {
    if (myModulePointer != null) {
      final Module module = myModulePointer.get();
      final ModulesProvider modulesProvider = context.getModulesProvider();
      if (module != null) {
        if (modulesProvider instanceof DefaultModulesProvider//optimization
            || ArrayUtil.contains(module, modulesProvider.getModules())) {
          return module;
        }
      }
      return modulesProvider.getModule(myModulePointer.getName());
    }
    return null;
  }

  public static class ModuleOutputPackagingElementState {
    private String myModuleName;

    @Attribute(MODULE_NAME_ATTRIBUTE)
    public String getModuleName() {
      return myModuleName;
    }

    public void setModuleName(String moduleName) {
      myModuleName = moduleName;
    }
  }
}
