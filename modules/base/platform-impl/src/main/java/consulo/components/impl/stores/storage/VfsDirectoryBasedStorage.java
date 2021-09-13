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
import com.intellij.openapi.components.*;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.util.text.StringUtilRt;
import com.intellij.openapi.vfs.*;
import com.intellij.openapi.vfs.tracker.VirtualFileTracker;
import com.intellij.util.containers.Interner;
import com.intellij.util.containers.SmartHashSet;
import consulo.application.options.PathMacrosService;
import consulo.components.impl.stores.DefaultStateSerializer;
import consulo.components.impl.stores.ReadOnlyModificationException;
import consulo.components.impl.stores.StorageUtil;
import consulo.disposer.Disposable;
import org.jdom.Element;
import org.jdom.JDOMException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Directory storage - based on Consulo VFS
 */
public final class VfsDirectoryBasedStorage extends StateStorageBase<DirectoryStorageData> {
  private final File myDir;
  private volatile VirtualFile myVirtualFile;
  private final StateSplitterEx mySplitter;

  private DirectoryStorageData myStorageData;

  public VfsDirectoryBasedStorage(@Nullable TrackingPathMacroSubstitutor pathMacroSubstitutor,
                                  @Nonnull String dir,
                                  @Nonnull StateSplitterEx splitter,
                                  @Nonnull Disposable parentDisposable,
                                  @Nullable final Listener listener) {
    super(pathMacroSubstitutor);

    myDir = new File(dir);
    mySplitter = splitter;

    VirtualFileTracker virtualFileTracker = ServiceManager.getService(VirtualFileTracker.class);
    if (listener != null) {
      virtualFileTracker.addTracker(LocalFileSystem.PROTOCOL_PREFIX + myDir.getAbsolutePath().replace(File.separatorChar, '/'), new VirtualFileAdapter() {
        @Override
        public void contentsChanged(@Nonnull VirtualFileEvent event) {
          notifyIfNeed(event);
        }

        @Override
        public void fileDeleted(@Nonnull VirtualFileEvent event) {
          if (event.getFile().equals(myVirtualFile)) {
            myVirtualFile = null;
          }
          notifyIfNeed(event);
        }

        @Override
        public void fileCreated(@Nonnull VirtualFileEvent event) {
          notifyIfNeed(event);
        }

        private void notifyIfNeed(@Nonnull VirtualFileEvent event) {
          // storage directory will be removed if the only child was removed
          if (event.getFile().isDirectory() || isStorageFile(event.getFile())) {
            listener.storageFileChanged(event, VfsDirectoryBasedStorage.this);
          }
        }
      }, false, parentDisposable);
    }
  }

  @Override
  public void analyzeExternalChangesAndUpdateIfNeed(@Nonnull Set<String> result) {
    // todo reload only changed file, compute diff
    DirectoryStorageData oldData = myStorageData;
    DirectoryStorageData newData = loadState();
    myStorageData = newData;
    if (oldData == null) {
      result.addAll(newData.getComponentNames());
    }
    else {
      result.addAll(oldData.getComponentNames());
      result.addAll(newData.getComponentNames());
    }
  }

  @Nullable
  @Override
  protected Element getStateAndArchive(@Nonnull DirectoryStorageData storageData, @Nonnull String componentName) {
    return storageData.getCompositeStateAndArchive(componentName, mySplitter);
  }

  @Nonnull
  private DirectoryStorageData loadState() {
    DirectoryStorageData storageData = new DirectoryStorageData();
    loadFrom(storageData, getVirtualFile(), myPathMacroSubstitutor);
    return storageData;
  }

