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
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.*;
import com.intellij.openapi.components.ex.ComponentManagerEx;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.AreaInstance;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusFactory;
import com.intellij.util.pico.CachingConstructorInjectionComponentAdapter;
import com.intellij.util.pico.DefaultPicoContainer;
import consulo.application.ApplicationProperties;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.TestOnly;
import org.picocontainer.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * @author mike
 */
public abstract class ComponentManagerImpl extends UserDataHolderBase implements ComponentManagerEx, Disposable {
  private static final Logger LOG = Logger.getInstance(ComponentManagerImpl.class);

  private final Map<Class, Object> myInitializedComponents = ContainerUtil.newConcurrentMap();

  private boolean myComponentsCreated = false;

  private volatile MutablePicoContainer myPicoContainer;
  private volatile boolean myDisposed = false;
  private volatile boolean myDisposeCompleted = false;

  private MessageBus myMessageBus;

  private final ComponentManagerConfigurator myConfigurator = new ComponentManagerConfigurator(this);
  private final ComponentManager myParentComponentManager;
  private ComponentsRegistry myComponentsRegistry = new ComponentsRegistry();
  private final Condition myDisposedCondition = o -> isDisposed();

  protected ComponentManagerImpl(ComponentManager parentComponentManager) {
    myParentComponentManager = parentComponentManager;
    bootstrapPicoContainer(toString());
  }

  protected ComponentManagerImpl(ComponentManager parentComponentManager, @Nonnull String name) {
    myParentComponentManager = parentComponentManager;
    bootstrapPicoContainer(name);
  }

  public void init() {
    createComponents();
  }

