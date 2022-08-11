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

package consulo.ide.impl.idea.ide;

import consulo.annotation.component.ExtensionImpl;
import consulo.dataContext.DataContext;
import consulo.ide.IdeView;
import consulo.ide.impl.idea.ide.dnd.FileCopyPasteUtil;
import consulo.ide.impl.idea.ide.dnd.LinuxDragAndDropSupport;
import consulo.language.editor.refactoring.copy.CopyFilesOrDirectoriesHandler;
import consulo.language.editor.refactoring.move.fileOrDirectory.MoveFilesOrDirectoriesHandler;
import consulo.language.psi.*;
import consulo.project.Project;
import consulo.ui.ex.PasteProvider;
import consulo.ui.ex.awt.CopyPasteManager;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;

import javax.annotation.Nonnull;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author yole
 */
@ExtensionImpl
public class FileListPasteProvider implements PasteProvider {
  @Override
  public void performPaste(@Nonnull DataContext dataContext) {
    final Project project = dataContext.getData(Project.KEY);
    final IdeView ideView = dataContext.getData(IdeView.KEY);
    if (project == null || ideView == null) return;

    if (!FileCopyPasteUtil.isFileListFlavorAvailable()) return;

    final Transferable contents = CopyPasteManager.getInstance().getContents();
    if (contents == null) return;
    final List<File> fileList = FileCopyPasteUtil.getFileList(contents);
    if (fileList == null) return;

    final List<PsiElement> elements = new ArrayList<PsiElement>();
    for (File file : fileList) {
      final VirtualFile vFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);
      if (vFile != null) {
        final PsiManager instance = PsiManager.getInstance(project);
        PsiFileSystemItem item = vFile.isDirectory() ? instance.findDirectory(vFile) : instance.findFile(vFile);
        if (item != null) {
          elements.add(item);
        }
      }
    }

    if (elements.size() > 0) {
      final PsiDirectory dir = ideView.getOrChooseDirectory();
      if (dir != null) {
        final boolean move = LinuxDragAndDropSupport.isMoveOperation(contents);
        if (move) {
          new MoveFilesOrDirectoriesHandler().doMove(PsiUtilCore.toPsiElementArray(elements), dir);
        }
        else {
          new CopyFilesOrDirectoriesHandler().doCopy(PsiUtilCore.toPsiElementArray(elements), dir);
        }
      }
    }
  }

  @Override
  public boolean isPastePossible(@Nonnull DataContext dataContext) {
    return true;
  }

  @Override
  public boolean isPasteEnabled(@Nonnull DataContext dataContext) {
    return dataContext.getData(IdeView.KEY) != null &&
           FileCopyPasteUtil.isFileListFlavorAvailable();
  }
}