  public void loadFrom(@Nonnull DirectoryStorageData data, @Nullable VirtualFile dir, @Nullable TrackingPathMacroSubstitutor pathMacroSubstitutor) {
    if (dir == null || !dir.exists()) {
      return;
    }

    Interner<String> interner = Interner.createStringInterner();
    for (VirtualFile file : dir.getChildren()) {
      if (!isStorageFile(file)) {
        continue;
      }

      try {
        Element element = JDOMUtil.loadDocument(file.contentsToByteArray()).getRootElement();
        String name = StorageData.getComponentNameIfValid(element);
        if (name == null) {
          continue;
        }

        if (!element.getName().equals(StorageData.COMPONENT)) {
          LOG.error("Incorrect root tag name (" + element.getName() + ") in " + file.getPresentableUrl());
          continue;
        }

        List<Element> elementChildren = element.getChildren();
        if (elementChildren.isEmpty()) {
          continue;
        }

        Element state = (Element)elementChildren.get(0).detach();
        JDOMUtil.internStringsInElement(state, interner);
        if (pathMacroSubstitutor != null) {
          pathMacroSubstitutor.expandPaths(state);
          pathMacroSubstitutor.addUnknownMacros(name, PathMacrosService.getInstance().getMacroNames(state));
        }
        data.setState(name, file.getName(), state);
      }
      catch (IOException | JDOMException e) {
        LOG.info("Unable to load state", e);
      }
    }
  }

  public static boolean isStorageFile(@Nonnull VirtualFile file) {
    // ignore system files like .DS_Store on Mac
    return StringUtilRt.endsWithIgnoreCase(file.getNameSequence(), DirectoryStorageData.DEFAULT_EXT);
  }

  @Nullable
  private VirtualFile getVirtualFile() {
    VirtualFile virtualFile = myVirtualFile;
    if (virtualFile == null) {
      myVirtualFile = virtualFile = LocalFileSystem.getInstance().findFileByIoFile(myDir);
    }
    return virtualFile;
  }

  @Override
  @Nonnull
  protected DirectoryStorageData getStorageData(boolean reloadData) {
    if (myStorageData != null && !reloadData) {
      return myStorageData;
    }

    myStorageData = loadState();
    return myStorageData;
  }

  @Override
  @Nullable
  public ExternalizationSession startExternalization() {
    return checkIsSavingDisabled() ? null : new MySaveSession(this, getStorageData());
  }

  @Nonnull
  public static VirtualFile createDir(@Nonnull File ioDir, @Nonnull Object requestor) {
    //noinspection ResultOfMethodCallIgnored
    ioDir.mkdirs();
    String parentFile = ioDir.getParent();
    VirtualFile parentVirtualFile = parentFile == null ? null : LocalFileSystem.getInstance().refreshAndFindFileByPath(parentFile.replace(File.separatorChar, '/'));
    if (parentVirtualFile == null) {
      throw new StateStorageException(ProjectBundle.message("project.configuration.save.file.not.found", parentFile));
    }
    return getFile(ioDir.getName(), parentVirtualFile, requestor);
  }

  @Nonnull
  public static VirtualFile getFile(@Nonnull String fileName, @Nonnull VirtualFile parentVirtualFile, @Nonnull Object requestor) {
    VirtualFile file = parentVirtualFile.findChild(fileName);
    if (file != null) {
      return file;
    }

    try {
      return Application.get().runWriteAction((ThrowableComputable<VirtualFile, IOException>)() -> {
        return parentVirtualFile.createChildData(requestor, fileName);
      });
    }
    catch (IOException e) {
      throw new StateStorageException(e);
    }
  }

  private static class MySaveSession implements SaveSession, ExternalizationSession {
    private final VfsDirectoryBasedStorage storage;
    private final DirectoryStorageData originalStorageData;
    private DirectoryStorageData copiedStorageData;

    private final Set<String> dirtyFileNames = new SmartHashSet<String>();
    private final Set<String> removedFileNames = new SmartHashSet<String>();

    private MySaveSession(@Nonnull VfsDirectoryBasedStorage storage, @Nonnull DirectoryStorageData storageData) {
      this.storage = storage;
      originalStorageData = storageData;
    }

