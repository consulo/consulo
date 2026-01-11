/*
 * Copyright 2013-2025 consulo.io
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
package consulo.desktop.awt.fileChooser.impl.system;

import com.formdev.flatlaf.util.SystemFileChooser;
import consulo.application.ApplicationManager;
import consulo.application.impl.internal.LaterInvocator;
import consulo.component.ComponentManager;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.FileChooserDialog;
import consulo.fileChooser.IdeaFileChooser;
import consulo.fileChooser.PathChooserDialog;
import consulo.ide.impl.idea.openapi.fileChooser.impl.FileChooserUtil;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.localize.UILocalize;
import consulo.undoRedo.CommandProcessor;
import consulo.undoRedo.internal.CommandProcessorEx;
import consulo.util.collection.ArrayUtil;
import consulo.util.concurrent.AsyncResult;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * @author VISTALL
 * @since 21/12/2025
 */
public class NativeFileChooseDialog implements PathChooserDialog, FileChooserDialog {
    private final FileChooserDescriptor myFileChooserDescriptor;
    private final WeakReference<Component> myParent;
    private final Project myProject;
    private final LocalizeValue myTitle;
    private VirtualFile[] virtualFiles;

    public NativeFileChooseDialog(FileChooserDescriptor descriptor, Component parent, Project project) {

        myFileChooserDescriptor = descriptor;
        myParent = new WeakReference<>(parent);
        myProject = project;
        myTitle = getChooserTitle(descriptor);
    }

    private static LocalizeValue getChooserTitle(@Nonnull FileChooserDescriptor descriptor) {
        return descriptor.getTitleValue().orIfEmpty(UILocalize.fileChooserDefaultTitle());
    }

    @Nonnull
    private java.util.List<VirtualFile> getChosenFiles(Stream<File> streamOfFiles) {
        java.util.List<VirtualFile> virtualFiles = new ArrayList<>();

        streamOfFiles.forEach(file -> {
            VirtualFile virtualFile = fileToVirtualFile(file);
            if (virtualFile != null && virtualFile.isValid()) {
                virtualFiles.add(virtualFile);
            }
        });
        return FileChooserUtil.getChosenFiles(myFileChooserDescriptor, virtualFiles);
    }

    private VirtualFile fileToVirtualFile(File file) {
        LocalFileSystem localFileSystem = LocalFileSystem.getInstance();
        String vfsPath = FileUtil.toSystemIndependentName(file.getAbsolutePath());
        return localFileSystem.refreshAndFindFileByPath(vfsPath);
    }

    @Override
    @RequiredUIAccess
    public void choose(@Nullable VirtualFile toSelect, @Nonnull Consumer<java.util.List<VirtualFile>> callback) {
        SystemFileChooser fileChooser = new SystemFileChooser();
        if (toSelect != null && toSelect.getParent() != null) {
            String directoryName;
            String fileName = null;
            if (toSelect.isDirectory()) {
                directoryName = toSelect.getCanonicalPath();
            }
            else {
                directoryName = toSelect.getParent().getCanonicalPath();
                fileName = toSelect.getPath();
            }

            if (directoryName != null) {
                fileChooser.setCurrentDirectory(new File(directoryName));
            }

            if (fileName != null) {
                fileChooser.setSelectedFile(new File(fileName));
            }
        }

        if (myFileChooserDescriptor.isChooseFolders()) {
            fileChooser.setFileSelectionMode(SystemFileChooser.DIRECTORIES_ONLY);
        } else if (myFileChooserDescriptor.isChooseFiles()) {
            fileChooser.setFileSelectionMode(SystemFileChooser.FILES_ONLY);
        }
// TODO
//        fileChooser.setFilenameFilter((dir, name) -> {
//            File file = new File(dir, name);
//            return myFileChooserDescriptor.isFileSelectable(fileToVirtualFile(file));
//        });

        fileChooser.setMultiSelectionEnabled(myFileChooserDescriptor.isChooseMultiple());

        CommandProcessorEx commandProcessor =
            ApplicationManager.getApplication() != null ? (CommandProcessorEx) CommandProcessor.getInstance() : null;
        boolean appStarted = commandProcessor != null;

        if (appStarted) {
            commandProcessor.enterModal();
            LaterInvocator.enterModal(fileChooser);
        }

        Component parent = myParent.get();
        try {
            fileChooser.showOpenDialog(parent);
        }
        finally {
            if (appStarted) {
                commandProcessor.leaveModal();
                LaterInvocator.leaveModal(fileChooser);
                if (parent != null) {
                    parent.requestFocus();
                }
            }
        }

        File[] files = fileChooser.getSelectedFiles();
        List<VirtualFile> virtualFileList = getChosenFiles(Stream.of(files));
        virtualFiles = virtualFileList.toArray(VirtualFile.EMPTY_ARRAY);

        if (!virtualFileList.isEmpty()) {
            try {
                if (virtualFileList.size() == 1) {
                    myFileChooserDescriptor.isFileSelectable(virtualFileList.get(0));
                }
                myFileChooserDescriptor.validateSelectedFiles(virtualFiles);
            }
            catch (Exception e) {
                if (parent == null) {
                    Messages.showErrorDialog(myProject, e.getMessage(), myTitle.get());
                }
                else {
                    Messages.showErrorDialog(parent, e.getMessage(), myTitle.get());
                }

                return;
            }

            if (!ArrayUtil.isEmpty(files)) {
                callback.accept(virtualFileList);
            }
            else if (callback instanceof IdeaFileChooser.FileChooserConsumer fileChooserConsumer) {
                fileChooserConsumer.cancelled();
            }
        }
    }

