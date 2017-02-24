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

import com.intellij.CommonBundle;
import com.intellij.notification.NotificationsManager;
import com.intellij.openapi.application.*;
import com.intellij.openapi.components.*;
import com.intellij.openapi.components.StateStorage.SaveSession;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.help.HelpManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.project.impl.ProjectImpl;
import com.intellij.openapi.project.impl.ProjectManagerImpl;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.*;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.messages.MessageBus;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ProjectStoreImpl extends BaseFileConfigurableStoreImpl implements IProjectStore {
  private static final Logger LOG = Logger.getInstance(ProjectStoreImpl.class);

  @NonNls
  private static final String OLD_PROJECT_SUFFIX = "_old.";
  @NonNls
  static final String OPTION_WORKSPACE = "workspace";

  private static int originalVersion = -1;

  protected ProjectImpl myProject;
  private String myPresentableUrl;

  ProjectStoreImpl(@NotNull ProjectImpl project, @NotNull PathMacroManager pathMacroManager) {
    super(pathMacroManager);
    myProject = project;
  }

  @Override
  public boolean checkVersion() {
    if (originalVersion >= 0 && originalVersion < ProjectManagerImpl.CURRENT_FORMAT_VERSION) {
      final VirtualFile projectFile = getProjectFile();
      LOG.assertTrue(projectFile != null);
      String message = ProjectBundle.message("project.convert.old.prompt", projectFile.getName(), ApplicationNamesInfo.getInstance().getProductName(),
                                             projectFile.getNameWithoutExtension() + OLD_PROJECT_SUFFIX + projectFile.getExtension());
      if (Messages.showYesNoDialog(message, CommonBundle.getWarningTitle(), Messages.getWarningIcon()) != Messages.YES) return false;

      List<String> conversionProblems = getConversionProblemsStorage();
      if (!ContainerUtil.isEmpty(conversionProblems)) {
        StringBuilder buffer = new StringBuilder();
        buffer.append(ProjectBundle.message("project.convert.problems.detected"));
        for (String s : conversionProblems) {
          buffer.append('\n');
          buffer.append(s);
        }
        buffer.append(ProjectBundle.message("project.convert.problems.help"));
        if (Messages.showOkCancelDialog(myProject, buffer.toString(), ProjectBundle.message("project.convert.problems.title"),
                                        ProjectBundle.message("project.convert.problems.help.button"), CommonBundle.getCloseButtonText(),
                                        Messages.getWarningIcon()) == Messages.OK) {
          HelpManager.getInstance().invokeHelp("project.migrationProblems");
        }
      }

      ApplicationManager.getApplication().runWriteAction(new Runnable() {
        @Override
        public void run() {
          try {
            VirtualFile projectDir = projectFile.getParent();
            assert projectDir != null;

            backup(projectDir, projectFile);

            VirtualFile workspaceFile = getWorkspaceFile();
            if (workspaceFile != null) {
              backup(projectDir, workspaceFile);
            }
          }
          catch (IOException e) {
            LOG.error(e);
          }
        }

        private void backup(final VirtualFile projectDir, final VirtualFile vile) throws IOException {
          final String oldName = vile.getNameWithoutExtension() + OLD_PROJECT_SUFFIX + vile.getExtension();
          VfsUtil.saveText(projectDir.findOrCreateChildData(this, oldName), VfsUtilCore.loadText(vile));
        }
      });
    }

    return originalVersion <= ProjectManagerImpl.CURRENT_FORMAT_VERSION ||
           MessageDialogBuilder.yesNo(CommonBundle.getWarningTitle(), ProjectBundle
                   .message("project.load.new.version.warning", myProject.getName(), ApplicationNamesInfo.getInstance().getProductName()))
                   .icon(Messages.getWarningIcon()).project(myProject).is();
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
  public void setProjectFilePath(@NotNull final String filePath) {
    final StateStorageManager stateStorageManager = getStateStorageManager();
    final LocalFileSystem fs = LocalFileSystem.getInstance();

    final File file = new File(filePath);

    final File dirStore = file.isDirectory() ? new File(file, Project.DIRECTORY_STORE_FOLDER) : new File(file.getParentFile(), Project.DIRECTORY_STORE_FOLDER);
    stateStorageManager.addMacro(StoragePathMacros.PROJECT_FILE, new File(dirStore, "misc.xml").getPath());

    final File ws = new File(dirStore, "workspace.xml");
    stateStorageManager.addMacro(StoragePathMacros.WORKSPACE_FILE, ws.getPath());

    stateStorageManager.addMacro(StoragePathMacros.PROJECT_CONFIG_DIR, dirStore.getPath());

    ApplicationManager.getApplication().invokeAndWait(new Runnable() {
      @Override
      public void run() {
        VfsUtil.markDirtyAndRefresh(false, true, true, fs.refreshAndFindFileByIoFile(dirStore));
      }
    }, ModalityState.defaultModalityState());

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
    StateStorage storage = getStateStorageManager().getStateStorage(StoragePathMacros.PROJECT_FILE, RoamingType.PER_USER);
    if (!(storage instanceof FileBasedStorage)) {
      return null;
    }

    return getBasePath(((FileBasedStorage)storage).getFile());
  }

  private String getBasePath(@NotNull File file) {
    if (myProject.isDefault()) {
      return file.getParent();
    }
    else {
      File parentFile = file.getParentFile();
      return parentFile == null ? null : parentFile.getParent();
    }
  }

  @NotNull
  @Override
  public String getProjectName() {
    final VirtualFile baseDir = getProjectBaseDir();
    assert baseDir != null : "project file=" + getProjectFilePath();

    final VirtualFile ideaDir = baseDir.findChild(Project.DIRECTORY_STORE_FOLDER);
    if (ideaDir != null && ideaDir.isValid()) {
      final VirtualFile nameFile = ideaDir.findChild(ProjectImpl.NAME_FILE);
      if (nameFile != null && nameFile.isValid()) {
        try {
          try (BufferedReader in = new BufferedReader(new InputStreamReader(nameFile.getInputStream(), CharsetToolkit.UTF8_CHARSET))) {
            final String name = in.readLine();
            if (name != null && name.length() > 0) {
              return name.trim();
            }
          }
        }
        catch (IOException ignored) {
        }
      }
    }

    return baseDir.getName().replace(":", "");
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
  public void loadProject() throws IOException, JDOMException, InvalidDataException, StateStorageException {
    myProject.init();
  }

  @Override
  public VirtualFile getProjectFile() {
    return myProject.isDefault() ? null : ((FileBasedStorage)getProjectFileStorage()).getVirtualFile();
  }

  @NotNull
  private XmlElementStorage getProjectFileStorage() {
    // XmlElementStorage if default project, otherwise FileBasedStorage
    XmlElementStorage storage = (XmlElementStorage)getStateStorageManager().getStateStorage(StoragePathMacros.PROJECT_FILE, RoamingType.PER_USER);
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
  public void loadProjectFromTemplate(@NotNull ProjectImpl defaultProject) {
    defaultProject.save();

    Element element = ((DefaultProjectStoreImpl)defaultProject.getStateStore()).getStateCopy();
    if (element != null) {
      getProjectFileStorage().setDefaultState(element);
    }
  }

  @NotNull
  @Override
  public String getProjectFilePath() {
    return myProject.isDefault() ? "" : ((FileBasedStorage)getProjectFileStorage()).getFilePath();
  }

  @NotNull
  @Override
  protected XmlElementStorage getMainStorage() {
    return getProjectFileStorage();
  }

  @NotNull
  @Override
  protected StateStorageManager createStateStorageManager() {
    return new ProjectStateStorageManager(myPathMacroManager.createTrackingSubstitutor(), myProject);
  }

  @NotNull
  @Override
  protected <T> Storage[] getComponentStorageSpecs(@NotNull PersistentStateComponent<T> persistentStateComponent,
                                                   @NotNull State stateSpec,
                                                   @NotNull StateStorageOperation operation) {
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

  static class ProjectStorageData extends BaseStorageData {
    protected final Project myProject;

    ProjectStorageData(final String rootElementName, Project project) {
      super(rootElementName);
      myProject = project;
    }

    protected ProjectStorageData(ProjectStorageData storageData) {
      super(storageData);
      myProject = storageData.myProject;
    }

    @Override
    public StorageData clone() {
      return new ProjectStorageData(this);
    }
  }

  static class WsStorageData extends ProjectStorageData {
    WsStorageData(final String rootElementName, final Project project) {
      super(rootElementName, project);
    }

    WsStorageData(final WsStorageData storageData) {
      super(storageData);
    }

    @Override
    public StorageData clone() {
      return new WsStorageData(this);
    }
  }

  static class IprStorageData extends ProjectStorageData {
    IprStorageData(final String rootElementName, Project project) {
      super(rootElementName, project);
    }

    IprStorageData(final IprStorageData storageData) {
      super(storageData);
    }

    @Override
    public void load(@NotNull Element rootElement, @Nullable PathMacroSubstitutor pathMacroSubstitutor, boolean intern) {
      final String v = rootElement.getAttributeValue(VERSION_OPTION);
      //noinspection AssignmentToStaticFieldFromInstanceMethod
      originalVersion = v != null ? Integer.parseInt(v) : 0;

      if (originalVersion != ProjectManagerImpl.CURRENT_FORMAT_VERSION) {
        convert(rootElement, originalVersion);
      }

      super.load(rootElement, pathMacroSubstitutor, intern);
    }

    protected void convert(final Element root, final int originalVersion) {
    }

    @Override
    public StorageData clone() {
      return new IprStorageData(this);
    }
  }

  @Override
  protected final void doSave(@Nullable List<SaveSession> saveSessions, @NotNull List<Pair<SaveSession, VirtualFile>> readonlyFiles) {
    ProjectImpl.UnableToSaveProjectNotification[] notifications =
            NotificationsManager.getNotificationsManager().getNotificationsOfType(ProjectImpl.UnableToSaveProjectNotification.class, myProject);
    if (notifications.length > 0) {
      throw new SaveCancelledException();
    }

    beforeSave(readonlyFiles);

    super.doSave(saveSessions, readonlyFiles);

    if (!readonlyFiles.isEmpty()) {
      ReadonlyStatusHandler.OperationStatus status;
      AccessToken token = ReadAction.start();
      try {
        status = ReadonlyStatusHandler.getInstance(myProject).ensureFilesWritable(getFilesList(readonlyFiles));
      }
      finally {
        token.finish();
      }

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

  protected void beforeSave(@NotNull List<Pair<SaveSession, VirtualFile>> readonlyFiles) {
  }

  @NotNull
  private static VirtualFile[] getFilesList(List<Pair<SaveSession, VirtualFile>> readonlyFiles) {
    final VirtualFile[] files = new VirtualFile[readonlyFiles.size()];
    for (int i = 0, size = readonlyFiles.size(); i < size; i++) {
      files[i] = readonlyFiles.get(i).second;
    }
    return files;
  }

  @NotNull
  @Override
  protected MessageBus getMessageBus() {
    return myProject.getMessageBus();
  }
}
