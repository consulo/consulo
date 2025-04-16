/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.vcs.readOnlyHandler;

import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.ide.impl.idea.openapi.vfs.VfsUtilCore;
import consulo.language.file.inject.VirtualFileWindow;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.MultiValuesMap;
import consulo.virtualFileSystem.ReadonlyStatusHandler;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.WritingAccessProvider;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.*;

@Singleton
@State(name = "ReadonlyStatusHandler", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
@ServiceImpl
public class ReadonlyStatusHandlerImpl extends ReadonlyStatusHandler implements PersistentStateComponent<ReadonlyStatusHandlerImpl.State> {
    @Nonnull
    private final Project myProject;
    private final List<WritingAccessProvider> myAccessProviders;

    public static class State {
        public boolean SHOW_DIALOG = true;
    }

    private State myState = new State();

    @Inject
    public ReadonlyStatusHandlerImpl(@Nonnull Project project) {
        myProject = project;
        myAccessProviders = myProject.isDefault() ? List.of() : WritingAccessProvider.getProvidersForProject(myProject);
    }

    @Override
    public State getState() {
        return myState;
    }

    @Override
    public void loadState(State state) {
        myState = state;
    }

    @Override
    @RequiredUIAccess
    public OperationStatus ensureFilesWritable(@Nonnull VirtualFile... files) {
        if (files.length == 0) {
            return new OperationStatusImpl(VirtualFile.EMPTY_ARRAY);
        }
        UIAccess.assertIsUIThread();

        Set<VirtualFile> realFiles = new HashSet<>(files.length);
        for (VirtualFile file : files) {
            if (file instanceof VirtualFileWindow virtualFileWindow) {
                file = virtualFileWindow.getDelegate();
            }
            if (file != null) {
                realFiles.add(file);
            }
        }
        files = VfsUtilCore.toVirtualFileArray(realFiles);

        for (WritingAccessProvider accessProvider : myAccessProviders) {
            Collection<VirtualFile> denied = ContainerUtil.filter(files, virtualFile -> !accessProvider.isPotentiallyWritable(virtualFile));

            if (denied.isEmpty()) {
                denied = accessProvider.requestWriting(files);
            }
            if (!denied.isEmpty()) {
                return new OperationStatusImpl(VfsUtilCore.toVirtualFileArray(denied));
            }
        }

        FileInfo[] fileInfos = createFileInfos(files);
        if (fileInfos.length == 0) { // if all files are already writable
            return createResultStatus(files);
        }

        if (myProject.getApplication().isUnitTestMode()) {
            return createResultStatus(files);
        }

        // This event count hack is necessary to allow actions that called this stuff could still get data from their data contexts.
        // Otherwise data manager stuff will fire up an assertion saying that event count has been changed (due to modal dialog show-up)
        // The hack itself is safe since we guarantee that focus will return to the same component had it before modal dialog have been shown.
        Runnable markEventCount = UIAccess.current().markEventCount();
        if (myState.SHOW_DIALOG) {
            new ReadOnlyStatusDialog(myProject, fileInfos).show();
        }
        else {
            processFiles(new ArrayList<>(Arrays.asList(fileInfos)), null); // the collection passed is modified
        }
        markEventCount.run();
        return createResultStatus(files);
    }

    private static OperationStatus createResultStatus(VirtualFile[] files) {
        List<VirtualFile> readOnlyFiles = new ArrayList<>();
        for (VirtualFile file : files) {
            if (file.exists()) {
                if (!file.isWritable()) {
                    readOnlyFiles.add(file);
                }
            }
        }

        return new OperationStatusImpl(VfsUtilCore.toVirtualFileArray(readOnlyFiles));
    }

    private FileInfo[] createFileInfos(VirtualFile[] files) {
        List<FileInfo> fileInfos = new ArrayList<>();
        for (VirtualFile file : files) {
            if (file != null && !file.isWritable() && file.isInLocalFileSystem()) {
                fileInfos.add(new FileInfo(file, myProject));
            }
        }
        return fileInfos.toArray(new FileInfo[fileInfos.size()]);
    }

    public static void processFiles(List<FileInfo> fileInfos, @Nullable String changelist) {
        FileInfo[] copy = fileInfos.toArray(new FileInfo[fileInfos.size()]);
        MultiValuesMap<HandleType, VirtualFile> handleTypeToFile = new MultiValuesMap<>();
        for (FileInfo fileInfo : copy) {
            handleTypeToFile.put(fileInfo.getSelectedHandleType(), fileInfo.getFile());
        }

        for (HandleType handleType : handleTypeToFile.keySet()) {
            handleType.processFiles(handleTypeToFile.get(handleType), changelist);
        }

        for (FileInfo fileInfo : copy) {
            if (!fileInfo.getFile().exists() || fileInfo.getFile().isWritable()) {
                fileInfos.remove(fileInfo);
            }
        }
    }

    private static class OperationStatusImpl extends OperationStatus {

        private final VirtualFile[] myReadonlyFiles;

        OperationStatusImpl(VirtualFile[] readonlyFiles) {
            myReadonlyFiles = readonlyFiles;
        }

        @Override
        @Nonnull
        public VirtualFile[] getReadonlyFiles() {
            return myReadonlyFiles;
        }

        @Override
        public boolean hasReadonlyFiles() {
            return myReadonlyFiles.length > 0;
        }

        @Override
        @Nonnull
        public String getReadonlyFilesMessage() {
            if (hasReadonlyFiles()) {
                StringBuilder buf = new StringBuilder();
                if (myReadonlyFiles.length > 1) {
                    for (VirtualFile file : myReadonlyFiles) {
                        buf.append('\n');
                        buf.append(file.getPresentableUrl());
                    }

                    return CommonLocalize.failedToMakeTheFollowingFilesWritableErrorMessage(buf.toString()).get();
                }
                else {
                    return CommonLocalize.failedToMakeFileWriteableErrorMessage(myReadonlyFiles[0].getPresentableUrl()).get();
                }
            }
            throw new RuntimeException("No readonly files");
        }
    }
}
