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

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.components.ComponentConfig;
import com.intellij.openapi.components.ComponentManager;
import com.intellij.openapi.components.NamedComponent;
import com.intellij.openapi.components.ServiceDescriptor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.AreaInstance;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusFactory;
import consulo.application.ApplicationProperties;
import consulo.injecting.InjectingContainer;
import consulo.injecting.pico.PicoInjectingContainer;
import gnu.trove.THashMap;
import org.jetbrains.annotations.TestOnly;
import org.picocontainer.ComponentAdapter;
import org.picocontainer.Disposable;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.PicoContainer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * @author mike
 */
public abstract class ComponentManagerImpl extends UserDataHolderBase implements ComponentManager, Disposable {
  private static final Logger LOG = Logger.getInstance(ComponentManagerImpl.class);

  private InjectingContainer myInjectingContainer;

  private volatile boolean myDisposed = false;

  private MessageBus myMessageBus;

  private final ComponentManager myParentComponentManager;
  private ComponentsRegistry myComponentsRegistry = new ComponentsRegistry();
  private final Condition myDisposedCondition = o -> isDisposed();

  private boolean myComponentsCreated = false;
  private int myNotLazyServicesCount;

  protected ComponentManagerImpl(ComponentManager parentComponentManager) {
    myParentComponentManager = parentComponentManager;
    bootstrapPicoContainer(toString());
  }

  protected ComponentManagerImpl(ComponentManager parentComponentManager, @Nonnull String name) {
    myParentComponentManager = parentComponentManager;
    bootstrapPicoContainer(name);
  }

  @Override
  public void initNotLazyServices() {
    try {
      if (myComponentsCreated) {
        throw new IllegalArgumentException("Injector already build");
      }

      List<Class> notLazyServices = new ArrayList<>();

      IdeaPluginDescriptor[] plugins = PluginManagerCore.getPlugins();
      for (IdeaPluginDescriptor plugin : plugins) {
        if (!PluginManagerCore.shouldSkipPlugin(plugin)) {
          ComponentConfig[] componentConfigs = getComponentConfigs(plugin);

          for (ComponentConfig componentConfig : componentConfigs) {
            registerComponent(isProjectDefault(), componentConfig, plugin);
          }
        }
      }

      myComponentsRegistry.loadClasses(notLazyServices);

      loadServices(notLazyServices);

      myNotLazyServicesCount = notLazyServices.size();

      for (Class<?> componentInterface : notLazyServices) {
        ProgressIndicator indicator = getProgressIndicator();
        if (indicator != null) {
          indicator.checkCanceled();
        }

        Object component = getComponent(componentInterface);
        assert component != null;
      }
    }
    finally {
      myComponentsCreated = true;
    }
  }

  private void registerComponent(final boolean defaultProject, @Nonnull ComponentConfig config, final PluginDescriptor descriptor) {
    if (defaultProject && !config.isLoadForDefaultProject()) return;

    registerComponent(config, descriptor);
  }

  @Override
  public int getNotLazyServicesCount() {
    return myNotLazyServicesCount;
  }

  private boolean isProjectDefault() {
    return this instanceof Project && ((Project)this).isDefault();
  }

  @Nonnull
  protected ComponentConfig[] getComponentConfigs(IdeaPluginDescriptor ideaPluginDescriptor) {
    return ComponentConfig.EMPTY_ARRAY;
  }

  private void loadServices(List<Class> notLazyServices) {
    ExtensionPointName<ServiceDescriptor> ep = getServiceExtensionPointName();
    if (ep != null) {
      MutablePicoContainer picoContainer = getPicoContainer();
      ServiceDescriptor[] descriptors = this instanceof Application ? ep.getExtensions() : ep.getExtensions((AreaInstance)this);
      for (ServiceDescriptor descriptor : descriptors) {
        PluginDescriptor pluginDescriptor = descriptor.getPluginDescriptor();

        ServiceComponentAdapter adapter = new ServiceComponentAdapter(descriptor, pluginDescriptor, this);
        picoContainer.registerComponent(adapter);

        if (!descriptor.isLazy()) {
          // if service is not lazy - add it for init at start
          notLazyServices.add(adapter.getComponentImplementation());
        }
      }
    }
  }

  protected <T> T runServiceInitialize(@Nonnull ServiceDescriptor descriptor, @Nonnull Supplier<T> runnable) {
    return runnable.get();
  }

  @Nullable
  protected ExtensionPointName<ServiceDescriptor> getServiceExtensionPointName() {
    return null;
  }

  @Nonnull
  @Override
  public MessageBus getMessageBus() {
    if (myDisposed) {
      ProgressManager.checkCanceled();
      throw new AssertionError("Already disposed");
    }
    assert myMessageBus != null : "Not initialized yet";
    return myMessageBus;
  }

  public boolean isComponentsCreated() {
    return myComponentsCreated;
  }

  @Override
  public <T> T getComponent(@Nonnull Class<T> clazz) {
    if (myDisposed) {
      ProgressManager.checkCanceled();
      throw new AssertionError("Already disposed: " + this);
    }
    return getInjectingContainer().getInstance(clazz);
  }

  @Nullable
  protected static ProgressIndicator getProgressIndicator() {
    PicoContainer container = Application.get().getPicoContainer();
    ComponentAdapter adapter = container.getComponentAdapterOfType(ProgressManager.class);
    if (adapter == null) return null;
    ProgressManager progressManager = (ProgressManager)adapter.getComponentInstance(container);
    boolean isProgressManagerInitialized = progressManager != null;
    return isProgressManagerInitialized ? ProgressIndicatorProvider.getGlobalProgressIndicator() : null;
  }

