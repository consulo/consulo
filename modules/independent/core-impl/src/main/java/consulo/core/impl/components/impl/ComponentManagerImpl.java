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
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.*;
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
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusFactory;
import consulo.annotation.inject.NotLazy;
import consulo.extensions.AreaInstanceEx;
import org.jdom.Element;
import org.jetbrains.annotations.TestOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Singleton;
import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author VISTALL
 */
public abstract class ComponentManagerImpl extends UserDataHolderBase implements ComponentManager, Disposable, AreaInstanceEx {
  private static final Logger LOG = Logger.getInstance(ComponentManagerImpl.class);

  private volatile boolean myDisposed = false;
  private volatile boolean myDisposeCompleted = false;

  private MessageBus myMessageBus;

  private final ComponentManager myParentComponentManager;
  private final Condition myDisposedCondition = o -> isDisposed();

  private Injector myInjector;
  private String myName;

  private final AtomicInteger myNotLazyLoadCount = new AtomicInteger();
  private final Map<Class, Boolean> myAllBindings = new ConcurrentHashMap<>();
  private final Map<Class, ComponentConfig> myComponentConfigByImplClass = new ConcurrentHashMap<>();

  private boolean myComponentCreated;

  private ExtensionsArea myExtensionsArea;

  private ThreadLocal<Object> myGetComponentThreadLocal = new ThreadLocal<>();

  protected ComponentManagerImpl(@Nullable ComponentManager parentComponentManager) {
    this(parentComponentManager, "");
  }

  protected ComponentManagerImpl(@Nullable ComponentManager parentComponentManager, @Nonnull String name) {
    myParentComponentManager = parentComponentManager;
    myName = name;

    myExtensionsArea = new ExtensionsAreaImpl(getAreaId(), this, new PluginManagerCore.IdeaLogProvider());
  }

  private Map<Object, Exception> myAlreadyInit = new ConcurrentHashMap<>();

  protected void _setupComponent(Object component) {
    try {
      Exception exception = myAlreadyInit.get(component);
      if (exception != null) {
        throw new IllegalArgumentException("duplicate init " + component.getClass().getSimpleName(), exception);
      }

      myAlreadyInit.put(component, new Exception("init trace"));
      myGetComponentThreadLocal.set(component);
      if (this != component && component instanceof Disposable) {
        Disposer.register(this, (Disposable)component);
      }

      boolean isNotLazy = component.getClass().isAnnotationPresent(NotLazy.class);
      boolean isXmlSerializer = component instanceof JDOMExternalizable || component instanceof PersistentStateComponent;
      if (isXmlSerializer) {
        initializeFromStateStore(component, !isNotLazy);
      }

      if (isNotLazy) {
        myNotLazyLoadCount.incrementAndGet();

        componentCreated(component.getClass());
      }
    }
    finally {
      myGetComponentThreadLocal.remove();
    }
  }

  protected void buildInjector() {
    myInjector = createInjector(myName);
  }

  @Nonnull
  private Injector createInjector(@Nonnull String name) {
    myMessageBus = MessageBusFactory.newMessageBus(name, myParentComponentManager == null ? null : myParentComponentManager.getMessageBus());

    ComponentManagerScope scope = new ComponentManagerScope(this);

    AbstractModule module = new AbstractModule() {
      @Override
      protected void configure() {
        bindListener(Matchers.any(), PostConstructMethodCaller.INSTANCE);
        bindListener(Matchers.any(), ScopeProvisionListener.INSTANCE);

        long start = System.currentTimeMillis();
        bootstrapBinder(scope, binder());
        logTime(myAllBindings.size() + " bindings done", start);
      }
    };

    long start = System.currentTimeMillis();
    try {
      if (myParentComponentManager != null) {
        Injector injector = ((AreaInstanceEx)myParentComponentManager).getInjector();
        return injector.createChildInjector(module);
      }
      else {
        return Guice.createInjector(module);
      }
    }
    finally {
      logTime("injector created", start);
    }
  }

