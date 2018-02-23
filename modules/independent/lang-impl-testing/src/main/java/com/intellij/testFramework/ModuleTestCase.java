/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.testFramework;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Consumer;
import com.intellij.util.SmartList;
import com.intellij.util.lang.CompoundRuntimeException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class ModuleTestCase extends IdeaTestCase {
  protected final Collection<Module> myModulesToDispose = new ArrayList<>();

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myModulesToDispose.clear();
  }

  @Override
  protected void tearDown() throws Exception {
    try {
      if (!myModulesToDispose.isEmpty()) {
        List<Throwable> errors = new SmartList<>();
        WriteAction.run(() -> {
          ModuleManager moduleManager = ModuleManager.getInstance(myProject);
          for (Module module : myModulesToDispose) {
            try {
              String moduleName = module.getName();
              if (moduleManager.findModuleByName(moduleName) != null) {
                moduleManager.disposeModule(module);
              }
            }
            catch (Throwable e) {
              errors.add(e);
            }
          }
        });
        CompoundRuntimeException.throwIfNotEmpty(errors);
      }
    }
    finally {
      myModulesToDispose.clear();
      super.tearDown();
    }
  }

  protected Module createModule(final File moduleFile) {
    final String path = moduleFile.getAbsolutePath();
    return createModule(path);
  }

  protected Module createModule(final String path) {
    Module module = WriteAction.compute(() -> ModuleManager.getInstance(myProject).newModule(new File(path).getName(), path));

    myModulesToDispose.add(module);
    return module;
  }

  protected Module loadModule(@Nonnull VirtualFile file) {
    return loadModule(file.getPath());
  }

  protected Module loadModule(@Nonnull String modulePath) {
    final ModuleManager moduleManager = ModuleManager.getInstance(myProject);
    Module module = null;
    /*try {
      module = ApplicationManager.getApplication().runWriteAction((ThrowableComputable<Module, Exception>)() -> moduleManager.loadModule(
              FileUtil.toSystemIndependentName(modulePath)));
    }
    catch (Exception e) {
      LOG.error(e);
      return null;
    }  */

    myModulesToDispose.add(module);
    return module;
  }

  @Nullable
  protected Module loadAllModulesUnder(@Nonnull VirtualFile rootDir) {
    return loadAllModulesUnder(rootDir, null);
  }

  @Nullable
  protected Module loadAllModulesUnder(@Nonnull VirtualFile rootDir, @javax.annotation.Nullable final Consumer<Module> moduleConsumer) {
    final Ref<Module> result = Ref.create();

   /* VfsUtilCore.visitChildrenRecursively(rootDir, new VirtualFileVisitor() {
      @Override
      public boolean visitFile(@NotNull VirtualFile file) {
        if (!file.isDirectory() && file.getName().endsWith(ModuleFileType.DOT_DEFAULT_EXTENSION)) {
          ModuleImpl module = (ModuleImpl)loadModule(file);
          if (moduleConsumer != null) {
            moduleConsumer.consume(module);
          }
          result.setIfNull(module);
        }
        return true;
      }
    });*/

    return result.get();
  }

  protected Module createModuleFromTestData(final String dirInTestData, final String newModuleFileName, final boolean addSourceRoot) throws IOException {
    final File dirInTestDataFile = new File(dirInTestData);
    assertTrue(dirInTestDataFile.isDirectory());
    final File moduleDir = createTempDirectory();
    FileUtil.copyDir(dirInTestDataFile, moduleDir);
    final Module module = createModule(moduleDir + "/" + newModuleFileName);
    final VirtualFile root = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(moduleDir);
    assertNotNull(root);
    new WriteCommandAction.Simple(module.getProject()) {
      @Override
      protected void run() throws Throwable {
        root.refresh(false, true);
      }
    }.execute().throwException();
    if (addSourceRoot) {
      PsiTestUtil.addSourceContentToRoots(module, root);
    }
    else {
      PsiTestUtil.addContentRoot(module, root);
    }
    return module;
  }
}
