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
package consulo.ide.impl.idea.packaging.impl.elements.moduleContent;

import consulo.compiler.artifact.element.*;
import consulo.module.Module;
import consulo.language.util.ModuleUtilCore;
import consulo.project.Project;
import consulo.module.content.layer.ContentEntry;
import consulo.module.content.layer.ContentFolder;
import consulo.module.content.layer.ModuleRootModel;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.DefaultModulesProvider;
import consulo.module.content.layer.ModulesProvider;
import consulo.virtualFileSystem.VirtualFile;
import consulo.compiler.artifact.ArtifactManager;
import consulo.compiler.artifact.ArtifactType;
import consulo.ide.impl.idea.packaging.impl.elements.ModuleOutputPackagingElement;
import consulo.ide.impl.idea.packaging.impl.ui.DelegatedPackagingElementPresentation;
import consulo.ide.impl.idea.packaging.impl.ui.ModuleElementPresentation;
import consulo.compiler.artifact.ui.ArtifactEditorContext;
import consulo.compiler.artifact.ui.PackagingElementPresentation;
import consulo.ide.impl.idea.util.ArrayUtil;
import consulo.util.collection.SmartList;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.util.xml.serializer.annotation.Attribute;
import consulo.application.AccessRule;
import consulo.compiler.ModuleCompilerPathsManager;
import consulo.language.content.LanguageContentFolderScopes;
import consulo.content.ContentFolderTypeProvider;
import consulo.component.util.pointer.NamedPointer;
import consulo.component.util.pointer.NamedPointerUtil;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author nik
 */
public class ModuleOutputPackagingElementImpl
        extends PackagingElement<ModuleOutputPackagingElementImpl.ModuleOutputPackagingElementState> implements ModuleOutputPackagingElement {
  @NonNls public static final String MODULE_NAME_ATTRIBUTE = "name";

  protected NamedPointer<Module> myModulePointer;
  protected final ContentFolderTypeProvider myContentFolderType;
  protected final Project myProject;

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
  public void computeIncrementalCompilerInstructions(@Nonnull IncrementalCompilerInstructionCreator creator,
                                                     @Nonnull PackagingElementResolvingContext resolvingContext,
                                                     @Nonnull ArtifactIncrementalCompilerContext compilerContext,
                                                     @Nonnull ArtifactType artifactType) {
    final Module module = findModule(resolvingContext);
    if (module != null) {
      final VirtualFile output = ModuleCompilerPathsManager.getInstance(module).getCompilerOutput(myContentFolderType);
      if (output != null) {
        creator.addDirectoryCopyInstructions(output, null);
      }
    }
  }

  @Nonnull
  @Override
  public Collection<VirtualFile> getSourceRoots(PackagingElementResolvingContext context) {
    Module module = NamedPointerUtil.get(myModulePointer);
    if (module == null) {
      return Collections.emptyList();
    }

    List<VirtualFile> roots = new SmartList<VirtualFile>();
    ModuleRootModel rootModel = context.getModulesProvider().getRootModel(module);
    for (ContentEntry entry : rootModel.getContentEntries()) {
      for (ContentFolder folder : entry.getFolders(LanguageContentFolderScopes.of(myContentFolderType))) {
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

  @Nonnull
  @Override
  public PackagingElementOutputKind getFilesKind(PackagingElementResolvingContext context) {
    return PackagingElementOutputKind.DIRECTORIES_WITH_CLASSES;
  }

  @Override
  public boolean isEqualTo(@Nonnull PackagingElement<?> element) {
    return element.getClass() == getClass() &&
           myModulePointer != null &&
           myModulePointer.equals(((ModuleOutputPackagingElementImpl)element).myModulePointer) &&
           myContentFolderType.equals(((ModuleOutputPackagingElementImpl)element).getContentFolderType());
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
  public void loadState(ArtifactManager artifactManager, ModuleOutputPackagingElementState state) {
    final String moduleName = state.getModuleName();
    myModulePointer = moduleName != null ? AccessRule.read(() -> ModuleUtilCore.createPointer(myProject, moduleName)) : null;
  }

  @Override
  @javax.annotation.Nullable
  public String getModuleName() {
    return NamedPointerUtil.getName(myModulePointer);
  }

  @Nonnull
  @Override
  public ContentFolderTypeProvider getContentFolderType() {
    return myContentFolderType;
  }

  @Override
  public PackagingElementPresentation createPresentation(@Nonnull ArtifactEditorContext context) {
    return new DelegatedPackagingElementPresentation(new ModuleElementPresentation(myModulePointer, context, myContentFolderType));
  }

  @Override
  @javax.annotation.Nullable
  public Module findModule(PackagingElementResolvingContext context) {
    final Module module = NamedPointerUtil.get(myModulePointer);
    final ModulesProvider modulesProvider = context.getModulesProvider();
    if (module != null) {
      if (modulesProvider instanceof DefaultModulesProvider//optimization
          || ArrayUtil.contains(module, modulesProvider.getModules())) {
        return module;
      }
    }
    return modulesProvider.getModule(myModulePointer.getName());
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
