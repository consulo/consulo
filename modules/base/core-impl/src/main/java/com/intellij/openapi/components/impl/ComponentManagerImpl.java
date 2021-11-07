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

import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.components.BaseComponent;
import com.intellij.openapi.components.ComponentManager;
import com.intellij.openapi.components.NamedComponent;
import com.intellij.openapi.components.ServiceDescriptor;
import com.intellij.openapi.extensions.ExtensionPoint;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.extensions.impl.ExtensionAreaId;
import com.intellij.openapi.extensions.impl.ExtensionPointImpl;
import com.intellij.openapi.extensions.impl.ExtensionsAreaImpl;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Condition;
import com.intellij.util.containers.MultiMap;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.impl.MessageBusFactory;
import com.intellij.util.messages.impl.MessageBusImpl;
import consulo.application.ApplicationProperties;
import consulo.container.plugin.ComponentConfig;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginListenerDescriptor;
import consulo.container.plugin.PluginManager;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.injecting.InjectingContainer;
import consulo.injecting.InjectingContainerBuilder;
import consulo.injecting.InjectingPoint;
import consulo.injecting.key.InjectingKey;
import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.plugins.internal.PluginExtensionRegistrator;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.dataholder.UserDataHolderBase;
import consulo.util.lang.Pair;
import consulo.util.lang.ThreeState;
import org.jetbrains.annotations.TestOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * @author mike
 */
public abstract class ComponentManagerImpl extends UserDataHolderBase implements ComponentManager, Disposable {
  protected class ComponentsRegistry {
    private final List<ComponentConfig> myComponentConfigs = new ArrayList<>();
    private final Map<Class, ComponentConfig> myComponentClassToConfig = new HashMap<>();

    private void loadClasses(List<Class> notLazyServices, InjectingContainerBuilder builder) {
      for (ComponentConfig config : myComponentConfigs) {
        loadClasses(config, notLazyServices, builder);
      }
    }

