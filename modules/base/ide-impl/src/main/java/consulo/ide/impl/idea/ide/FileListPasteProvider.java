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
import consulo.language.editor.util.IdeView;
import consulo.language.editor.refactoring.copy.CopyFilesOrDirectoriesHandler;
import consulo.language.editor.refactoring.move.fileOrDirectory.MoveFilesOrDirectoriesHandler;
import consulo.language.psi.*;
import consulo.project.Project;
import consulo.language.editor.FilePasteProvider;
import consulo.ui.UIAccess;
import org.jspecify.annotations.Nullable;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.clipboard.ClipboardFeature;
import consulo.ui.clipboard.DataTransferType;
import consulo.ui.ex.CopyPasteManager;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author yole
 */
@ExtensionImpl(id = "fileList")
public class FileListPasteProvider implements FilePasteProvider {
  @Override
  @RequiredUIAccess
  public void performPaste(DataContext dataContext) {
    Project project = dataContext.getData(Project.KEY);
    IdeView ideView = dataContext.getData(IdeView.KEY);
    if (project == null || ideView == null) return;

    UIAccess uiAccess = UIAccess.current();
    CopyPasteManager.getInstance()
      .getContentsAsync(DataTransferType.FILE_LIST)
      .whenCompleteAsync((fileList, throwable) -> {
        if (throwable == null) {
          pasteFiles(project, ideView, fileList);
        }
      }, uiAccess);
  }

  @RequiredUIAccess
  private void pasteFiles(Project project, IdeView ideView, @Nullable List<File> fileList) {
    if (fileList == null) return;

    List<PsiElement> elements = new ArrayList<>();
    for (File file : fileList) {
      VirtualFile vFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);
      if (vFile != null) {
        PsiManager instance = PsiManager.getInstance(project);
        PsiFileSystemItem item = vFile.isDirectory() ? instance.findDirectory(vFile) : instance.findFile(vFile);
        if (item != null) {
          elements.add(item);
        }
      }
    }

    if (elements.size() > 0) {
      PsiDirectory dir = ideView.getOrChooseDirectory();
      if (dir != null) {
        PsiCopyPasteManagerImpl.MyData psiData = CopyPasteManager.getInstance().getLocalContents().get(PsiCopyPasteManagerImpl.PSI_DATA);
        boolean move = psiData != null && !psiData.isCopied();
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
  public boolean isPastePossible(DataContext dataContext) {
    return true;
  }

  @Override
  public boolean isPasteEnabled(DataContext dataContext) {
    return dataContext.hasData(IdeView.KEY)
      && (CopyPasteManager.getInstance().getLocalContents().contains(DataTransferType.FILE_LIST)
          || UIAccess.current().getClipboard().isSupported(ClipboardFeature.UNRESTRICTED_READ));
  }
}
