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
package consulo.project.impl.internal.store;

import consulo.project.ui.notification.NotificationService;
import consulo.annotation.component.ServiceImpl;
import consulo.application.AccessRule;
import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.component.internal.StateComponent;
import consulo.component.messagebus.MessageBus;
import consulo.component.persist.*;
import consulo.component.store.impl.internal.*;
import consulo.component.store.impl.internal.storage.FileBasedStorage;
import consulo.component.store.internal.PathMacrosService;
import consulo.component.store.internal.StateStorage;
import consulo.component.store.internal.StateStorage.SaveSession;
import consulo.component.store.impl.internal.storage.XmlElementStorage;
import consulo.component.store.internal.StateStorageManager;
import consulo.component.store.internal.TrackingPathMacroSubstitutor;
import consulo.project.Project;
import consulo.project.ProjectType;
import consulo.application.concurrent.coroutine.WriteLock;
import consulo.ui.UIAccess;
import consulo.util.concurrent.coroutine.Continuation;
import consulo.util.concurrent.coroutine.CoroutineContext;
import consulo.util.concurrent.coroutine.CoroutineStep;

import java.util.function.BiFunction;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.project.impl.internal.ProjectImpl;
import consulo.project.impl.internal.ProjectStorageUtil;
import consulo.project.macro.ProjectPathMacroManager;
import consulo.util.collection.ContainerUtil;
import consulo.util.io.FileUtil;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.ReadonlyStatusHandler;
import consulo.virtualFileSystem.RefreshQueue;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.event.VFileEvent;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jspecify.annotations.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.jdom.Element;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Singleton
@ServiceImpl(profiles = ProjectImpl.REGULAR_PROJECT)
public class ProjectStoreImpl extends BaseFileConfigurableStoreImpl implements IProjectStore {
  protected ProjectImpl myProject;
  private String myPresentableUrl;
  private @Nullable Path myProjectBaseDir;

  @Inject
  ProjectStoreImpl(Project project, Provider<ProjectPathMacroManager> pathMacroManager, Provider<ApplicationDefaultStoreCache> applicationDefaultStoreCache) {
    super(applicationDefaultStoreCache, pathMacroManager);
    myProject = (ProjectImpl)project;
  }

  @Override
  public TrackingPathMacroSubstitutor[] getSubstitutors() {
    return new TrackingPathMacroSubstitutor[]{getStateStorageManager().getMacroSubstitutor()};
  }

  @Override
  protected Project getProject() {
    return myProject;
  }

  @Override
  public CoroutineContext createCoroutineContext() {
    return myProject.coroutineContext();
  }

  @Override
  protected CoroutineStep<Object, Object> applyStateStep(BiFunction<Object, Continuation<?>, Object> function) {
    return WriteLock.apply(function);
  }

  @Override
  public void setProjectFilePath(Path filePath) {
    StateStorageManager stateStorageManager = getStateStorageManager();

    Path baseDir = Files.isDirectory(filePath) ? filePath : filePath.getParent();

    Path dirStore = baseDir.resolve(Project.DIRECTORY_STORE_FOLDER);

    String defaultFilePath = dirStore.resolve("misc").toString();

    stateStorageManager.addMacro(StoragePathMacros.PROJECT_FILE, defaultFilePath);

    stateStorageManager.addMacro(StoragePathMacros.DEFAULT_FILE, defaultFilePath);

    Path ws = dirStore.resolve("workspace");

    stateStorageManager.addMacro(StoragePathMacros.WORKSPACE_FILE, ws.toString());

    stateStorageManager.addMacro(StoragePathMacros.PROJECT_CONFIG_DIR, dirStore.toString());

    myProjectBaseDir = baseDir;
    myPresentableUrl = null;
  }

  private boolean isSingleFile() {
    return myProject.getType() != ProjectType.REGULAR;
  }

  @Override
  public @Nullable VirtualFile getProjectBaseDir() {
    if (isSingleFile()) return null;

    Path baseDir = myProjectBaseDir;
    if (baseDir == null) return null;

    return VirtualFileManager.getInstance().findFileByNioPath(baseDir);
  }

  @Override
  public String getProjectBasePath() {
    if (isSingleFile()) return null;

    // the base dir is known as soon as the project file path is set - computed without touching the file system,
    // which may be a remote platform that is not reachable right now
    Path baseDir = myProjectBaseDir;
    return baseDir == null ? null : baseDir.toString();
  }

  
  @Override
  public String getProjectName() {
    Path baseDir = myProjectBaseDir;
    assert baseDir != null;
    return readProjectName(baseDir);
  }

  public static String readProjectName(Path dir) {
    if (Files.isDirectory(dir)) {
      Path nameFile = dir.resolve(Project.DIRECTORY_STORE_FOLDER).resolve(ProjectImpl.NAME_FILE);
      if (Files.exists(nameFile)) {
        try {
          return Files.readString(nameFile);
        }
        catch (IOException ignored) {
        }
      }
    }
    Path fileName = dir.getFileName();
    return fileName == null ? dir.toString() : fileName.toString();
  }

