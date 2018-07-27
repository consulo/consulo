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
package com.intellij.openapi.components.impl.stores;

import com.intellij.notification.NotificationsManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.components.*;
import com.intellij.openapi.components.StateStorage.SaveSession;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.impl.ProjectImpl;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.ReadonlyStatusHandler;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.SmartList;
import com.intellij.util.messages.MessageBus;
import consulo.application.AccessRule;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class ProjectStoreImpl extends BaseFileConfigurableStoreImpl implements IProjectStore {
  private static final Logger LOG = Logger.getInstance(ProjectStoreImpl.class);

  protected ProjectImpl myProject;
  private String myPresentableUrl;

  @Inject
  ProjectStoreImpl(@Nonnull Project project) {
    super(PathMacroManager.getInstance(project));
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

    ApplicationManager.getApplication().invokeAndWait(() -> VfsUtil.markDirtyAndRefresh(false, true, true, fs.refreshAndFindFileByIoFile(dirStore)), ModalityState.defaultModalityState());

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
    StateStorage storage = getStateStorageManager().getStateStorage(StoragePathMacros.DEFAULT_FILE, RoamingType.PER_USER);
    if (!(storage instanceof FileBasedStorage)) {
      return null;
    }

    return getBasePath(((FileBasedStorage)storage).getFile());
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
    return myProject.isDefault() ? null : ((FileBasedStorage)getDefaultFileStorage()).getVirtualFile();
  }

  @Nonnull
  private XmlElementStorage getDefaultFileStorage() {
    // XmlElementStorage if default project, otherwise FileBasedStorage
    XmlElementStorage storage = (XmlElementStorage)getStateStorageManager().getStateStorage(StoragePathMacros.DEFAULT_FILE, RoamingType.PER_USER);
    assert storage != null;
    return storage;
  }

  @Override
  public VirtualFile getWorkspaceFile() {
    if (myProject.isDefault()) return null;
    final FileBasedStorage storage = (FileBasedStorage)getStateStorageManager().getStateStorage(StoragePathMacros.WORKSPACE_FILE, RoamingType.DISABLED);
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
    return myProject.isDefault() ? "" : ((FileBasedStorage)getDefaultFileStorage()).getFilePath();
  }

  @Nonnull
  @Override
  protected XmlElementStorage getMainStorage() {
    return getDefaultFileStorage();
  }

  @Nonnull
  @Override
  protected StateStorageManager createStateStorageManager() {
    return new ProjectStateStorageManager(myPathMacroManager.createTrackingSubstitutor(), myProject);
  }

  @Nonnull
  @Override
  protected <T> Storage[] getComponentStorageSpecs(@Nonnull PersistentStateComponent<T> persistentStateComponent, @Nonnull State stateSpec, @Nonnull StateStorageOperation operation) {
    Storage[] storages = stateSpec.storages();
    if (storages.length == 1) {
      return storages;
    }
    assert storages.length > 0;

    if (operation == StateStorageOperation.READ) {
      List<Storage> result = new SmartList<>();
      for (int i = storages.length - 1; i >= 0; i--) {
        Storage storage = storages[i];
        if (storage.scheme() == StorageScheme.DIRECTORY_BASED) {
          result.add(storage);
        }
      }

      for (Storage storage : storages) {
        if (storage.scheme() == StorageScheme.DEFAULT) {
          result.add(storage);
        }
      }

      return result.toArray(new Storage[result.size()]);
    }
    else if (operation == StateStorageOperation.WRITE) {
      List<Storage> result = new SmartList<>();
      for (Storage storage : storages) {
        if (storage.scheme() == StorageScheme.DIRECTORY_BASED) {
          result.add(storage);
        }
      }

      if (!result.isEmpty()) {
        return result.toArray(new Storage[result.size()]);
      }

      for (Storage storage : storages) {
        if (storage.scheme() == StorageScheme.DEFAULT) {
          result.add(storage);
        }
      }

      return result.toArray(new Storage[result.size()]);
    }
    else {
      return new Storage[]{};
    }
  }

  @Override
  protected final void doSave(@Nullable List<SaveSession> saveSessions, @Nonnull List<Pair<SaveSession, VirtualFile>> readonlyFiles) {
    ProjectImpl.UnableToSaveProjectNotification[] notifications = NotificationsManager.getNotificationsManager().getNotificationsOfType(ProjectImpl.UnableToSaveProjectNotification.class, myProject);
    if (notifications.length > 0) {
      throw new SaveCancelledException();
    }

    beforeSave(readonlyFiles);

    super.doSave(saveSessions, readonlyFiles);

    if (!readonlyFiles.isEmpty()) {
      ReadonlyStatusHandler.OperationStatus status = AccessRule.read(() -> ReadonlyStatusHandler.getInstance(myProject).ensureFilesWritable(getFilesList(readonlyFiles)));
      if (status.hasReadonlyFiles()) {
        ProjectImpl.dropUnableToSaveProjectNotification(myProject, status.getReadonlyFiles());
        throw new SaveCancelledException();
      }
      else {
        List<Pair<SaveSession, VirtualFile>> oldList = new ArrayList<>(readonlyFiles);
        readonlyFiles.clear();
        for (Pair<SaveSession, VirtualFile> entry : oldList) {
          executeSave(entry.first, readonlyFiles);
        }

        if (!readonlyFiles.isEmpty()) {
          ProjectImpl.dropUnableToSaveProjectNotification(myProject, getFilesList(readonlyFiles));
          throw new SaveCancelledException();
        }
      }
    }
  }

  @Override
  protected final void doSaveAsync(@Nullable List<SaveSession> saveSessions, @Nonnull List<Pair<SaveSession, VirtualFile>> readonlyFiles) {
    ProjectImpl.UnableToSaveProjectNotification[] notifications = NotificationsManager.getNotificationsManager().getNotificationsOfType(ProjectImpl.UnableToSaveProjectNotification.class, myProject);
    if (notifications.length > 0) {
      throw new SaveCancelledException();
    }

    beforeSave(readonlyFiles);

    super.doSaveAsync(saveSessions, readonlyFiles);

    if (!readonlyFiles.isEmpty()) {
      ReadonlyStatusHandler.OperationStatus status = AccessRule.read(() -> ReadonlyStatusHandler.getInstance(myProject).ensureFilesWritable(getFilesList(readonlyFiles)));
      if (status.hasReadonlyFiles()) {
        ProjectImpl.dropUnableToSaveProjectNotification(myProject, status.getReadonlyFiles());
        throw new SaveCancelledException();
      }
      else {
        List<Pair<SaveSession, VirtualFile>> oldList = new ArrayList<>(readonlyFiles);
        readonlyFiles.clear();
        for (Pair<SaveSession, VirtualFile> entry : oldList) {
          executeSave(entry.first, readonlyFiles);
        }

        if (!readonlyFiles.isEmpty()) {
          ProjectImpl.dropUnableToSaveProjectNotification(myProject, getFilesList(readonlyFiles));
          throw new SaveCancelledException();
        }
      }
    }
  }

  protected void beforeSave(@Nonnull List<Pair<SaveSession, VirtualFile>> readonlyFiles) {
  }

  @Nonnull
  private static VirtualFile[] getFilesList(List<Pair<SaveSession, VirtualFile>> readonlyFiles) {
    final VirtualFile[] files = new VirtualFile[readonlyFiles.size()];
    for (int i = 0, size = readonlyFiles.size(); i < size; i++) {
      files[i] = readonlyFiles.get(i).second;
    }
    return files;
  }

  @Nonnull
  @Override
  protected MessageBus getMessageBus() {
    return myProject.getMessageBus();
  }
}
