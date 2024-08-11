/*
 * Copyright 2013-2024 consulo.io
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
package consulo.component.store.impl.internal.storage.nio;

import consulo.component.persist.StateSplitterEx;
import consulo.component.persist.Storage;
import consulo.component.store.impl.internal.DefaultStateSerializer;
import consulo.component.store.impl.internal.PathMacrosService;
import consulo.component.store.impl.internal.StateStorageException;
import consulo.component.store.impl.internal.TrackingPathMacroSubstitutor;
import consulo.component.store.impl.internal.storage.*;
import consulo.disposer.Disposable;
import consulo.util.collection.SmartHashSet;
import consulo.util.interner.Interner;
import consulo.util.io.PathKt;
import consulo.util.jdom.JDOMUtil;
import consulo.util.jdom.interner.JDOMInterner;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.util.xml.serializer.WriteExternalException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jdom.Element;
import org.jdom.JDOMException;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2024-08-11
 */
public class PathDirectoryBasedStorage extends StateStorageBase<DirectoryStorageData> {
    private final Path myDir;
    private final StateSplitterEx mySplitter;

    private DirectoryStorageData myStorageData;

    public PathDirectoryBasedStorage(@Nullable TrackingPathMacroSubstitutor pathMacroSubstitutor,
                                     @Nonnull String dir,
                                     @Nonnull StateSplitterEx splitter,
                                     @Nonnull Disposable parentDisposable,
                                     @Nullable final StateStorageListener listener,
                                     @Nonnull PathMacrosService pathMacrosService) {
        super(pathMacroSubstitutor, pathMacrosService);

        myDir = Path.of(dir);
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

    public void loadFrom(@Nonnull DirectoryStorageData data, @Nonnull Path dir, @Nullable TrackingPathMacroSubstitutor pathMacroSubstitutor) {
        if (!Files.exists(dir)) {
            return;
        }

        Interner<String> interner = Interner.createStringInterner();
        try {
            Files.walk(dir, 1).filter(PathDirectoryBasedStorage::isStorageFile).forEach(path -> {
                try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                    Element element = JDOMUtil.loadDocument(reader).getRootElement();
                    String name = StorageData.getComponentNameIfValid(element);
                    if (name == null) {
                        return;
                    }

                    if (!element.getName().equals(StorageData.COMPONENT)) {
                        LOG.error("Incorrect root tag name (" + element.getName() + ") in " + path);
                        return;
                    }

                    List<Element> elementChildren = element.getChildren();
                    if (elementChildren.isEmpty()) {
                        return;
                    }

                    Element state = elementChildren.get(0).detach();
                    JDOMInterner.internStringsInElement(state, interner);
                    if (pathMacroSubstitutor != null) {
                        pathMacroSubstitutor.expandPaths(state);
                        pathMacroSubstitutor.addUnknownMacros(name, myPathMacrosService.getMacroNames(state));
                    }

                    data.setState(name, path.getFileName().toString(), state);
                }
                catch (IOException | JDOMException e) {
                    LOG.info("Unable to load state", e);
                }
            });
        }
        catch (IOException e) {
            throw new StateStorageException(e);
        }
    }

    public static boolean isStorageFile(@Nonnull Path path) {
        String fileName = path.getFileName().toString();
        // ignore system files like .DS_Store on Mac
        return StringUtil.endsWithIgnoreCase(fileName, DirectoryStorageData.DEFAULT_EXT);
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
        private final PathDirectoryBasedStorage storage;
        private final DirectoryStorageData originalStorageData;
        private DirectoryStorageData copiedStorageData;

        private final Set<String> dirtyFileNames = new SmartHashSet<>();
        private final Set<String> removedFileNames = new SmartHashSet<>();

        private MySaveSession(@Nonnull PathDirectoryBasedStorage storage, @Nonnull DirectoryStorageData storageData) {
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
            Path dir = storage.myDir;
            if (copiedStorageData.isEmpty()) {
                if (Files.exists(dir)) {
                    try {
                        PathKt.delete(dir);
                    }
                    catch (IOException e) {
                        throw new StateStorageException(e);
                    }
                }
                storage.myStorageData = copiedStorageData;
                return;
            }

            try {
                Files.createDirectories(dir);
            }
            catch (IOException e) {
                throw new StateStorageException(e);
            }

            if (!dirtyFileNames.isEmpty()) {
                saveStates(dir);
            }
            if (Files.exists(dir) && !removedFileNames.isEmpty()) {
                try {
                    deleteFiles(dir);
                }
                catch (IOException e) {
                    throw new StateStorageException(e);
                }
            }

            storage.myStorageData = copiedStorageData;
        }

        private void saveStates(@Nonnull Path dir) {
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

                        Path childFile = dir.resolve(fileName);
                        Files.createDirectories(dir);
                        byte[] byteOut = StorageUtil.writeToBytes(storeElement, "\n");
                        PathStorageUtil.writeFile(childFile, byteOut, null);
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

        private void deleteFiles(@Nonnull Path dir) throws IOException {
            Files.walk(dir, 1).forEach(path -> {
                if (removedFileNames.contains(path.getFileName().toString())) {
                    try {
                        PathKt.delete(path);
                    }
                    catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }
    }
}
