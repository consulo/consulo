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
package consulo.component.impl.internal.inject;

import consulo.component.internal.inject.InjectingContainer;
import consulo.component.persist.PersistentStateComponentAsync;
import consulo.component.internal.inject.InjectingKey;
import consulo.component.internal.inject.PostInjectListener;
import consulo.logging.Logger;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineContext;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.util.concurrent.coroutine.step.CallSubroutine;
import consulo.util.concurrent.coroutine.step.CodeExecution;
import consulo.util.lang.ControlFlowException;
import consulo.util.lang.ExceptionUtil;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import org.jspecify.annotations.Nullable;
import java.lang.reflect.Type;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2018-08-23
 */
class BaseComponentAdapter<T> implements ComponentAdapter<T> {
  private static final Logger LOG = Logger.getInstance(BaseComponentAdapter.class);

  private final InjectingKey<T> myInterfaceKey;

  private final Object myLock = new Object();

  private InjectingKey<? extends T> myImplementationKey;

  private Type[] myConstructorParameterTypes;

  private Function<Object[], T> myConstructorFactory;

  
  private PostInjectListener<T> myAfterInjectionListener = (time, object) -> {
  };

  private Function<Provider<T>, T> myRemap = Provider::get;

  private volatile T myInstanceIfSingleton;

  private volatile CompletableFuture<T> myInstanceFuture;

  private boolean myForceSingleton;

  private volatile String myCreationTrace;

  public BaseComponentAdapter(InjectingKey<T> interfaceKey) {
    myInterfaceKey = interfaceKey;
    myImplementationKey = interfaceKey;
  }

  public void setAfterInjectionListener(PostInjectListener<T> afterInjectionListener) {
    myAfterInjectionListener = afterInjectionListener;
  }

  public void setRemap(Function<Provider<T>, T> remap) {
    myRemap = remap;
  }

  public void setImplementationKey(InjectingKey<? extends T> implementationKey) {
    myImplementationKey = implementationKey;
  }

  public void setForceSingleton() {
    myForceSingleton = true;
  }

  public void setConstructorParameterTypes(Type[] constructorParameterTypes) {
    myConstructorParameterTypes = constructorParameterTypes;
  }

  public void setConstructorFactory(Function<Object[], T> constructorFactory) {
    myConstructorFactory = constructorFactory;
  }

  
  @Override
  public Class getComponentClass() {
    return myInterfaceKey.getTargetClass();
  }

  @Override
  public Class getComponentImplClass() {
    return myImplementationKey.getTargetClass();
  }

  @Override
  public Class<?> getComponentImplClassIfCheap() {
    return myImplementationKey.getTargetClass();
  }

  @Override
  public @Nullable T getComponentInstanceOfCreated(InstanceContainer container) {
    T instance = myInstanceIfSingleton;
    if (instance != null) {
      return instance;
    }

    return null;
  }

  /**
   * Creates the instance without blocking the caller. An already created singleton is returned as a completed
   * future, so no coroutine is started; otherwise one chain does construct then post inject then publish.
   * Concurrent callers share the memoised future, so the instance is created exactly once.
   */
  @Override
  public CompletableFuture<T> getComponentInstanceAsync(InstanceContainer container) {
    T instance = myInstanceIfSingleton;
    if (instance != null) {
      return CompletableFuture.completedFuture(instance);
    }

    synchronized (myLock) {
      instance = myInstanceIfSingleton;
      if (instance != null) {
        return CompletableFuture.completedFuture(instance);
      }

      CompletableFuture<T> future = myInstanceFuture;
      if (future == null) {
        myInstanceFuture = future = startAsyncCreation(container);
      }
      return future;
    }
  }

  @SuppressWarnings("unchecked")
  private CompletableFuture<T> startAsyncCreation(InstanceContainer container) {
    Class<? extends T> targetClass = myImplementationKey.getTargetClass();

    CoroutineContext context = container.getCoroutineContext();
    if (context == null) {
      // only a component loading its state through coroutines actually needs one
      if (PersistentStateComponentAsync.class.isAssignableFrom(targetClass)) {
        throw new PicoInitializationException("No coroutine context bound for async creation of " + targetClass.getName());
      }
      return CompletableFuture.completedFuture(getComponentInstance(container));
    }

    Object[] holder = new Object[1];
    long time = System.nanoTime();

    Coroutine<Object, Object> chain = Coroutine.<Object, Object>first(CodeExecution.apply(input -> {
        holder[0] = createInstance(container);
        return null;
      }))
      .then(CallSubroutine.call(() -> (Coroutine<Object, Object>)myAfterInjectionListener.afterInjectAsync(time, (T)holder[0])))
      .then(CodeExecution.apply(input -> publish((T)holder[0])));

    CompletableFuture<T> future = (CompletableFuture<T>)chain.runAsync(CoroutineScope.of(context), null).toFuture();
    // a failed creation must not be memoised, otherwise the service can never be created again
    return future.whenComplete((result, error) -> {
      if (error != null) {
        synchronized (myLock) {
          myInstanceFuture = null;
        }
      }
    });
  }

