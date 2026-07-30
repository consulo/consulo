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

import consulo.application.ApplicationManager;

import consulo.application.util.ConcurrentFactoryMap;
import consulo.component.ComponentManager;
import consulo.component.ProcessCanceledException;
import consulo.component.internal.StateComponent;
import consulo.component.macro.PathMacroSubstitutor;
import consulo.component.messagebus.MessageBus;
import consulo.component.persist.*;
import consulo.component.store.impl.internal.storage.StorageUtil;
import consulo.component.store.impl.internal.storage.VfsEventCollectingSaveSession;
import consulo.component.store.internal.*;
import consulo.logging.Logger;
import consulo.virtualFileSystem.RefreshQueue;
import consulo.virtualFileSystem.event.VFileEvent;
import consulo.ui.UIAccess;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.SmartHashSet;
import consulo.util.concurrent.coroutine.Continuation;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineContext;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.util.concurrent.coroutine.CoroutineStep;
import consulo.util.concurrent.coroutine.step.CallSubroutine;
import consulo.util.concurrent.coroutine.step.CodeExecution;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;
import jakarta.inject.Provider;
import org.jdom.Element;

import java.io.File;
import java.util.*;
import java.util.function.Function;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class ComponentStoreImpl implements IComponentStore {
  private static final Logger LOG = Logger.getInstance(ComponentStoreImpl.class);
  private static ThreadLocal<Boolean> ourInsideSavingSessionLocal = ThreadLocal.withInitial(() -> Boolean.FALSE);

  /**
   * Seed of {@link #myComponentsModificationCount}. Deliberately distinct from
   * {@link PersistentStateComponentAsync#UNTRACKED}, so that a component reporting UNTRACKED never compares
   * equal to the seed and gets silently skipped on every save.
   */
  private static final long UNKNOWN_MODIFICATION_COUNT = Long.MIN_VALUE;

  public static void assertIfInsideSavingSession() {
    if (Objects.equals(ourInsideSavingSessionLocal.get(), Boolean.TRUE)) {
      throw new IllegalStateException("Can't call another thread inside saving session. Thread: " + Thread.currentThread());
    }
  }

  private final Map<String, StateComponentInfo> myComponents = new ConcurrentHashMap<>();
  private final Map<String, Long> myComponentsModificationCount = ConcurrentFactoryMap.createMap(k -> UNKNOWN_MODIFICATION_COUNT);

  private final List<SettingsSavingComponent> mySettingsSavingComponents = new CopyOnWriteArrayList<>();

  private final Provider<ApplicationDefaultStoreCache> myApplicationDefaultStoreCache;

  protected ComponentStoreImpl(Provider<ApplicationDefaultStoreCache> applicationDefaultStoreCache) {
    myApplicationDefaultStoreCache = applicationDefaultStoreCache;
  }

  @Override
  public @Nullable StateComponentInfo loadStateIfStorable(Object component) {
    StateComponentInfo componentInfo = registerIfStorable(component);
    if (componentInfo == null) {
      return null;
    }

    if (componentInfo.isAsync()) {
      throw new IllegalArgumentException("Async state component must be loaded via #loadStateIfStorableAsync: " + component.getClass().getName());
    }

    try {
      Object state = readState(componentInfo, null, false);
      if (state != null) {
        applyState(componentInfo, state);
      }
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
  @SuppressWarnings({"unchecked", "rawtypes"})
  public Coroutine<?, StateComponentInfo> loadStateIfStorableAsync(Object component) {
    StateComponentInfo componentInfo = registerIfStorable(component);
    if (componentInfo == null) {
      return Coroutine.first(CodeExecution.apply(input -> null));
    }

    if (!componentInfo.isAsync()) {
      return Coroutine.first(CodeExecution.apply(input -> {
        loadSyncStateReportingErrors(componentInfo);
        return componentInfo;
      }));
    }

    PersistentStateComponentAsync<Object> asyncComponent = (PersistentStateComponentAsync<Object>)componentInfo.getComponent();
    Object[] stateHolder = new Object[1];

    return Coroutine.<Object, Object>first(CodeExecution.apply(input -> {
        stateHolder[0] = readState(componentInfo, null, false);
        return null;
      }))
      .then(CallSubroutine.call(() -> {
        Object state = stateHolder[0];
        return state == null ? Coroutine.empty() : (Coroutine)asyncComponent.loadState(state);
      }))
      .then(CodeExecution.apply(input -> {
        if (stateHolder[0] != null) {
          storeModificationCountAfterLoad(componentInfo);
        }
        return componentInfo;
      }));
  }

  private @Nullable StateComponentInfo registerIfStorable(Object component) {
    if (component instanceof SettingsSavingComponent) {
      mySettingsSavingComponents.add((SettingsSavingComponent)component);
    }

    return StateComponentInfo.build(component, getProject());
  }

  private void loadSyncStateReportingErrors(StateComponentInfo componentInfo) {
    try {
      Object state = readState(componentInfo, null, false);
      if (state != null) {
        applyState(componentInfo, state);
      }
    }
    catch (StateStorageException | ProcessCanceledException e) {
      throw e;
    }
    catch (Exception e) {
      LOG.error(e);
    }
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
      StateComponentInfo componentInfo = myComponents.get(name);
      if (isUnchangedByModTracker(componentInfo, force)) {
        continue;
      }

      StateComponent component = componentInfo.getComponent();
      if (component instanceof PersistentStateComponentAsync) {
        @SuppressWarnings({"unchecked", "rawtypes"})
        Coroutine<Object, Object> stateCoroutine = (Coroutine)((PersistentStateComponentAsync<?>)component).getState();
        chain = chain
          .then(CallSubroutine.call(stateCoroutine))
          .then(CodeExecution.<Object, Object>apply(state -> {
            if (state != null) {
              states.put(name, state);
            }
            return null;
          }));
      }
      else if (component instanceof PersistentStateComponentWithAsyncGet) {
        @SuppressWarnings({"unchecked", "rawtypes"})
        Coroutine<Object, Object> stateCoroutine = (Coroutine)((PersistentStateComponentWithAsyncGet<?>)component).getStateAsync();
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
          Object state = ((PersistentStateComponent<?>)component).getState();
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

  @Override
  public abstract CoroutineContext createCoroutineContext();

  /**
   * The step applying reloaded state to a synchronous component. Scope stores override this to take the write
   * lock; the default runs unguarded, which is what stores without an {@code Application} need.
   */
  protected CoroutineStep<Object, Object> applyStateStep(Function<Object, Object> function) {
    return CodeExecution.apply(function);
  }

  protected void doSave(boolean force, @Nullable List<StateStorage.SaveSession> saveSessions, List<Pair<StateStorage.SaveSession, File>> readonlyFiles) {
    if (saveSessions == null) {
      return;
    }

    // Collect the VFS content-change events from all storages and apply them once, synchronously, after every write.
    // This keeps the VFS record in sync with the lock-free NIO writes before any later filesystem refresh can observe a
    // diff (and thus report a bogus external change / project reload).
    List<VFileEvent> events = new ArrayList<>();
    for (StateStorage.SaveSession session : saveSessions) {
      executeSave(session, force, readonlyFiles, events);
    }

    if (!events.isEmpty()) {
      RefreshQueue.getInstance().processEvents(events);
    }
  }

  protected static void executeSave(StateStorage.SaveSession session, boolean force, List<Pair<StateStorage.SaveSession, File>> readonlyFiles, @Nullable List<VFileEvent> events) {
    try {
      if (session instanceof VfsEventCollectingSaveSession collecting) {
        collecting.save(force, events);
      }
      else {
        session.save(force);
      }
    }
    catch (ReadOnlyModificationException e) {
      readonlyFiles.add(Pair.create(session, e.getFile()));
    }
  }

  /**
   * @return the component's modification count, or {@link #UNKNOWN_MODIFICATION_COUNT} when it does not track
   * modifications and must therefore always be serialized
   */
  private static long modificationCountOf(StateComponent component) {
    if (component instanceof PersistentStateComponentWithModificationTracker) {
      return ((PersistentStateComponentWithModificationTracker<?>)component).getStateModificationCount();
    }
    if (component instanceof PersistentStateComponentAsync) {
      long count = ((PersistentStateComponentAsync<?>)component).getStateModificationCount();
      return count == PersistentStateComponentAsync.UNTRACKED ? UNKNOWN_MODIFICATION_COUNT : count;
    }
    return UNKNOWN_MODIFICATION_COUNT;
  }

  private boolean isUnchangedByModTracker(StateComponentInfo componentInfo, boolean force) {
    if (force) {
      return false;
    }

    long count = modificationCountOf(componentInfo.getComponent());
    if (count == UNKNOWN_MODIFICATION_COUNT) {
      return false;
    }
    return count == myComponentsModificationCount.get(componentInfo.getName());
  }

  @SuppressWarnings("RequiredXAction")
  private void commitComponent(StateComponentInfo componentInfo, StateStorageManager.ExternalizationSession session, @Nullable Object stateObject, boolean force) {
    if (stateObject == null) {
      return;
    }

    StateComponent component = componentInfo.getComponent();

    Storage[] storageSpecs = getComponentStorageSpecs(component, componentInfo.getState(), StateStorageOperation.WRITE);
    session.setState(storageSpecs, component, componentInfo.getName(), stateObject);

    if (!force) {
      long count = modificationCountOf(component);
      if (count != UNKNOWN_MODIFICATION_COUNT) {
        myComponentsModificationCount.put(componentInfo.getName(), count);
      }
    }
  }

  private void doAddComponent(String componentName, StateComponentInfo stateComponentInfo) {
    StateComponentInfo existing = myComponents.get(componentName);
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

  /**
   * Registers the component and reads its persisted state, falling back to the bundled default state.
   *
   * @return the state to apply, or null when neither the storages nor a default file provide one
   */
  private @Nullable Object readState(StateComponentInfo componentInfo, @Nullable Collection<? extends StateStorage> changedStorages, boolean reloadData) {
    StateComponent component = componentInfo.getComponent();
    State stateSpec = componentInfo.getState();
    String name = stateSpec.name();

    if (changedStorages == null || !reloadData) {
      doAddComponent(name, componentInfo);
    }

    Class<?> stateClass = componentInfo.getStateClass();
    Object state = null;

    Storage[] storageSpecs = getComponentStorageSpecs(component, stateSpec, StateStorageOperation.READ);
    for (Storage storageSpec : storageSpecs) {
      StateStorage stateStorage = getStateStorageManager().getStateStorage(storageSpec);
      if (stateStorage != null && (stateStorage.hasState(component, name, stateClass, reloadData) || (changedStorages != null && changedStorages.contains(stateStorage)))) {
        state = stateStorage.getState(component, name, stateClass);
        break;
      }
    }

    if (state == null) {
      state = loadDefaultState(componentInfo, component, stateClass);
    }

    validateUnusedMacros(name, true);

    return state;
  }

  /**
   * Applies a state read by {@link #readState} to a synchronous component.
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  private void applyState(StateComponentInfo componentInfo, Object state) {
    StateComponent component = componentInfo.getComponent();
    if (component instanceof PersistentStateComponentAsync) {
      throw new IllegalArgumentException("Async state component must be loaded via #loadStateIfStorableAsync: " + component.getClass().getName());
    }

    ((PersistentStateComponent)component).loadState(state);

    storeModificationCountAfterLoad(componentInfo);
  }

  private void storeModificationCountAfterLoad(StateComponentInfo componentInfo) {
    long modCount = modificationCountOf(componentInfo.getComponent());
    if (modCount != UNKNOWN_MODIFICATION_COUNT) {
      myComponentsModificationCount.put(componentInfo.getName(), modCount);
    }
  }

  protected @Nullable PathMacroSubstitutor getPathMacroManagerForDefaults() {
    return null;
  }

  private @Nullable <T> T loadDefaultState(StateComponentInfo stateComponentInfo, Object component, Class<T> stateClass) {
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

  
  protected Storage[] getComponentStorageSpecs(StateComponent persistentStateComponent, State stateSpec, StateStorageOperation operation) {
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
  public Continuation<?> reinitComponents(Set<String> componentNames) {
    return runReinit(componentNames, Collections.<StateStorage>emptySet());
  }


  protected abstract MessageBus getMessageBus();

  @Override
  public @Nullable Continuation<?> reload(Collection<? extends StateStorage> changedStorages) {
    if (changedStorages.isEmpty()) {
      return null;
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
      return null;
    }

    return runReinit(componentNames, changedStorages);
  }

  private Continuation<?> runReinit(Set<String> componentNames, Collection<? extends StateStorage> changedStorages) {
    MessageBus messageBus = getMessageBus();

    Coroutine<Object, Object> chain = Coroutine.first(applyStateStep(input -> {
      messageBus.syncPublisher(BatchUpdateListener.class).onBatchUpdateStarted();
      return null;
    }));

    for (String componentName : componentNames) {
      chain = chain.then(CallSubroutine.call(() -> reinitComponent(componentName, changedStorages)));
    }

    chain = chain.then(applyStateStep(input -> {
      messageBus.syncPublisher(BatchUpdateListener.class).onBatchUpdateFinished();
      return null;
    }));

    return chain.runAsync(CoroutineScope.of(createCoroutineContext()), null);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private Coroutine<Object, Object> reinitComponent(String componentName, Collection<? extends StateStorage> changedStorages) {
    StateComponentInfo componentInfo = myComponents.get(componentName);
    if (componentInfo == null) {
      return Coroutine.empty();
    }

    boolean changedStoragesEmpty = changedStorages.isEmpty();
    Collection<? extends StateStorage> storages = changedStoragesEmpty ? null : changedStorages;

    if (!componentInfo.isAsync()) {
      // reading touches storages, applying mutates the model - so only the second half needs the write lock
      return Coroutine.<Object, Object>first(CodeExecution.apply(input -> readState(componentInfo, storages, changedStoragesEmpty)))
        .then(applyStateStep(state -> {
          if (state != null) {
            applyState(componentInfo, state);
          }
          componentInfo.afterLoad(false);
          return null;
        }));
    }

    PersistentStateComponentAsync<Object> asyncComponent = (PersistentStateComponentAsync<Object>)componentInfo.getComponent();
    Object[] stateHolder = new Object[1];

    return Coroutine.<Object, Object>first(CodeExecution.apply(input -> {
        stateHolder[0] = readState(componentInfo, storages, changedStoragesEmpty);
        return null;
      }))
      .then(CallSubroutine.call(() -> {
        Object state = stateHolder[0];
        return state == null ? Coroutine.empty() : (Coroutine)asyncComponent.loadState(state);
      }))
      .then(CodeExecution.apply(input -> {
        if (stateHolder[0] != null) {
          storeModificationCountAfterLoad(componentInfo);
        }
        return null;
      }))
      .then(CallSubroutine.call(() -> (Coroutine)asyncComponent.afterLoad(false)));
  }
}
