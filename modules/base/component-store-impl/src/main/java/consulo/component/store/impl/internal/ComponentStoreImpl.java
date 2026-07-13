/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

import consulo.annotation.access.RequiredWriteAction;
import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.util.ConcurrentFactoryMap;
import consulo.component.ComponentManager;
import consulo.component.ProcessCanceledException;
import consulo.component.macro.PathMacroSubstitutor;
import consulo.component.messagebus.MessageBus;
import consulo.component.persist.*;
import consulo.component.store.impl.internal.storage.StorageUtil;
import consulo.component.store.internal.*;
import consulo.logging.Logger;
import consulo.ui.UIAccess;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.SmartHashSet;
import consulo.util.concurrent.coroutine.Continuation;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineContext;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.util.concurrent.coroutine.step.CallSubroutine;
import consulo.util.concurrent.coroutine.step.CodeExecution;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;
import jakarta.inject.Provider;
import org.jdom.Element;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class ComponentStoreImpl implements IComponentStore {
  private static final Logger LOG = Logger.getInstance(ComponentStoreImpl.class);
  private static ThreadLocal<Boolean> ourInsideSavingSessionLocal = ThreadLocal.withInitial(() -> Boolean.FALSE);

  public static void assertIfInsideSavingSession() {
    if (Objects.equals(ourInsideSavingSessionLocal.get(), Boolean.TRUE)) {
      throw new IllegalStateException("Can't call another thread inside saving session. Thread: " + Thread.currentThread());
    }
  }

  private final Map<String, StateComponentInfo<?>> myComponents = new ConcurrentHashMap<>();
  private final Map<String, Long> myComponentsModificationCount = ConcurrentFactoryMap.createMap(k -> -1L);

  private final List<SettingsSavingComponent> mySettingsSavingComponents = new CopyOnWriteArrayList<>();

  private final Provider<ApplicationDefaultStoreCache> myApplicationDefaultStoreCache;

  protected ComponentStoreImpl(Provider<ApplicationDefaultStoreCache> applicationDefaultStoreCache) {
    myApplicationDefaultStoreCache = applicationDefaultStoreCache;
  }

  @Override
  public <T> StateComponentInfo<T> loadStateIfStorable(T component) {
    if (component instanceof SettingsSavingComponent) {
      mySettingsSavingComponents.add((SettingsSavingComponent)component);
    }

    StateComponentInfo<T> componentInfo = StateComponentInfo.build(component, getProject());
    if (componentInfo == null) {
      return null;
    }

    try {
      loadState(componentInfo, null, false);
    }
    catch (StateStorageException | ProcessCanceledException e) {
      throw e;
    }
    catch (Exception e) {
      LOG.error(e);
    }
    return componentInfo;
  }

  @Override
  public Continuation<?> saveAsync(UIAccess uiAccess, List<Pair<StateStorage.SaveSession, File>> readonlyFiles) {
    CoroutineScope scope = CoroutineScope.of(createCoroutineContext());
    return createSaveCoroutine(readonlyFiles).runAsync(scope, null);
  }

  @Override
  public Coroutine<Object, Object> createSaveCoroutine(List<Pair<StateStorage.SaveSession, File>> readonlyFiles) {
    boolean force = false;

    StateStorageManager.ExternalizationSession externalizationSession = myComponents.isEmpty() ? null : getStateStorageManager().startExternalization();

    String[] names;
    if (externalizationSession != null) {
      names = ArrayUtil.toStringArray(myComponents.keySet());
      Arrays.sort(names);
    }
    else {
      names = new String[0];
    }

    Map<String, Object> states = new ConcurrentHashMap<>();

    Coroutine<Object, Object> chain = Coroutine.first(CodeExecution.<Object, Object>apply(input -> input));

    for (String name : names) {
      StateComponentInfo<?> componentInfo = myComponents.get(name);
      if (isUnchangedByModTracker(componentInfo, force)) {
        continue;
      }

      PersistentStateComponent<?> component = componentInfo.getComponent();
      if (component instanceof PersistentStateComponentAsync) {
        @SuppressWarnings({"unchecked", "rawtypes"})
        Coroutine<Object, Object> stateCoroutine = (Coroutine)((PersistentStateComponentAsync<?>)component).getStateAsync();
        chain = chain
          .then(CallSubroutine.call(stateCoroutine))
          .then(CodeExecution.<Object, Object>apply(state -> {
            if (state != null) {
              states.put(name, state);
            }
            return null;
          }));
      }
      else {
        chain = chain.then(CodeExecution.<Object, Object>apply(input -> {
          Object state = component.getState();
          if (state != null) {
            states.put(name, state);
          }
          return null;
        }));
      }
    }

    StateStorageManager.ExternalizationSession finalSession = externalizationSession;
    String[] finalNames = names;
    chain = chain.then(CodeExecution.<Object, Object>apply(input -> {
      if (finalSession != null) {
        for (String name : finalNames) {
          commitComponent(myComponents.get(name), finalSession, states.get(name), force);
        }
      }

      for (SettingsSavingComponent settingsSavingComponent : mySettingsSavingComponents) {
        try {
          settingsSavingComponent.save();
        }
        catch (Throwable e) {
          LOG.error(e);
        }
      }

      doSave(force, finalSession == null ? null : finalSession.createSaveSessions(force), readonlyFiles);
      return input;
    }));

    return chain;
  }

  protected abstract CoroutineContext createCoroutineContext();

  protected void doSave(boolean force, @Nullable List<StateStorage.SaveSession> saveSessions, List<Pair<StateStorage.SaveSession, File>> readonlyFiles) {
    if (saveSessions != null) {
      for (StateStorage.SaveSession session : saveSessions) {
        executeSave(session, force, readonlyFiles);
      }
    }
  }

  protected static void executeSave(StateStorage.SaveSession session, boolean force, List<Pair<StateStorage.SaveSession, File>> readonlyFiles) {
    try {
      session.save(force);
    }
    catch (ReadOnlyModificationException e) {
      readonlyFiles.add(Pair.create(session, e.getFile()));
    }
  }

  private boolean isUnchangedByModTracker(StateComponentInfo<?> componentInfo, boolean force) {
    PersistentStateComponent<?> component = componentInfo.getComponent();
    if (component instanceof PersistentStateComponentWithModificationTracker && !force) {
      long count = ((PersistentStateComponentWithModificationTracker<?>)component).getStateModificationCount();
      return count == myComponentsModificationCount.get(componentInfo.getName());
    }
    return false;
  }

  @SuppressWarnings({"unchecked", "RequiredXAction"})
  private <T> void commitComponent(StateComponentInfo<T> componentInfo, StateStorageManager.ExternalizationSession session, @Nullable Object stateObject, boolean force) {
    if (stateObject == null) {
      return;
    }

    PersistentStateComponent<T> component = componentInfo.getComponent();
    T state = (T)stateObject;

    Storage[] storageSpecs = getComponentStorageSpecs(component, componentInfo.getState(), StateStorageOperation.WRITE);
    session.setState(storageSpecs, component, componentInfo.getName(), state);

    if (component instanceof PersistentStateComponentWithModificationTracker && !force) {
      long count = ((PersistentStateComponentWithModificationTracker<T>)component).getStateModificationCount();
      myComponentsModificationCount.put(componentInfo.getName(), count);
    }
  }

  private void doAddComponent(String componentName, StateComponentInfo<?> stateComponentInfo) {
    StateComponentInfo<?> existing = myComponents.get(componentName);
    if (existing != null && !existing.equals(stateComponentInfo)) {
      LOG.error("Conflicting component name '" + componentName + "': " + existing.getComponent().getClass() + " and " + stateComponentInfo.getComponent().getClass());
    }
    myComponents.put(componentName, stateComponentInfo);
  }

  protected @Nullable ComponentManager getProject() {
    return null;
  }

  private void validateUnusedMacros(@Nullable String componentName, boolean service) {
    ComponentManager project = getProject();
    if (project == null) return;

    if (!ApplicationManager.getApplication().isHeadlessEnvironment() && !ApplicationManager.getApplication().isUnitTestMode()) {
      if (service && componentName != null && project.isInitialized()) {
        TrackingPathMacroSubstitutor substitutor = getStateStorageManager().getMacroSubstitutor();
        if (substitutor != null) {
          StorageUtil.notifyUnknownMacros(substitutor, project, componentName);
        }
      }
    }
  }

  private <T> void loadState(StateComponentInfo<T> componentInfo, @Nullable Collection<? extends StateStorage> changedStorages, boolean reloadData) {
    PersistentStateComponent<T> component = componentInfo.getComponent();
    State stateSpec = componentInfo.getState();
    String name = stateSpec.name();

    if (changedStorages == null || !reloadData) {
      doAddComponent(name, componentInfo);
    }

    Class<T> stateClass = ComponentSerializationUtil.getStateClass(component.getClass());
    T state = null;

    Storage[] storageSpecs = getComponentStorageSpecs(component, stateSpec, StateStorageOperation.READ);
    for (Storage storageSpec : storageSpecs) {
      StateStorage stateStorage = getStateStorageManager().getStateStorage(storageSpec);
      if (stateStorage != null && (stateStorage.hasState(component, name, stateClass, reloadData) || (changedStorages != null && changedStorages.contains(stateStorage)))) {
        state = stateStorage.getState(component, name, stateClass);
        break;
      }
    }

    if (state != null) {
      component.loadState(state);

      storeModificationCountAfterLoad(component, componentInfo);
    }
    else {
      T defaultState = loadDefaultState(componentInfo, component, stateClass);
      if (defaultState != null) {
        component.loadState(defaultState);

        storeModificationCountAfterLoad(component, componentInfo);
      }
    }

    validateUnusedMacros(name, true);
  }

  private <T> void storeModificationCountAfterLoad(PersistentStateComponent<T> component, StateComponentInfo<T> componentInfo) {
    if (component instanceof PersistentStateComponentWithModificationTracker) {
      long modCount = ((PersistentStateComponentWithModificationTracker<T>)component).getStateModificationCount();

      myComponentsModificationCount.put(componentInfo.getName(), modCount);
    }
  }

  protected @Nullable PathMacroSubstitutor getPathMacroManagerForDefaults() {
    return null;
  }

  private @Nullable <T> T loadDefaultState(StateComponentInfo<T> stateComponentInfo, Object component, Class<T> stateClass) {
    String defaultStateFilePath = stateComponentInfo.getState().defaultStateFilePath();

    if (StringUtil.isEmpty(defaultStateFilePath)) {
      return null;
    }

    try {
      Element element = myApplicationDefaultStoreCache.get().findDefaultStoreElement(component.getClass(), defaultStateFilePath);
      if (element != null) {
        return deserializeDefaultStore(element.clone(), stateClass);
      }
    }
    catch (Exception e) {
      throw new StateStorageException("Error loading default state from: " + defaultStateFilePath + ", component: " + component, e);
    }
    return null;
  }

  private @Nullable <T> T deserializeDefaultStore(Element documentElement, Class<T> stateClass) {
    PathMacroSubstitutor pathMacroManager = getPathMacroManagerForDefaults();
    if (pathMacroManager != null) {
      pathMacroManager.expandPaths(documentElement);
    }

    return DefaultStateSerializer.deserializeState(documentElement, stateClass);
  }

  
  protected <T> Storage[] getComponentStorageSpecs(PersistentStateComponent<T> persistentStateComponent, State stateSpec, StateStorageOperation operation) {
    Storage[] storages = stateSpec.storages();
    if (storages.length == 1) {
      return storages;
    }
    assert storages.length > 0;

    int actualStorageCount = 0;
    for (Storage storage : storages) {
      if (!storage.deprecated()) {
        actualStorageCount++;
      }
    }

    if (actualStorageCount > 1) {
      LOG.error("State chooser not specified for: " + persistentStateComponent.getClass());
    }

    if (!storages[0].deprecated()) {
      boolean othersAreDeprecated = true;
      for (int i = 1; i < storages.length; i++) {
        if (!storages[i].deprecated()) {
          othersAreDeprecated = false;
          break;
        }
      }

      if (othersAreDeprecated) {
        return storages;
      }
    }

    Storage[] sorted = Arrays.copyOf(storages, storages.length);
    Arrays.sort(sorted, (o1, o2) -> {
      int w1 = o1.deprecated() ? 1 : 0;
      int w2 = o2.deprecated() ? 1 : 0;
      return w1 - w2;
    });
    return sorted;
  }

  @Override
  public void reinitComponents(Set<String> componentNames, boolean reloadData) {
    reinitComponents(componentNames, Collections.<StateStorage>emptySet());
  }

  protected boolean reinitComponent(String componentName, Collection<? extends StateStorage> changedStorages) {
    StateComponentInfo<?> componentInfo = myComponents.get(componentName);
    if (componentInfo == null) {
      return false;
    }

    boolean changedStoragesEmpty = changedStorages.isEmpty();
    loadState(componentInfo, changedStoragesEmpty ? null : changedStorages, changedStoragesEmpty);
    return true;
  }

  
  protected abstract MessageBus getMessageBus();

  @Override
  public boolean reload(Collection<? extends StateStorage> changedStorages) {
    if (changedStorages.isEmpty()) {
      return false;
    }

    Set<String> componentNames = new SmartHashSet<>();
    for (StateStorage storage : changedStorages) {
      try {
        // we must update (reload in-memory storage data) even if non-reloadable component will be detected later
        // not saved -> user does own modification -> new (on disk) state will be overwritten and not applied
        storage.analyzeExternalChangesAndUpdateIfNeed(componentNames);
      }
      catch (Throwable e) {
        LOG.error(e);
      }
    }

    if (componentNames.isEmpty()) {
      return false;
    }

    reinitComponents(componentNames, changedStorages);
    return true;
  }

  private void reinitComponents(Set<String> componentNames, Collection<? extends StateStorage> changedStorages) {
    MessageBus messageBus = getMessageBus();
    messageBus.syncPublisher(BatchUpdateListener.class).onBatchUpdateStarted();
    try {
      for (String componentName : componentNames) {
        reinitComponent(componentName, changedStorages);
      }
    }
    finally {
      messageBus.syncPublisher(BatchUpdateListener.class).onBatchUpdateFinished();
    }
  }
}
