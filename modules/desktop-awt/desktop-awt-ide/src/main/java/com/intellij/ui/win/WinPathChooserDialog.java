/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.ui.win;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.impl.LaterInvocator;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.CommandProcessorEx;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDialog;
import com.intellij.openapi.fileChooser.PathChooserDialog;
import com.intellij.openapi.fileChooser.impl.FileChooserUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.UIBundle;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Consumer;
import com.intellij.util.ui.OwnerOptional;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.concurrent.AsyncResult;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * @author Denis Fokin
 */

public class WinPathChooserDialog implements PathChooserDialog, FileChooserDialog {
  private FileDialog myFileDialog;
  private final FileChooserDescriptor myFileChooserDescriptor;
  private final WeakReference<Component> myParent;
  private final Project myProject;
  private final String myTitle;
  private VirtualFile[] virtualFiles;

  public WinPathChooserDialog(FileChooserDescriptor descriptor, Component parent, Project project) {

    myFileChooserDescriptor = descriptor;
    myParent = new WeakReference<>(parent);
    myProject = project;
    myTitle = getChooserTitle(descriptor);

    Consumer<Dialog> dialogConsumer = owner -> myFileDialog = new FileDialog(owner, myTitle, FileDialog.LOAD);
    Consumer<Frame> frameConsumer = owner -> myFileDialog = new FileDialog(owner, myTitle, FileDialog.LOAD);

    OwnerOptional.fromComponent(parent).ifDialog(dialogConsumer).ifFrame(frameConsumer).ifNull(frameConsumer);
  }

  private static String getChooserTitle(final FileChooserDescriptor descriptor) {
    final String title = descriptor.getTitle();
    return title != null ? title : UIBundle.message("file.chooser.default.title");
  }

  @Nonnull
  private List<VirtualFile> getChosenFiles(final Stream<File> streamOfFiles) {
    final List<VirtualFile> virtualFiles = new ArrayList<>();

    streamOfFiles.forEach(file -> {
      final VirtualFile virtualFile = fileToVirtualFile(file);
      if (virtualFile != null && virtualFile.isValid()) {
        virtualFiles.add(virtualFile);
      }
    });
    return FileChooserUtil.getChosenFiles(myFileChooserDescriptor, virtualFiles);
  }

  private VirtualFile fileToVirtualFile(File file) {
    final LocalFileSystem localFileSystem = LocalFileSystem.getInstance();
    final String vfsPath = FileUtil.toSystemIndependentName(file.getAbsolutePath());
    return localFileSystem.refreshAndFindFileByPath(vfsPath);
  }

  @Override
  public void choose(@Nullable VirtualFile toSelect, @Nonnull Consumer<List<VirtualFile>> callback) {
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
      myFileDialog.setDirectory(directoryName);
      myFileDialog.setFile(fileName);
    }


    myFileDialog.setFilenameFilter((dir, name) -> {
      File file = new File(dir, name);
      return myFileChooserDescriptor.isFileSelectable(fileToVirtualFile(file));
    });

    myFileDialog.setMultipleMode(myFileChooserDescriptor.isChooseMultiple());

    final CommandProcessorEx commandProcessor = ApplicationManager.getApplication() != null ? (CommandProcessorEx)CommandProcessor.getInstance() : null;
    final boolean appStarted = commandProcessor != null;


    if (appStarted) {
      commandProcessor.enterModal();
      LaterInvocator.enterModal(myFileDialog);
    }

    Component parent = myParent.get();
    try {
      myFileDialog.setVisible(true);
    }
    finally {
      if (appStarted) {
        commandProcessor.leaveModal();
        LaterInvocator.leaveModal(myFileDialog);
        if (parent != null) parent.requestFocus();
      }
    }

    File[] files = myFileDialog.getFiles();
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
          Messages.showErrorDialog(myProject, e.getMessage(), myTitle);
        }
        else {
          Messages.showErrorDialog(parent, e.getMessage(), myTitle);
        }

        return;
      }

      if (!ArrayUtil.isEmpty(files)) {
        callback.consume(virtualFileList);
      }
      else if (callback instanceof FileChooser.FileChooserConsumer) {
        ((FileChooser.FileChooserConsumer)callback).cancelled();
      }
    }
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public AsyncResult<VirtualFile[]> chooseAsync(@Nullable VirtualFile toSelect) {
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
      myFileDialog.setDirectory(directoryName);
      myFileDialog.setFile(fileName);
    }


    myFileDialog.setFilenameFilter((dir, name) -> {
      File file = new File(dir, name);
      return myFileChooserDescriptor.isFileSelectable(fileToVirtualFile(file));
    });

    myFileDialog.setMultipleMode(myFileChooserDescriptor.isChooseMultiple());

    AsyncResult<VirtualFile[]> result = AsyncResult.undefined();
    SwingUtilities.invokeLater(() -> {
      final CommandProcessorEx commandProcessor = ApplicationManager.getApplication() != null ? (CommandProcessorEx)CommandProcessor.getInstance() : null;
      final boolean appStarted = commandProcessor != null;

      if (appStarted) {
        commandProcessor.enterModal();
        LaterInvocator.enterModal(myFileDialog);
      }

      Component parent = myParent.get();
      try {
        myFileDialog.setVisible(true);
      }
      finally {
        if (appStarted) {
          commandProcessor.leaveModal();
          LaterInvocator.leaveModal(myFileDialog);
          if (parent != null) parent.requestFocus();
        }
      }

      File[] files = myFileDialog.getFiles();
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
            Messages.showErrorDialog(myProject, e.getMessage(), myTitle);
          }
          else {
            Messages.showErrorDialog(parent, e.getMessage(), myTitle);
          }

          result.setRejected();
          return;
        }

        if (!ArrayUtil.isEmpty(files)) {
          result.setDone(VfsUtil.toVirtualFileArray(virtualFileList));
        }
        else {
          result.setRejected();
        }
      }
    });
    return result;
  }

  @Nonnull
  @RequiredUIAccess
  @Override
  public AsyncResult<VirtualFile[]> chooseAsync(@Nullable Project project, @Nonnull VirtualFile[] toSelectFiles) {
    VirtualFile toSelect = toSelectFiles.length > 0 ? toSelectFiles[0] : null;
    return chooseAsync(toSelect);
  }

  @Nonnull
  @Override
  public VirtualFile[] choose(@Nullable Project project, @Nonnull VirtualFile... toSelectFiles) {
    VirtualFile toSelect = toSelectFiles.length > 0 ? toSelectFiles[0] : null;
    choose(toSelect, files -> {
    });
    return virtualFiles;
  }
}
