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
package consulo.component.impl.internal;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.TopicAPI;
import consulo.component.ComponentManager;
import consulo.component.ComponentManagerDisposedException;
import consulo.component.bind.InjectingBinding;
import consulo.component.extension.ExtensionPoint;
import consulo.component.impl.internal.messagebus.MessageBusFactory;
import consulo.component.impl.internal.messagebus.MessageBusImpl;
import consulo.component.internal.ComponentBinding;
import consulo.component.internal.NewExtensionAreaImpl;
import consulo.component.internal.inject.*;
import consulo.component.messagebus.MessageBus;
import consulo.component.util.PluginExceptionUtil;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginManager;
import consulo.container.util.StatCollector;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.collection.MultiMap;
import consulo.util.dataholder.UserDataHolderBase;
import consulo.util.lang.ThreeState;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * @author mike
 */
public abstract class BaseComponentManager extends UserDataHolderBase implements ComponentManager, Disposable {
    private static final Logger LOG = Logger.getInstance(BaseComponentManager.class);

    private InjectingContainer myInjectingContainer;

    protected volatile boolean temporarilyDisposed = false;

    private MessageBusImpl myMessageBus;

    protected final ComponentManager myParent;

    private final BooleanSupplier myDisposedCondition = this::isDisposed;

    private boolean myNotLazyStepFinished;

    @Nonnull
    private final String myName;

    private List<Class> myNotLazyServices = new ArrayList<>();

    private Map<Class<?>, Object> myChecker = new ConcurrentHashMap<>();

    private Class<?> myCurrentNotLazyServiceClass;

    private volatile ThreeState myDisposeState = ThreeState.NO;

    private NewExtensionAreaImpl myExtensionArea;

    private final ComponentScope myComponentScope;

    @Nonnull
    private final ComponentBinding myComponentBinding;

    protected BaseComponentManager(@Nullable ComponentManager parent,
                                   @Nonnull String name,
                                   @Nullable ComponentScope componentScope,
                                   @Nonnull ComponentBinding componentBinding,
                                   boolean buildInjectionContainer) {
        myParent = parent;
        myName = name;
        myComponentScope = componentScope;
        myComponentBinding = componentBinding;

        if (buildInjectionContainer) {
            buildInjectingContainer();
        }
    }

    protected void buildInjectingContainer() {
        myMessageBus = MessageBusFactory.newMessageBus(myComponentBinding.topicBindingLoader(), this, myParent == null ? null : myParent.getMessageBus());

        MultiMap<String, InjectingBinding> mapByTopic = new MultiMap<>();

        fillListenerDescriptors(mapByTopic);

        myMessageBus.setLazyListeners(mapByTopic);

        myExtensionArea = new NewExtensionAreaImpl(this, myComponentBinding, getComponentScope(), this::checkCanceled, this::getApplication);

        myExtensionArea.registerFromInjectingBinding(getComponentScope());

        InjectingContainer root = findRootContainer();

        InjectingContainerBuilder builder = myParent == null ? root.childBuilder() : myParent.getInjectingContainer().childBuilder();

        registerServices(builder);

        loadServices(myNotLazyServices, builder);

        bootstrapInjectingContainer(builder);

        myInjectingContainer = builder.build();
    }

    public abstract ComponentManager getApplication();

    @Nullable
    @Override
    public ComponentManager getParent() {
        return myParent;
    }

