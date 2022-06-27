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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;

/**
 * @author VISTALL
 * @since 7:57/12.11.13
 */
@ServiceAPI(ComponentScope.PROJECT)
public interface ModuleExtensionHelper {
  @Nonnull
  public static ModuleExtensionHelper getInstance(@Nonnull Project project) {
    return project.getInstance(ModuleExtensionHelper.class);
  }

  public boolean hasModuleExtension(@Nonnull Class<? extends ModuleExtension> clazz);

  @Nonnull
  public <T extends ModuleExtension<T>> Collection<T> getModuleExtensions(@Nonnull Class<T> clazz);

  @Nonnull
  public String getModuleExtensionName(@Nonnull ModuleExtension<?> moduleExtension);

  @Nullable
  public Image getModuleExtensionIcon(@Nonnull String extensionId);
}
