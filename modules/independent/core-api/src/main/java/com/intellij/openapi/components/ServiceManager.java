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
package com.intellij.openapi.components;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.extensions.AreaInstance;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NotNullLazyKey;
import com.intellij.util.NotNullFunction;

import javax.annotation.Nonnull;

/**
 * For old-style components, the contract specifies a lifecycle: the component gets created and notified during the project opening process.
 * For services, there's no such contract, so we don't even load the class implementing the service until someone requests it.
 */
public final class ServiceManager {
  private ServiceManager() { }

  public static <T> T getService(@Nonnull Class<T> serviceClass) {
    return Application.get().getInjector().getInstance(serviceClass);
  }

  public static <T> T getService(@Nonnull AreaInstance instance, @Nonnull Class<T> serviceClass) {
    return instance.getInjector().getInstance(serviceClass);
  }

  /**
   * Creates lazy caching key to store project-level service instance from {@link #getService(Project, Class)}.
   *
   * @param serviceClass Service class to create key for.
   * @param <T>          Service class type.
   * @return Key instance.
   */
  public static <T> NotNullLazyKey<T, Project> createLazyKey(@Nonnull final Class<T> serviceClass) {
    return NotNullLazyKey.create("Service: " + serviceClass.getName(), new NotNullFunction<Project, T>() {
      @Override
      @Nonnull
      public T fun(Project project) {
        return getService(project, serviceClass);
      }
    });
  }
}