  public boolean initializeIfStorableComponent(@Nonnull Object component, boolean service, boolean lazy) {
    return false;
  }

  protected void handleInitComponentError(@Nonnull Throwable ex, @Nullable String componentClassName, @Nullable ComponentConfig config) {
    LOG.error(ex);
  }

  public void registerComponent(ComponentConfig config, PluginDescriptor pluginDescriptor) {
    if (!config.prepareClasses(isHeadless())) {
      return;
    }

    config.pluginDescriptor = pluginDescriptor;
    myComponentsRegistry.addConfig(config);
  }

  @TestOnly
  @SuppressWarnings("unchecked")
  public synchronized <T> T registerComponentInstance(@Nonnull Class<T> componentKey, @Nonnull T componentImplementation) {
    Object componentInstance = getPicoContainer().getComponentInstance(componentKey);

    getPicoContainer().unregisterComponent(componentKey.getName());
    getPicoContainer().registerComponentInstance(componentKey.getName(), componentImplementation);
    return (T)componentInstance;
  }

  @Override
  @Nonnull
  @Deprecated
  public MutablePicoContainer getPicoContainer() {
    InjectingContainer container = myInjectingContainer;
    if (container == null || myDisposed) {
      ProgressManager.checkCanceled();
      throw new AssertionError("Already disposed: " + toString());
    }
    return ((PicoInjectingContainer)container).getContainer();
  }

  @Nonnull
  @Override
  public InjectingContainer getInjectingContainer() {
    InjectingContainer container = myInjectingContainer;
    if (container == null || myDisposed) {
      ProgressManager.checkCanceled();
      throw new AssertionError("Already disposed: " + toString());
    }
    return container;
  }

  @Nonnull
  protected InjectingContainer createInjectingContainer() {
    return new PicoInjectingContainer(myParentComponentManager == null ? null : (PicoInjectingContainer)myParentComponentManager.getInjectingContainer());
  }

  @Override
  public synchronized void dispose() {
    Application.get().assertIsDispatchThread();

    if (myMessageBus != null) {
      myMessageBus.dispose();
      myMessageBus = null;
    }

    myInjectingContainer.dispose();
    myInjectingContainer = null;

    myComponentsRegistry = null;
    myComponentsCreated = false;
    myNotLazyServicesCount = 0;
    myDisposed = true;
  }

  @Override
  public boolean isDisposed() {
    return myDisposed || temporarilyDisposed;
  }

  protected volatile boolean temporarilyDisposed = false;

  @TestOnly
  public void setTemporarilyDisposed(boolean disposed) {
    temporarilyDisposed = disposed;
  }

  protected void bootstrapPicoContainer(@Nonnull String name) {
    myInjectingContainer = createInjectingContainer();

    myMessageBus = MessageBusFactory.newMessageBus(name, myParentComponentManager == null ? null : myParentComponentManager.getMessageBus());
  }

  protected ComponentManager getParentComponentManager() {
    return myParentComponentManager;
  }

  private static class HeadlessHolder {
    private static final boolean myHeadless = Application.get().isHeadlessEnvironment();
  }

  private boolean isHeadless() {
    return HeadlessHolder.myHeadless;
  }

  @Nullable
  public Object getComponent(final ComponentConfig componentConfig) {
    return getPicoContainer().getComponentInstance(componentConfig.getInterfaceClass());
  }

  public ComponentConfig getConfig(Class componentImplementation) {
    return myComponentsRegistry.getConfig(componentImplementation);
  }

  @Override
  @Nonnull
  public Condition getDisposed() {
    return myDisposedCondition;
  }

  @Nonnull
  public static String getComponentName(@Nonnull final Object component) {
    if (component instanceof NamedComponent) {
      return ((NamedComponent)component).getComponentName();
    }
    else {
      return component.getClass().getName();
    }
  }

  protected boolean logSlowComponents() {
    return LOG.isDebugEnabled() || ApplicationProperties.isInSandbox();
  }

  protected class ComponentsRegistry {
    private final List<ComponentConfig> myComponentConfigs = new ArrayList<>();
    private final Map<Class, ComponentConfig> myComponentClassToConfig = new THashMap<>();

    private void loadClasses(List<Class> initAfter) {
      for (ComponentConfig config : myComponentConfigs) {
        loadClasses(config, initAfter);
      }
    }

    private void loadClasses(@Nonnull ComponentConfig config, List<Class> notLazyServices) {
      ClassLoader loader = config.getClassLoader();

      try {
        final Class<?> interfaceClass = Class.forName(config.getInterfaceClass(), true, loader);
        final Class<?> implementationClass =
                Comparing.equal(config.getInterfaceClass(), config.getImplementationClass()) ? interfaceClass : Class.forName(config.getImplementationClass(), true, loader);

        getPicoContainer().registerComponent(new ComponentConfigComponentAdapter(ComponentManagerImpl.this, config, implementationClass));
        myComponentClassToConfig.put(implementationClass, config);

        notLazyServices.add(interfaceClass);
      }
      catch (Throwable t) {
        handleInitComponentError(t, null, config);
      }
    }

    private void addConfig(ComponentConfig config) {
      myComponentConfigs.add(config);
    }

    public ComponentConfig getConfig(final Class componentImplementation) {
      return myComponentClassToConfig.get(componentImplementation);
    }
  }
}
