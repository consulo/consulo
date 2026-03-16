/*
 * Copyright 2013-2023 consulo.io
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
package consulo.application.impl.internal.concurent;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.concurrent.ApplicationConcurrency;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.util.concurrent.coroutine.CoroutineContext;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @author VISTALL
 * @since 13/09/2023
 */
@Singleton
@ServiceImpl
public class ApplicationConcurrencyImpl implements ApplicationConcurrency {
    private final AppScheduledExecutorService myScheduledExecutorService;

    private final CoroutineContext myCoroutineContext;

    @Inject
    public ApplicationConcurrencyImpl(Application application) {
        myScheduledExecutorService = new AppScheduledExecutorService("Global Instance", this);
        myCoroutineContext = new CoroutineContext(myScheduledExecutorService, myScheduledExecutorService);
        myCoroutineContext.putCopyableUserData(Application.KEY, application);
    }

    
    @Override
    public CoroutineContext coroutineContext() {
        return myCoroutineContext;
    }

    
    @Override
    public Executor executor() {
        return myScheduledExecutorService.getBackendExecutorService();
    }

    
    @Override
    public ExecutorService getExecutorService() {
        return myScheduledExecutorService.getBackendExecutorService();
    }

    
    @Override
    public ScheduledExecutorService getScheduledExecutorService() {
        return myScheduledExecutorService;
    }

    
    @Override
    public ScheduledExecutorService createBoundedScheduledExecutorService(String name, int maxThreads) {
        AppDelayQueue delayQueue = ((AppScheduledExecutorService) getScheduledExecutorService()).getDelayQueue();
        return new BoundedScheduledExecutorService(name, getExecutorService(), maxThreads, delayQueue);
    }

    @Override
    
    public ExecutorService createBoundedApplicationPoolExecutor(String name,
                                                                Executor backendExecutor,
                                                                int maxThreads) {
        return new BoundedTaskExecutor(name, backendExecutor, maxThreads, true);
    }

    
    @Override
    public ExecutorService createBoundedApplicationPoolExecutor(String name, int maxThreads, Disposable parentDisposable) {
        return createBoundedApplicationPoolExecutor(name, getExecutorService(), maxThreads, parentDisposable);
    }

    @Override
    
    public ExecutorService createBoundedApplicationPoolExecutor(String name,
                                                                Executor backendExecutor,
                                                                int maxThreads,
                                                                Disposable parentDisposable) {
        BoundedTaskExecutor executor = new BoundedTaskExecutor(name, backendExecutor, maxThreads, true);
        Disposer.register(parentDisposable, executor::shutdownNow);
        return executor;
    }
}
