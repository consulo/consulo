/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.openapi.components.impl;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceDescriptor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.openapi.util.Disposer;
import com.intellij.util.pico.AssignableToComponentAdapter;
import com.intellij.util.pico.CachingConstructorInjectionComponentAdapter;
import org.picocontainer.*;

import javax.annotation.Nonnull;

/**
 * from ServiceManagerImpl
 */
public class ServiceComponentAdapter implements AssignableToComponentAdapter {
  private static final Logger LOGGER = Logger.getInstance(ServiceComponentAdapter.class);

  private ComponentAdapter myDelegate;
  private final ServiceDescriptor myDescriptor;
  private final PluginDescriptor myPluginDescriptor;
  private final ComponentManagerImpl myComponentManager;
  private volatile Object myInitializedComponentInstance = null;

  public ServiceComponentAdapter(final ServiceDescriptor descriptor, final PluginDescriptor pluginDescriptor, ComponentManagerImpl componentManager) {
    myDescriptor = descriptor;
    myPluginDescriptor = pluginDescriptor;
    myComponentManager = componentManager;
    myDelegate = null;
  }

  public PluginDescriptor getPluginDescriptor() {
    return myPluginDescriptor;
  }

  public ServiceDescriptor getDescriptor() {
    return myDescriptor;
  }

  @Override
  public String getComponentKey() {
    return myDescriptor.getInterface();
  }

  @Override
  public Class getComponentImplementation() {
    return loadClass(myDescriptor.getInterface());
  }

  private Class loadClass(final String className) {
    try {
      final ClassLoader classLoader = myPluginDescriptor != null ? myPluginDescriptor.getPluginClassLoader() : getClass().getClassLoader();

      return Class.forName(className, true, classLoader);
    }
    catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Object getComponentInstance(@Nonnull PicoContainer container) throws PicoInitializationException, PicoIntrospectionException {
    Object oldInstance = myInitializedComponentInstance;
    if (oldInstance != null) {
      return oldInstance;
    }

    synchronized (this) {
      oldInstance = myInitializedComponentInstance;
      if (oldInstance != null) {
        // DCL is fine, field is volatile
        return oldInstance;
      }

      ComponentAdapter delegate = getDelegate();

      if (LOGGER.isDebugEnabled() &&
          ApplicationManager.getApplication().isWriteAccessAllowed() &&
          !ApplicationManager.getApplication().isUnitTestMode() &&
          PersistentStateComponent.class.isAssignableFrom(delegate.getComponentImplementation())) {
        LOGGER.warn(new Throwable("Getting service from write-action leads to possible deadlock. Service implementation " + myDescriptor.getImplementation()));
      }

      return myComponentManager.runServiceInitialize(myDescriptor, () -> {
        Object instance = delegate.getComponentInstance(container);
        if (instance instanceof Disposable) {
          Disposer.register(myComponentManager, (Disposable)instance);
        }

        myComponentManager.initializeIfStorableComponent(instance, true);

        myInitializedComponentInstance = instance;
        return instance;
      });
    }
  }

  public ComponentAdapter getDelegateWithoutInitialize() {
    return myDelegate;
  }

  @Nonnull
  private synchronized ComponentAdapter getDelegate() {
    if (myDelegate == null) {
      Class<?> implClass;
      try {
        ClassLoader classLoader = myPluginDescriptor != null ? myPluginDescriptor.getPluginClassLoader() : getClass().getClassLoader();
        implClass = Class.forName(myDescriptor.getImplementation(), true, classLoader);
      }
      catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
      }

      myDelegate = new CachingConstructorInjectionComponentAdapter(getComponentKey(), implClass, null, true);
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
    return myDescriptor.getInterface();
  }

  @Override
  public String toString() {
    return "ServiceComponentAdapter[" + myDescriptor.getInterface() + "]: implementation=" + myDescriptor.getImplementation() + ", plugin=" + myPluginDescriptor;
  }
}