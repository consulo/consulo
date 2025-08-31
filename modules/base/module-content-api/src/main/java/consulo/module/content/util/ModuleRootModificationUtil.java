/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.module.content.util;

import consulo.application.WriteAction;
import consulo.module.Module;
import consulo.content.library.Library;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.module.content.layer.orderEntry.DependencyScope;
import consulo.module.content.layer.orderEntry.LibraryOrderEntry;
import consulo.module.content.layer.orderEntry.ModuleOrderEntry;
import consulo.content.base.BinariesOrderRootType;
import consulo.content.base.SourcesOrderRootType;
import consulo.ui.annotation.RequiredUIAccess;

import java.util.Collections;
import java.util.List;

/**
 * @author nik
 */
public class ModuleRootModificationUtil {
  public static void addModuleLibrary(Module module, String libName, List<String> classesRoots, List<String> sourceRoots) {
    addModuleLibrary(module, libName, classesRoots, sourceRoots, DependencyScope.COMPILE);
  }

  public static void addModuleLibrary(Module module, String libName, List<String> classesRoots, List<String> sourceRoots,
                                      DependencyScope scope) {
    ModifiableRootModel model = ModuleRootManager.getInstance(module).getModifiableModel();
    Library library = model.getModuleLibraryTable().createLibrary(libName);
    Library.ModifiableModel libraryModel = library.getModifiableModel();
    for (String root : classesRoots) {
      libraryModel.addRoot(root, BinariesOrderRootType.getInstance());
    }
    for (String root : sourceRoots) {
      libraryModel.addRoot(root, SourcesOrderRootType.getInstance());
    }
    model.findLibraryOrderEntry(library).setScope(scope);
    WriteAction.run(() -> {
      libraryModel.commit();
      model.commit();
    });
  }

  public static void addModuleLibrary(Module module, String classesRootUrl) {
    addModuleLibrary(module, null, Collections.singletonList(classesRootUrl), Collections.<String>emptyList());
  }

  public static void addDependency(Module module, Library library) {
    addDependency(module, library, DependencyScope.COMPILE, false);
  }

  public static void addDependency(Module module, Library library, DependencyScope scope, boolean exported) {
    ModifiableRootModel model = ModuleRootManager.getInstance(module).getModifiableModel();
    LibraryOrderEntry entry = model.addLibraryEntry(library);
    entry.setExported(exported);
    entry.setScope(scope);
    doCommit(model);
  }

  public static void addDependency(Module from, Module to) {
    addDependency(from, to, DependencyScope.COMPILE, false);
  }

  public static void addDependency(Module from, Module to, DependencyScope scope, boolean exported) {
    ModifiableRootModel model = ModuleRootManager.getInstance(from).getModifiableModel();
    ModuleOrderEntry entry = model.addModuleOrderEntry(to);
    entry.setScope(scope);
    entry.setExported(exported);
    doCommit(model);
  }

  private static void doCommit(ModifiableRootModel model) {
    WriteAction.run(model::commit);
  }
}
