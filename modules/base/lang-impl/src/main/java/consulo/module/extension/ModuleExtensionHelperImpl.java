/*
 * Copyright 2013-2016 consulo.io
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
package consulo.module.extension;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.util.containers.MultiMap;
import consulo.annotation.access.RequiredReadAction;

import javax.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.Collection;
import java.util.Map;

/**
 * @author VISTALL
 * @since 8:00/12.11.13
 */
@Singleton
public class ModuleExtensionHelperImpl extends ModuleExtensionHelper {
  private final Project myProject;
  private MultiMap<Class<? extends ModuleExtension>, ModuleExtension> myExtensions;

  @Inject
  public ModuleExtensionHelperImpl(Project project) {
    myProject = project;
    project.getMessageBus().connect().subscribe(ModuleExtension.CHANGE_TOPIC, (oldExtension, newExtension) -> myExtensions = null);
  }

  @Override
  public boolean hasModuleExtension(@Nonnull Class<? extends ModuleExtension> clazz) {
    checkInit();

    assert myExtensions != null;

    return !getModuleExtensions(clazz).isEmpty();
  }

  @Override
  @Nonnull
  @SuppressWarnings("unchecked")
  public <T extends ModuleExtension<T>> Collection<T> getModuleExtensions(@Nonnull Class<T> clazz) {
    checkInit();

    assert myExtensions != null;

    Collection<ModuleExtension> moduleExtensions = myExtensions.get(clazz);
    if(moduleExtensions.isEmpty()) {
      for (Map.Entry<Class<? extends ModuleExtension>, Collection<ModuleExtension>> entry : myExtensions.entrySet()) {
        Class<? extends ModuleExtension> targetCheck = entry.getKey();

        if(clazz.isAssignableFrom(targetCheck)) {
          myExtensions.put(clazz, moduleExtensions = entry.getValue());
          break;
        }
      }
    }
    return (Collection)moduleExtensions;
  }

  @RequiredReadAction
  private void checkInit() {
    if(myExtensions == null) {
      myExtensions = new MultiMap<>();
      for (Module o : ModuleManager.getInstance(myProject).getModules()) {
        for (ModuleExtension moduleExtension : ModuleRootManager.getInstance(o).getExtensions()) {
          myExtensions.putValue(moduleExtension.getClass(), moduleExtension);
        }
      }
    }
  }
}