  private void loadServices() {
    ExtensionPointName<ServiceDescriptor> ep = getServiceExtensionPointName();
    if (ep != null) {
      MutablePicoContainer picoContainer = getPicoContainer();
      ServiceDescriptor[] descriptors = this instanceof Application ? ep.getExtensions() : ep.getExtensions((AreaInstance)this);
      for (ServiceDescriptor descriptor : descriptors) {
        PluginDescriptor pluginDescriptor = descriptor.getPluginDescriptor();

        picoContainer.registerComponent(new ServiceComponentAdapter(descriptor, pluginDescriptor, this));

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
    if (myDisposeCompleted || myDisposed) {
      ProgressManager.checkCanceled();
      throw new AssertionError("Already disposed");
    }
    assert myMessageBus != null : "Not initialized yet";
    return myMessageBus;
  }

  public boolean isComponentsCreated() {
    return myComponentsCreated;
  }

  private void createComponents() {
    try {
      myComponentsRegistry.loadClasses();

      loadServices();

      Class[] componentInterfaces = myComponentsRegistry.getComponentInterfaces();
      for (Class componentInterface : componentInterfaces) {
        ProgressIndicator indicator = getProgressIndicator();
        if (indicator != null) {
          indicator.checkCanceled();
        }
        createComponent(componentInterface);
      }
    }
    finally {
      myComponentsCreated = true;
    }
  }

  protected synchronized Object createComponent(@Nonnull Class componentInterface) {
    final Object component = getPicoContainer().getComponentInstance(componentInterface.getName());
    LOG.assertTrue(component != null, "Can't instantiate component for: " + componentInterface);
    return component;
  }

  protected synchronized void disposeComponents() {
    assert !myDisposeCompleted : "Already disposed!";

    final List<Object> components = myComponentsRegistry.getRegisteredImplementations();
    myDisposed = true;

    for (int i = components.size() - 1; i >= 0; i--) {
      Object component = components.get(i);
      if (component instanceof BaseComponent) {
        try {
          ((BaseComponent)component).disposeComponent();
        }
        catch (Throwable e) {
          LOG.error(e);
        }
      }
    }

    myComponentsCreated = false;
  }

  @SuppressWarnings({"unchecked"})
  @Nullable
  protected <T> T getComponentFromContainer(@Nonnull Class<T> interfaceClass) {
    final T initializedComponent = (T)myInitializedComponents.get(interfaceClass);
    if (initializedComponent != null) return initializedComponent;

    synchronized (this) {
      if (myComponentsRegistry == null || !myComponentsRegistry.containsInterface(interfaceClass)) {
        return null;
      }

      Object lock = myComponentsRegistry.getComponentLock(interfaceClass);

      synchronized (lock) {
        T dcl = (T)myInitializedComponents.get(interfaceClass);
        if (dcl != null) return dcl;

        T component = (T)getPicoContainer().getComponentInstance(interfaceClass.getName());
        if (component == null) {
          component = (T)createComponent(interfaceClass);
        }

        if (component == null) {
          throw new IncorrectOperationException("createComponent() returns null for: " + interfaceClass);
        }

        myInitializedComponents.put(interfaceClass, component);

        if (component instanceof com.intellij.openapi.Disposable) {
          Disposer.register(this, (com.intellij.openapi.Disposable)component);
        }

        return component;
      }
    }
  }

  @Override
  public <T> T getComponent(@Nonnull Class<T> interfaceClass) {
    if (myDisposeCompleted) {
      ProgressManager.checkCanceled();
      throw new AssertionError("Already disposed: " + this);
    }
    return getComponent(interfaceClass, null);
  }

  @Override
  public <T> T getComponent(@Nonnull Class<T> interfaceClass, T defaultImplementation) {
    final T fromContainer = getComponentFromContainer(interfaceClass);
    if (fromContainer != null) return fromContainer;
    if (defaultImplementation != null) return defaultImplementation;
    return null;
  }

  @Nullable
  protected static ProgressIndicator getProgressIndicator() {
    PicoContainer container = ApplicationManager.getApplication().getPicoContainer();
    ComponentAdapter adapter = container.getComponentAdapterOfType(ProgressManager.class);
    if (adapter == null) return null;
    ProgressManager progressManager = (ProgressManager)adapter.getComponentInstance(container);
    boolean isProgressManagerInitialized = progressManager != null;
    return isProgressManagerInitialized ? ProgressIndicatorProvider.getGlobalProgressIndicator() : null;
  }

  protected float getPercentageOfComponentsLoaded() {
    return myComponentsRegistry.getPercentageOfComponentsLoaded();
  }

  public boolean initializeComponent(@Nonnull Object component, boolean service) {
    return false;
  }

  protected void handleInitComponentError(@Nonnull Throwable ex, @Nullable String componentClassName, @Nullable ComponentConfig config) {
    LOG.error(ex);
  }

  @Override
  @SuppressWarnings({"NonPrivateFieldAccessedInSynchronizedContext"})
  public synchronized void registerComponent(final ComponentConfig config, final PluginDescriptor pluginDescriptor) {
    if (!config.prepareClasses(isHeadless())) {
      return;
    }

    config.pluginDescriptor = pluginDescriptor;
    myComponentsRegistry.registerComponent(config);
  }

  public synchronized void registerComponentImplementation(@Nonnull Class componentKey, @Nonnull Class componentImplementation) {
    getPicoContainer().registerComponentImplementation(componentKey.getName(), componentImplementation);
    myInitializedComponents.remove(componentKey);
  }

  @TestOnly
  public synchronized <T> T registerComponentInstance(@Nonnull Class<T> componentKey, @Nonnull T componentImplementation) {
    getPicoContainer().unregisterComponent(componentKey.getName());
    getPicoContainer().registerComponentInstance(componentKey.getName(), componentImplementation);
    @SuppressWarnings("unchecked") T t = (T)myInitializedComponents.remove(componentKey);
    return t;
  }

  @Override
  public synchronized boolean hasComponent(@Nonnull Class interfaceClass) {
    return myComponentsRegistry != null && myComponentsRegistry.containsInterface(interfaceClass);
  }

  @Override
  @Nonnull
  public MutablePicoContainer getPicoContainer() {
    MutablePicoContainer container = myPicoContainer;
    if (container == null || myDisposeCompleted) {
      ProgressManager.checkCanceled();
      throw new AssertionError("Already disposed: " + toString());
    }
    return container;
  }

  @Nonnull
  protected MutablePicoContainer createPicoContainer() {
    MutablePicoContainer result;

    if (myParentComponentManager != null) {
      result = new DefaultPicoContainer(myParentComponentManager.getPicoContainer());
    }
    else {
      result = new DefaultPicoContainer();
    }

    return result;
  }

  protected boolean isComponentSuitable(Map<String, String> options) {
    return !isTrue(options, "internal") || ApplicationManager.getApplication().isInternal();
  }

  private static boolean isTrue(Map<String, String> options, @NonNls @Nonnull String option) {
    return options != null && options.containsKey(option) && Boolean.valueOf(options.get(option)).booleanValue();
  }

  @Override
  public synchronized void dispose() {
    ApplicationManager.getApplication().assertIsDispatchThread();
    myDisposeCompleted = true;

    if (myMessageBus != null) {
      myMessageBus.dispose();
      myMessageBus = null;
    }

    myInitializedComponents.clear();
    myComponentsRegistry = null;
    myPicoContainer = null;
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

  protected void loadComponentsConfiguration(@Nonnull ComponentConfig[] components, @Nullable PluginDescriptor descriptor, boolean defaultProject) {
    myConfigurator.loadComponentsConfiguration(components, descriptor, defaultProject);
  }

  protected void bootstrapPicoContainer(@Nonnull String name) {
    myPicoContainer = createPicoContainer();

    myMessageBus = MessageBusFactory.newMessageBus(name, myParentComponentManager == null ? null : myParentComponentManager.getMessageBus());
    final MutablePicoContainer picoContainer = getPicoContainer();
    picoContainer.registerComponentInstance(MessageBus.class, myMessageBus);
  }


  protected ComponentManager getParentComponentManager() {
    return myParentComponentManager;
  }

  private static class HeadlessHolder {
    private static final boolean myHeadless = ApplicationManager.getApplication().isHeadlessEnvironment();
  }

  private boolean isHeadless() {
    return HeadlessHolder.myHeadless;
  }

  @Override
  public void registerComponent(@Nonnull final ComponentConfig config) {
    registerComponent(config, null);
  }

  @Nonnull
  public ComponentConfig[] getComponentConfigurations() {
    return myComponentsRegistry.getComponentConfigurations();
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
    private final Map<Class, Object> myInterfaceToLockMap = new THashMap<>();
    private final Map<Class, Class> myInterfaceToClassMap = new THashMap<>();
    private final List<Class> myComponentInterfaces = new ArrayList<>(); // keeps order of component's registration
    private final List<ComponentConfig> myComponentConfigs = new ArrayList<>();
    private final List<Object> myImplementations = new ArrayList<>();
    private final Map<Class, ComponentConfig> myComponentClassToConfig = new THashMap<>();
    private boolean myClassesLoaded = false;

    private void loadClasses() {
      assert !myClassesLoaded;

      for (ComponentConfig config : myComponentConfigs) {
        loadClasses(config);
      }

      myClassesLoaded = true;
    }

    private void loadClasses(@Nonnull ComponentConfig config) {
      ClassLoader loader = config.getClassLoader();

      try {
        final Class<?> interfaceClass = Class.forName(config.getInterfaceClass(), true, loader);
        final Class<?> implementationClass =
                Comparing.equal(config.getInterfaceClass(), config.getImplementationClass()) ? interfaceClass : Class.forName(config.getImplementationClass(), true, loader);

        if (myInterfaceToClassMap.get(interfaceClass) != null) {
          throw new RuntimeException("Component already registered: " + interfaceClass.getName());
        }

        getPicoContainer().registerComponent(new ComponentConfigComponentAdapter(config, implementationClass));
        myInterfaceToClassMap.put(interfaceClass, implementationClass);
        myComponentClassToConfig.put(implementationClass, config);
        myComponentInterfaces.add(interfaceClass);
      }
      catch (Throwable t) {
        handleInitComponentError(t, null, config);
      }
    }

    private Object getComponentLock(final Class componentClass) {
      Object lock = myInterfaceToLockMap.get(componentClass);
      if (lock == null) {
        myInterfaceToLockMap.put(componentClass, lock = new Object());
      }
      return lock;
    }

    private Class[] getComponentInterfaces() {
      assert myClassesLoaded;
      return myComponentInterfaces.toArray(new Class[myComponentInterfaces.size()]);
    }

    private boolean containsInterface(final Class interfaceClass) {
      return myInterfaceToClassMap.containsKey(interfaceClass);
    }

    public float getPercentageOfComponentsLoaded() {
      return ((float)myImplementations.size()) / myComponentConfigs.size();
    }

    private void registerComponentInstance(final Object component) {
      myImplementations.add(component);
    }

    @Nonnull
    public List<Object> getRegisteredImplementations() {
      return myImplementations;
    }

    private void registerComponent(ComponentConfig config) {
      myComponentConfigs.add(config);

      if (myClassesLoaded) {
        loadClasses(config);
      }
    }

    public ComponentConfig[] getComponentConfigurations() {
      return myComponentConfigs.toArray(new ComponentConfig[myComponentConfigs.size()]);
    }

    public ComponentConfig getConfig(final Class componentImplementation) {
      return myComponentClassToConfig.get(componentImplementation);
    }
  }

  private class ComponentConfigComponentAdapter implements ComponentAdapter {
    private final ComponentConfig myConfig;
    private final ComponentAdapter myDelegate;
    private boolean myInitialized = false;
    private boolean myInitializing = false;

    public ComponentConfigComponentAdapter(final ComponentConfig config, Class<?> implementationClass) {
      myConfig = config;

      final String componentKey = config.getInterfaceClass();
      myDelegate = new CachingConstructorInjectionComponentAdapter(componentKey, implementationClass, null, true) {
        @Override
        public Object getComponentInstance(PicoContainer picoContainer) throws PicoInitializationException, PicoIntrospectionException, ProcessCanceledException {
          ProgressIndicator indicator = getProgressIndicator();
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
                myComponentsRegistry.registerComponentInstance(componentInstance);

                if (componentInstance instanceof com.intellij.openapi.Disposable) {
                  Disposer.register(ComponentManagerImpl.this, (com.intellij.openapi.Disposable)componentInstance);
                }

                boolean isStorableComponent = initializeComponent(componentInstance, false);

                if (componentInstance instanceof BaseComponent) {
                  if (!isStorableComponent) {
                    LOG.warn("Not storable component implement initComponent() method, which can moved to constructor, component: " + componentInstance.getClass().getName());
                  }

                  ((BaseComponent)componentInstance).initComponent();
                }

                long ms = (System.nanoTime() - startTime) / 1000000;
                if (ms > 10 && logSlowComponents()) {
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
            handleInitComponentError(t, componentKey, config);
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
}
