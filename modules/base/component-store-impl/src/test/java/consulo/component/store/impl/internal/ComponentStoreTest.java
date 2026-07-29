/*
 * Copyright 2013-2026 consulo.io
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
package consulo.component.store.impl.internal;

import consulo.component.messagebus.MessageBus;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.PersistentStateComponentAsync;
import consulo.component.persist.PersistentStateComponentWithAsyncGet;
import consulo.component.persist.PersistentStateComponentWithModificationTracker;
import consulo.component.persist.RoamingType;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.store.internal.StateComponentInfo;
import consulo.component.store.internal.StateStorage;
import consulo.component.store.internal.StateStorageManager;
import consulo.component.store.internal.StreamProvider;
import consulo.component.store.internal.TrackingPathMacroSubstitutor;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineContext;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.util.concurrent.coroutine.step.CodeExecution;
import jakarta.inject.Provider;
import org.jdom.Element;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ComponentStoreTest {
    private ExecutorService myExecutor;
    private ScheduledExecutorService myScheduler;
    private InMemoryStorage myStorage;
    private TestStore myStore;

    @BeforeEach
    public void setUp() {
        myExecutor = Executors.newSingleThreadExecutor();
        myScheduler = Executors.newSingleThreadScheduledExecutor();
        myStorage = new InMemoryStorage();
        myStore = new TestStore(myStorage, CoroutineContext.of(myExecutor, myScheduler));
    }

    @AfterEach
    public void tearDown() {
        myExecutor.shutdownNow();
        myScheduler.shutdownNow();
    }

    @Test
    public void nonStorableComponentIsIgnored() {
        assertThat(myStore.loadStateIfStorable("plain object")).isNull();
    }

    @Test
    public void loadAppliesStoredStateAndCallsAfterLoad() {
        myStorage.put("Simple", option("name", "stored"));

        Simple component = new Simple();
        StateComponentInfo info = Objects.requireNonNull(myStore.loadStateIfStorable(component));

        assertThat(info.getName()).isEqualTo("Simple");
        assertThat(component.myLoadedName).isEqualTo("stored");
        assertThat(component.myAfterLoadFlags).isEmpty();

        info.afterLoad(true);
        assertThat(component.myAfterLoadFlags).containsExactly(true);
    }

    @Test
    public void reinitCallsAfterLoadWithFirstFalse() {
        Simple component = new Simple();
        myStore.loadStateIfStorable(component);
        component.myAfterLoadFlags.clear();

        myStorage.put("Simple", option("name", "changed-on-disk"));
        myStore.reinitComponents(Collections.singleton("Simple")).await();

        assertThat(component.myAfterLoadFlags).containsExactly(false);
    }

    @Test
    public void reinitCallsAfterLoadEvenWhenNothingStored() {
        Simple component = new Simple();
        myStore.loadStateIfStorable(component);
        component.myAfterLoadFlags.clear();

        myStore.reinitComponents(Collections.singleton("Simple")).await();

        assertThat(component.myAfterLoadFlags).containsExactly(false);
    }

    @Test
    public void asyncReinitCallsAfterLoadWithFirstFalse() {
        FullyAsync component = new FullyAsync();
        loadAsync(component);
        component.myAfterLoadFlags.clear();

        myStorage.put("FullyAsync", option("name", "changed-on-disk"));
        myStore.reinitComponents(Collections.singleton("FullyAsync")).await();

        assertThat(component.myAfterLoadFlags).containsExactly(false);
    }

    @Test
    public void loadLeavesComponentUntouchedWhenNothingStored() {
        Simple component = new Simple();
        myStore.loadStateIfStorable(component);

        assertThat(component.myLoadCount).isZero();
    }

    @Test
    public void saveWritesComponentState() {
        Simple component = new Simple();
        myStore.loadStateIfStorable(component);
        component.myName = "written";

        save();

        assertThat(optionValue("Simple", "name")).isEqualTo("written");
    }

    @Test
    public void saveSkipsComponentReturningNullState() {
        NullState component = new NullState();
        myStore.loadStateIfStorable(component);

        save();

        assertThat(myStorage.get("NullState")).isNull();
        assertThat(component.myGetStateCount).isEqualTo(1);
    }

    @Test
    public void modificationTrackerSkipsUnchangedComponent() {
        Tracked component = new Tracked();
        component.myModificationCount = 1;
        myStore.loadStateIfStorable(component);

        component.myName = "first";
        save();
        assertThat(optionValue("Tracked", "name")).isEqualTo("first");

        component.myName = "second";
        save();
        assertThat(optionValue("Tracked", "name")).isEqualTo("first");

        component.myModificationCount = 2;
        save();
        assertThat(optionValue("Tracked", "name")).isEqualTo("second");
    }

    /**
     * The seed of ComponentStoreImpl#myComponentsModificationCount must not collide with a value a component
     * can actually report, otherwise a tracker sitting at that value is skipped before it is ever written.
     */
    @Test
    public void modificationTrackerAtMinusOneIsStillSavedOnce() {
        Tracked component = new Tracked();
        component.myModificationCount = -1;
        myStore.loadStateIfStorable(component);

        component.myName = "persisted";
        save();
        assertThat(optionValue("Tracked", "name")).isEqualTo("persisted");

        component.myName = "not-persisted";
        save();
        assertThat(optionValue("Tracked", "name")).isEqualTo("persisted");
    }

    @Test
    public void untrackedAsyncComponentIsSavedOnEverySave() {
        FullyAsync component = new FullyAsync();
        loadAsync(component);

        component.myName = "first";
        save();
        assertThat(optionValue("FullyAsync", "name")).isEqualTo("first");

        component.myName = "second";
        save();
        assertThat(optionValue("FullyAsync", "name")).isEqualTo("second");
    }

    @Test
    public void asyncComponentLoadsThroughItsCoroutine() {
        myStorage.put("FullyAsync", option("name", "stored"));

        FullyAsync component = new FullyAsync();
        loadAsync(component);

        assertThat(component.myLoadCount).isEqualTo(1);
        assertThat(component.myName).isEqualTo("stored");
    }

    @Test
    public void asyncComponentRejectsSynchronousLoad() {
        assertThatThrownBy(() -> myStore.loadStateIfStorable(new FullyAsync()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("loadStateIfStorableAsync");
    }

    @Test
    public void asyncReinitReloadsThroughItsCoroutine() {
        FullyAsync component = new FullyAsync();
        loadAsync(component);

        myStorage.put("FullyAsync", option("name", "changed-on-disk"));
        myStore.reinitComponents(Collections.singleton("FullyAsync")).await();

        assertThat(component.myName).isEqualTo("changed-on-disk");
    }

    @Test
    public void asyncGetComponentIsSavedThroughItsCoroutine() {
        AsyncGet component = new AsyncGet();
        myStore.loadStateIfStorable(component);
        component.myName = "from-coroutine";

        save();

        assertThat(component.myGetStateAsyncCount).isEqualTo(1);
        assertThat(optionValue("AsyncGet", "name")).isEqualTo("from-coroutine");
    }

    @Test
    public void reinitReloadsComponentFromChangedStorage() {
        Simple component = new Simple();
        myStore.loadStateIfStorable(component);
        assertThat(component.myLoadCount).isZero();

        myStorage.put("Simple", option("name", "changed-on-disk"));
        myStore.reinitComponents(Collections.singleton("Simple")).await();

        assertThat(component.myLoadedName).isEqualTo("changed-on-disk");
    }

    private void save() {
        myStore.createSaveCoroutine(new ArrayList<>()).runBlocking(CoroutineScope.of(myStore.myContext), null);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void loadAsync(Object component) {
        ((Coroutine)myStore.loadStateIfStorableAsync(component)).runBlocking(CoroutineScope.of(myStore.myContext), null);
    }

    private @Nullable String optionValue(String componentName, String optionName) {
        Element element = myStorage.get(componentName);
        if (element == null) {
            return null;
        }
        for (Element child : element.getChildren("option")) {
            if (optionName.equals(child.getAttributeValue("name"))) {
                return child.getAttributeValue("value");
            }
        }
        return null;
    }

    private static Element option(String name, String value) {
        Element state = new Element("state");
        Element option = new Element("option");
        option.setAttribute("name", name);
        option.setAttribute("value", value);
        state.addContent(option);
        return state;
    }

    private static class TestStore extends ComponentStoreImpl {
        private final StateStorageManager myStateStorageManager;
        private final CoroutineContext myContext;

        private TestStore(InMemoryStorage storage, CoroutineContext context) {
            super(nullCacheProvider());
            myStateStorageManager = new InMemoryStorageManager(storage);
            myContext = context;
        }

        @SuppressWarnings("unchecked")
        private static Provider<ApplicationDefaultStoreCache> nullCacheProvider() {
            return () -> null;
        }

        @Override
        public void load() {
        }

        @Override
        public StateStorageManager getStateStorageManager() {
            return myStateStorageManager;
        }

        @Override
        public CoroutineContext createCoroutineContext() {
            return myContext;
        }

        @Override
        protected MessageBus getMessageBus() {
            return (MessageBus)Proxy.newProxyInstance(MessageBus.class.getClassLoader(), new Class<?>[]{MessageBus.class}, (proxy, method, args) -> {
                if ("syncPublisher".equals(method.getName())) {
                    Class<?> topic = (Class<?>)Objects.requireNonNull(args)[0];
                    return Proxy.newProxyInstance(topic.getClassLoader(), new Class<?>[]{topic}, (p, m, a) -> null);
                }
                return null;
            });
        }
    }

    private static class InMemoryStorage implements StateStorage {
        private final Map<String, Element> myElements = new LinkedHashMap<>();

        private @Nullable Element get(String componentName) {
            return myElements.get(componentName);
        }

        private void put(String componentName, Element element) {
            myElements.put(componentName, element);
        }

        @Override
        public <T> @Nullable T getState(@Nullable Object component, String componentName, Class<T> stateClass) {
            return DefaultStateSerializer.deserializeState(myElements.get(componentName), stateClass);
        }

        @Override
        public boolean hasState(@Nullable Object component, String componentName, Class<?> aClass, boolean reloadData) {
            return myElements.containsKey(componentName);
        }

        @Override
        public @Nullable ExternalizationSession startExternalization() {
            return new ExternalizationSession() {
                private final Map<String, Element> myPending = new LinkedHashMap<>();

                @Override
                public void setState(Object component, String componentName, Object state, @Nullable Storage storageSpec) {
                    Element element = DefaultStateSerializer.serializeState(state, storageSpec);
                    if (element != null) {
                        myPending.put(componentName, element);
                    }
                }

                @Override
                public @Nullable SaveSession createSaveSession(boolean force) {
                    if (myPending.isEmpty()) {
                        return null;
                    }
                    Map<String, Element> committed = new LinkedHashMap<>(myPending);
                    return saveForce -> myElements.putAll(committed);
                }
            };
        }

        @Override
        public void analyzeExternalChangesAndUpdateIfNeed(Set<String> result) {
            result.addAll(myElements.keySet());
        }
    }

    private static class InMemoryStorageManager implements StateStorageManager {
        private final InMemoryStorage myStorage;
        private final Map<String, String> myMacros = new HashMap<>();

        private InMemoryStorageManager(InMemoryStorage storage) {
            myStorage = storage;
        }

        @Override
        public void addMacro(String macro, String expansion) {
            myMacros.put(macro, expansion);
        }

        @Override
        public String buildFileSpec(Storage storage) {
            return storage.value();
        }

        @Override
        public @Nullable TrackingPathMacroSubstitutor getMacroSubstitutor() {
            return null;
        }

        @Override
        public @Nullable StateStorage getStateStorage(Storage storageSpec) {
            return myStorage;
        }

        @Override
        public @Nullable StateStorage getStateStorage(String fileSpec, RoamingType roamingType) {
            return myStorage;
        }

        @Override
        public Collection<String> getStorageFileNames() {
            return Collections.emptyList();
        }

        @Override
        public void clearStateStorage(String file) {
        }

        @Override
        public @Nullable ExternalizationSession startExternalization() {
            StateStorage.ExternalizationSession delegate = Objects.requireNonNull(myStorage.startExternalization());
            return new ExternalizationSession() {
                @Override
                public void setState(Storage[] storageSpecs, Object component, String componentName, Object state) {
                    delegate.setState(component, componentName, state, storageSpecs[0]);
                }

                @Override
                public List<StateStorage.SaveSession> createSaveSessions(boolean force) {
                    StateStorage.SaveSession session = delegate.createSaveSession(force);
                    return session == null ? Collections.emptyList() : Collections.singletonList(session);
                }
            };
        }

        @Override
        public String expandMacros(String file) {
            return file;
        }

        @Override
        public String collapseMacros(String path) {
            return path;
        }

        @Override
        public void setStreamProvider(@Nullable StreamProvider streamProvider) {
        }

        @Override
        public @Nullable StreamProvider getStreamProvider() {
            return null;
        }
    }

    public static class Bean {
        public String name = "default";
    }

    @State(name = "Simple", storages = @Storage("simple.xml"))
    public static class Simple implements PersistentStateComponent<Bean> {
        public String myName = "default";
        public String myLoadedName = "";
        public int myLoadCount;
        public final List<Boolean> myAfterLoadFlags = new ArrayList<>();

        @Override
        public @Nullable Bean getState() {
            Bean bean = new Bean();
            bean.name = myName;
            return bean;
        }

        @Override
        public void loadState(Bean state) {
            myName = state.name;
            myLoadedName = state.name;
            myLoadCount++;
        }

        @Override
        public void afterLoad(boolean first) {
            myAfterLoadFlags.add(first);
        }
    }

    @State(name = "NullState", storages = @Storage("null-state.xml"))
    public static class NullState implements PersistentStateComponent<Bean> {
        public int myGetStateCount;

        @Override
        public @Nullable Bean getState() {
            myGetStateCount++;
            return null;
        }

        @Override
        public void loadState(Bean state) {
        }
    }

    @State(name = "Tracked", storages = @Storage("tracked.xml"))
    public static class Tracked implements PersistentStateComponentWithModificationTracker<Bean> {
        public String myName = "default";
        public long myModificationCount;
        public int myGetStateCount;

        @Override
        public long getStateModificationCount() {
            return myModificationCount;
        }

        @Override
        public @Nullable Bean getState() {
            myGetStateCount++;
            Bean bean = new Bean();
            bean.name = myName;
            return bean;
        }

        @Override
        public void loadState(Bean state) {
            myName = state.name;
        }
    }

    @State(name = "FullyAsync", storages = @Storage("fully-async.xml"))
    public static class FullyAsync implements PersistentStateComponentAsync<Bean> {
        public String myName = "default";
        public int myLoadCount;
        public final List<Boolean> myAfterLoadFlags = new ArrayList<>();

        @Override
        public Coroutine<?, Bean> getState() {
            return Coroutine.first(CodeExecution.apply(input -> {
                Bean bean = new Bean();
                bean.name = myName;
                return bean;
            }));
        }

        @Override
        public Coroutine<?, ?> loadState(Bean state) {
            return Coroutine.first(CodeExecution.apply(input -> {
                myName = state.name;
                myLoadCount++;
                return null;
            }));
        }

        @Override
        public Coroutine<?, ?> afterLoad(boolean first) {
            return Coroutine.first(CodeExecution.apply(input -> {
                myAfterLoadFlags.add(first);
                return null;
            }));
        }
    }

    @State(name = "AsyncGet", storages = @Storage("async-get.xml"))
    public static class AsyncGet implements PersistentStateComponentWithAsyncGet<Bean> {
        public String myName = "default";
        public int myGetStateAsyncCount;

        @Override
        public Coroutine<?, Bean> getStateAsync() {
            return Coroutine.first(CodeExecution.apply(input -> {
                myGetStateAsyncCount++;
                Bean bean = new Bean();
                bean.name = myName;
                return bean;
            }));
        }

        @Override
        public void loadState(Bean state) {
            myName = state.name;
        }
    }
}