    @SuppressWarnings("unchecked")
    private void loadClasses(@Nonnull ComponentConfig config, List<Class> notLazyServices, InjectingContainerBuilder builder) {
      ClassLoader loader = config.getClassLoader();

      try {
        final Class interfaceClass = Class.forName(config.getInterfaceClass(), false, loader);
        final Class implementationClass = Comparing.equal(config.getInterfaceClass(), config.getImplementationClass()) ? interfaceClass : Class.forName(config.getImplementationClass(), false, loader);

        InjectingPoint<Object> point = builder.bind(interfaceClass);

        // force singleton
        point.forceSingleton();
        // to impl class
        point.to(implementationClass);
        // post processor
        point.injectListener((startTime, componentInstance) -> {
          if (myChecker.containsKey(interfaceClass)) {
            throw new IllegalArgumentException("Duplicate init of " + interfaceClass);
          }
          myChecker.put(interfaceClass, componentInstance);

          if (componentInstance instanceof Disposable) {
            Disposer.register(ComponentManagerImpl.this, (Disposable)componentInstance);
          }

          boolean isStorableComponent = initializeIfStorableComponent(componentInstance, false, false);

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
          if (ms > 10 && logSlowComponents()) {
            LOG.info(componentInstance.getClass().getName() + " initialized in " + ms + " ms");
          }
        });

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

  private static final Logger LOG = Logger.getInstance(ComponentManagerImpl.class);

  private InjectingContainer myInjectingContainer;

  protected volatile boolean temporarilyDisposed = false;

  private MessageBusImpl myMessageBus;

  protected final ComponentManager myParent;

  private ComponentsRegistry myComponentsRegistry = new ComponentsRegistry();
  private final Condition myDisposedCondition = o -> isDisposed();

  private boolean myNotLazyStepFinished;

  private ExtensionsAreaImpl myExtensionsArea;
  @Nonnull
  private final String myName;
  @Nullable
  private final ExtensionAreaId myExtensionAreaId;

  private List<Class> myNotLazyServices = new ArrayList<>();

  private Map<Class<?>, Object> myChecker = new ConcurrentHashMap<>();

  private Class<?> myCurrentNotLazyServiceClass;

  private volatile ThreeState myDisposeState = ThreeState.NO;

  protected ComponentManagerImpl(@Nullable ComponentManager parent, @Nonnull String name, @Nullable ExtensionAreaId extensionAreaId, boolean buildInjectionContainer) {
    myParent = parent;
    myName = name;
    myExtensionAreaId = extensionAreaId;

    if (buildInjectionContainer) {
      buildInjectingContainer();
    }
  }

  protected void buildInjectingContainer() {
    myMessageBus = MessageBusFactory.newMessageBus(this, myParent == null ? null : myParent.getMessageBus());

    MultiMap<String, PluginListenerDescriptor> mapByTopic = new MultiMap<>();

    fillListenerDescriptors(mapByTopic);

    myMessageBus.setLazyListeners(mapByTopic);

    myExtensionsArea = new ExtensionsAreaImpl(this, this::checkCanceled);

    registerExtensionPointsAndExtensions(myExtensionsArea);

    myExtensionsArea.setLocked();

    InjectingContainer root = findRootContainer();

    InjectingContainerBuilder builder = myParent == null ? root.childBuilder() : myParent.getInjectingContainer().childBuilder();

    registerServices(builder);

    loadServices(myNotLazyServices, builder);

    bootstrapInjectingContainer(builder);

    myInjectingContainer = builder.build();
  }

  @Nonnull
  @Override
  public Supplier<ThreeState> getDisposeState() {
    return this::getDisposeStateImpl;
  }

  @Nonnull
  private ThreeState getDisposeStateImpl() {
    if (temporarilyDisposed) {
      return ThreeState.YES;
    }
    return myDisposeState;
  }

  @Nonnull
  protected InjectingContainer findRootContainer() {
    InjectingContainer root;
    String jdkModuleMain = Platform.current().jvm().getRuntimeProperty("jdk.module.main");
    if (jdkModuleMain != null) {
      PluginDescriptor plugin = PluginManager.getPlugin(getClass());
      assert plugin != null;
      ModuleLayer moduleLayer = (ModuleLayer)plugin.getModuleLayer();
      assert moduleLayer != null;
      root = InjectingContainer.root(moduleLayer);
    }
    else {
      root = InjectingContainer.root(getClass().getClassLoader());
    }
    return root;
  }

  protected void fillListenerDescriptors(MultiMap<String, PluginListenerDescriptor> mapByTopic) {
    for (PluginDescriptor plugin : PluginManager.getPlugins()) {
      if (!PluginManagerCore.shouldSkipPlugin(plugin)) {
        List<PluginListenerDescriptor> descriptors = getPluginListenerDescriptors(plugin);

        for (PluginListenerDescriptor descriptor : descriptors) {
          mapByTopic.putValue(descriptor.topicClassName, descriptor);
        }
      }
    }
  }

  protected void registerExtensionPointsAndExtensions(ExtensionsAreaImpl area) {
    PluginExtensionRegistrator.registerExtensionPointsAndExtensions(myExtensionAreaId, area);
  }

  protected void registerServices(InjectingContainerBuilder builder) {
    for (PluginDescriptor plugin : PluginManager.getPlugins()) {
      if (!PluginManagerCore.shouldSkipPlugin(plugin)) {
        List<ComponentConfig> componentConfigs = getComponentConfigs(plugin);

        for (ComponentConfig componentConfig : componentConfigs) {
          registerComponent(componentConfig, plugin);
        }
      }
    }

    myComponentsRegistry.loadClasses(myNotLazyServices, builder);
  }

  protected void bootstrapInjectingContainer(@Nonnull InjectingContainerBuilder builder) {
  }

  @Nonnull
  protected List<ComponentConfig> getComponentConfigs(PluginDescriptor ideaPluginDescriptor) {
    return Collections.emptyList();
  }

  @Nonnull
  protected List<PluginListenerDescriptor> getPluginListenerDescriptors(PluginDescriptor pluginDescriptor) {
    return Collections.emptyList();
  }

  private void loadServices(List<Class> notLazyServices, InjectingContainerBuilder builder) {
    ExtensionPointName<ServiceDescriptor> ep = getServiceExtensionPointName();
    if (ep != null) {
      ExtensionPointImpl<ServiceDescriptor> extensionPoint = myExtensionsArea.getExtensionPointImpl(ep);
      // there no injector at that level - build it via hardcode
      List<Pair<ServiceDescriptor, PluginDescriptor>> descriptorList = extensionPoint.buildUnsafe(aClass -> new ServiceDescriptor());
      // and cache it
      extensionPoint.setExtensionCache(descriptorList);

      for (ServiceDescriptor descriptor : extensionPoint.getExtensionList()) {
        InjectingKey<Object> key = InjectingKey.of(descriptor.getInterface(), getTargetClassLoader(descriptor.getPluginDescriptor()));
        InjectingKey<Object> implKey = InjectingKey.of(descriptor.getImplementation(), getTargetClassLoader(descriptor.getPluginDescriptor()));

        InjectingPoint<Object> point = builder.bind(key);
        // bind to impl class
        point.to(implKey);
        // require singleton
        point.forceSingleton();
        // remap object initialization
        point.factory(objectProvider -> runServiceInitialize(descriptor, objectProvider::get));

        point.injectListener((time, instance) -> {

          if (myChecker.containsKey(key.getTargetClass())) {
            throw new IllegalArgumentException("Duplicate init of " + key.getTargetClass());
          }
          myChecker.put(key.getTargetClass(), instance);

          if (instance instanceof Disposable) {
            Disposer.register(this, (Disposable)instance);
          }

          initializeIfStorableComponent(instance, true, descriptor.isLazy());
        });

        if (!descriptor.isLazy()) {
          // if service is not lazy - add it for init at start
          notLazyServices.add(key.getTargetClass());
        }
      }
    }
  }

  @Nonnull
  private static ClassLoader getTargetClassLoader(PluginDescriptor pluginDescriptor) {
    return pluginDescriptor != null ? pluginDescriptor.getPluginClassLoader() : ComponentManagerImpl.class.getClassLoader();
  }

  protected <T> T runServiceInitialize(@Nonnull ServiceDescriptor descriptor, @Nonnull Supplier<T> runnable) {
    if (!myNotLazyStepFinished && !descriptor.isLazy() && myCurrentNotLazyServiceClass != null) {
      if (!Objects.equals(descriptor.getInterface(), myCurrentNotLazyServiceClass.getName()) && InjectingContainer.LOG_INJECTING_PROBLEMS) {
        LOG.warn(new IllegalAccessException("Initializing not lazy service [" + descriptor.getInterface() + "] from another service [" + myCurrentNotLazyServiceClass.getName() + "]"));
      }
    }
    return runnable.get();
  }

  @Nullable
  protected ExtensionPointName<ServiceDescriptor> getServiceExtensionPointName() {
    return null;
  }

  public boolean initializeIfStorableComponent(@Nonnull Object component, boolean service, boolean lazy) {
    return false;
  }

  protected void handleInitComponentError(@Nonnull Throwable ex, @Nullable Class componentClass, @Nullable ComponentConfig config) {
    LOG.error(ex);
  }

  private void registerComponent(ComponentConfig config, PluginDescriptor pluginDescriptor) {
    config.prepareClasses();

    config.pluginDescriptor = pluginDescriptor;
    myComponentsRegistry.addConfig(config);
  }

  @Override
  public void initNotLazyServices(@Nullable ProgressIndicator progressIndicator) {
    try {
      if (myNotLazyStepFinished) {
        throw new IllegalArgumentException("Injector already build");
      }

      if (progressIndicator != null) {
        progressIndicator.setIndeterminate(false);
        progressIndicator.setFraction(0);
      }

      int i = 1;
      for (Class<?> serviceClass : myNotLazyServices) {
        try {
          myCurrentNotLazyServiceClass = serviceClass;

          if (progressIndicator != null) {
            progressIndicator.checkCanceled();

            progressIndicator.setFraction(i / (float)myNotLazyServices.size());
          }
          else {
            checkCanceled();
          }

          Object component = getComponent(serviceClass);
          assert component != null;
        }
        finally {
          i++;
        }
      }
    }
    finally {
      myCurrentNotLazyServiceClass = null;
      myNotLazyStepFinished = true;
    }
  }

  @Override
  public int getNotLazyServicesCount() {
    return myNotLazyServices.size();
  }

  @Nonnull
  @Override
  public MessageBus getMessageBus() {
    if (myDisposeState == ThreeState.YES) {
      checkCanceled();
      throw new AssertionError("Already disposed");
    }
    assert myMessageBus != null : "Not initialized yet";
    return myMessageBus;
  }

  @Nullable
  @Override
  public <T> T getInstanceIfCreated(@Nonnull Class<T> clazz) {
    if (myDisposeState == ThreeState.YES) {
      checkCanceled();
      throw new AssertionError("Already disposed: " + this);
    }
    return getInjectingContainer().getInstanceIfCreated(clazz);
  }

  @Override
  public <T> T getComponent(@Nonnull Class<T> clazz) {
    if (myDisposeState == ThreeState.YES) {
      checkCanceled();
      throw new AssertionError("Already disposed: " + this);
    }
    return getInjectingContainer().getInstance(clazz);
  }

  @Nonnull
  @Override
  public InjectingContainer getInjectingContainer() {
    InjectingContainer container = myInjectingContainer;
    if (container == null || myDisposeState == ThreeState.YES) {
      checkCanceled();
      throw new AssertionError("Already disposed: " + toString());
    }
    return container;
  }

  protected void checkCanceled() {
  }

  @Nonnull
  public ExtensionPoint[] getExtensionPoints() {
    return myExtensionsArea.getExtensionPoints();
  }

  @Nonnull
  @Override
  public <T> ExtensionPoint<T> getExtensionPoint(@Nonnull ExtensionPointName<T> extensionPointName) {
    return myExtensionsArea.getExtensionPoint(extensionPointName);
  }

  @TestOnly
  public void setTemporarilyDisposed(boolean disposed) {
    temporarilyDisposed = disposed;
  }

  public ComponentConfig getConfig(Class componentImplementation) {
    return myComponentsRegistry.getConfig(componentImplementation);
  }

  @Override
  @Nonnull
  public Condition getDisposed() {
    return myDisposedCondition;
  }

  @Override
  public boolean isDisposed() {
    return myDisposeState == ThreeState.YES;
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

  @Override
  @RequiredUIAccess
  public void dispose() {
    Application.get().assertIsDispatchThread();

    myDisposeState = ThreeState.UNSURE;

    if (myMessageBus != null) {
      Disposer.dispose(myMessageBus);
      myMessageBus = null;
    }

    myExtensionsArea = null;
    myInjectingContainer.dispose();
    myInjectingContainer = null;

    myComponentsRegistry = null;
    myNotLazyStepFinished = false;
    myNotLazyServices.clear();

    myDisposeState = ThreeState.YES;
  }

  @Override
  public String toString() {
    return myName;
  }
}
