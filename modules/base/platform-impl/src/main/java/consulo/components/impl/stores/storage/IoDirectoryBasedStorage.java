/*
 * Copyright 2013-2019 consulo.io
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

import com.intellij.openapi.components.StateSplitterEx;
import com.intellij.openapi.components.StateStorageException;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.TrackingPathMacroSubstitutor;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtilRt;
import com.intellij.util.containers.Interner;
import com.intellij.util.containers.SmartHashSet;
import consulo.application.options.PathMacrosService;
import consulo.components.impl.stores.DefaultStateSerializer;
import consulo.components.impl.stores.StorageUtil;
import consulo.disposer.Disposable;
import org.jdom.Element;
import org.jdom.JDOMException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2019-02-13
 */
public class IoDirectoryBasedStorage extends StateStorageBase<DirectoryStorageData> {
  private final File myDir;
  private final StateSplitterEx mySplitter;

  private DirectoryStorageData myStorageData;

  public IoDirectoryBasedStorage(@Nullable TrackingPathMacroSubstitutor pathMacroSubstitutor,
                                 @Nonnull String dir,
                                 @Nonnull StateSplitterEx splitter,
                                 @Nonnull Disposable parentDisposable,
                                 @Nullable final Listener listener) {
    super(pathMacroSubstitutor);

    myDir = new File(dir);
    mySplitter = splitter;
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
    loadFrom(storageData, myDir, myPathMacroSubstitutor);
    return storageData;
  }

  public void loadFrom(@Nonnull DirectoryStorageData data, @Nullable File dir, @Nullable TrackingPathMacroSubstitutor pathMacroSubstitutor) {
    if (dir == null || !dir.exists()) {
      return;
    }

    Interner<String> interner = Interner.createStringInterner();
    File[] files = dir.listFiles();
    if (files == null) {
      return;
    }

    for (File file : files) {
      if (!isStorageFile(file)) {
        continue;
      }

      try {
        Element element = JDOMUtil.loadDocument(file).getRootElement();
        String name = StorageData.getComponentNameIfValid(element);
        if (name == null) {
          continue;
        }

        if (!element.getName().equals(StorageData.COMPONENT)) {
          LOG.error("Incorrect root tag name (" + element.getName() + ") in " + file.getPath());
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

  public static boolean isStorageFile(@Nonnull File file) {
    // ignore system files like .DS_Store on Mac
    return StringUtilRt.endsWithIgnoreCase(file.getName(), DirectoryStorageData.DEFAULT_EXT);
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

  private static class MySaveSession implements SaveSession, ExternalizationSession {
    private final IoDirectoryBasedStorage storage;
    private final DirectoryStorageData originalStorageData;
    private DirectoryStorageData copiedStorageData;

    private final Set<String> dirtyFileNames = new SmartHashSet<>();
    private final Set<String> removedFileNames = new SmartHashSet<>();

    private MySaveSession(@Nonnull IoDirectoryBasedStorage storage, @Nonnull DirectoryStorageData storageData) {
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
      File dir = storage.myDir;
      if (copiedStorageData.isEmpty()) {
        if (dir.exists()) {
          try {
            StorageUtil.deleteFile(dir);
          }
          catch (IOException e) {
            throw new StateStorageException(e);
          }
        }
        storage.myStorageData = copiedStorageData;
        return;
      }

      FileUtil.createDirectory(dir);

      if (!dirtyFileNames.isEmpty()) {
        saveStates(dir);
      }
      if (dir.exists() && !removedFileNames.isEmpty()) {
        deleteFiles(dir);
      }

      storage.myStorageData = copiedStorageData;
    }

    private void saveStates(@Nonnull final File dir) {
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

            File childFile = new File(dir, fileName);
            FileUtil.createParentDirs(childFile);
            byte[] byteOut = StorageUtil.writeToBytes(storeElement, "\n");
            StorageUtil.writeFile(childFile, byteOut, null);
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

    private void deleteFiles(@Nonnull File dir) {
      for (File file : dir.listFiles()) {
        if (removedFileNames.contains(file.getName())) {
          FileUtil.delete(file);
        }
      }
    }
  }
}
