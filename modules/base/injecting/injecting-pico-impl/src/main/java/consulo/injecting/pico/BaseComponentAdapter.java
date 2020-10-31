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
package consulo.injecting.pico;

import consulo.injecting.InjectingContainer;
import consulo.injecting.PostInjectListener;
import consulo.injecting.key.InjectingKey;
import consulo.logging.Logger;
import consulo.util.lang.ControlFlowException;
import consulo.util.lang.ExceptionUtil;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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

  @Nonnull
  private PostInjectListener<T> myAfterInjectionListener = (time, object) -> {
  };

  private Function<Provider<T>, T> myRemap = Provider::get;

  private T myInstanceIfSingleton;

  private boolean myForceSingleton;

  private volatile String myCreationTrace;

  public BaseComponentAdapter(InjectingKey<T> interfaceKey) {
    myInterfaceKey = interfaceKey;
    myImplementationKey = interfaceKey;
  }

  public void setAfterInjectionListener(@Nonnull PostInjectListener<T> afterInjectionListener) {
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

  @Override
  public String getComponentKey() {
    return myInterfaceKey.getTargetClassName();
  }

  @Override
  public Class getComponentImplementation() {
    return myImplementationKey.getTargetClass();
  }

  @Nullable
  @Override
  public T getComponentInstanceOfCreated(DefaultPicoContainer container) {
    T instance = myInstanceIfSingleton;
    if (instance != null) {
      return instance;
    }

    return null;
  }

  @Override
  public T getComponentInstance(@Nonnull DefaultPicoContainer container) throws PicoInitializationException, PicoIntrospectionException {
    T instance = myInstanceIfSingleton;
    if (instance != null) {
      return instance;
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
        ConstructorInjectionComponentAdapter<T> delegate = new ConstructorInjectionComponentAdapter<T>(getComponentKey(), getComponentImplementation());
        instance = myRemap.apply(() -> GetInstanceValidator.createObject(targetClass, () -> (T)delegate.getComponentInstance(container)));

        try {
          myAfterInjectionListener.afterInject(l, instance);
        }
        catch (Throwable t) {
          if(t instanceof ControlFlowException) {
            throw t;
          }

          LOG.error("Problem with after inject: " + targetClass.getName(), t);
        }
      }
      catch (Throwable t) {
        if(t instanceof ControlFlowException) {
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