  @Override
  public String getPresentableUrl() {
    if (isSingleFile()) {
      return null;
    }
    if (myPresentableUrl == null) {
      String url = !isSingleFile() ? getProjectBasePath() : getProjectFilePath();
      if (url != null) {
        myPresentableUrl = FileUtil.toSystemDependentName(url);
      }
    }
    return myPresentableUrl;
  }

  @Override
  public VirtualFile getProjectFile() {
    return isSingleFile() ? null : ((FileBasedStorage)getDefaultFileStorage()).getVirtualFile();
  }

  
  private XmlElementStorage getDefaultFileStorage() {
    // XmlElementStorage if default project, otherwise FileBasedStorage
    XmlElementStorage storage = (XmlElementStorage)getStateStorageManager().getStateStorage(StoragePathMacros.DEFAULT_FILE, RoamingType.DEFAULT);
    assert storage != null;
    return storage;
  }

  @Override
  public VirtualFile getWorkspaceFile() {
    if (isSingleFile()) return null;
    FileBasedStorage storage = (FileBasedStorage)getStateStorageManager().getStateStorage(StoragePathMacros.WORKSPACE_FILE, RoamingType.DISABLED);
    assert storage != null;
    return storage.getVirtualFile();
  }

  @Override
  public void loadProjectFromTemplate(ProjectImpl defaultProject, UIAccess uiAccess) {
    CoroutineContext coroutineContext = myProject.getApplication().coroutineContext().copy();
    coroutineContext.putCopyableUserData(UIAccess.KEY, uiAccess);

    defaultProject.getStateStore().createSaveCoroutine(new ArrayList<>()).runBlocking(CoroutineScope.of(coroutineContext), null);

    Element element = ((DefaultProjectStoreImpl)defaultProject.getStateStore()).getStateCopy();
    if (element != null) {
      getDefaultFileStorage().setDefaultState(element);
    }
  }

  
  @Override
  public String getProjectFilePath() {
    return isSingleFile() ? "" : ((FileBasedStorage)getDefaultFileStorage()).getFilePath();
  }

  
  @Override
  protected XmlElementStorage getMainStorage() {
    return getDefaultFileStorage();
  }

  
  @Override
  protected StateStorageManager createStateStorageManager() {
    return new ProjectStateStorageManager(myProject, new TrackingPathMacroSubstitutorImpl(myPathMacroManager), Application.get().getInstance(PathMacrosService.class));
  }

  
  @Override
  protected Storage[] getComponentStorageSpecs(StateComponent persistentStateComponent, State stateSpec, StateStorageOperation operation) {
    Storage[] storages = stateSpec.storages();
    if (storages.length == 1) {
      return storages;
    }

    assert storages.length > 0;
    return storages;
  }

  @Override
  protected final void doSave(boolean force, @Nullable List<SaveSession> saveSessions, List<Pair<SaveSession, Path>> readonlyFiles) {
    List<ProjectStorageUtil.UnableToSaveProjectNotification> notifications = NotificationService.getInstance().getNotificationsOfType(ProjectStorageUtil.UnableToSaveProjectNotification.class, myProject);
    if (!notifications.isEmpty()) {
      throw new SaveCancelledException();
    }

    beforeSave(readonlyFiles);

    super.doSave(force, saveSessions, readonlyFiles);

    if (!readonlyFiles.isEmpty()) {
      ReadonlyStatusHandler.OperationStatus status = AccessRule.read(() -> {
        List<Path> filesList = getFilesList(readonlyFiles);
        VirtualFileManager virtualFileManager = VirtualFileManager.getInstance();
        VirtualFile[] files = filesList.stream().map(virtualFileManager::findFileByNioPath).filter(Objects::nonNull).toArray(VirtualFile[]::new);
        return ReadonlyStatusHandler.getInstance(myProject).ensureFilesWritable(files);
      });

      if (status.hasReadonlyFiles()) {
        ProjectStorageUtil.dropUnableToSaveProjectNotification(myProject, ContainerUtil.map(status.getReadonlyFiles(), VirtualFile::toNioPath));
        throw new SaveCancelledException();
      }
      else {
        List<Pair<SaveSession, Path>> oldList = new ArrayList<>(readonlyFiles);
        readonlyFiles.clear();
        List<VFileEvent> retryEvents = new ArrayList<>();
        for (Pair<SaveSession, Path> entry : oldList) {
          executeSave(entry.first, false, readonlyFiles, retryEvents);
        }
        if (!retryEvents.isEmpty()) {
          RefreshQueue.getInstance().processEvents(retryEvents);
        }

        if (!readonlyFiles.isEmpty()) {
          ProjectStorageUtil.dropUnableToSaveProjectNotification(myProject, getFilesList(readonlyFiles));
          throw new SaveCancelledException();
        }
      }
    }
  }

  protected void beforeSave(List<Pair<SaveSession, Path>> readonlyFiles) {
  }

  
  private static List<Path> getFilesList(List<Pair<SaveSession, Path>> readonlyFiles) {
    return readonlyFiles.stream().map(saveSessionFilePair -> saveSessionFilePair.getSecond()).collect(Collectors.toList());
  }

  
  @Override
  protected MessageBus getMessageBus() {
    return myProject.getMessageBus();
  }
}