    @Override
    public int getProfiles() {
        return ComponentProfiles.PRODUCTION;
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
            ModuleLayer moduleLayer = plugin.getModuleLayer();
            assert moduleLayer != null;
            root = InjectingContainer.root(moduleLayer);
        }
        else {
            root = InjectingContainer.root(getClass().getClassLoader());
        }
        return root;
    }

    protected void fillListenerDescriptors(MultiMap<String, InjectingBinding> mapByTopic) {
        InjectingBindingHolder holder = myComponentBinding.injectingBindingLoader().getHolder(TopicAPI.class, getComponentScope());

        for (List<InjectingBinding> bindings : holder.getBindings().values()) {
            for (InjectingBinding binding : bindings) {
                if (InjectingBindingHolder.isValid(binding, getProfiles())) {
                    mapByTopic.put(binding.getApiClassName(), bindings);
                }
            }
        }
    }

    protected void registerServices(InjectingContainerBuilder builder) {

    }

    protected void bootstrapInjectingContainer(@Nonnull InjectingContainerBuilder builder) {
        builder.bind(ComponentBinding.class).to(myComponentBinding);
    }

    @SuppressWarnings("unchecked")
    protected void loadServices(List<Class> notLazyServices, InjectingContainerBuilder builder) {
        InjectingBindingHolder holder = myComponentBinding.injectingBindingLoader().getHolder(ServiceAPI.class, getComponentScope());

        int profiles = getProfiles();

        for (List<InjectingBinding> listOfBindings : holder.getBindings().values()) {
            try {
                InjectingBinding injectingBinding = InjectingBindingHolder.findValid(listOfBindings, profiles);
                if (injectingBinding == null) {
                    LOG.error("There no valid binding " + listOfBindings);
                    continue;
                }

                InjectingKey<Object> key = InjectingKey.of(injectingBinding.getApiClass());
                InjectingKey<Object> implKey = InjectingKey.of(injectingBinding.getImplClass());

                InjectingPoint<Object> point = builder.bind(key);
                // bind to impl class
                point.to(implKey);
                // require singleton
                point.forceSingleton();
                // remap object initialization
                point.factory(objectProvider -> runServiceInitialize(injectingBinding, objectProvider::get));

                point.constructorParameterTypes(injectingBinding.getParameterTypes());
                point.constructorFactory(injectingBinding::create);

                point.injectListener((time, instance) -> {

                    if (myChecker.containsKey(key.getTargetClass())) {
                        throw new IllegalArgumentException("Duplicate init of " + key.getTargetClass());
                    }
                    myChecker.put(key.getTargetClass(), instance);

                    if (instance instanceof Disposable) {
                        Disposer.register(this, (Disposable) instance);
                    }

                    initializeIfStorableComponent(instance, true, injectingBinding.isLazy());
                });

                if (!injectingBinding.isLazy()) {
                    // if service is not lazy - add it for init at start
                    notLazyServices.add(key.getTargetClass());
                }
            }
            catch (Throwable e) {
                LOG.error(e);
            }
        }
    }

    protected <T> T runServiceInitialize(@Nonnull InjectingBinding binding, @Nonnull Supplier<T> runnable) {
        if (!myNotLazyStepFinished && !binding.isLazy() && myCurrentNotLazyServiceClass != null) {
            if (!Objects.equals(binding.getApiClass(), myCurrentNotLazyServiceClass) && InjectingContainer.LOG_INJECTING_PROBLEMS) {
                LOG.warn(new IllegalAccessException("Initializing not lazy service [" + binding.getApiClass().getName() + "] from another service [" + myCurrentNotLazyServiceClass.getName() + "]"));
            }
        }
        return runnable.get();
    }

    @Nonnull
    public final ComponentScope getComponentScope() {
        return myComponentScope;
    }

    public boolean initializeIfStorableComponent(@Nonnull Object component, boolean service, boolean lazy) {
        return false;
    }

    @Override
    public void initNotLazyServices() {
        try {
            if (myNotLazyStepFinished) {
                throw new IllegalArgumentException("Injector already build");
            }

            Object progressIndicator = initProgressIndicatorForLazyServices();

            StatCollector stat = new StatCollector();

            int i = 1;
            for (Class<?> serviceClass : myNotLazyServices) {
                try {
                    myCurrentNotLazyServiceClass = serviceClass;

                    checkCanceledAndChangeProgress(progressIndicator, i, myNotLazyServices.size());

                    stat.markWith(serviceClass.getName(), () -> getInstance(serviceClass));  // init it
                }
                catch (Throwable t) {
                    PluginExceptionUtil.logPluginError(LOG, t.getMessage(), t, serviceClass);
                }
                finally {
                    i++;
                }
            }

            if (i != 1) {
                stat.dump(getClass().getSimpleName() + " not lazy services initialize", LOG::info);
            }
        }
        finally {
            myCurrentNotLazyServiceClass = null;
            myNotLazyStepFinished = true;
        }
    }

    protected void checkCanceledAndChangeProgress(@Nullable Object progressIndicator, int pos, int maxPos) {
        checkCanceled();
    }

    @Nullable
    protected Object initProgressIndicatorForLazyServices() {
        return null;
    }

    @Override
    public int getNotLazyServicesCount() {
        return myNotLazyServices.size();
    }

    @Nonnull
    @Override
    public MessageBus getMessageBus() {
        if (myMessageBus == null) {
            throw throwDisposed();
        }
        return myMessageBus;
    }

    @Nullable
    @Override
    public <T> T getInstanceIfCreated(@Nonnull Class<T> clazz) {
        if (myInjectingContainer == null) {
            throw throwDisposed();
        }
        return getInjectingContainer().getInstanceIfCreated(clazz);
    }

    @Nonnull
    @Override
    public <T> T getInstance(@Nonnull Class<T> clazz) {
        if (myInjectingContainer == null) {
            throw throwDisposed();
        }
        return getInjectingContainer().getInstance(clazz);
    }

    @Nonnull
    @Override
    public InjectingContainer getInjectingContainer() {
        InjectingContainer container = myInjectingContainer;
        if (container == null || myDisposeState == ThreeState.YES) {
            throw throwDisposed();
        }
        return container;
    }

    @Nonnull
    private RuntimeException throwDisposed() {
        checkCanceled();
        return new ComponentManagerDisposedException("Already disposed: " + this);
    }

    protected void checkCanceled() {
    }

    @Nonnull
    @Override
    public <T> ExtensionPoint<T> getExtensionPoint(@Nonnull Class<T> extensionClass) {
        if (myExtensionArea == null) {
            throw throwDisposed();
        }
        
        return myExtensionArea.getExtensionPoint(extensionClass);
    }

    @TestOnly
    public void setTemporarilyDisposed(boolean disposed) {
        temporarilyDisposed = disposed;
    }

    @Override
    @Nonnull
    public BooleanSupplier getDisposed() {
        return myDisposedCondition;
    }

    @Override
    public boolean isDisposed() {
        return myDisposeState == ThreeState.YES;
    }

    protected boolean logSlowComponents() {
        return LOG.isDebugEnabled();
    }

    @Override
    @RequiredUIAccess
    public void dispose() {
        myDisposeState = ThreeState.UNSURE;

        if (myMessageBus != null) {
            Disposer.dispose((Disposable) myMessageBus);
            myMessageBus = null;
        }

        myExtensionArea = null;
        myInjectingContainer.dispose();
        myInjectingContainer = null;

        myNotLazyStepFinished = false;
        myNotLazyServices.clear();

        myDisposeState = ThreeState.YES;
    }

    @Override
    public String toString() {
        return myName;
    }
}
