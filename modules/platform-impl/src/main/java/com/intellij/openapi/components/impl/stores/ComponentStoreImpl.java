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
package com.intellij.openapi.components.impl.stores;

import com.intellij.openapi.application.*;
import com.intellij.openapi.application.ex.DecodeDefaultsUtil;
import com.intellij.openapi.components.*;
import com.intellij.openapi.components.StateStorage.SaveSession;
import com.intellij.openapi.components.impl.stores.StateStorageManager.ExternalizationSession;
import com.intellij.openapi.components.store.ReadOnlyModificationException;
import com.intellij.openapi.components.store.StateStorageBase;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.SmartHashSet;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.xmlb.JDOMXIncluder;
import consulo.components.impl.stores.StateComponentInfo;
import gnu.trove.THashMap;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class ComponentStoreImpl implements IComponentStore.Reloadable {
  private static final Logger LOG = Logger.getInstance(ComponentStoreImpl.class);

  private final Map<String, StateComponentInfo<?>> myComponents = Collections.synchronizedMap(new THashMap<>());
  private final List<SettingsSavingComponent> mySettingsSavingComponents = new CopyOnWriteArrayList<>();

  @Override
  public void initComponent(@NotNull Object component) {
    if (component instanceof SettingsSavingComponent) {
      mySettingsSavingComponents.add((SettingsSavingComponent)component);
    }

    StateComponentInfo<?> componentInfo = StateComponentInfo.of(component, getProject());
    if (componentInfo == null) {
      return;
    }

    AccessToken token = ReadAction.start();
    try {
      initComponent(componentInfo, null, false);
    }
    catch (StateStorageException | ProcessCanceledException e) {
      throw e;
    }
    catch (Exception e) {
      LOG.error(e);
    }
    finally {
      token.finish();
    }
  }

  @Override
  public final void save(@NotNull List<Pair<StateStorage.SaveSession, VirtualFile>> readonlyFiles) {
    ExternalizationSession externalizationSession = myComponents.isEmpty() ? null : getStateStorageManager().startExternalization();
    if (externalizationSession != null) {
      String[] names = ArrayUtilRt.toStringArray(myComponents.keySet());
      Arrays.sort(names);
      for (String name : names) {
        StateComponentInfo<?> componentInfo = myComponents.get(name);

        commitComponent(componentInfo, externalizationSession);
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

    doSave(externalizationSession == null ? null : externalizationSession.createSaveSessions(), readonlyFiles);
  }

  protected void doSave(@Nullable List<SaveSession> saveSessions, @NotNull List<Pair<SaveSession, VirtualFile>> readonlyFiles) {
    if (saveSessions != null) {
      for (SaveSession session : saveSessions) {
        executeSave(session, readonlyFiles);
      }
    }
  }

  protected static void executeSave(@NotNull SaveSession session, @NotNull List<Pair<SaveSession, VirtualFile>> readonlyFiles) {
    try {
      session.save();
    }
    catch (ReadOnlyModificationException e) {
      readonlyFiles.add(Pair.create(session, e.getFile()));
    }
  }

  private <T> void commitComponent(@NotNull StateComponentInfo<T> componentInfo, @NotNull ExternalizationSession session) {
    PersistentStateComponent<T> component = componentInfo.getComponent();

    T state = component.getState();
    if (state != null) {
      Storage[] storageSpecs = getComponentStorageSpecs(component, componentInfo.getState(), StateStorageOperation.WRITE);
      session.setState(storageSpecs, component, componentInfo.getName(), state);
    }
  }

  private void doAddComponent(@NotNull String componentName, @NotNull StateComponentInfo<?> stateComponentInfo) {
    StateComponentInfo<?> existing = myComponents.get(componentName);
    if (existing != null && !existing.equals(stateComponentInfo)) {
      LOG.error("Conflicting component name '" + componentName + "': " + existing.getComponent().getClass() + " and " + stateComponentInfo.getComponent()
              .getClass());
    }
    myComponents.put(componentName, stateComponentInfo);
  }

  @Nullable
  protected Project getProject() {
    return null;
  }

  private void validateUnusedMacros(@Nullable final String componentName, final boolean service) {
    final Project project = getProject();
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

  private <T> String initComponent(@NotNull StateComponentInfo<T> componentInfo,
                                   @Nullable Collection<? extends StateStorage> changedStorages,
                                   boolean reloadData) {
    PersistentStateComponent<T> component = componentInfo.getComponent();
    State stateSpec = componentInfo.getState();
    String name = stateSpec.name();

    if (changedStorages == null || !reloadData) {
      doAddComponent(name, componentInfo);
    }

    if (optimizeTestLoading()) {
      return name;
    }

    Class<T> stateClass = ComponentSerializationUtil.getStateClass(component.getClass());
    T state = getDefaultState(component, name, stateClass);

    Storage[] storageSpecs = getComponentStorageSpecs(component, stateSpec, StateStorageOperation.READ);
    for (Storage storageSpec : storageSpecs) {
      StateStorage stateStorage = getStateStorageManager().getStateStorage(storageSpec);
      if (stateStorage != null &&
          (stateStorage.hasState(component, name, stateClass, reloadData) || (changedStorages != null && changedStorages.contains(stateStorage)))) {
        state = stateStorage.getState(component, name, stateClass, state);
        break;
      }
    }

    if (state != null) {
      component.loadState(state);
    }

    validateUnusedMacros(name, true);

    return name;
  }

  @Nullable
  protected abstract PathMacroManager getPathMacroManagerForDefaults();

  @Nullable
  protected <T> T getDefaultState(@NotNull Object component, @NotNull String componentName, @NotNull final Class<T> stateClass) {
    URL url = DecodeDefaultsUtil.getDefaults(component, componentName);
    if (url == null) {
      return null;
    }

    try {
      Element documentElement = JDOMXIncluder.resolve(JDOMUtil.loadDocument(url), url.toExternalForm()).detachRootElement();

      PathMacroManager pathMacroManager = getPathMacroManagerForDefaults();
      if (pathMacroManager != null) {
        pathMacroManager.expandPaths(documentElement);
      }

      return DefaultStateSerializer.deserializeState(documentElement, stateClass, null);
    }
    catch (IOException | JDOMException e) {
      throw new StateStorageException("Error loading state from " + url, e);
    }
  }

  @NotNull
  protected <T> Storage[] getComponentStorageSpecs(@NotNull PersistentStateComponent<T> persistentStateComponent,
                                                   @NotNull State stateSpec,
                                                   @NotNull StateStorageOperation operation) {
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

  protected boolean optimizeTestLoading() {
    return false;
  }

  @Override
  public boolean isReloadPossible(@NotNull final Set<String> componentNames) {
    for (String componentName : componentNames) {
      final StateComponentInfo<?> component = myComponents.get(componentName);
      if (!component.getState().reloadable()) {
        return false;
      }
    }
    return true;
  }

  @Override
  @NotNull
  public final Collection<String> getNotReloadableComponents(@NotNull Collection<String> componentNames) {
    Set<String> notReloadableComponents = null;
    for (String componentName : componentNames) {
      StateComponentInfo<?> component = myComponents.get(componentName);
      if (!component.getState().reloadable()) {
        if (notReloadableComponents == null) {
          notReloadableComponents = new LinkedHashSet<>();
        }
        notReloadableComponents.add(componentName);
      }
    }
    return notReloadableComponents == null ? Collections.<String>emptySet() : notReloadableComponents;
  }

  @Override
  public void reinitComponents(@NotNull Set<String> componentNames, boolean reloadData) {
    reinitComponents(componentNames, Collections.<String>emptySet(), Collections.<StateStorage>emptySet());
  }

  protected boolean reinitComponent(@NotNull String componentName, @NotNull Collection<? extends StateStorage> changedStorages) {
    StateComponentInfo<?> componentInfo = myComponents.get(componentName);
    if (componentInfo == null) {
      return false;
    }

    boolean changedStoragesEmpty = changedStorages.isEmpty();
    initComponent(componentInfo, changedStoragesEmpty ? null : changedStorages, changedStoragesEmpty);
    return true;
  }

  @NotNull
  protected abstract MessageBus getMessageBus();

  @Override
  @Nullable
  public final Collection<String> reload(@NotNull Collection<? extends StateStorage> changedStorages) {
    if (changedStorages.isEmpty()) {
      return Collections.emptySet();
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
      return Collections.emptySet();
    }

    Collection<String> notReloadableComponents = getNotReloadableComponents(componentNames);
    reinitComponents(componentNames, notReloadableComponents, changedStorages);
    return notReloadableComponents.isEmpty() ? null : notReloadableComponents;
  }

  // used in settings repository plugin
  public void reinitComponents(@NotNull Set<String> componentNames,
                               @NotNull Collection<String> notReloadableComponents,
                               @NotNull Collection<? extends StateStorage> changedStorages) {
    MessageBus messageBus = getMessageBus();
    messageBus.syncPublisher(BatchUpdateListener.TOPIC).onBatchUpdateStarted();
    try {
      for (String componentName : componentNames) {
        if (!notReloadableComponents.contains(componentName)) {
          reinitComponent(componentName, changedStorages);
        }
      }
    }
    finally {
      messageBus.syncPublisher(BatchUpdateListener.TOPIC).onBatchUpdateFinished();
    }
  }

  public enum ReloadComponentStoreStatus {
    RESTART_AGREED,
    RESTART_CANCELLED,
    ERROR,
    SUCCESS,
  }

  @NotNull
  public static ReloadComponentStoreStatus reloadStore(@NotNull Collection<StateStorage> changedStorages, @NotNull IComponentStore.Reloadable store) {
    Collection<String> notReloadableComponents;
    boolean willBeReloaded = false;
    try {
      AccessToken token = WriteAction.start();
      try {
        notReloadableComponents = store.reload(changedStorages);
      }
      catch (Throwable e) {
        Messages.showWarningDialog(ProjectBundle.message("project.reload.failed", e.getMessage()), ProjectBundle.message("project.reload.failed.title"));
        return ReloadComponentStoreStatus.ERROR;
      }
      finally {
        token.finish();
      }

      if (ContainerUtil.isEmpty(notReloadableComponents)) {
        return ReloadComponentStoreStatus.SUCCESS;
      }

      willBeReloaded = askToRestart(store, notReloadableComponents, changedStorages);
      return willBeReloaded ? ReloadComponentStoreStatus.RESTART_AGREED : ReloadComponentStoreStatus.RESTART_CANCELLED;
    }
    finally {
      if (!willBeReloaded) {
        for (StateStorage storage : changedStorages) {
          if (storage instanceof StateStorageBase) {
            ((StateStorageBase)storage).enableSaving();
          }
        }
      }
    }
  }

  // used in settings repository plugin
  public static boolean askToRestart(@NotNull Reloadable store,
                                     @NotNull Collection<String> notReloadableComponents,
                                     @Nullable Collection<? extends StateStorage> changedStorages) {
    StringBuilder message = new StringBuilder();
    String storeName = store instanceof IApplicationStore ? "Application" : "Project";
    message.append(storeName).append(' ');
    message.append("components were changed externally and cannot be reloaded:\n\n");
    int count = 0;
    for (String component : notReloadableComponents) {
      if (count == 10) {
        message.append('\n').append("and ").append(notReloadableComponents.size() - count).append(" more").append('\n');
      }
      else {
        message.append(component).append('\n');
        count++;
      }
    }

    message.append("\nWould you like to ");
    if (store instanceof IApplicationStore) {
      message.append(ApplicationManager.getApplication().isRestartCapable() ? "restart" : "shutdown").append(' ');
      message.append(ApplicationNamesInfo.getInstance().getProductName()).append('?');
    }
    else {
      message.append("reload project?");
    }

    if (Messages.showYesNoDialog(message.toString(), storeName + " Files Changed", Messages.getQuestionIcon()) == Messages.YES) {
      if (changedStorages != null) {
        for (StateStorage storage : changedStorages) {
          if (storage instanceof StateStorageBase) {
            ((StateStorageBase)storage).disableSaving();
          }
        }
      }
      return true;
    }
    return false;
  }
}
