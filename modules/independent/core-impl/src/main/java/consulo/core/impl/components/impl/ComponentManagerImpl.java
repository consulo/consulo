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
package consulo.core.impl.components.impl;

import com.google.inject.*;
import com.google.inject.matcher.Matchers;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.*;
import com.intellij.openapi.components.ex.ComponentManagerEx;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.extensions.ExtensionsArea;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.openapi.extensions.impl.ExtensionComponentAdapter;
import com.intellij.openapi.extensions.impl.ExtensionPointImpl;
import com.intellij.openapi.extensions.impl.ExtensionsAreaImpl;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.*;
import com.intellij.util.containers.ConcurrentHashSet;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusFactory;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.TestOnly;
import org.picocontainer.Disposable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author VISTALL
 *         <p>
 *         guice variant of ComponentManagerImpl from picocontainer impl - base author mike
 */
public abstract class ComponentManagerImpl extends UserDataHolderBase implements ComponentManagerEx, Disposable {
  private static final Logger LOG = Logger.getInstance(ComponentManagerImpl.class);

  private volatile boolean myDisposed = false;
  private volatile boolean myDisposeCompleted = false;

  private MessageBus myMessageBus;

  private final ComponentManager myParentComponentManager;
  private final Condition myDisposedCondition = o -> isDisposed();

  private Injector myInjector;
  private String myName;

  private final Set<Class> myInitedComponents = new ConcurrentHashSet<>();

  private final Map<Class, Boolean> myBindings = new ConcurrentHashMap<>();

  private final List<Class> mySingletonList = new CopyOnWriteArrayList<>();

  private boolean myComponentCreated;

  private ExtensionsArea myExtensionsArea;

  protected ComponentManagerImpl(@Nullable ComponentManager parentComponentManager) {
    this(parentComponentManager, "");
  }

  protected ComponentManagerImpl(@Nullable ComponentManager parentComponentManager, @Nonnull String name) {
    myParentComponentManager = parentComponentManager;
    myName = name;

    myExtensionsArea = new ExtensionsAreaImpl(getAreaId(), this, new PluginManagerCore.IdeaLogProvider());
  }

  protected void buildInjector() {
    myInjector = createInjector(myName);
  }

  @Nonnull
  private Injector createInjector(@Nonnull String name) {
    AbstractModule module = new AbstractModule() {
      @Override
      protected void configure() {
        bootstrapBinder(name, binder());
      }
    };

    if (myParentComponentManager != null) {
      Injector injector = myParentComponentManager.getInjector();
      return injector.createChildInjector(module);
    }
    else {
      return Guice.createInjector(module);
    }
  }

  @Nonnull
  @Override
  public ExtensionsArea getExtensionsArea() {
    return myExtensionsArea;
  }

  @Override
  @Nonnull
  public Injector getInjector() {
    return myInjector;
  }

  @SuppressWarnings("unchecked")
  public void init() {
    for (Map.Entry<Class, Boolean> entry : myBindings.entrySet()) {
      Class key = entry.getKey();

      Object instance = myInjector.getInstance(key);
      assert instance != null;
    }
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
    return myComponentCreated;
  }

  protected synchronized Object createComponent(@Nonnull Class componentInterface) {
    final Object component = getPicoContainer().getComponentInstance(componentInterface.getName());
    LOG.assertTrue(component != null, "Can't instantiate component for: " + componentInterface);
    return component;
  }

  protected synchronized void disposeComponents() {
    assert !myDisposeCompleted : "Already disposed!";

    for (Class<?> o : ContainerUtil.reverse(new ArrayList<>(myBindings.keySet()))) {
      Object instance = myInjector.getInstance(o);
      if (instance instanceof BaseComponent) {
        ((BaseComponent)instance).disposeComponent();
      }
    }
  }

