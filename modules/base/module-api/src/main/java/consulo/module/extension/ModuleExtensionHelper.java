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

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.project.Project;
import consulo.ui.image.Image;

import org.jspecify.annotations.Nullable;
import java.util.Collection;

/**
 * @author VISTALL
 * @since 2013-11-12
 */
@ServiceAPI(ComponentScope.PROJECT)
public interface ModuleExtensionHelper {
  
  public static ModuleExtensionHelper getInstance(Project project) {
    return project.getInstance(ModuleExtensionHelper.class);
  }

  public boolean hasModuleExtension(Class<? extends ModuleExtension> clazz);

  /**
   * Whether a module of the project has the extension of this id enabled - by id, not by class: the extensions of
   * different providers may share one class, as those of the external systems do.
   */
  public boolean hasModuleExtension(String extensionId);

  /**
   * The enabled extensions of this id, one per module having it.
   */
  public Collection<ModuleExtension<?>> getModuleExtensions(String extensionId);

  
  public <T extends ModuleExtension<T>> Collection<T> getModuleExtensions(Class<T> clazz);

  
  public String getModuleExtensionName(ModuleExtension<?> moduleExtension);

  public @Nullable Image getModuleExtensionIcon(String extensionId);
}
