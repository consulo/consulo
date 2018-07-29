/*
 * Copyright 2000-2010 JetBrains s.r.o.
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

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.extensions.AreaInstance;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.UserDataHolder;
import com.intellij.util.messages.MessageBus;

import javax.annotation.Nonnull;

/**
 * Provides access to components. Serves as a base interface for {@link Application}
 * and {@link Project}.
 *
 * @see ProjectComponent
 * @see Application
 * @see Project
 */
public interface ComponentManager extends AreaInstance, UserDataHolder, Disposable {

  /**
   * Gets the component by its interface class.
   *
   * @param interfaceClass the interface class of the component
   * @return component that matches interface class or null if there is no such component
   */
  <T> T getComponent(@Nonnull Class<T> interfaceClass);

  /**
   * Gets the component by its interface class but returns a specified default implementation
   * if the actual component doesn't exist in the container.
   *
   * @param interfaceClass the interface class of the component
   * @param defaultImplementationIfAbsent the default implementation
   * @return component that matches interface class or default if there is no such component
   */
  @Deprecated
  default <T> T getComponent(@Nonnull Class<T> interfaceClass, T defaultImplementationIfAbsent) {
    return getComponent(interfaceClass);
  }

  /**
   * Checks whether there is a component with the specified interface class.
   *
   * @param interfaceClass interface class of component to be checked
   * @return <code>true</code> if there is a component with the specified interface class;
   * <code>false</code> otherwise
   */
  boolean hasComponent(@Nonnull Class interfaceClass);

  /**
   * Gets all components whose implementation class is derived from <code>baseClass</code>.
   *
   * @return array of components
   * @deprecated use extension points instead
   */
  @Nonnull
  @Deprecated
  <T> T[] getComponents(@Nonnull Class<T> baseClass);

  @Nonnull
  MessageBus getMessageBus();

  boolean isDisposed();

  @Nonnull
  <T> T[] getExtensions(@Nonnull ExtensionPointName<T> extensionPointName);

  /**
   * @return condition for this component being disposed.
   * see {@link Application#invokeLater(Runnable, Condition)} for the usage example.
   */
  @Nonnull
  Condition getDisposed();
}