  private void logTime(String id, long start) {
    long diff = System.currentTimeMillis() - start;
    String areaId = getAreaId();
    if (areaId.isEmpty()) {
      areaId = "application";
    }

    areaId = areaId.toLowerCase(Locale.US);

    LOG.info(areaId + " " + id + " in " + diff + " ms");
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
    long time = System.currentTimeMillis();
    int i = 0;
    for (Map.Entry<Class, Boolean> entry : myAllBindings.entrySet()) {
      Class key = entry.getKey();

      // hold @NotLazy
      if (entry.getValue()) {
        i++;
        long l = System.currentTimeMillis();
        Object instance = myInjector.getInstance(key);
        assert instance != null;
        LOG.info(instance.getClass().getSimpleName() + " init in " + (System.currentTimeMillis() - l) + " ms");
      }
    }
    logTime(i + " notlazy services done", time);
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

  protected void componentCreated(@Nonnull Class componentInterface) {
  }

  protected synchronized void disposeComponents() {
    assert !myDisposeCompleted : "Already disposed!";

    for (Class<?> o : ContainerUtil.reverse(new ArrayList<>(myAllBindings.keySet()))) {
      Object instance = myInjector.getInstance(o);
      if (instance instanceof BaseComponent) {
        ((BaseComponent)instance).disposeComponent();
      }
    }
  }

  @Override
  public <T> T getComponent(@Nonnull Class<T> interfaceClass) {
    Object o = myGetComponentThreadLocal.get();

    if (o != null) {
      throw new IllegalArgumentException("Can't initialize value while object setup " + o.getClass());
    }
    if (myDisposeCompleted) {
      ProgressManager.checkCanceled();
      throw new AssertionError("Already disposed: " + this);
    }

    T instance = getCustomComponentInstance(interfaceClass);
    if (instance != null) {
      return instance;
    }

    return myInjector.getInstance(interfaceClass);
  }

  @Override
  public <T> T createInstance(@Nonnull Class<T> interfaceClass) {
    Object o = myGetComponentThreadLocal.get();

    if (o != null) {
      throw new IllegalArgumentException("Can't initialize value while object setup " + o.getClass());
    }
    if (myDisposeCompleted) {
      ProgressManager.checkCanceled();
      throw new AssertionError("Already disposed: " + this);
    }

    T instance = getCustomComponentInstance(interfaceClass);
    if (instance != null) {
      return instance;
    }

    T t = myInjector.getInstance(interfaceClass);
    if (t.getClass().isAnnotationPresent(Singleton.class)) {
      throw new IllegalArgumentException();
    }
    return t;
  }

  @Nullable
  protected <T> T getCustomComponentInstance(@Nonnull Class<T> clazz) {
    return null;
  }

  @Nullable
  protected static ProgressIndicator getProgressIndicator() {
    Injector container = ((AreaInstanceEx)Application.get()).getInjector();
    Binding<ProgressManager> adapter = container.getBinding(ProgressManager.class);
    if (adapter == null) return null;
    adapter.getProvider();
    return adapter.getProvider().get().getProgressIndicator();
  }

  protected float getPercentageOfComponentsLoaded() {
    float value = myNotLazyLoadCount.get();
    return value / getNotLazyComponentsSize();
  }

  public void initializeFromStateStore(@Nonnull Object component, boolean service) {
  }

  protected void handleInitComponentError(@Nonnull Throwable ex, @Nullable String componentClassName, @Nullable ComponentConfig config) {
    LOG.error(ex);
  }

  @Override
  public boolean hasComponent(@Nonnull Class interfaceClass) {
    return myAllBindings.get(interfaceClass) != null;
  }

  @Override
  @SuppressWarnings({"unchecked"})
  @Nonnull
  public <T> T[] getComponents(@Nonnull Class<T> baseClass) {
    List<T> list = new ArrayList<T>();
    for (Map.Entry<Class, Boolean> entry : myAllBindings.entrySet()) {
      // skip services
      if (!entry.getValue()) {
        continue;
      }

      Class key = entry.getKey();
      if (key.isAssignableFrom(baseClass)) {
        list.add((T)getInjector().getInstance(key));
      }
    }
    return list.toArray((T[])Array.newInstance(baseClass, list.size()));
  }

  protected boolean isComponentSuitable(Map<String, String> options) {
    return !isTrue(options, "internal") || ApplicationManager.getApplication().isInternal();
  }

  private static boolean isTrue(Map<String, String> options, @Nonnull String option) {
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

    myComponentConfigByImplClass.clear();
    myAllBindings.clear();
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

  protected void bootstrapBinder(Scope scope, Binder binder) {
    boolean isDefault = this instanceof Project && ((Project)this).isDefault();

    IdeaPluginDescriptor[] plugins = PluginManagerCore.getPlugins();
    for (IdeaPluginDescriptor plugin : plugins) {
      if (!PluginManagerCore.shouldSkipPlugin(plugin)) {
        ComponentConfig[] components = selectComponentConfigs(plugin);
        for (ComponentConfig component : components) {
          load(binder, isDefault, component, plugin, scope);
        }
      }
    }

    PluginManagerCore.registerExtensionPointsAndExtensions(this);

    // we cant initialize extensions since injector is not inited
    ExtensionPointImpl<ServiceDescriptor> point = (ExtensionPointImpl<ServiceDescriptor>)myExtensionsArea.getExtensionPoint(getServiceEpName());

    Set<ExtensionComponentAdapter> extensionAdapters = point.getExtensionAdapters();

    for (ExtensionComponentAdapter extension : extensionAdapters) {
      Element descriptorElement = extension.getDescribingElement();

      ComponentConfig componentConfig = new ComponentConfig();
      componentConfig.setInterfaceClass(descriptorElement.getAttributeValue(ServiceDescriptor.serviceInterfaceAttribute));
      componentConfig.setImplementationClass(descriptorElement.getAttributeValue(ServiceDescriptor.serviceImplementationAttribute));

      load(binder, isDefault, componentConfig, extension.getPluginDescriptor(), scope);
    }

    myComponentCreated = true;
  }

  private Map<Class, Class> myRebindCheck = new HashMap<>();

  private void load(Binder binder, final boolean defaultProject, @Nonnull ComponentConfig config, @Nonnull PluginDescriptor descriptor, Scope scope) {
    if (defaultProject && !config.isLoadForDefaultProject() || !isComponentSuitable(config.options)) {
      return;
    }

    if (!config.prepareClasses(isHeadless(), isCompilerServer())) {
      return;
    }

    ClassLoader loader = descriptor.getPluginClassLoader();

    try {
      final Class interfaceClass = Class.forName(config.getInterfaceClass(), true, loader);
      final Class implementationClass = Comparing.equal(config.getInterfaceClass(), config.getImplementationClass()) ? interfaceClass : Class.forName(config.getImplementationClass(), true, loader);

      myComponentConfigByImplClass.put(implementationClass, config);

      if (interfaceClass != implementationClass) {
        binder.bind(interfaceClass).to(implementationClass).in(scope);
      }
      else {
        binder.bind(implementationClass).in(scope);
      }

      if (myRebindCheck.containsKey(interfaceClass)) {
        LOG.error("Service rebind: " + interfaceClass.getName());
      }
      else {
        myRebindCheck.put(interfaceClass, implementationClass);
      }

      if (!implementationClass.isAnnotationPresent(com.google.inject.Singleton.class) && !implementationClass.isAnnotationPresent(Singleton.class)) {
        LOG.warn("Component is not annotated by @Singleton. " + implementationClass.getName());
      }

      myAllBindings.put(interfaceClass, implementationClass.isAnnotationPresent(NotLazy.class));
    }
    catch (Throwable e) {
      handleInitComponentError(e, null, config);
    }
  }

  @Nullable
  public ComponentConfig getConfigByImplClass(@Nonnull Class<?> clazz) {
    return myComponentConfigByImplClass.get(clazz);
  }

  @Nonnull
  public abstract String getAreaId();

  @Nonnull
  protected abstract ExtensionPointName<ServiceDescriptor> getServiceEpName();

  @Nonnull
  protected abstract ComponentConfig[] selectComponentConfigs(IdeaPluginDescriptor descriptor);

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

  public int getNotLazyComponentsSize() {
    int i = 0;
    for (Boolean value : myAllBindings.values()) {
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
}