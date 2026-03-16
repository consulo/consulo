/*
 * Copyright 2013-2018 consulo.io
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
package consulo.test.light.impl;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ComponentScope;
import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.event.ApplicationListener;
import consulo.application.progress.ProgressIndicatorProvider;
import consulo.application.progress.ProgressManager;
import consulo.component.ComponentManager;
import consulo.component.bind.InjectingBinding;
import consulo.component.impl.internal.BaseComponentManager;
import consulo.component.internal.ComponentBinding;
import consulo.component.internal.inject.InjectingContainer;
import consulo.component.internal.inject.InjectingContainerBuilder;
import consulo.disposer.Disposable;
import consulo.ui.ModalityState;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.util.collection.MultiMap;
import consulo.util.concurrent.coroutine.Continuation;
import consulo.util.lang.function.ThrowableSupplier;
import consulo.util.lang.ref.SimpleReference;
import consulo.virtualFileSystem.encoding.ApplicationEncodingManager;
import consulo.virtualFileSystem.encoding.EncodingRegistry;
import consulo.virtualFileSystem.fileType.FileTypeRegistry;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2018-08-25
 */
public class LightApplication extends BaseComponentManager implements Application {
    private final ComponentBinding myComponentBinding;
    private final LightExtensionRegistrator myRegistrator;

    private final ProgressManager myProgressManager;

    public LightApplication(Disposable lastDisposable, ComponentBinding componentBinding, LightExtensionRegistrator registrator) {
        super(null,
            "LightApplication",
            ComponentScope.APPLICATION,
            componentBinding,
            false);
        myComponentBinding = componentBinding;
        myRegistrator = registrator;

        myProgressManager = new LightProgressManager();

        ApplicationManager.setApplication(this, lastDisposable);

        buildInjectingContainer();
    }

    public ComponentBinding getComponentBinding() {
        return myComponentBinding;
    }

    @Override
    public ProgressManager getProgressManager() {
        return myProgressManager;
    }

    @Override
    public int getProfiles() {
        return ComponentProfiles.LIGHT_TEST;
    }

    @Override
    protected void fillListenerDescriptors(MultiMap<String, InjectingBinding> mapByTopic) {
    }

    @Override
    public ComponentManager getApplication() {
        return this;
    }

    @Override
    protected InjectingContainer findRootContainer() {
        return InjectingContainer.root(getClass().getClassLoader());
    }

    @Override
    protected void bootstrapInjectingContainer(InjectingContainerBuilder builder) {
        super.bootstrapInjectingContainer(builder);

        builder.bind(Application.class).to(this);
        builder.bind(FileTypeRegistry.class).forceSingleton().to(LightFileTypeRegistry.class);
        builder.bind(EncodingRegistry.class).to(ApplicationEncodingManager::getInstance);
        builder.bind(ProgressIndicatorProvider.class).to(myProgressManager);
    }

    @Override
    protected void registerServices(InjectingContainerBuilder builder) {
        myRegistrator.registerServices(builder);
    }

    @Override
    public void runReadAction(Runnable action) {
        action.run();
    }

    @Override
    public <T> T runReadAction(Supplier<T> computation) {
        return computation.get();
    }

    @Override
    public boolean tryRunReadAction(Runnable action) {
        action.run();
        return true;
    }

    @Override
    public <T, E extends Throwable> boolean tryRunReadAction(SimpleReference<T> ref, ThrowableSupplier<T, E> computation) throws E {
        ref.set(computation.get());
        return true;
    }

    @RequiredUIAccess
    @Override
    public void runWriteAction(Runnable action) {
        throw new UnsupportedOperationException();
    }

    @RequiredUIAccess
    @Override
    public <T> T runWriteAction(Supplier<T> computation) {
        throw new UnsupportedOperationException();
    }

    @RequiredReadAction
    @Override
    public void assertReadAccessAllowed() {

    }

    @RequiredWriteAction
    @Override
    public void assertWriteAccessAllowed() {

    }

    @RequiredUIAccess
    @Override
    public void assertIsDispatchThread() {

    }

    @Override
    public void addApplicationListener(ApplicationListener listener) {

    }

    @Override
    public void addApplicationListener(ApplicationListener listener, Disposable parent) {

    }

    @Override
    public void removeApplicationListener(ApplicationListener listener) {

    }

    @RequiredUIAccess
    @Override
    public Continuation<Void> saveAll() {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<Void> saveAllWithProgress(UIAccess uiAccess) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void saveSettings() {

    }

    @Override
    public void exit() {

    }

    @Override
    public boolean isReadAccessAllowed() {
        return true;
    }

    @Override
    public boolean isDispatchThread() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isWriteThread() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void invokeLater(Runnable runnable) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void invokeLater(Runnable runnable, BooleanSupplier expired) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void invokeLater(Runnable runnable, ModalityState state) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void invokeLater(Runnable runnable, ModalityState state, BooleanSupplier expired) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void invokeAndWait(Runnable runnable, ModalityState modalityState) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getStartTime() {
        return 0;
    }

    @RequiredUIAccess
    @Override
    public long getIdleTime() {
        return 0;
    }

    @Override
    public boolean isHeadlessEnvironment() {
        return true;
    }

    @Override
    public Future<?> executeOnPooledThread(Runnable action) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> Future<T> executeOnPooledThread(Callable<T> action) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDisposeInProgress() {
        return false;
    }

    @Override
    public boolean isRestartCapable() {
        return false;
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public Image getIcon() {
        throw new UnsupportedOperationException();
    }

    @Override
    public UIAccess getLastUIAccess() {
        throw new UnsupportedOperationException();
    }

    @RequiredUIAccess
    @Override
    public <T, E extends Throwable> T runWriteAction(ThrowableSupplier<T, E> computation) throws E {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasWriteAction(Class<?> actionClass) {
        return false;
    }

    @Override
    public <T, E extends Throwable> T runReadAction(ThrowableSupplier<T, E> computation) throws E {
        return computation.get();
    }
}
