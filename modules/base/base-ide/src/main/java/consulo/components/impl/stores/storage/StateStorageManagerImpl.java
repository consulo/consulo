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
package consulo.components.impl.stores.storage;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.*;
import com.intellij.openapi.components.StateStorage.SaveSession;
import com.intellij.openapi.util.Couple;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.PathUtilRt;
import com.intellij.util.ReflectionUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.messages.MessageBus;
import consulo.components.impl.stores.StreamProvider;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.logging.Logger;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class StateStorageManagerImpl implements StateStorageManager, Disposable {
  private static final Logger LOG = Logger.getInstance(StateStorageManagerImpl.class);

  private static final boolean ourHeadlessEnvironment;

  static {
    final Application app = ApplicationManager.getApplication();
    ourHeadlessEnvironment = app.isHeadlessEnvironment() || app.isUnitTestMode();
  }

  private final Map<String, String> myMacros = new LinkedHashMap<>();
  private final Lock myStorageLock = new ReentrantLock();
  private final Map<String, StateStorage> myStorages = new HashMap<>();
  private final TrackingPathMacroSubstitutor myPathMacroSubstitutor;
  @Nonnull
  private final StateStorageFacade myStateStorageFacade;
  private final String myRootTagName;
  protected final Supplier<MessageBus> myMessageBusSupplier;

  private StreamProvider myStreamProvider;

  public StateStorageManagerImpl(@Nullable TrackingPathMacroSubstitutor pathMacroSubstitutor,
                                 String rootTagName,
                                 @Nullable Disposable parentDisposable,
                                 @Nonnull Supplier<MessageBus> messageBusSupplier,
                                 @Nonnull StateStorageFacade stateStorageFacade) {
    myMessageBusSupplier = messageBusSupplier;
    myRootTagName = rootTagName;
    myPathMacroSubstitutor = pathMacroSubstitutor;
    myStateStorageFacade = stateStorageFacade;
    if (parentDisposable != null) {
      Disposer.register(parentDisposable, this);
    }
  }

  @Nonnull
  protected abstract String getConfigurationMacro(boolean directorySpec);

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public String buildFileSpec(@Nonnull Storage storage) {
    boolean directorySpec = !storage.stateSplitter().equals(StateSplitterEx.class);

    String file = storage.file();
    if (!StringUtil.isEmpty(file)) {
      return file;
    }

    String value = storage.value();
    if (value.isEmpty()) {
      LOG.error("Storage.value() is empty");
      return StoragePathMacros.DEFAULT_FILE;
    }

    if (value.equals(StoragePathMacros.WORKSPACE_FILE)) {
      return value;
    }
    return getConfigurationMacro(directorySpec) + "/" + value + (directorySpec ? "/" : "");
  }


  @Nonnull
  private StateStorage createStateStorage(@Nonnull Storage storageSpec) {
    if (!storageSpec.stateSplitter().equals(StateSplitterEx.class)) {
      StateSplitterEx splitter = ReflectionUtil.newInstance(storageSpec.stateSplitter());
      return myStateStorageFacade.createDirectoryBasedStorage(myPathMacroSubstitutor, expandMacros(buildFileSpec(storageSpec)), splitter, this, createStorageTopicListener());
    }
    else {
      return createFileStateStorage(buildFileSpec(storageSpec), storageSpec.roamingType());
    }
  }

  @Override
  public TrackingPathMacroSubstitutor getMacroSubstitutor() {
    return myPathMacroSubstitutor;
  }

  @Override
  public synchronized void addMacro(@Nonnull String macro, @Nonnull String expansion) {
    assert !macro.isEmpty();
    // backward compatibility
    if (macro.charAt(0) != '$') {
      LOG.warn("Add macros instead of macro name: " + macro);
      expansion = '$' + macro + '$';
    }
    myMacros.put(macro, expansion);
  }

  @Override
  @Nonnull
  public StateStorage getStateStorage(@Nonnull Storage storageSpec) {
    String key = buildFileSpec(storageSpec);

    myStorageLock.lock();
    try {
      StateStorage stateStorage = myStorages.get(key);
      if (stateStorage == null) {
        stateStorage = createStateStorage(storageSpec);
        myStorages.put(key, stateStorage);
      }
      return stateStorage;
    }
    finally {
      myStorageLock.unlock();
    }
  }

  @Nullable
  @Override
  public StateStorage getStateStorage(@Nonnull String fileSpec, @Nonnull RoamingType roamingType) {
    myStorageLock.lock();
    try {
      StateStorage stateStorage = myStorages.get(fileSpec);
      if (stateStorage == null) {
        stateStorage = createFileStateStorage(fileSpec, roamingType);
        myStorages.put(fileSpec, stateStorage);
      }
      return stateStorage;
    }
    finally {
      myStorageLock.unlock();
    }
  }

  @Nonnull
  @Override
  public Couple<Collection<VfsFileBasedStorage>> getCachedFileStateStorages(@Nonnull Collection<String> changed, @Nonnull Collection<String> deleted) {
    myStorageLock.lock();
    try {
      return Couple.of(getCachedFileStorages(changed), getCachedFileStorages(deleted));
    }
    finally {
      myStorageLock.unlock();
    }
  }

  @Nonnull
  private Collection<VfsFileBasedStorage> getCachedFileStorages(@Nonnull Collection<String> fileSpecs) {
    if (fileSpecs.isEmpty()) {
      return Collections.emptyList();
    }

    List<VfsFileBasedStorage> result = null;
    for (String fileSpec : fileSpecs) {
      StateStorage storage = myStorages.get(fileSpec);
      if (storage instanceof VfsFileBasedStorage) {
        if (result == null) {
          result = new ArrayList<>();
        }
        result.add((VfsFileBasedStorage)storage);
      }
    }
    return result == null ? Collections.<VfsFileBasedStorage>emptyList() : result;
  }

  @Nonnull
  @Override
  public Collection<String> getStorageFileNames() {
    myStorageLock.lock();
    try {
      return myStorages.keySet();
    }
    finally {
      myStorageLock.unlock();
    }
  }

  @Override
  public void clearStateStorage(@Nonnull String file) {
    myStorageLock.lock();
    try {
      myStorages.remove(file);
    }
    finally {
      myStorageLock.unlock();
    }
  }

  @Nonnull
  private StateStorage createFileStateStorage(@Nonnull String fileSpec, @Nullable RoamingType roamingType) {
    String filePath = FileUtil.toSystemIndependentName(expandMacros(fileSpec));

    if (!ourHeadlessEnvironment && PathUtilRt.getFileName(filePath).lastIndexOf('.') < 0) {
      throw new IllegalArgumentException("Extension is missing for storage file: " + filePath);
    }

    if (roamingType == RoamingType.PER_USER && fileSpec.equals(StoragePathMacros.WORKSPACE_FILE)) {
      roamingType = RoamingType.DISABLED;
    }

    return myStateStorageFacade
            .createFileBasedStorage(filePath, fileSpec, roamingType, getMacroSubstitutor(fileSpec), myRootTagName, StateStorageManagerImpl.this, createStorageTopicListener(), myStreamProvider,
                                    isUseXmlProlog());
  }

  @Nullable
  protected StateStorage.Listener createStorageTopicListener() {
    MessageBus messageBus = myMessageBusSupplier.get();
    return messageBus == null ? null : messageBus.syncPublisher(StateStorage.STORAGE_TOPIC);
  }

  protected boolean isUseXmlProlog() {
    return true;
  }

  @Nullable
  @Override
  public final StreamProvider getStreamProvider() {
    return myStreamProvider;
  }

  protected TrackingPathMacroSubstitutor getMacroSubstitutor(@Nonnull final String fileSpec) {
    return myPathMacroSubstitutor;
  }

  private static final Pattern MACRO_PATTERN = Pattern.compile("(\\$[^\\$]*\\$)");

  @Override
  @Nonnull
  public synchronized String expandMacros(@Nonnull String file) {
    Matcher matcher = MACRO_PATTERN.matcher(file);
    while (matcher.find()) {
      String m = matcher.group(1);
      if (!myMacros.containsKey(m)) {
        throw new IllegalArgumentException("Unknown macro: " + m + " in storage file spec: " + file);
      }
    }

    String expanded = file;
    for (String macro : myMacros.keySet()) {
      expanded = StringUtil.replace(expanded, macro, myMacros.get(macro));
    }
    return expanded;
  }

  @Nonnull
  @Override
  public String collapseMacros(@Nonnull String path) {
    String result = path;
    for (String macro : myMacros.keySet()) {
      result = StringUtil.replace(result, myMacros.get(macro), macro);
    }
    return result;
  }

  @Nonnull
  @Override
  public ExternalizationSession startExternalization() {
    return new StateStorageManagerExternalizationSession();
  }

  protected class StateStorageManagerExternalizationSession implements ExternalizationSession {
    final Map<StateStorage, StateStorage.ExternalizationSession> mySessions = new LinkedHashMap<>();

    @Override
    public void setState(@Nonnull Storage[] storageSpecs, @Nonnull Object component, @Nonnull String componentName, @Nonnull Object state) {
      for (Storage storageSpec : storageSpecs) {
        StateStorage stateStorage = getStateStorage(storageSpec);
        StateStorage.ExternalizationSession session = getExternalizationSession(stateStorage);
        if (session != null) {
          // empty element as null state, so, will be deleted
          session.setState(component, componentName, storageSpec.deprecated() ? new Element("empty") : state, storageSpec);
        }
      }
    }

    @Nullable
    private StateStorage.ExternalizationSession getExternalizationSession(@Nonnull StateStorage stateStorage) {
      StateStorage.ExternalizationSession session = mySessions.get(stateStorage);
      if (session == null) {
        session = stateStorage.startExternalization();
        if (session != null) {
          mySessions.put(stateStorage, session);
        }
      }
      return session;
    }

    @Nonnull
    @Override
    public List<SaveSession> createSaveSessions(boolean force) {
      if (mySessions.isEmpty()) {
        return Collections.emptyList();
      }

      List<SaveSession> saveSessions = null;
      Collection<StateStorage.ExternalizationSession> externalizationSessions = mySessions.values();
      for (StateStorage.ExternalizationSession session : externalizationSessions) {
        SaveSession saveSession = session.createSaveSession(force);
        if (saveSession != null) {
          if (saveSessions == null) {
            if (externalizationSessions.size() == 1) {
              return Collections.singletonList(saveSession);
            }
            saveSessions = new ArrayList<>();
          }
          saveSessions.add(saveSession);
        }
      }
      return ContainerUtil.notNullize(saveSessions);
    }
  }

  @Override
  public void dispose() {
  }

  @Override
  public void setStreamProvider(@Nullable StreamProvider streamProvider) {
    myStreamProvider = streamProvider;
  }
}
