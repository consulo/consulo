/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ide;

import consulo.annotation.DeprecationInfo;
import consulo.application.Application;
import consulo.component.ComponentManager;
import consulo.module.Module;
import consulo.project.Project;
import consulo.util.dataholder.NotNullLazyKey;

import jakarta.annotation.Nonnull;

@Deprecated
@DeprecationInfo("Use ComponentManager#getInstance() method")
public class ServiceManager {
  private ServiceManager() {
  }

  @Nonnull
  public static <T> T getService(@Nonnull Class<T> serviceClass) {
    return getService(Application.get(), serviceClass);
  }

  @Nonnull
  public static <T> T getService(@Nonnull Project project, @Nonnull Class<T> serviceClass) {
    return getService((ComponentManager)project, serviceClass);
  }

  @Nonnull
  public static <T> T getService(@Nonnull Module module, @Nonnull Class<T> serviceClass) {
    return getService((ComponentManager)module, serviceClass);
  }

  @Nonnull
  public static <T> T getService(@Nonnull ComponentManager manager, @Nonnull Class<T> serviceClass) {
    return manager.getInstance(serviceClass);
  }

  /**
   * Creates lazy caching key to store project-level service instance from {@link #getService(Project, Class)}.
   *
   * @param serviceClass Service class to create key for.
   * @param <T>          Service class type.
   * @return Key instance.
   */
  @Nonnull
  @Deprecated
  @DeprecationInfo("Use ComponentUtil.createLazyInject")
  public static <T> NotNullLazyKey<T, Project> createLazyKey(@Nonnull Class<T> serviceClass) {
    return NotNullLazyKey.create("Service: " + serviceClass.getName(), project -> getService(project, serviceClass));
  }
}
