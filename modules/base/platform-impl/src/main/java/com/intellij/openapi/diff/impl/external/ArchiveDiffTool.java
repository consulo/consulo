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
package com.intellij.openapi.diff.impl.external;

import com.intellij.ide.diff.ArchiveFileDiffElement;
import com.intellij.ide.diff.DirDiffSettings;
import consulo.fileTypes.ArchiveFileType;
import consulo.disposer.Disposable;
import com.intellij.openapi.diff.*;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.IdeFocusManager;
import javax.annotation.Nonnull;

import java.awt.*;

/**
 * @author Konstantin Bulenkov
 */
public class ArchiveDiffTool implements DiffTool {
  public static final ArchiveDiffTool INSTANCE = new ArchiveDiffTool();

  private ArchiveDiffTool(){}

  @Override
  public void show(DiffRequest request) {
    final DiffContent[] contents = request.getContents();
    final VirtualFile file1 = contents[0].getFile();
    final VirtualFile file2 = contents[1].getFile();
    assert file1 != null && file2 != null;
    final ArchiveFileDiffElement element = new ArchiveFileDiffElement(file1);
    final ArchiveFileDiffElement element1 = new ArchiveFileDiffElement(file2);
    final DirDiffSettings settings = new DirDiffSettings();
    settings.showInFrame = false;
    settings.enableChoosers = false;
    DirDiffManager.getInstance(request.getProject()).showDiff(element, element1, settings);
  }

  @Override
  public boolean canShow(DiffRequest request) {
    final DiffContent[] contents = request.getContents();
    final DialogWrapper instance = DialogWrapper.findInstance(IdeFocusManager.getInstance(request.getProject()).getFocusOwner());
    if (instance != null && instance.isModal()) return false;
    if (contents.length == 2) {
      final VirtualFile file1 = contents[0].getFile();
      final VirtualFile file2 = contents[1].getFile();
      if (file1 != null && file2 != null) {
        final FileType type1 = contents[0].getContentType();
        final FileType type2 = contents[1].getContentType();
        return type1 == type2 && type1 instanceof ArchiveFileType;
      }
    }
    return false;
  }

  @Override
  public DiffViewer createComponent(String title, DiffRequest request, Window window, @Nonnull Disposable parentDisposable) {
    return null;
  }
}
