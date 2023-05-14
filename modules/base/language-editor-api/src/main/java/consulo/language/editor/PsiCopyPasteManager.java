/*
 * Copyright 2013-2022 consulo.io
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
package consulo.language.editor;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiDirectoryContainer;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFileSystemItem;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 24-Aug-22
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface PsiCopyPasteManager {
  public static PsiCopyPasteManager getInstance() {
    return Application.get().getInstance(PsiCopyPasteManager.class);
  }

  @Nullable
  public PsiElement[] getElements(boolean[] isCopied);

  public void setElements(PsiElement[] elements, boolean copied);

  public void clear();

  public boolean isCutElement(Object element);

  @Nullable
  public static List<File> asFileList(final PsiElement[] elements) {
    final List<File> result = new ArrayList<>();
    for (PsiElement element : elements) {
      final PsiFileSystemItem psiFile;
      if (element instanceof PsiFileSystemItem) {
        psiFile = (PsiFileSystemItem)element;
      }
      else if (element instanceof PsiDirectoryContainer) {
        final PsiDirectory[] directories = ((PsiDirectoryContainer)element).getDirectories();
        psiFile = directories[0];
      }
      else {
        psiFile = element.getContainingFile();
      }
      if (psiFile != null) {
        VirtualFile vFile = psiFile.getVirtualFile();
        if (vFile != null && vFile.getFileSystem() instanceof LocalFileSystem) {
          result.add(new File(vFile.getPath()));
        }
      }
    }
    return result.isEmpty() ? null : result;
  }
}
