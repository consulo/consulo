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
package consulo.component;

import consulo.component.extension.ExtensionPoint;
import consulo.component.extension.ExtensionPointId;
import consulo.component.extension.ExtensionPointName;
import consulo.component.messagebus.MessageBus;
import consulo.disposer.Disposable;
import consulo.injecting.InjectingContainer;
import consulo.injecting.InjectingContainerOwner;
import consulo.util.dataholder.UserDataHolder;
import consulo.util.lang.ThreeState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

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
  default <T> T getInstance(@Nonnull Class<T> clazz) {
    return getInjectingContainer().getInstance(clazz);
  }

  @Nullable
  default <T> T getInstanceIfCreated(@Nonnull Class<T> clazz) {
    return getInjectingContainer().getInstanceIfCreated(clazz);
  }

  @Nonnull
  default <T> T getUnbindedInstance(@Nonnull Class<T> clazz) {
    return getInjectingContainer().getUnbindedInstance(clazz);
  }

  @Deprecated
  default <T> T getComponent(@Nonnull Class<T> clazz) {
    return getInstance(clazz);
  }

  @Nonnull
  MessageBus getMessageBus();

  @Nonnull
  @Deprecated
  default <T> T[] getExtensions(@Nonnull ExtensionPointName<T> extensionPointName) {
    return getExtensionPoint(extensionPointName).getExtensions();
  }

  @Nonnull
  default <T> List<T> getExtensionList(@Nonnull ExtensionPointName<T> extensionPointName) {
    return getExtensionPoint(extensionPointName).getExtensionList();
  }

  @Nonnull
  @Deprecated
  default <T> ExtensionPoint<T> getExtensionPoint(@Nonnull ExtensionPointName<T> extensionPointName) {
    return getExtensionPoint(extensionPointName.getId());
  }

  @Nonnull
  default <T> ExtensionPoint<T> getExtensionPoint(@Nonnull ExtensionPointId<T> extensionPointId) {
    throw new UnsupportedOperationException();
  }

  @Nullable
  default <T, K extends T> K findExtension(@Nonnull ExtensionPointName<T> extensionPointName, @Nonnull Class<K> extensionClass) {
    return getExtensionPoint(extensionPointName).findExtension(extensionClass);
  }

  /**
   * @return condition for this component being disposed.
   * see {@link Application#invokeLater(Runnable, BooleanSupplier)} for the usage example.
   */
  @Nonnull
  BooleanSupplier getDisposed();

  @Nonnull
  default Supplier<ThreeState> getDisposeState() {
    return () -> isDisposed() ? ThreeState.YES : ThreeState.NO;
  }

  default boolean isDisposed() {
    return getDisposeState().get() == ThreeState.YES;
  }

  @Deprecated
  default boolean isDisposedOrDisposeInProgress() {
    Supplier<ThreeState> disposeState = getDisposeState();
    return disposeState.get() != ThreeState.NO;
  }

  default boolean isInitialized() {
    return true;
  }
}