    @Override
    public void setState(@Nonnull Object component, @Nonnull String componentName, @Nonnull Object state, Storage storageSpec) {
      Element compositeState;
      try {
        compositeState = DefaultStateSerializer.serializeState(state, storageSpec);
      }
      catch (WriteExternalException e) {
        LOG.debug(e);
        return;
      }
      catch (Throwable e) {
        LOG.error("Unable to serialize " + componentName + " state", e);
        return;
      }

      removedFileNames.addAll(originalStorageData.getFileNames(componentName));
      if (JDOMUtil.isEmpty(compositeState)) {
        doSetState(componentName, null, null);
      }
      else {
        for (Pair<Element, String> pair : storage.mySplitter.splitState(compositeState)) {
          removedFileNames.remove(pair.second);
          doSetState(componentName, pair.second, pair.first);
        }

        if (!removedFileNames.isEmpty()) {
          for (String fileName : removedFileNames) {
            doSetState(componentName, fileName, null);
          }
        }
      }
    }

    private void doSetState(@Nonnull String componentName, @Nullable String fileName, @Nullable Element subState) {
      if (copiedStorageData == null) {
        copiedStorageData = DirectoryStorageData.setStateAndCloneIfNeed(componentName, fileName, subState, originalStorageData);
        if (copiedStorageData != null && fileName != null) {
          dirtyFileNames.add(fileName);
        }
      }
      else if (copiedStorageData.setState(componentName, fileName, subState) != null && fileName != null) {
        dirtyFileNames.add(fileName);
      }
    }

    @Override
    @Nullable
    public SaveSession createSaveSession(boolean force) {
      return storage.checkIsSavingDisabled() || copiedStorageData == null ? null : this;
    }

    @Override
    public void save(boolean force) {
      VirtualFile dir = storage.getVirtualFile();
      if (copiedStorageData.isEmpty()) {
        if (dir != null && dir.exists()) {
          try {
            StorageUtil.deleteFile(this, dir);
          }
          catch (IOException e) {
            throw new StateStorageException(e);
          }
        }
        storage.myStorageData = copiedStorageData;
        return;
      }

      if (dir == null || !dir.isValid()) {
        dir = createDir(storage.myDir, this);
      }

      if (!dirtyFileNames.isEmpty()) {
        saveStates(dir);
      }
      if (dir.exists() && !removedFileNames.isEmpty()) {
        deleteFiles(dir);
      }

      storage.myVirtualFile = dir;
      storage.myStorageData = copiedStorageData;
    }

    private void saveStates(@Nonnull final VirtualFile dir) {
      final Element storeElement = new Element(StorageData.COMPONENT);

      for (final String componentName : copiedStorageData.getComponentNames()) {
        copiedStorageData.processComponent(componentName, (fileName, state) -> {
          if (!dirtyFileNames.contains(fileName)) {
            return;
          }

          Element element = copiedStorageData.stateToElement(fileName, state);
          if (storage.myPathMacroSubstitutor != null) {
            storage.myPathMacroSubstitutor.collapsePaths(element);
          }

          try {
            storeElement.setAttribute(StorageData.NAME, componentName);
            storeElement.addContent(element);

            byte[] byteOut;
            VirtualFile file = getFile(fileName, dir, MySaveSession.this);
            if (file.exists()) {
              byteOut = StorageUtil.writeToBytes(storeElement);
            }
            else {
              byteOut = StorageUtil.writeToBytes(storeElement);
            }
            StorageUtil.writeFile(null, MySaveSession.this, file, byteOut, null);
          }
          catch (IOException e) {
            LOG.error(e);
          }
          finally {
            element.detach();
          }
        });
      }
    }

    private void deleteFiles(@Nonnull VirtualFile dir) {
      Application.get().runWriteAction(() -> {
        for (VirtualFile file : dir.getChildren()) {
          if (removedFileNames.contains(file.getName())) {
            deleteFile(file, this);
          }
        }
      });
    }
  }

  public static void deleteFile(@Nonnull VirtualFile file, @Nonnull Object requestor) {
    try {
      file.delete(requestor);
    }
    catch (FileNotFoundException ignored) {
      throw new ReadOnlyModificationException(VfsUtil.virtualToIoFile(file));
    }
    catch (IOException e) {
      throw new StateStorageException(e);
    }
  }
}
