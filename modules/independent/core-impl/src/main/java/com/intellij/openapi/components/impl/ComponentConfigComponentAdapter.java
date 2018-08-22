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

import com.intellij.diagnostic.PluginException;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.BaseComponent;
import com.intellij.openapi.components.ComponentConfig;
import com.intellij.openapi.components.StateStorageException;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.util.Disposer;
import com.intellij.util.pico.CachingConstructorInjectionComponentAdapter;
import org.picocontainer.*;

class ComponentConfigComponentAdapter implements ComponentAdapter {
  private static final Logger LOG = Logger.getInstance(ComponentConfigComponentAdapter.class);

  private final ComponentManagerImpl myComponentManager;
  private final ComponentConfig myConfig;
  private final ComponentAdapter myDelegate;
  private boolean myInitialized = false;
  private boolean myInitializing = false;

  public ComponentConfigComponentAdapter(ComponentManagerImpl componentManager, final ComponentConfig config, Class<?> implementationClass) {
    myComponentManager = componentManager;
    myConfig = config;

    final String componentKey = config.getInterfaceClass();
    myDelegate = new CachingConstructorInjectionComponentAdapter(componentKey, implementationClass, null, true) {
      @Override
      public Object getComponentInstance(PicoContainer picoContainer) throws PicoInitializationException, PicoIntrospectionException, ProcessCanceledException {
        ProgressIndicator indicator = ComponentManagerImpl.getProgressIndicator();
        if (indicator != null) {
          indicator.checkCanceled();
        }

        Object componentInstance = null;
        try {
          long startTime = myInitialized ? 0 : System.nanoTime();

          componentInstance = super.getComponentInstance(picoContainer);

          if (!myInitialized) {
            if (myInitializing) {
              if (myConfig.pluginDescriptor != null) {
                LOG.error(new PluginException("Cyclic component initialization: " + componentKey, myConfig.pluginDescriptor.getPluginId()));
              }
              else {
                LOG.error(new Throwable("Cyclic component initialization: " + componentKey));
              }
            }

            try {
              myInitializing = true;

              if (componentInstance instanceof Disposable) {
                Disposer.register(myComponentManager, (Disposable)componentInstance);
              }

              boolean isStorableComponent = myComponentManager.initializeIfStorableComponent(componentInstance, false, false);

              if (componentInstance instanceof BaseComponent) {
                try {
                  ((BaseComponent)componentInstance).initComponent();

                  if (!isStorableComponent) {
                    LOG.warn("Not storable component implement initComponent() method, which can moved to constructor, component: " + componentInstance.getClass().getName());
                  }
                }
                catch (BaseComponent.DefaultImplException ignored) {
                  // skip default impl
                }
              }

              long ms = (System.nanoTime() - startTime) / 1000000;
              if (ms > 10 && myComponentManager.logSlowComponents()) {
                LOG.info(componentInstance.getClass().getName() + " initialized in " + ms + " ms");
              }
            }
            finally {
              myInitializing = false;
            }

            myInitialized = true;
          }
        }
        catch (ProcessCanceledException | StateStorageException e) {
          throw e;
        }
        catch (Throwable t) {
          myComponentManager.handleInitComponentError(t, componentKey, config);
        }

        return componentInstance;
      }
    };
  }

  @Override
  public Object getComponentKey() {
    return myConfig.getInterfaceClass();
  }

  @Override
  public Class getComponentImplementation() {
    return myDelegate.getComponentImplementation();
  }

  @Override
  public Object getComponentInstance(final PicoContainer container) throws PicoInitializationException, PicoIntrospectionException {
    return myDelegate.getComponentInstance(container);
  }

  @Override
  public void verify(final PicoContainer container) throws PicoIntrospectionException {
    myDelegate.verify(container);
  }

  @Override
  public void accept(final PicoVisitor visitor) {
    visitor.visitComponentAdapter(this);
    myDelegate.accept(visitor);
  }

  @Override
  public String toString() {
    return "ComponentConfigAdapter[" + getComponentKey() + "]: implementation=" + getComponentImplementation() + ", plugin=" + myConfig.getPluginId();
  }
}
