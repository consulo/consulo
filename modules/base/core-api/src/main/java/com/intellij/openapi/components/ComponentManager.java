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
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.extensions.ExtensionsArea;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.UserDataHolder;
import com.intellij.util.messages.MessageBus;
import consulo.injecting.InjectingContainer;
import consulo.injecting.InjectingContainerOwner;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * Provides access to components. Serves as a base interface for {@link Application}
 * and {@link Project}.
 *
 * @see Application
 * @see Project
 */
public interface ComponentManager extends UserDataHolder, Disposable, InjectingContainerOwner {
  default void initNotLazyServices() {
    throw new UnsupportedOperationException();
  }

  default int getNotLazyServicesCount() {
    throw new UnsupportedOperationException();
  }

  @Override
  @Nonnull
  default InjectingContainer getInjectingContainer() {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  default ExtensionsArea getExtensionsArea() {
    throw new UnsupportedOperationException();
  }

  /**
   * Gets the component by its interface class.
   *
   * @param clazz the interface class of the component
   * @return component that matches interface class or null if there is no such component
   */
  default <T> T getComponent(@Nonnull Class<T> clazz) {
    return getInjectingContainer().getInstance(clazz);
  }

  @Nonnull
  MessageBus getMessageBus();

  boolean isDisposed();

  default boolean isDisposedOrDisposeInProgress() {
    return isDisposed();
  }

  @Nonnull
  @Deprecated
  default <T> T[] getExtensions(@Nonnull ExtensionPointName<T> extensionPointName) {
    return getExtensionsArea().getExtensionPoint(extensionPointName).getExtensions();
  }

  @Nonnull
  default <T> List<T> getExtensionList(@Nonnull ExtensionPointName<T> extensionPointName) {
    return getExtensionsArea().getExtensionPoint(extensionPointName).getExtensionList();
  }

  @Nullable
  default <T, K extends T> K findExtension(@Nonnull ExtensionPointName<T> extensionPointName, @Nonnull Class<K> extensionClass) {
    return getExtensionsArea().getExtensionPoint(extensionPointName).findExtension(extensionClass);
  }

  /**
   * @return condition for this component being disposed.
   * see {@link Application#invokeLater(Runnable, Condition)} for the usage example.
   */
  @Nonnull
  Condition getDisposed();
}
