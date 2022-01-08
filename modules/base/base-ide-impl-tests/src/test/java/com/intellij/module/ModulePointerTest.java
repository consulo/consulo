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
package com.intellij.module;

import consulo.disposer.Disposable;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.testFramework.PlatformTestCase;
import consulo.util.pointers.NamedPointer;

/**
 * @author nik
 */
public abstract class ModulePointerTest extends PlatformTestCase {
  public void testCreateByName() throws Exception {
    final NamedPointer<Module> pointer = ModuleUtilCore.createPointer(getProject(), "m");
    assertSame(pointer, ModuleUtilCore.createPointer(getProject(), "m"));
    assertNull(pointer.get());
    assertEquals("m", pointer.getName());

    final Module module = addModule("m");

    assertSame(module, pointer.get());
    assertEquals("m", pointer.getName());
  }

  public void testCreateByModule() throws Exception {
    final Module module = addModule("x");
    final NamedPointer<Module> pointer = ModuleUtilCore.createPointer(module);
    assertSame(pointer, ModuleUtilCore.createPointer(module));
    assertSame(pointer, ModuleUtilCore.createPointer(getProject(), "x"));
    assertSame(module, pointer.get());
    assertEquals("x", pointer.getName());

    ModifiableModuleModel model = getModuleManager().getModifiableModel();
    model.disposeModule(module);
    commitModel(model);

    assertNull(pointer.get());
    assertEquals("x", pointer.getName());

    final Module newModule = addModule("x");
    assertSame(pointer, ModuleUtilCore.createPointer(newModule));
  }

  public void testRenameModule() throws Exception {
    final NamedPointer<Module> pointer = ModuleUtilCore.createPointer(getProject(), "abc");
    final Module module = addModule("abc");
    ModifiableModuleModel model = getModuleManager().getModifiableModel();
    model.renameModule(module, "xyz");
    commitModel(model);
    assertSame(module, pointer.get());
    assertEquals("xyz", pointer.getName());
  }

  public void testDisposePointerFromUncommitedModifiableModel() throws Exception {
    final NamedPointer<Module> pointer = ModuleUtilCore.createPointer(getProject(), "xxx");

    final ModifiableModuleModel modifiableModel = getModuleManager().getModifiableModel();
    final Module module = modifiableModel.newModule("xxx", myProject.getBaseDir().getPath());
    assertSame(pointer, ModuleUtilCore.createPointer(module));
    assertSame(pointer, ModuleUtilCore.createPointer(getProject(), "xxx"));

    assertSame(module, pointer.get());
    assertEquals("xxx", pointer.getName());

    modifiableModel.dispose();

    assertNull(pointer.get());
    assertEquals("xxx", pointer.getName());
  }

  private ModuleManager getModuleManager() {
    return ModuleManager.getInstance(myProject);
  }

  private Module addModule(final String name) {
    final ModifiableModuleModel model = getModuleManager().getModifiableModel();
    final Module module = model.newModule(name, myProject.getBaseDir().getPath());
    commitModel(model);
    disposeOnTearDown(new Disposable() {
      @Override
      public void dispose() {
        if (!module.isDisposed()) {
          getModuleManager().disposeModule(module);
        }
      }
    });
    return module;
  }

  private static void commitModel(final ModifiableModuleModel model) {
    WriteAction.run(() -> model.commit());
  }
}
