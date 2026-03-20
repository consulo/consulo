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

import org.jspecify.annotations.Nullable;

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
    
    default InjectingContainer getInjectingContainer() {
        throw new UnsupportedOperationException();
    }

    
    default <T> T getInstance(Class<T> clazz) {
        return getInjectingContainer().getInstance(clazz);
    }

    default @Nullable <T> T getInstanceIfCreated(Class<T> clazz) {
        return getInjectingContainer().getInstanceIfCreated(clazz);
    }

    
    default <T> T getUnbindedInstance(Class<T> clazz) {
        return getInjectingContainer().getUnbindedInstance(clazz);
    }

    
    default <T> T getUnbindedInstance(
        Class<T> clazz,
        Type[] constructorTypes,
        Function<Object[], T> constructor
    ) {
        return getInjectingContainer().getUnbindedInstance(clazz, constructorTypes, constructor);
    }

    @Deprecated
    @DeprecationInfo("JetBrains version")
    default <T> T getService(Class<T> clazz) {
        return getInstance(clazz);
    }

    @Deprecated
    default <T> T getComponent(Class<T> clazz) {
        return getInstance(clazz);
    }

    
    MessageBus getMessageBus();

    
    @Deprecated
    @DeprecationInfo("Prefer safe iteration via methods of ExtensionPoint")
    default <T> List<T> getExtensionList(Class<T> extensionPointName) {
        return getExtensionPoint(extensionPointName).getExtensionList();
    }

    
    default <T> ExtensionPoint<T> getExtensionPoint(Class<T> extensionClass) {
        throw new UnsupportedOperationException();
    }

    /**
     * @return condition for this component being disposed.
     * see {@link Application#invokeLater(Runnable, BooleanSupplier)} for the usage example.
     */
    BooleanSupplier getDisposed();

    
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

    default @Nullable ComponentManager getParent() {
        return null;
    }
}
