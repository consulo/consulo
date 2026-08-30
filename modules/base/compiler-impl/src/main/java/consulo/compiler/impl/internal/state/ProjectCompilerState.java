/*
 * Copyright 2013-2026 consulo.io
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
package consulo.compiler.impl.internal.state;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.compiler.CompilerPaths;
import consulo.disposer.Disposable;
import consulo.index.io.data.DataExternalizer;
import consulo.index.io.data.DataInputOutputUtil;
import consulo.index.io.data.IOUtil;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.collection.Sets;
import consulo.util.io.FileUtil;
import consulo.util.lang.Pair;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 2026-08-30
 */
@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public class ProjectCompilerState implements Disposable {
    private static final Logger LOG = Logger.getInstance(ProjectCompilerState.class);

    private static final int VERSION = 1;
    private static final String INVALIDATE_MARKER = "invalidate";
    public static final long UNKNOWN_STAMP = -1L;

    public static void requestGlobalInvalidation() {
        FileUtil.createIfDoesntExist(new File(CompilerPaths.getCompilerSystemDirectory(), INVALIDATE_MARKER));
    }

    private static synchronized void processGlobalInvalidationMarker() {
        File marker = new File(CompilerPaths.getCompilerSystemDirectory(), INVALIDATE_MARKER);
        if (marker.exists()) {
            FileUtil.delete(CompilerPaths.getCompilerSystemDirectory());
        }
    }

    private static final DataExternalizer<Long> STAMP_EXTERNALIZER = new DataExternalizer<>() {
        @Override
        public void save(DataOutput out, Long value) throws IOException {
            DataInputOutputUtil.writeTIME(out, value);
        }

        @Override
        public Long read(DataInput in) throws IOException {
            return DataInputOutputUtil.readTIME(in);
        }
    };

    private static final DataExternalizer<Set<String>> PATH_SET_EXTERNALIZER = new DataExternalizer<>() {
        @Override
        public void save(DataOutput out, Set<String> value) throws IOException {
            DataInputOutputUtil.writeINT(out, value.size());
            for (String path : value) {
                IOUtil.writeUTF(out, path);
            }
        }

        @Override
        public Set<String> read(DataInput in) throws IOException {
            int size = DataInputOutputUtil.readINT(in);
            Set<String> result = new LinkedHashSet<>(size);
            for (int i = 0; i < size; i++) {
                result.add(IOUtil.readUTF(in));
            }
            return result;
        }
    };

    private static final DataExternalizer<OutputSourceInfo> OUTPUT_SOURCE_INFO_EXTERNALIZER = new DataExternalizer<>() {
        @Override
        public void save(DataOutput out, OutputSourceInfo value) throws IOException {
            value.write(out);
        }

        @Override
        public OutputSourceInfo read(DataInput in) throws IOException {
            return OutputSourceInfo.read(in);
        }
    };

    private static class Stores {
        private final PathPersistentMap<Long> myStamps;
        private final PathPersistentMap<Set<String>> mySrcToOut;
        private final PathPersistentMap<OutputSourceInfo> myOutToSrc;
        private final PathPersistentMap<OutputSourceInfo> myOutputsToDelete;

        Stores(File dir) throws IOException {
            myStamps = new PathPersistentMap<>(new File(dir, "stamps"), STAMP_EXTERNALIZER);
            mySrcToOut = new PathPersistentMap<>(new File(dir, "src_to_out"), PATH_SET_EXTERNALIZER);
            myOutToSrc = new PathPersistentMap<>(new File(dir, "out_to_src"), OUTPUT_SOURCE_INFO_EXTERNALIZER);
            myOutputsToDelete = new PathPersistentMap<>(new File(dir, "outputs_to_delete"), OUTPUT_SOURCE_INFO_EXTERNALIZER);
        }

        void force() {
            myStamps.force();
            mySrcToOut.force();
            myOutToSrc.force();
            myOutputsToDelete.force();
        }

        void close() {
            closeSilently(myStamps);
            closeSilently(mySrcToOut);
            closeSilently(myOutToSrc);
            closeSilently(myOutputsToDelete);
        }

        private static void closeSilently(PathPersistentMap<?> map) {
            try {
                map.close();
            }
            catch (IOException e) {
                LOG.error(e);
            }
        }
    }

    public static ProjectCompilerState getInstance(Project project) {
        return project.getInstance(ProjectCompilerState.class);
    }

    private final Project myProject;
    private final Object myLock = new Object();
    private final Set<String> myDirtyPaths = Sets.newHashSet(FileUtil.PATH_HASHING_STRATEGY);

    private @Nullable Stores myStores;
    private boolean myBroken;

    @Inject
    public ProjectCompilerState(Project project) {
        myProject = project;
    }

    public static String normalizePath(String path) {
        return FileUtil.toSystemIndependentName(path);
    }

    public boolean markSourceDirty(String srcPath) {
        return markSourceDirty(srcPath, null);
    }

    public boolean markSourceDirty(String srcPath, @Nullable Predicate<String> skipOutputDeletion) {
        synchronized (myLock) {
            if (!myDirtyPaths.add(srcPath)) {
                return false;
            }
            Stores stores = stores();
            if (stores == null) {
                return true;
            }
            try {
                stores.myStamps.remove(srcPath);
                moveOutputsToDeleteQueue(stores, srcPath, skipOutputDeletion);
            }
            catch (IOException e) {
                handleStoreError(e);
            }
            return true;
        }
    }

    public void removeSource(String srcPath, @Nullable Predicate<String> skipOutputDeletion) {
        synchronized (myLock) {
            myDirtyPaths.remove(srcPath);
            Stores stores = stores();
            if (stores == null) {
                return;
            }
            try {
                stores.myStamps.remove(srcPath);
                moveOutputsToDeleteQueue(stores, srcPath, skipOutputDeletion);
            }
            catch (IOException e) {
                handleStoreError(e);
            }
        }
    }

    private static void moveOutputsToDeleteQueue(
        Stores stores,
        String srcPath,
        @Nullable Predicate<String> skipOutputDeletion
    ) throws IOException {
        Set<String> outputs = stores.mySrcToOut.get(srcPath);
        if (outputs == null) {
            return;
        }
        for (String outPath : outputs) {
            if (skipOutputDeletion != null && skipOutputDeletion.test(outPath)) {
                stores.myOutputsToDelete.remove(outPath);
                continue;
            }
            OutputSourceInfo info = stores.myOutToSrc.get(outPath);
            String className = info != null ? info.className() : null;
            stores.myOutputsToDelete.put(outPath, new OutputSourceInfo(srcPath, className));
        }
        stores.mySrcToOut.remove(srcPath);
    }

    public void setCompiled(String srcPath, long stamp, Map<String, @Nullable String> outputPathToClassName) {
        synchronized (myLock) {
            myDirtyPaths.remove(srcPath);
            Stores stores = stores();
            if (stores == null) {
                return;
            }
            try {
                stores.myStamps.put(srcPath, stamp);
                if (outputPathToClassName.isEmpty()) {
                    stores.mySrcToOut.remove(srcPath);
                }
                else {
                    stores.mySrcToOut.put(srcPath, new LinkedHashSet<>(outputPathToClassName.keySet()));
                    for (Map.Entry<String, @Nullable String> entry : outputPathToClassName.entrySet()) {
                        stores.myOutToSrc.put(entry.getKey(), new OutputSourceInfo(srcPath, entry.getValue()));
                    }
                }
            }
            catch (IOException e) {
                handleStoreError(e);
            }
        }
    }

    public void unmarkDirty(String srcPath) {
        synchronized (myLock) {
            myDirtyPaths.remove(srcPath);
        }
    }

    public boolean isDirty(String srcPath) {
        synchronized (myLock) {
            return myDirtyPaths.contains(srcPath);
        }
    }

    public boolean hasDirtyPaths() {
        synchronized (myLock) {
            return !myDirtyPaths.isEmpty();
        }
    }

    public long getStamp(String srcPath) {
        synchronized (myLock) {
            Stores stores = stores();
            if (stores == null) {
                return UNKNOWN_STAMP;
            }
            try {
                Long stamp = stores.myStamps.get(srcPath);
                return stamp == null ? UNKNOWN_STAMP : stamp;
            }
            catch (IOException e) {
                handleStoreError(e);
                return UNKNOWN_STAMP;
            }
        }
    }

    public boolean isKnownSource(String srcPath) {
        synchronized (myLock) {
            Stores stores = stores();
            if (stores == null) {
                return false;
            }
            try {
                return stores.myStamps.get(srcPath) != null || stores.mySrcToOut.get(srcPath) != null;
            }
            catch (IOException e) {
                handleStoreError(e);
                return false;
            }
        }
    }

    public Set<String> getOutputs(String srcPath) {
        synchronized (myLock) {
            Stores stores = stores();
            if (stores == null) {
                return Set.of();
            }
            try {
                Set<String> outputs = stores.mySrcToOut.get(srcPath);
                return outputs == null ? Set.of() : outputs;
            }
            catch (IOException e) {
                handleStoreError(e);
                return Set.of();
            }
        }
    }

    public @Nullable OutputSourceInfo getOutputInfo(String outPath) {
        synchronized (myLock) {
            Stores stores = stores();
            if (stores == null) {
                return null;
            }
            try {
                return stores.myOutToSrc.get(outPath);
            }
            catch (IOException e) {
                handleStoreError(e);
                return null;
            }
        }
    }

    public void removeOutputInfo(String outPath) {
        synchronized (myLock) {
            Stores stores = stores();
            if (stores == null) {
                return;
            }
            try {
                stores.myOutToSrc.remove(outPath);
            }
            catch (IOException e) {
                handleStoreError(e);
            }
        }
    }

    public List<String> getClassNames(String srcPath) {
        synchronized (myLock) {
            Stores stores = stores();
            if (stores == null) {
                return List.of();
            }
            try {
                List<String> result = new ArrayList<>();
                Set<String> outputs = stores.mySrcToOut.get(srcPath);
                if (outputs != null) {
                    for (String outPath : outputs) {
                        OutputSourceInfo info = stores.myOutToSrc.get(outPath);
                        if (info != null && info.className() != null) {
                            result.add(info.className());
                        }
                    }
                }
                return result;
            }
            catch (IOException e) {
                handleStoreError(e);
                return List.of();
            }
        }
    }

    public void scheduleOutputDeletion(String outPath, String srcPath, @Nullable String className) {
        synchronized (myLock) {
            Stores stores = stores();
            if (stores == null) {
                return;
            }
            try {
                stores.myOutputsToDelete.put(outPath, new OutputSourceInfo(srcPath, className));
            }
            catch (IOException e) {
                handleStoreError(e);
            }
        }
    }

    public void unscheduleOutputDeletion(String outPath) {
        synchronized (myLock) {
            Stores stores = stores();
            if (stores == null) {
                return;
            }
            try {
                stores.myOutputsToDelete.remove(outPath);
            }
            catch (IOException e) {
                handleStoreError(e);
            }
        }
    }

    public List<Pair<String, OutputSourceInfo>> getOutputsToDelete() {
        synchronized (myLock) {
            Stores stores = stores();
            if (stores == null) {
                return List.of();
            }
            try {
                List<Pair<String, OutputSourceInfo>> result = new ArrayList<>();
                for (String outPath : stores.myOutputsToDelete.getAllKeys()) {
                    OutputSourceInfo info = stores.myOutputsToDelete.get(outPath);
                    if (info != null) {
                        result.add(Pair.create(outPath, info));
                    }
                }
                return result;
            }
            catch (IOException e) {
                handleStoreError(e);
                return List.of();
            }
        }
    }

    public boolean processSourcePaths(Predicate<? super String> processor) {
        synchronized (myLock) {
            Stores stores = stores();
            if (stores == null) {
                return true;
            }
            try {
                return stores.myStamps.processKeys(processor);
            }
            catch (IOException e) {
                handleStoreError(e);
                return true;
            }
        }
    }

    public void clearDirty() {
        synchronized (myLock) {
            myDirtyPaths.clear();
        }
    }

    public void force() {
        synchronized (myLock) {
            if (myStores != null) {
                myStores.force();
            }
        }
    }

    public void wipe() {
        synchronized (myLock) {
            myDirtyPaths.clear();
            Stores stores = stores();
            if (stores == null) {
                return;
            }
            try {
                stores.myStamps.wipe();
                stores.mySrcToOut.wipe();
                stores.myOutToSrc.wipe();
                stores.myOutputsToDelete.wipe();
            }
            catch (IOException e) {
                handleStoreError(e);
            }
        }
    }

    public void reset() {
        synchronized (myLock) {
            myDirtyPaths.clear();
            if (myStores != null) {
                myStores.close();
                myStores = null;
            }
            myBroken = false;
        }
    }

    @Override
    public void dispose() {
        synchronized (myLock) {
            if (myStores != null) {
                myStores.close();
                myStores = null;
            }
        }
    }

    private @Nullable Stores stores() {
        if (myBroken) {
            return null;
        }
        if (myStores == null) {
            myStores = openStores();
            if (myStores == null) {
                myBroken = true;
            }
        }
        return myStores;
    }

    private @Nullable Stores openStores() {
        processGlobalInvalidationMarker();

        File dir = new File(CompilerPaths.getCacheStoreDirectory(myProject), "translation");
        File versionFile = new File(dir, "version");
        try {
            if (readVersion(versionFile) != VERSION) {
                FileUtil.delete(dir);
            }
            FileUtil.createDirectory(dir);
            Stores stores = new Stores(dir);
            Files.writeString(versionFile.toPath(), String.valueOf(VERSION), StandardCharsets.UTF_8);
            return stores;
        }
        catch (IOException e) {
            LOG.error("Failed to open compiler state stores, forcing rebuild", e);
            FileUtil.delete(dir);
            requestRebuild();
            return null;
        }
    }

    private static int readVersion(File versionFile) {
        try {
            return Integer.parseInt(Files.readString(versionFile.toPath(), StandardCharsets.UTF_8).trim());
        }
        catch (IOException | NumberFormatException ignored) {
            return -1;
        }
    }

    private void handleStoreError(IOException e) {
        LOG.error("Compiler state store failure, forcing rebuild", e);
        if (myStores != null) {
            myStores.close();
            myStores = null;
        }
        myBroken = true;
        requestRebuild();
    }

    private void requestRebuild() {
        FileUtil.createIfDoesntExist(CompilerPaths.getRebuildMarkerFile(myProject));
    }
}
