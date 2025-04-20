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

import consulo.annotation.DeprecationInfo;
import consulo.annotation.component.ComponentProfiles;
import consulo.component.extension.ExtensionPoint;
import consulo.component.internal.inject.InjectingContainer;
import consulo.component.internal.inject.InjectingContainerOwner;
import consulo.component.messagebus.MessageBus;
import consulo.disposer.Disposable;
import consulo.util.dataholder.UserDataHolder;
import consulo.util.lang.ThreeState;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.lang.reflect.Type;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Provides access to components. Serves as a base interface for {@link Application}
 * and {@link Project}.
 *
 * @see Application
 * @see Project
 * @see Module
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

    @Nonnull
    default <T> T getUnbindedInstance(
        @Nonnull Class<T> clazz,
        @Nonnull Type[] constructorTypes,
        @Nonnull Function<Object[], T> constructor
    ) {
        return getInjectingContainer().getUnbindedInstance(clazz, constructorTypes, constructor);
    }

    @Deprecated
    @DeprecationInfo("JetBrains version")
    default <T> T getService(@Nonnull Class<T> clazz) {
        return getInstance(clazz);
    }

    @Deprecated
    default <T> T getComponent(@Nonnull Class<T> clazz) {
        return getInstance(clazz);
    }

    @Nonnull
    MessageBus getMessageBus();


    @Nonnull
    default <T> List<T> getExtensionList(@Nonnull Class<T> extensionPointName) {
        return getExtensionPoint(extensionPointName).getExtensionList();
    }

    @Nonnull
    default <T> ExtensionPoint<T> getExtensionPoint(@Nonnull Class<T> extensionClass) {
        throw new UnsupportedOperationException();
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

    default int getProfiles() {
        return ComponentProfiles.DEFAULT;
    }

    @Nullable
    default ComponentManager getParent() {
        return null;
    }
}