  @Override
  public <T> T getComponent(@Nonnull Class<T> interfaceClass) {
    return getComponent(interfaceClass, null);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T getComponent(@Nonnull Class<T> interfaceClass, T defaultImplementation) {
    if (myDisposeCompleted) {
      ProgressManager.checkCanceled();
      throw new AssertionError("Already disposed: " + this);
    }

    if (myBindings.containsKey(interfaceClass)) {
      return myInjector.getInstance(interfaceClass);
    }
    return defaultImplementation;
  }

  @Nullable
  protected static ProgressIndicator getProgressIndicator() {
    Injector container = Application.get().getInjector();
    Binding<ProgressManager> adapter = container.getBinding(ProgressManager.class);
    if (adapter == null) return null;
    adapter.getProvider();
    return adapter.getProvider().get().getProgressIndicator();
  }

  protected float getPercentageOfComponentsLoaded() {
    return 1f;
  }

  @Override
  public void initializeFromStateStore(@Nonnull Object component, boolean service) {
  }

  protected void handleInitComponentError(@Nonnull Throwable ex, @Nullable String componentClassName, @Nullable ComponentConfig config) {
    LOG.error(ex);
  }

  @Override
  @SuppressWarnings({"NonPrivateFieldAccessedInSynchronizedContext"})
  public synchronized void registerComponent(@Nonnull final ComponentConfig config, final PluginDescriptor pluginDescriptor) {
    if (!config.prepareClasses(isHeadless(), isCompilerServer())) {
      return;
    }

    config.pluginDescriptor = pluginDescriptor;
  }

  @Override
  public boolean hasComponent(@Nonnull Class interfaceClass) {
    return myBindings.get(interfaceClass) != null;
  }

  @Override
  @SuppressWarnings({"unchecked"})
  @Nonnull
  public synchronized <T> T[] getComponents(@Nonnull Class<T> baseClass) {
    return (T[])new Object[0];
  }

  protected boolean isComponentSuitable(Map<String, String> options) {
    return !isTrue(options, "internal") || ApplicationManager.getApplication().isInternal();
  }

  private static boolean isTrue(Map<String, String> options, @NonNls @Nonnull String option) {
    return options != null && options.containsKey(option) && Boolean.valueOf(options.get(option));
  }

  @Override
  public synchronized void dispose() {
    ApplicationManager.getApplication().assertIsDispatchThread();
    myDisposeCompleted = true;

    if (myMessageBus != null) {
      myMessageBus.dispose();
      myMessageBus = null;
    }

    myBindings.clear();
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

  protected void bootstrapBinder(String name, Binder binder) {
    myMessageBus = MessageBusFactory.newMessageBus(name, myParentComponentManager == null ? null : myParentComponentManager.getMessageBus());

    binder.bind(MessageBus.class).toInstance(myMessageBus);

    binder.bindListener(Matchers.any(), new TypeListener() {
      @Override
      public <I> void hear(TypeLiteral<I> typeLiteral, TypeEncounter<I> typeEncounter) {
        typeEncounter.register((InjectionListener<? super I>)(component) -> {
          ComponentManagerImpl manager = ComponentManagerImpl.this;
          if (manager != component && component instanceof com.intellij.openapi.Disposable) {
            Disposer.register(manager, (com.intellij.openapi.Disposable)component);
          }

          if (component instanceof JDOMExternalizable || component instanceof PersistentStateComponent) {
            initializeFromStateStore(component, myBindings.get(typeLiteral.getRawType()) == Boolean.FALSE);
          }

          if (component instanceof BaseComponent) {
            if (myInitedComponents.add(component.getClass())) {
              ((BaseComponent)component).initComponent();
            }
          }

          if (mySingletonList.contains(component.getClass())) {
            LOG.warn("Duplicate initialization. Class is not annotated by @Singleton " + component.getClass() + "?");
          }
          else {
            mySingletonList.add(component.getClass());
          }
        });
      }
    });

    boolean isDefault = this instanceof Project && ((Project)this).isDefault();

    IdeaPluginDescriptor[] plugins = PluginManagerCore.getPlugins();
    for (IdeaPluginDescriptor plugin : plugins) {
      if (!PluginManagerCore.shouldSkipPlugin(plugin)) {
        ComponentConfig[] components = selectComponentConfigs(plugin);
        for (ComponentConfig component : components) {
          Class interfaceClazz = loadSingleConfig(binder, isDefault, component, plugin);
          if (interfaceClazz != null) {
            myBindings.put(interfaceClazz, Boolean.TRUE);
          }
        }
      }
    }

    PluginManagerCore.registerExtensionPointsAndExtensions(this);

    if (this instanceof Project && isDefault) {
      myComponentCreated = true;
      return;
    }

    // we cant initialize extensions since injector is not inited
    ExtensionPointImpl<ServiceDescriptor> point = (ExtensionPointImpl<ServiceDescriptor>)myExtensionsArea.getExtensionPoint(getServiceEpName());

    Set<ExtensionComponentAdapter> extensionAdapters = point.getExtensionAdapters();

    for (ExtensionComponentAdapter extension : extensionAdapters) {
      ServiceDescriptor descriptor = extension.apply(null, (injector, aClass) -> new ServiceDescriptor());

      ComponentConfig componentConfig = new ComponentConfig();
      componentConfig.setInterfaceClass(descriptor.serviceInterface);
      componentConfig.setCompilerServerImplementationClass(descriptor.serviceImplementationForCompilerServer);
      componentConfig.setImplementationClass(descriptor.serviceImplementation);
      componentConfig.pluginDescriptor = descriptor.myPluginDescriptor;

      Class interfaceClazz = loadSingleConfig(binder, isDefault, componentConfig, descriptor.myPluginDescriptor);
      if (interfaceClazz != null) {
        myBindings.put(interfaceClazz, Boolean.FALSE);
      }
    }

    myComponentCreated = true;
  }

  @Nullable
  private Class loadSingleConfig(Binder binder, final boolean defaultProject, @Nonnull ComponentConfig config, final PluginDescriptor descriptor) {
    if (defaultProject && !config.isLoadForDefaultProject()) return null;

    if (!isComponentSuitable(config.options)) return null;

    return registerComponent(binder, config, descriptor);
  }

  @Nullable
  public Class registerComponent(Binder binder, @Nonnull final ComponentConfig config, final PluginDescriptor pluginDescriptor) {
    if (!config.prepareClasses(isHeadless(), isCompilerServer())) {
      return null;
    }

    config.pluginDescriptor = pluginDescriptor;

    ClassLoader loader = config.getClassLoader();

    try {
      final Class interfaceClass = Class.forName(config.getInterfaceClass(), true, loader);
      final Class implementationClass = Comparing.equal(config.getInterfaceClass(), config.getImplementationClass()) ? interfaceClass : Class.forName(config.getImplementationClass(), true, loader);

      if (interfaceClass != implementationClass) {
        binder.bind(interfaceClass).to(implementationClass);
      }
      else {
        binder.bind(interfaceClass);
      }

      return interfaceClass;
    }
    catch (Throwable e) {
      handleInitComponentError(e, null, config);
    }

    return null;
  }

  @Nonnull
  public abstract String getAreaId();

  @Nonnull
  protected abstract ExtensionPointName<ServiceDescriptor> getServiceEpName();

  @Nonnull
  protected abstract ComponentConfig[] selectComponentConfigs(IdeaPluginDescriptor descriptor);

  protected ComponentManager getParentComponentManager() {
    return myParentComponentManager;
  }

  private static class HeadlessHolder {
    private static final boolean myHeadless = Application.get().isHeadlessEnvironment();
    private static final boolean myCompilerServer = Application.get().isCompilerServerMode();
  }

  private boolean isHeadless() {
    return HeadlessHolder.myHeadless;
  }

  private boolean isCompilerServer() {
    return HeadlessHolder.myCompilerServer;
  }

  public int getComponentsSize() {
    int i = 0;
    for (Boolean value : myBindings.values()) {
      if (value) {
        i++;
      }
    }
    return i;
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
    return LOG.isDebugEnabled();
  }
}