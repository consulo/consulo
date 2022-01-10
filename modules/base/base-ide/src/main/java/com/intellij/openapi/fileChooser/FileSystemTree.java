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
package com.intellij.openapi.fileChooser;

import consulo.disposer.Disposable;
import consulo.util.dataholder.Key;
import com.intellij.openapi.vfs.VirtualFile;
import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import javax.swing.*;
import java.util.List;

public interface FileSystemTree extends Disposable {
  Key<FileSystemTree> DATA_KEY = Key.create("FileSystemTree");

  JTree getTree();

  void updateTree();

  @Nullable
  VirtualFile getSelectedFile();

  @Nonnull
  VirtualFile[] getSelectedFiles();

  @Nullable
  VirtualFile getNewFileParent();

  @Nullable
  <T> T getData(Key<T> key);

  void select(VirtualFile file, @Nullable Runnable onDone);
  void select(VirtualFile[] files, @Nullable Runnable onDone);

  void expand(VirtualFile file, @Nullable Runnable onDone);

  void addListener(Listener listener, Disposable parent);

  boolean isUnderRoots(@Nonnull VirtualFile file);

  boolean selectionExists();

  boolean areHiddensShown();

  void showHiddens(boolean showHidden);

  interface Listener {
    void selectionChanged(@Nonnull List<? extends VirtualFile> selection);
  }
}
