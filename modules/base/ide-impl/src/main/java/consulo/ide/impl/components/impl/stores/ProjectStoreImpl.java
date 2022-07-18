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
package consulo.ide.impl.components.impl.stores;

import consulo.ide.impl.idea.openapi.components.impl.ProjectPathMacroManager;
import consulo.ide.impl.idea.openapi.project.impl.ProjectImpl;
import consulo.ide.impl.idea.openapi.util.io.FileUtil;
import consulo.ide.impl.idea.openapi.util.text.StringUtil;
import consulo.ide.impl.idea.openapi.vfs.VfsUtil;
import consulo.application.AccessRule;
import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.component.impl.internal.macro.BasePathMacroManager;
import consulo.component.messagebus.MessageBus;
import consulo.component.persist.*;
import consulo.component.store.impl.internal.BaseFileConfigurableStoreImpl;
import consulo.component.store.impl.internal.*;
import consulo.component.store.impl.internal.storage.StateStorage;
import consulo.component.store.impl.internal.storage.StateStorage.SaveSession;
import consulo.component.store.impl.internal.storage.VfsFileBasedStorage;
import consulo.component.store.impl.internal.storage.XmlElementStorage;
import consulo.ide.impl.components.impl.stores.storage.ProjectStateStorageManager;
import consulo.project.Project;
import consulo.project.ui.notification.NotificationsManager;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.ReadonlyStatusHandler;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class ProjectStoreImpl extends BaseFileConfigurableStoreImpl implements IProjectStore {
  protected ProjectImpl myProject;
  private String myPresentableUrl;

  @Inject
  ProjectStoreImpl(@Nonnull Project project, @Nonnull ProjectPathMacroManager pathMacroManager, @Nonnull Provider<ApplicationDefaultStoreCache> applicationDefaultStoreCache) {
    super(applicationDefaultStoreCache, pathMacroManager);
    myProject = (ProjectImpl)project;
  }

  @Override
  public TrackingPathMacroSubstitutor[] getSubstitutors() {
    return new TrackingPathMacroSubstitutor[]{getStateStorageManager().getMacroSubstitutor()};
  }

  @Override
  protected boolean optimizeTestLoading() {
    return myProject.isOptimiseTestLoadSpeed();
  }

  @Override
  protected Project getProject() {
    return myProject;
  }

  @Override
  public void setProjectFilePath(@Nonnull final String filePath) {
    final StateStorageManager stateStorageManager = getStateStorageManager();
    final LocalFileSystem fs = LocalFileSystem.getInstance();

    final File file = new File(filePath);

    final File dirStore = file.isDirectory() ? new File(file, Project.DIRECTORY_STORE_FOLDER) : new File(file.getParentFile(), Project.DIRECTORY_STORE_FOLDER);
    String defaultFilePath = new File(dirStore, "misc.xml").getPath();
    // deprecated
    stateStorageManager.addMacro(StoragePathMacros.PROJECT_FILE, defaultFilePath);
    stateStorageManager.addMacro(StoragePathMacros.DEFAULT_FILE, defaultFilePath);

    final File ws = new File(dirStore, "workspace.xml");
    stateStorageManager.addMacro(StoragePathMacros.WORKSPACE_FILE, ws.getPath());

    stateStorageManager.addMacro(StoragePathMacros.PROJECT_CONFIG_DIR, dirStore.getPath());

    ApplicationManager.getApplication().invokeAndWait(() -> VfsUtil.markDirtyAndRefresh(false, true, true, fs.refreshAndFindFileByIoFile(dirStore)), IdeaModalityState.defaultModalityState());

    myPresentableUrl = null;
  }

  @Override
  public void setProjectFilePathNoUI(@Nonnull final String filePath) {
    final StateStorageManager stateStorageManager = getStateStorageManager();
    final LocalFileSystem fs = LocalFileSystem.getInstance();

    final File file = new File(filePath);

    final File dirStore = file.isDirectory() ? new File(file, Project.DIRECTORY_STORE_FOLDER) : new File(file.getParentFile(), Project.DIRECTORY_STORE_FOLDER);
    String defaultFilePath = new File(dirStore, "misc.xml").getPath();
    // deprecated
    stateStorageManager.addMacro(StoragePathMacros.PROJECT_FILE, defaultFilePath);
    stateStorageManager.addMacro(StoragePathMacros.DEFAULT_FILE, defaultFilePath);

    final File ws = new File(dirStore, "workspace.xml");
    stateStorageManager.addMacro(StoragePathMacros.WORKSPACE_FILE, ws.getPath());

    stateStorageManager.addMacro(StoragePathMacros.PROJECT_CONFIG_DIR, dirStore.getPath());

    VfsUtil.markDirtyAndRefresh(false, true, true, fs.refreshAndFindFileByIoFile(dirStore));

    myPresentableUrl = null;
  }

  @Override
  @Nullable
  public VirtualFile getProjectBaseDir() {
    if (myProject.isDefault()) return null;

    final String path = getProjectBasePath();
    if (path == null) return null;

    return LocalFileSystem.getInstance().findFileByPath(path);
  }

  @Override
  public String getProjectBasePath() {
    if (myProject.isDefault()) return null;

    final String path = getProjectFilePath();
    if (!StringUtil.isEmptyOrSpaces(path)) {
      return getBasePath(new File(path));
    }

    //we are not yet initialized completely ("open directory", etc)
    StateStorage storage = getStateStorageManager().getStateStorage(StoragePathMacros.DEFAULT_FILE, RoamingType.DEFAULT);
    if (!(storage instanceof VfsFileBasedStorage)) {
      return null;
    }

    return getBasePath(((VfsFileBasedStorage)storage).getFile());
  }

  private String getBasePath(@Nonnull File file) {
    if (myProject.isDefault()) {
      return file.getParent();
    }
    else {
      File parentFile = file.getParentFile();
      return parentFile == null ? null : parentFile.getParent();
    }
  }

  @Nonnull
  @Override
  public String getProjectName() {
    final String path = getProjectBasePath();
    assert path != null;
    return readProjectName(new File(path));
  }

  public static String readProjectName(@Nonnull File file) {
    if (file.isDirectory()) {
      final File nameFile = new File(new File(file, Project.DIRECTORY_STORE_FOLDER), ProjectImpl.NAME_FILE);
      if (nameFile.exists()) {
        try {
          return FileUtil.loadFile(nameFile, true);
        }
        catch (IOException ignored) {
        }
      }
    }
    return file.getName();
  }

  @Override
  public String getPresentableUrl() {
    if (myProject.isDefault()) {
      return null;
    }
    if (myPresentableUrl == null) {
      String url = !myProject.isDefault() ? getProjectBasePath() : getProjectFilePath();
      if (url != null) {
        myPresentableUrl = FileUtil.toSystemDependentName(url);
      }
    }
    return myPresentableUrl;
  }

  @Override
  public VirtualFile getProjectFile() {
    return myProject.isDefault() ? null : ((VfsFileBasedStorage)getDefaultFileStorage()).getVirtualFile();
  }

  @Nonnull
  private XmlElementStorage getDefaultFileStorage() {
    // XmlElementStorage if default project, otherwise FileBasedStorage
    XmlElementStorage storage = (XmlElementStorage)getStateStorageManager().getStateStorage(StoragePathMacros.DEFAULT_FILE, RoamingType.DEFAULT);
    assert storage != null;
    return storage;
  }

  @Override
  public VirtualFile getWorkspaceFile() {
    if (myProject.isDefault()) return null;
    final VfsFileBasedStorage storage = (VfsFileBasedStorage)getStateStorageManager().getStateStorage(StoragePathMacros.WORKSPACE_FILE, RoamingType.DISABLED);
    assert storage != null;
    return storage.getVirtualFile();
  }

  @Override
  public void loadProjectFromTemplate(@Nonnull ProjectImpl defaultProject) {
    defaultProject.save();

    Element element = ((DefaultProjectStoreImpl)defaultProject.getStateStore()).getStateCopy();
    if (element != null) {
      getDefaultFileStorage().setDefaultState(element);
    }
  }

  @Nonnull
  @Override
  public String getProjectFilePath() {
    return myProject.isDefault() ? "" : ((VfsFileBasedStorage)getDefaultFileStorage()).getFilePath();
  }

  @Nonnull
  @Override
  protected XmlElementStorage getMainStorage() {
    return getDefaultFileStorage();
  }

  @Nonnull
  @Override
  protected StateStorageManager createStateStorageManager() {
    return new ProjectStateStorageManager(myProject, new TrackingPathMacroSubstitutorImpl((BasePathMacroManager)myPathMacroManager), Application.get().getInstance(PathMacrosService.class));
  }

  @Nonnull
  @Override
  protected <T> Storage[] getComponentStorageSpecs(@Nonnull PersistentStateComponent<T> persistentStateComponent, @Nonnull State stateSpec, @Nonnull StateStorageOperation operation) {
    Storage[] storages = stateSpec.storages();
    if (storages.length == 1) {
      return storages;
    }

    assert storages.length > 0;
    return storages;
  }

  @Override
  protected final void doSave(boolean force, @Nullable List<SaveSession> saveSessions, @Nonnull List<Pair<SaveSession, File>> readonlyFiles) {
    ProjectStorageUtil.UnableToSaveProjectNotification[] notifications =
            NotificationsManager.getNotificationsManager().getNotificationsOfType(ProjectStorageUtil.UnableToSaveProjectNotification.class, myProject);
    if (notifications.length > 0) {
      throw new SaveCancelledException();
    }

    beforeSave(readonlyFiles);

    super.doSave(force, saveSessions, readonlyFiles);

    if (!readonlyFiles.isEmpty()) {
      ReadonlyStatusHandler.OperationStatus status = AccessRule.read(() -> {
        List<File> filesList = getFilesList(readonlyFiles);
        VirtualFile[] files = filesList.stream().map(file -> LocalFileSystem.getInstance().findFileByIoFile(file)).toArray(VirtualFile[]::new);
        return ReadonlyStatusHandler.getInstance(myProject).ensureFilesWritable(files);
      });

      if (status.hasReadonlyFiles()) {
        ProjectStorageUtil.dropUnableToSaveProjectNotification(myProject, VfsUtil.virtualToIoFiles(Arrays.asList(status.getReadonlyFiles())));
        throw new SaveCancelledException();
      }
      else {
        List<Pair<SaveSession, File>> oldList = new ArrayList<>(readonlyFiles);
        readonlyFiles.clear();
        for (Pair<SaveSession, File> entry : oldList) {
          executeSave(entry.first, false, readonlyFiles);
        }

        if (!readonlyFiles.isEmpty()) {
          ProjectStorageUtil.dropUnableToSaveProjectNotification(myProject, getFilesList(readonlyFiles));
          throw new SaveCancelledException();
        }
      }
    }
  }

  protected void beforeSave(@Nonnull List<Pair<SaveSession, File>> readonlyFiles) {
  }

  @Nonnull
  private static List<File> getFilesList(List<Pair<SaveSession, File>> readonlyFiles) {
    return readonlyFiles.stream().map(saveSessionFilePair -> saveSessionFilePair.getSecond()).collect(Collectors.toList());
  }

  @Nonnull
  @Override
  protected MessageBus getMessageBus() {
    return myProject.getMessageBus();
  }
}
