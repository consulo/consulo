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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.pico.AssignableToComponentAdapter;
import com.intellij.util.pico.CachingConstructorInjectionComponentAdapter;
import consulo.injecting.PostInjectListener;
import consulo.injecting.key.InjectingKey;
import org.picocontainer.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2018-08-23
 */
public class BaseComponentAdapter<T> implements AssignableToComponentAdapter {
  private static final Logger LOGGER = Logger.getInstance(BaseComponentAdapter.class);

  private ComponentAdapter myDelegate;

  private final InjectingKey<T> myInterfaceKey;
  private InjectingKey<? extends T> myImplementationKey;

  @Nullable
  private PostInjectListener<T> myAfterInjectionListener;

  private Function<Provider<T>, T> myRemap = Provider::get;

  private volatile T myInstanceIfSingleton;

  private boolean myForceSingleton;

  public BaseComponentAdapter(InjectingKey<T> interfaceKey) {
    myInterfaceKey = interfaceKey;
    myImplementationKey = interfaceKey;
    myDelegate = null;
  }

  public void setAfterInjectionListener(@Nullable PostInjectListener<T> afterInjectionListener) {
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

  @Override
  @SuppressWarnings("unchecked")
  public Object getComponentInstance(@Nonnull PicoContainer container) throws PicoInitializationException, PicoIntrospectionException {
    T instance = myInstanceIfSingleton;
    if (instance != null) {
      return instance;
    }

    ComponentAdapter delegate = getDelegate();
    synchronized (this) {
      // double check
      instance = myInstanceIfSingleton;
      if (instance != null) {
        return instance;
      }

      boolean isAnnotationSingleton = myImplementationKey.getTargetClass().isAnnotationPresent(Singleton.class);

      boolean isSingleton = myForceSingleton || isAnnotationSingleton;

      long l = System.nanoTime();

      instance = myRemap.apply(() -> (T)delegate.getComponentInstance(container));

      if (myAfterInjectionListener != null) {
        myAfterInjectionListener.afterInject(l, instance);
      }

      if (isSingleton) {
        myInstanceIfSingleton = instance;
      }

      if (isSingleton && !isAnnotationSingleton) {
        LOGGER.warn("Class " + myImplementationKey.getTargetClass().getName() + " is not annotated by @Singleton");
      }
    }

    return instance;
  }

  @Nonnull
  public synchronized ComponentAdapter getDelegate() {
    if (myDelegate == null) {
      myDelegate = new CachingConstructorInjectionComponentAdapter(getComponentKey(), getComponentImplementation(), null, true);
    }
    return myDelegate;
  }

  @Override
  public void verify(final PicoContainer container) throws PicoIntrospectionException {
    getDelegate().verify(container);
  }

  @Override
  public void accept(final PicoVisitor visitor) {
    visitor.visitComponentAdapter(this);
  }

  @Override
  public String getAssignableToClassName() {
    return myImplementationKey.getTargetClassName();
  }

  @Override
  public String toString() {
    return "LazyComponentAdapter[" + myInterfaceKey.getTargetClassName() + "]";
  }
}