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

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import javax.annotation.Nonnull;

import java.util.Collection;

/**
 * @author VISTALL
 * @since 7:57/12.11.13
 */
public abstract class ModuleExtensionHelper {
  @Nonnull
  public static ModuleExtensionHelper getInstance(@Nonnull Project project) {
    return ServiceManager.getService(project, ModuleExtensionHelper.class);
  }

  public abstract boolean hasModuleExtension(@Nonnull Class<? extends ModuleExtension> clazz);

  @Nonnull
  public abstract <T extends ModuleExtension<T>> Collection<T> getModuleExtensions(@Nonnull Class<T> clazz);
}