    @Nonnull
    @Override
    @RequiredUIAccess
    public AsyncResult<VirtualFile[]> chooseAsync(@Nullable VirtualFile toSelect) {
        SystemFileChooser fileChooser = new SystemFileChooser();

        if (toSelect != null && toSelect.getParent() != null) {
            String directoryName;
            String fileName = null;
            if (toSelect.isDirectory()) {
                directoryName = toSelect.getCanonicalPath();
            }
            else {
                directoryName = toSelect.getParent().getCanonicalPath();
                fileName = toSelect.getPath();
            }

            if (directoryName != null) {
                fileChooser.setCurrentDirectory(new File(directoryName));
            }

            if (fileName != null) {
                fileChooser.setSelectedFile(new File(fileName));
            }
        }

        if (myFileChooserDescriptor.isChooseFolders()) {
            fileChooser.setFileSelectionMode(SystemFileChooser.DIRECTORIES_ONLY);
        }
        else if (myFileChooserDescriptor.isChooseFiles()) {
            fileChooser.setFileSelectionMode(SystemFileChooser.FILES_ONLY);
        }
// TODO
//        fileChooser.setFilenameFilter((dir, name) -> {
//            File file = new File(dir, name);
//            return myFileChooserDescriptor.isFileSelectable(fileToVirtualFile(file));
//        });

        fileChooser.setMultiSelectionEnabled(myFileChooserDescriptor.isChooseMultiple());

        AsyncResult<VirtualFile[]> result = AsyncResult.undefined();
        SwingUtilities.invokeLater(() -> {
            CommandProcessorEx commandProcessor =
                ApplicationManager.getApplication() != null ? (CommandProcessorEx) CommandProcessor.getInstance() : null;
            boolean appStarted = commandProcessor != null;

            if (appStarted) {
                commandProcessor.enterModal();
                LaterInvocator.enterModal(fileChooser);
            }

            Component parent = myParent.get();
            try {
                fileChooser.showOpenDialog(parent);
            }
            finally {
                if (appStarted) {
                    commandProcessor.leaveModal();
                    LaterInvocator.leaveModal(fileChooser);
                    if (parent != null) {
                        parent.requestFocus();
                    }
                }
            }

            File[] files = fileChooser.getSelectedFiles();
            List<VirtualFile> virtualFileList = getChosenFiles(Stream.of(files));
            virtualFiles = virtualFileList.toArray(VirtualFile.EMPTY_ARRAY);

            if (!virtualFileList.isEmpty()) {
                try {
                    if (virtualFileList.size() == 1) {
                        myFileChooserDescriptor.isFileSelectable(virtualFileList.get(0));
                    }
                    myFileChooserDescriptor.validateSelectedFiles(virtualFiles);
                }
                catch (Exception e) {
                    if (parent == null) {
                        Messages.showErrorDialog(myProject, e.getMessage(), myTitle.get());
                    }
                    else {
                        Messages.showErrorDialog(parent, e.getMessage(), myTitle.get());
                    }

                    result.setRejected();
                    return;
                }

                if (!ArrayUtil.isEmpty(files)) {
                    result.setDone(VirtualFileUtil.toVirtualFileArray(virtualFileList));
                }
                else {
                    result.setRejected();
                }
            }
        });
        return result;
    }

    @Nonnull
    @Override
    @RequiredUIAccess
    public AsyncResult<VirtualFile[]> chooseAsync(@Nullable ComponentManager project, @Nonnull VirtualFile[] toSelectFiles) {
        VirtualFile toSelect = toSelectFiles.length > 0 ? toSelectFiles[0] : null;
        return chooseAsync(toSelect);
    }

    @Nonnull
    @Override
    @RequiredUIAccess
    public VirtualFile[] choose(@Nullable ComponentManager project, @Nonnull VirtualFile... toSelectFiles) {
        VirtualFile toSelect = toSelectFiles.length > 0 ? toSelectFiles[0] : null;
        choose(toSelect, files -> {
        });
        return virtualFiles;
    }
}
