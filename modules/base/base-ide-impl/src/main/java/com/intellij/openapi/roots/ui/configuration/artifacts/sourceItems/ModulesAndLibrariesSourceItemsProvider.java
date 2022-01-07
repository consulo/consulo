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
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.elements.PackagingElementFactory;
import com.intellij.packaging.elements.PackagingElementType;
import com.intellij.packaging.impl.artifacts.ArtifactUtil;
import com.intellij.packaging.impl.elements.FileCopyElementType;
import com.intellij.packaging.impl.elements.FileCopyPackagingElement;
import com.intellij.packaging.impl.elements.ModuleOutputPackagingElement;
import com.intellij.packaging.impl.elements.moduleContent.ModuleOutputElementTypeBase;
import com.intellij.packaging.ui.ArtifactEditorContext;
import com.intellij.packaging.ui.PackagingSourceItem;
import com.intellij.packaging.ui.PackagingSourceItemsProvider;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Processor;
import consulo.roots.ContentFolderScopes;
import consulo.roots.types.BinariesOrderRootType;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * @author nik
 */
public class ModulesAndLibrariesSourceItemsProvider extends PackagingSourceItemsProvider {

  @Override
  @Nonnull
  public Collection<? extends PackagingSourceItem> getSourceItems(@Nonnull ArtifactEditorContext editorContext,
                                                                  @Nonnull Artifact artifact,
                                                                  PackagingSourceItem parent) {
    if (parent == null) {
      return createModuleItems(editorContext, artifact, ArrayUtil.EMPTY_STRING_ARRAY);
    }
    else if (parent instanceof ModuleGroupItem) {
      return createModuleItems(editorContext, artifact, ((ModuleGroupItem)parent).getPath());
    }
    else if (parent instanceof ModuleSourceItemGroup) {
      return createClasspathItems(editorContext, artifact, ((ModuleSourceItemGroup)parent).getModule());
    }
    return Collections.emptyList();
  }

  private static Collection<? extends PackagingSourceItem> createClasspathItems(ArtifactEditorContext editorContext,
                                                                                Artifact artifact,
                                                                                @Nonnull Module module) {
    final List<PackagingSourceItem> items = new ArrayList<PackagingSourceItem>();
    final ModuleRootModel rootModel = editorContext.getModulesProvider().getRootModel(module);
    List<Library> libraries = new ArrayList<Library>();
    for (OrderEntry orderEntry : rootModel.getOrderEntries()) {
      if (orderEntry instanceof LibraryOrderEntry) {
        final LibraryOrderEntry libraryEntry = (LibraryOrderEntry)orderEntry;
        final Library library = libraryEntry.getLibrary();
        final DependencyScope scope = libraryEntry.getScope();
        if (library != null && scope.isForProductionRuntime()) {
          libraries.add(library);
        }
      }
    }

    for (PackagingElementType element : PackagingElementFactory.getInstance(module.getProject()).getAllElementTypes()) {
      if(element instanceof ModuleOutputElementTypeBase) {
        ModuleOutputElementTypeBase moduleOutputType = (ModuleOutputElementTypeBase)element;
        boolean can = canAddModuleOutputType(editorContext, artifact, moduleOutputType, module);
        if(can) {
          items.add(new ModuleOutputSourceItem(module, moduleOutputType));
        }
      }
    }

    for (Library library : getNotAddedLibraries(editorContext, artifact, libraries)) {
      items.add(new LibrarySourceItem(library));
    }
    return items;
  }

  private static Collection<? extends PackagingSourceItem> createModuleItems(ArtifactEditorContext editorContext, Artifact artifact,
                                                                             @Nonnull String[] groupPath) {
    final Module[] modules = editorContext.getModulesProvider().getModules();
    final List<PackagingSourceItem> items = new ArrayList<PackagingSourceItem>();
    Set<String> groups = new HashSet<String>();
    for (Module module : modules) {
      String[] path = ModuleManager.getInstance(editorContext.getProject()).getModuleGroupPath(module);
      if (path == null) {
        path = ArrayUtil.EMPTY_STRING_ARRAY;
      }

      if (Comparing.equal(path, groupPath)) {
        items.add(new ModuleSourceItemGroup(module));
      }
      else if (ArrayUtil.startsWith(path, groupPath)) {
        groups.add(path[groupPath.length]);
      }
    }
    for (String group : groups) {
      items.add(0, new ModuleGroupItem(ArrayUtil.append(groupPath, group)));
    }
    return items;
  }

  private static <T extends ModuleOutputElementTypeBase> boolean canAddModuleOutputType(@Nonnull final ArtifactEditorContext context,
                                                                                           @Nonnull Artifact artifact,
                                                                                           T type,
                                                                                           final Module module) {
    final Ref<Boolean> find = new Ref<Boolean>(true);
    ArtifactUtil
      .processPackagingElements(artifact, type, new Processor<ModuleOutputPackagingElement>() {
        @Override
        public boolean process(ModuleOutputPackagingElement moduleOutputPackagingElement) {
          if(moduleOutputPackagingElement.findModule(context) == module) {
            find.set(false);
          }
          return true;
        }
      }, context, true);

    // if it already added - we cant add new
    if(!find.get()) {
      return false;
    }

    for(ContentEntry c :  context.getModulesProvider().getRootModel(module).getContentEntries()) {
      if(c.getFolderFiles(ContentFolderScopes.of(type.getContentFolderType())).length > 0) {
        return true;
      }
    }
    return false;
  }

  private static List<? extends Library> getNotAddedLibraries(@Nonnull final ArtifactEditorContext context,
                                                              @Nonnull Artifact artifact,
                                                              List<Library> librariesList) {
    final Set<VirtualFile> roots = new HashSet<VirtualFile>();
    ArtifactUtil
      .processPackagingElements(artifact, FileCopyElementType.getInstance(), new Processor<FileCopyPackagingElement>() {
        @Override
        public boolean process(FileCopyPackagingElement fileCopyPackagingElement) {
          final VirtualFile root = fileCopyPackagingElement.getLibraryRoot();
          if (root != null) {
            roots.add(root);
          }
          return true;
        }
      }, context, true);
    final List<Library> result = new ArrayList<Library>();
    for (Library library : librariesList) {
      if (!roots.containsAll(Arrays.asList(library.getFiles(BinariesOrderRootType.getInstance())))) {
        result.add(library);
      }
    }
    return result;
  }
}
