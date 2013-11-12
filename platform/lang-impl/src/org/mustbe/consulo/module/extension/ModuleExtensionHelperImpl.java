/*
 * Copyright 2013 must-be.org
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
package org.mustbe.consulo.module.extension;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ModuleRootModel;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.util.containers.MultiMap;
import org.consulo.annotations.Immutable;
import org.consulo.module.extension.ModuleExtension;
import org.consulo.module.extension.ModuleExtensionChangeListener;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

/**
 * @author VISTALL
 * @since 8:00/12.11.13
 */
public class ModuleExtensionHelperImpl extends ModuleExtensionHelper {
  private final Project myProject;
  private MultiMap<Class<? extends ModuleExtension>, ModuleExtension> myExtensions;

  public ModuleExtensionHelperImpl(Project project) {
    myProject = project;
    project.getMessageBus().connect().subscribe(ModuleExtension.CHANGE_TOPIC, new ModuleExtensionChangeListener() {
      @Override
      public void extensionChanged(@NotNull ModuleExtension<?> oldExtension, @NotNull ModuleExtension<?> newExtension) {
        myExtensions = null;
      }
    });
  }

  @Override
  public boolean hasModuleExtension(@NotNull Class<? extends ModuleExtension> clazz) {
    checkInit();
    return myExtensions != null && myExtensions.containsKey(clazz);
  }

  @Override
  @NotNull
  @Immutable
  @SuppressWarnings("unchecked")
  public <T extends ModuleExtension<T>> Collection<T> getModuleExtensions(@NotNull Class<T> clazz) {
    checkInit();
    if(myExtensions != null) {
      return (Collection) myExtensions.get(clazz);
    }
    else {
      return Collections.emptyList();
    }
  }

  private void checkInit() {
    if(myExtensions == null) {
      myExtensions = new MultiMap<Class<? extends ModuleExtension>, ModuleExtension>();
      for (Module o : ModuleManager.getInstance(myProject).getModules()) {
        for (ModuleExtension moduleExtension : ModuleRootManager.getInstance(o).getExtensions()) {
          myExtensions.putValue(moduleExtension.getClass(), moduleExtension);
        }
      }
    }
  }
}