  private T createInstance(InstanceContainer container) {
    Class<? extends T> targetClass = myImplementationKey.getTargetClass();

    ConstructorInjectionComponentAdapter<T> delegate;
    if (myConstructorParameterTypes != null && myConstructorFactory != null) {
      delegate = new NewConstructorInjectionComponentAdapter<T>(getComponentClass(), getComponentImplClass(), myConstructorParameterTypes, myConstructorFactory);
    }
    else {
      delegate = new ConstructorInjectionComponentAdapter<T>(getComponentClass(), getComponentImplClass());
    }

    return myRemap.apply(() -> GetInstanceValidator.createObject(targetClass, () -> delegate.getComponentInstance(container)));
  }

  private @Nullable T publish(T instance) {
    Class<? extends T> targetClass = myImplementationKey.getTargetClass();
    if (myForceSingleton || targetClass.isAnnotationPresent(Singleton.class)) {
      synchronized (myLock) {
        myInstanceIfSingleton = instance;
        myInstanceFuture = null;
      }
    }
    return instance;
  }

  @Override
  public T getComponentInstance(InstanceContainer container) throws PicoInitializationException, PicoIntrospectionException {
    T instance = myInstanceIfSingleton;
    if (instance != null) {
      return instance;
    }

    // an async creation may already be in flight - join it rather than creating a second instance
    CompletableFuture<T> inFlight = myInstanceFuture;
    if (inFlight != null) {
      return inFlight.join();
    }

    boolean isSingleton, isAnnotationSingleton;
    Class<? extends T> targetClass = myImplementationKey.getTargetClass();
    synchronized (myLock) {
      // double check
      instance = myInstanceIfSingleton;
      if (instance != null) {
        return instance;
      }

      isAnnotationSingleton = targetClass.isAnnotationPresent(Singleton.class);

      isSingleton = myForceSingleton || isAnnotationSingleton;

      String creationTrace = myCreationTrace;
      if (creationTrace != null) {
        String currentTrace = exceptionText("current trace");
        LOG.error("Cycle initialization: " + targetClass.getName() + "\n" + currentTrace + ",\n\n" + creationTrace);
      }

      if (isSingleton) {
        myCreationTrace = exceptionText("creation trace");
      }

      long l = System.nanoTime();

      try {
        ConstructorInjectionComponentAdapter<T> delegate;
        if (myConstructorParameterTypes != null && myConstructorFactory != null) {
          delegate = new NewConstructorInjectionComponentAdapter<T>(getComponentClass(), getComponentImplClass(), myConstructorParameterTypes, myConstructorFactory);
        }
        else {
          delegate = new ConstructorInjectionComponentAdapter<T>(getComponentClass(), getComponentImplClass());
        }

        instance = myRemap.apply(() -> GetInstanceValidator.createObject(targetClass, () -> (T)delegate.getComponentInstance(container)));

        try {
          myAfterInjectionListener.afterInject(l, instance);
        }
        catch (Throwable t) {
          if (t instanceof ControlFlowException) {
            throw t;
          }

          LOG.error("Problem with after inject: " + targetClass.getName(), t);
        }
      }
      catch (Throwable t) {
        if (t instanceof ControlFlowException) {
          throw t;
        }
        LOG.error("Problem with initializing: " + targetClass.getName(), t);
      }
      finally {
        if (isSingleton) {
          myCreationTrace = null;
          myInstanceIfSingleton = instance;
        }
      }
    }

    if (InjectingContainer.LOG_INJECTING_PROBLEMS && isSingleton && !isAnnotationSingleton) {
      LOG.warn("Class " + targetClass.getName() + " is not annotated by @Singleton");
    }

    return instance;
  }

  private static String exceptionText(String id) {
    Thread thread = Thread.currentThread();
    return ExceptionUtil.getThrowableText(new Exception(id + ". Thread: " + thread));
  }

  @Override
  public String toString() {
    return "BaseComponentAdapter[" + myInterfaceKey.getTargetClassName() + "]";
  }
}