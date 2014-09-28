/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
import com.intellij.ide.highlighter.ProjectFileType;
import com.intellij.ide.highlighter.WorkspaceFileType;
import com.intellij.notification.NotificationsManager;
import com.intellij.openapi.application.*;
import com.intellij.openapi.components.*;
import com.intellij.openapi.components.store.ComponentSaveSession;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.help.HelpManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.project.impl.ProjectImpl;
import com.intellij.openapi.project.impl.ProjectManagerImpl;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.*;
import com.intellij.util.PathUtilRt;
import com.intellij.util.containers.OrderedSet;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ProjectStoreImpl extends BaseFileConfigurableStoreImpl implements IProjectStore {
  private static final Logger LOG = Logger.getInstance(ProjectStoreImpl.class);

  @NonNls private static final String OLD_PROJECT_SUFFIX = "_old.";
  @NonNls static final String OPTION_WORKSPACE = "workspace";

  static final Storage DEFAULT_STORAGE_ANNOTATION = new MyStorage();
  private static int originalVersion = -1;

  protected ProjectImpl myProject;
  private StorageScheme myScheme = StorageScheme.DEFAULT;
  private String myPresentableUrl;

  ProjectStoreImpl(@NotNull ProjectImpl project) {
    super(project);

    myProject = project;
  }

  @Override
  public boolean checkVersion() {
    final ApplicationNamesInfo appNamesInfo = ApplicationNamesInfo.getInstance();
    if (originalVersion >= 0 && originalVersion < ProjectManagerImpl.CURRENT_FORMAT_VERSION) {
      final VirtualFile projectFile = getProjectFile();
      LOG.assertTrue(projectFile != null);
      String name = projectFile.getNameWithoutExtension();

      String message = ProjectBundle.message("project.convert.old.prompt", projectFile.getName(),
                                             appNamesInfo.getProductName(),
                                             name + OLD_PROJECT_SUFFIX + projectFile.getExtension());
      if (Messages.showYesNoDialog(message, CommonBundle.getWarningTitle(), Messages.getWarningIcon()) != Messages.YES) return false;

      List<String> conversionProblems = getConversionProblemsStorage();
      if (conversionProblems != null && !conversionProblems.isEmpty()) {
        StringBuilder buffer = new StringBuilder();
        buffer.append(ProjectBundle.message("project.convert.problems.detected"));
        for (String s : conversionProblems) {
          buffer.append('\n');
          buffer.append(s);
        }
        buffer.append(ProjectBundle.message("project.convert.problems.help"));
        final int result = Messages.showOkCancelDialog(myProject, buffer.toString(), ProjectBundle.message("project.convert.problems.title"),
                                                       ProjectBundle.message("project.convert.problems.help.button"),
                                                       CommonBundle.getCloseButtonText(), Messages.getWarningIcon());
        if (result == Messages.OK) {
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
          final VirtualFile oldFile = projectDir.findOrCreateChildData(this, oldName);
          assert oldFile != null : projectDir + ", " + oldName;
          VfsUtil.saveText(oldFile, VfsUtilCore.loadText(vile));
        }
      });
    }

    if (originalVersion > ProjectManagerImpl.CURRENT_FORMAT_VERSION) {
      String message =
              ProjectBundle.message("project.load.new.version.warning", myProject.getName(), appNamesInfo.getProductName());

      if (Messages.showYesNoDialog(message, CommonBundle.getWarningTitle(), Messages.getWarningIcon()) != Messages.YES) return false;
    }

    return true;
  }

  @Override
  public TrackingPathMacroSubstitutor[] getSubstitutors() {
    return new TrackingPathMacroSubstitutor[] {getStateStorageManager().getMacroSubstitutor()};
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
    if (!isIprPath(file)) {
      myScheme = StorageScheme.DIRECTORY_BASED;

      final File dirStore = file.isDirectory() ? new File(file, Project.DIRECTORY_STORE_FOLDER)
                                               : new File(file.getParentFile(), Project.DIRECTORY_STORE_FOLDER);
      stateStorageManager.addMacro(StoragePathMacros.PROJECT_FILE, new File(dirStore, "misc.xml").getPath());

      final File ws = new File(dirStore, "workspace.xml");
      stateStorageManager.addMacro(StoragePathMacros.WORKSPACE_FILE, ws.getPath());
      if (!ws.exists() && !file.isDirectory()) {
        useOldWsContent(filePath, ws);
      }

      stateStorageManager.addMacro(StoragePathMacros.PROJECT_CONFIG_DIR, dirStore.getPath());

      ApplicationManager.getApplication().invokeAndWait(new Runnable() {
        @Override
        public void run() {
          VfsUtil.markDirtyAndRefresh(false, true, true, fs.refreshAndFindFileByIoFile(dirStore));
        }
      }, ModalityState.defaultModalityState());
    }
    else {
      myScheme = StorageScheme.DEFAULT;

      stateStorageManager.addMacro(StoragePathMacros.PROJECT_FILE, filePath);

      final String workspacePath = composeWsPath(filePath);
      stateStorageManager.addMacro(StoragePathMacros.WORKSPACE_FILE, workspacePath);

      ApplicationManager.getApplication().invokeAndWait(new Runnable() {
        @Override
        public void run() {
          VfsUtil.markDirtyAndRefresh(false, true, false, fs.refreshAndFindFileByPath(filePath), fs.refreshAndFindFileByPath(workspacePath));
        }
      }, ModalityState.defaultModalityState());
    }

    myPresentableUrl = null;
  }

  private static boolean isIprPath(final File file) {
    return FileUtilRt.extensionEquals(file.getName(), ProjectFileType.DEFAULT_EXTENSION);
  }

  private static String composeWsPath(String filePath) {
    final int lastDot = filePath.lastIndexOf(".");
    final String filePathWithoutExt = lastDot > 0 ? filePath.substring(0, lastDot) : filePath;
    return filePathWithoutExt + WorkspaceFileType.DOT_DEFAULT_EXTENSION;
  }

  private static void useOldWsContent(final String filePath, final File ws) {
    final File oldWs = new File(composeWsPath(filePath));
    if (oldWs.exists()) {
      try {
        final InputStream is = new FileInputStream(oldWs);
        try {
          final byte[] bytes = FileUtil.loadBytes(is, (int)oldWs.length());

          final OutputStream os = new FileOutputStream(ws);
          try {
            os.write(bytes);
          }
          finally {
            os.close();
          }
        }
        finally {
          is.close();
        }
      }
      catch (IOException e) {
        LOG.error(e);
      }
    }
  }

  @Override
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
    if (myScheme == StorageScheme.DEFAULT) {
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
    if (myScheme == StorageScheme.DIRECTORY_BASED) {
      final VirtualFile baseDir = getProjectBaseDir();
      assert baseDir != null : "scheme=" + myScheme + " project file=" + getProjectFilePath();

      final VirtualFile ideaDir = baseDir.findChild(Project.DIRECTORY_STORE_FOLDER);
      if (ideaDir != null && ideaDir.isValid()) {
        final VirtualFile nameFile = ideaDir.findChild(ProjectImpl.NAME_FILE);
        if (nameFile != null && nameFile.isValid()) {
          try {
            BufferedReader in = new BufferedReader(new InputStreamReader(nameFile.getInputStream(), "UTF-8"));
            try {
              final String name = in.readLine();
              if (name != null && name.length() > 0) {
                return name.trim();
              }
            }
            finally {
              in.close();
            }
          }
          catch (IOException ignored) { }
        }
      }

      return baseDir.getName().replace(":", "");
    }
    else {
      String temp = PathUtilRt.getFileName(((FileBasedStorage)getProjectFileStorage()).getFilePath());
      FileType fileType = FileTypeManager.getInstance().getFileTypeByFileName(temp);
      if (fileType instanceof ProjectFileType) {
        temp = temp.substring(0, temp.length() - fileType.getDefaultExtension().length() - 1);
      }
      final int i = temp.lastIndexOf(File.separatorChar);
      if (i >= 0) {
        temp = temp.substring(i + 1, temp.length() - i + 1);
      }
      return temp;
    }
  }

  @NotNull
  @Override
  public StorageScheme getStorageScheme() {
    return myScheme;
  }

  @Override
  public String getPresentableUrl() {
    if (myProject.isDefault()) {
      return null;
    }
    if (myPresentableUrl == null) {
      String url = myScheme == StorageScheme.DIRECTORY_BASED ? getProjectBasePath() : getProjectFilePath();
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
  public void loadProjectFromTemplate(@NotNull final ProjectImpl defaultProject) {
    XmlElementStorage stateStorage = getProjectFileStorage();

    defaultProject.save();
    final IProjectStore projectStore = defaultProject.getStateStore();
    assert projectStore instanceof DefaultProjectStoreImpl;
    DefaultProjectStoreImpl defaultProjectStore = (DefaultProjectStoreImpl)projectStore;
    final Element element = defaultProjectStore.getStateCopy();
    if (element != null) {
      stateStorage.setDefaultState(element);
    }
  }

  @NotNull
  @Override
  public String getProjectFilePath() {
    return myProject.isDefault() ? "" : ((FileBasedStorage)getProjectFileStorage()).getFilePath();
  }

  @Override
  protected XmlElementStorage getMainStorage() {
    return getProjectFileStorage();
  }

  @NotNull
  @Override
  protected StateStorageManager createStateStorageManager() {
    return new ProjectStateStorageManager(PathMacroManager.getInstance(getComponentManager()).createTrackingSubstitutor(), myProject);
  }

  static class  ProjectStorageData extends BaseStorageData {
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
  protected SaveSessionImpl createSaveSession() throws StateStorageException {
    return new ProjectSaveSession();
  }

  protected class ProjectSaveSession extends SaveSessionImpl {
    @NotNull
    @Override
    public ComponentSaveSession save(@NotNull List<Pair<StateStorageManager.SaveSession, VirtualFile>> readonlyFiles) {
      ProjectImpl.UnableToSaveProjectNotification[] notifications =
              NotificationsManager.getNotificationsManager().getNotificationsOfType(ProjectImpl.UnableToSaveProjectNotification.class, myProject);
      if (notifications.length > 0) {
        throw new SaveCancelledException();
      }

      beforeSave(readonlyFiles);

      super.save(readonlyFiles);

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
          readonlyFiles.clear();
          for (Pair<StateStorageManager.SaveSession, VirtualFile> entry : readonlyFiles) {
            executeSave(entry.first, readonlyFiles);
          }

          if (!readonlyFiles.isEmpty()) {
            ProjectImpl.dropUnableToSaveProjectNotification(myProject, getFilesList(readonlyFiles));
            throw new SaveCancelledException();
          }
        }
      }

      return this;
    }

    @NotNull
    private VirtualFile[] getFilesList(List<Pair<StateStorageManager.SaveSession, VirtualFile>> readonlyFiles) {
      final VirtualFile[] files = new VirtualFile[readonlyFiles.size()];
      for (int i = 0, size = readonlyFiles.size(); i < size; i++) {
        files[i] = readonlyFiles.get(i).second;
      }
      return files;
    }

    protected void beforeSave(@NotNull List<Pair<StateStorageManager.SaveSession, VirtualFile>> readonlyFiles) {
    }
  }

  private final StateStorageChooser<PersistentStateComponent<?>> myStateStorageChooser = new StateStorageChooser<PersistentStateComponent<?>>() {
    @Override
    public Storage[] selectStorages(final Storage[] storages, final PersistentStateComponent<?> component, final StateStorageOperation operation) {
      if (operation == StateStorageOperation.READ) {
        OrderedSet<Storage> result = new OrderedSet<Storage>();

        for (Storage storage : storages) {
          if (storage.scheme() == myScheme) {
            result.add(0, storage);
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
        List<Storage> result = new ArrayList<Storage>();
        for (Storage storage : storages) {
          if (storage.scheme() == myScheme) {
            result.add(storage);
          }
        }

        if (!result.isEmpty()) return result.toArray(new Storage[result.size()]);

        for (Storage storage : storages) {
          if (storage.scheme() == StorageScheme.DEFAULT) {
            result.add(storage);
          }
        }

        return result.toArray(new Storage[result.size()]);
      }

      return new Storage[]{};
    }
  };

  @Override
  protected StateStorageChooser<PersistentStateComponent<?>> getDefaultStateStorageChooser() {
    return myStateStorageChooser;
  }

  @NotNull
  @Override
  protected <T> Storage[] getComponentStorageSpecs(@NotNull final PersistentStateComponent<T> persistentStateComponent,
                                                   final StateStorageOperation operation) throws StateStorageException {
    Storage[] result = super.getComponentStorageSpecs(persistentStateComponent, operation);

    if (operation == StateStorageOperation.READ) {
      Storage[] upd = new Storage[result.length + 1];
      System.arraycopy(result, 0, upd, 0, result.length);
      upd[result.length] = DEFAULT_STORAGE_ANNOTATION;
      result = upd;
    }

    return result;
  }

  @SuppressWarnings("ClassExplicitlyAnnotation")
  private static class MyStorage implements Storage {
    @Override
    public String id() {
      return "___Default___";
    }

    @Override
    public boolean isDefault() {
      return true;
    }

    @Override
    public String file() {
      return StoragePathMacros.PROJECT_FILE;
    }

    @Override
    public StorageScheme scheme() {
      return  StorageScheme.DEFAULT;
    }

    @Override
    public RoamingType roamingType() {
      return RoamingType.PER_USER;
    }

    @Override
    public Class<? extends StateStorage> storageClass() {
      return StateStorage.class;
    }

    @Override
    public Class<? extends StateSplitter> stateSplitter() {
      return StateSplitter.class;
    }

    @NotNull
    @Override
    public Class<? extends Annotation> annotationType() {
      throw new UnsupportedOperationException("Method annotationType not implemented in " + getClass());
    }
  }

  @Override
  public boolean reload(@NotNull Set<Pair<VirtualFile, StateStorage>> changedFiles) throws IOException {
    return reload(changedFiles, myProject.getMessageBus()) == null;
  }
}
