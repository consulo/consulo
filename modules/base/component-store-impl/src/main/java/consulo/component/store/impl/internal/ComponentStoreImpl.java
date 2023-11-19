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
import consulo.application.ApplicationManager;
import consulo.application.concurrent.DataLock;
import consulo.application.util.ConcurrentFactoryMap;
import consulo.component.ComponentManager;
import consulo.component.ProcessCanceledException;
import consulo.component.macro.PathMacroSubstitutor;
import consulo.component.messagebus.MessageBus;
import consulo.component.persist.*;
import consulo.component.store.impl.internal.StateStorageManager.ExternalizationSession;
import consulo.component.store.impl.internal.storage.StateStorage;
import consulo.component.store.impl.internal.storage.StateStorage.SaveSession;
import consulo.component.store.impl.internal.storage.StorageUtil;
import consulo.logging.Logger;
import consulo.ui.UIAccess;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.SmartHashSet;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Provider;
import org.jdom.Element;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class ComponentStoreImpl implements IComponentStore {
  private static final Logger LOG = Logger.getInstance(ComponentStoreImpl.class);
  private static ThreadLocal<Boolean> ourInsideSavingSessionLocal = ThreadLocal.withInitial(() -> Boolean.FALSE);

  public static void assertIfInsideSavingSession() {
    if (ourInsideSavingSessionLocal.get() == Boolean.TRUE) {
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
  public <T> StateComponentInfo<T> loadStateIfStorable(@Nonnull T component) {
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
  public final void save(boolean force, @Nonnull List<Pair<SaveSession, File>> readonlyFiles) {
    ExternalizationSession externalizationSession = myComponents.isEmpty() ? null : getStateStorageManager().startExternalization();
    if (externalizationSession != null) {
      String[] names = ArrayUtil.toStringArray(myComponents.keySet());
      Arrays.sort(names);
      for (String name : names) {
        StateComponentInfo<?> componentInfo = myComponents.get(name);

        commitComponentInsideSingleUIWriteThread(componentInfo, externalizationSession, force);
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

    doSave(force, externalizationSession == null ? null : externalizationSession.createSaveSessions(force), readonlyFiles);
  }

  @RequiredWriteAction
  @Override
  public final CompletableFuture<?> saveAsync(boolean force,
                                              @Nonnull UIAccess uiAccess,
                                              @Nonnull List<Pair<SaveSession, File>> readonlyFiles) {
    List<CompletableFuture<?>> initialFutures = new ArrayList<>();

    ExternalizationSession externalizationSession = myComponents.isEmpty() ? null : getStateStorageManager().startExternalization();
    if (externalizationSession != null) {
      String[] names = ArrayUtil.toStringArray(myComponents.keySet());
      Arrays.sort(names);
      for (String name : names) {
        StateComponentInfo<?> componentInfo = myComponents.get(name);

        initialFutures.add(commitComponentAsync(componentInfo, externalizationSession, uiAccess, force));
      }
    }

    for (SettingsSavingComponent settingsSavingComponent : mySettingsSavingComponents) {
      try {
        initialFutures.add(settingsSavingComponent.saveAsync());
      }
      catch (Throwable e) {
        LOG.error(e);
      }
    }

    CompletableFuture<Void> joined = CompletableFuture.allOf(initialFutures.toArray(CompletableFuture[]::new));
    return joined.thenCompose(v -> doSaveAsync(force,
                                               externalizationSession == null ? null : externalizationSession.createSaveSessions(force),
                                               readonlyFiles));
  }

  protected void doSave(boolean force, @Nullable List<SaveSession> saveSessions, @Nonnull List<Pair<SaveSession, File>> readonlyFiles) {
    if (saveSessions != null) {
      for (SaveSession session : saveSessions) {
        executeSave(session, force, readonlyFiles);
      }
    }
  }

  @Nonnull
  protected CompletableFuture<?> doSaveAsync(boolean force,
                                             @Nullable List<SaveSession> saveSessions,
                                             @Nonnull List<Pair<SaveSession, File>> readonlyFiles) {
    if (saveSessions == null) {
      return CompletableFuture.completedFuture(null);
    }

    List<CompletableFuture> futures = new ArrayList<>(saveSessions.size());
    for (SaveSession session : saveSessions) {
      futures.add(executeSaveAsync(session, force, readonlyFiles));
    }
    return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
  }

  protected static void executeSave(@Nonnull SaveSession session, boolean force, @Nonnull List<Pair<SaveSession, File>> readonlyFiles) {
    try {
      session.save(force);
    }
    catch (ReadOnlyModificationException e) {
      readonlyFiles.add(Pair.create(session, e.getFile()));
    }
  }

  protected static CompletableFuture<?> executeSaveAsync(@Nonnull SaveSession session,
                                                         boolean force,
                                                         @Nonnull List<Pair<SaveSession, File>> readonlyFiles) {
    try {
      return session.saveAsync(force);
    }
    catch (ReadOnlyModificationException e) {
      readonlyFiles.add(Pair.create(session, e.getFile()));
    }

    return CompletableFuture.completedFuture(null);
  }

  @SuppressWarnings({"unchecked", "RequiredXAction"})
  private <T> void commitComponentInsideSingleUIWriteThread(@Nonnull StateComponentInfo<T> componentInfo,
                                                            @Nonnull ExternalizationSession session,
                                                            boolean force) {
    PersistentStateComponent<T> component = componentInfo.getComponent();

    long countToSet = -1;
    if (component instanceof PersistentStateComponentWithModificationTracker && !force) {
      long count = ((PersistentStateComponentWithModificationTracker<T>)component).getStateModificationCount();

      long oldCount = myComponentsModificationCount.get(componentInfo.getName());

      if (count == oldCount) {
        return;
      }

      countToSet = count;
    }

    T state;
    if (component instanceof PersistentStateComponentWithUIState) {
      PersistentStateComponentWithUIState<T, Object> uiComponent = (PersistentStateComponentWithUIState<T, Object>)component;
      Object uiState = uiComponent.getStateFromUI();
      state = uiComponent.getState(uiState);
    }
    else {
      state = component.getState();
    }

    setStateImpl(state, componentInfo, countToSet, session);
  }

  @SuppressWarnings({"unchecked", "RequiredXAction"})
  private <T> CompletableFuture<T> commitComponentAsync(@Nonnull StateComponentInfo<T> componentInfo,
                                                        @Nonnull ExternalizationSession session,
                                                        @Nonnull UIAccess uiAccess,
                                                        boolean force) {
    PersistentStateComponent<T> component = componentInfo.getComponent();

    long countToSet = -1;
    if (component instanceof PersistentStateComponentWithModificationTracker && !force) {
      long count = ((PersistentStateComponentWithModificationTracker<T>)component).getStateModificationCount();

      long oldCount = myComponentsModificationCount.get(componentInfo.getName());

      if (count == oldCount) {
        return CompletableFuture.completedFuture(null);
      }

      countToSet = count;
    }

    if (component instanceof PersistentStateComponentWithUIState) {
      PersistentStateComponentWithUIState<T, Object> uiComponent = (PersistentStateComponentWithUIState<T, Object>)component;
      final long finalCountToSet = countToSet;

      DataLock dataLock = DataLock.getInstance();
      return uiAccess.giveAsync(uiComponent::getStateFromUI)
                     .thenApplyAsync(uiComponent::getState, dataLock.writeExecutor())
                     .whenComplete((state, throwable) -> setStateImpl(state, componentInfo, finalCountToSet, session));
    }
    else {
      T state = component.getState();
      setStateImpl(state, componentInfo, countToSet, session);
      return CompletableFuture.completedFuture(null);
    }
  }

  private <T> void setStateImpl(T state,
                                @Nonnull StateComponentInfo<T> componentInfo,
                                long countToSet,
                                @Nonnull ExternalizationSession session) {
    if (state == null) {
      return;
    }

    PersistentStateComponent<T> component = componentInfo.getComponent();

    Storage[] storageSpecs = getComponentStorageSpecs(component, componentInfo.getState(), StateStorageOperation.WRITE);

    session.setState(storageSpecs, component, componentInfo.getName(), state);

    if (countToSet != -1) {
      myComponentsModificationCount.put(componentInfo.getName(), countToSet);
    }
  }

  private void doAddComponent(@Nonnull String componentName, @Nonnull StateComponentInfo<?> stateComponentInfo) {
    StateComponentInfo<?> existing = myComponents.get(componentName);
    if (existing != null && !existing.equals(stateComponentInfo)) {
      LOG.error("Conflicting component name '" + componentName + "': " + existing.getComponent()
                                                                                 .getClass() + " and " + stateComponentInfo.getComponent()
                                                                                                                           .getClass());
    }
    myComponents.put(componentName, stateComponentInfo);
  }

  @Nullable
  protected ComponentManager getProject() {
    return null;
  }

  private void validateUnusedMacros(@Nullable final String componentName, final boolean service) {
    final ComponentManager project = getProject();
    if (project == null) return;

    if (!ApplicationManager.getApplication().isHeadlessEnvironment() && !ApplicationManager.getApplication().isUnitTestMode()) {
      if (service && componentName != null && project.isInitialized()) {
        final TrackingPathMacroSubstitutor substitutor = getStateStorageManager().getMacroSubstitutor();
        if (substitutor != null) {
          StorageUtil.notifyUnknownMacros(substitutor, project, componentName);
        }
      }
    }
  }

  private <T> void loadState(@Nonnull StateComponentInfo<T> componentInfo,
                             @Nullable Collection<? extends StateStorage> changedStorages,
                             boolean reloadData) {
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
      if (stateStorage != null && (stateStorage.hasState(component,
                                                         name,
                                                         stateClass,
                                                         reloadData) || (changedStorages != null && changedStorages.contains(stateStorage)))) {
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

  private <T> void storeModificationCountAfterLoad(PersistentStateComponent<T> component, @Nonnull StateComponentInfo<T> componentInfo) {
    if (component instanceof PersistentStateComponentWithModificationTracker) {
      long modCount = ((PersistentStateComponentWithModificationTracker<T>)component).getStateModificationCount();

      myComponentsModificationCount.put(componentInfo.getName(), modCount);
    }
  }

  @Nullable
  protected PathMacroSubstitutor getPathMacroManagerForDefaults() {
    return null;
  }

  @Nullable
  private <T> T loadDefaultState(@Nonnull StateComponentInfo<T> stateComponentInfo,
                                 @Nonnull Object component,
                                 @Nonnull final Class<T> stateClass) {
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

  @Nullable
  private <T> T deserializeDefaultStore(@Nonnull Element documentElement, Class<T> stateClass) {
    PathMacroSubstitutor pathMacroManager = getPathMacroManagerForDefaults();
    if (pathMacroManager != null) {
      pathMacroManager.expandPaths(documentElement);
    }

    return DefaultStateSerializer.deserializeState(documentElement, stateClass);
  }

  @Nonnull
  protected <T> Storage[] getComponentStorageSpecs(@Nonnull PersistentStateComponent<T> persistentStateComponent,
                                                   @Nonnull State stateSpec,
                                                   @Nonnull StateStorageOperation operation) {
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
  public void reinitComponents(@Nonnull Set<String> componentNames, boolean reloadData) {
    reinitComponents(componentNames, Collections.<StateStorage>emptySet());
  }

  protected boolean reinitComponent(@Nonnull String componentName, @Nonnull Collection<? extends StateStorage> changedStorages) {
    StateComponentInfo<?> componentInfo = myComponents.get(componentName);
    if (componentInfo == null) {
      return false;
    }

    boolean changedStoragesEmpty = changedStorages.isEmpty();
    loadState(componentInfo, changedStoragesEmpty ? null : changedStorages, changedStoragesEmpty);
    return true;
  }

  @Nonnull
  protected abstract MessageBus getMessageBus();

  @Override
  public boolean reload(@Nonnull Collection<? extends StateStorage> changedStorages) {
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

  private void reinitComponents(@Nonnull Set<String> componentNames, @Nonnull Collection<? extends StateStorage> changedStorages) {
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
