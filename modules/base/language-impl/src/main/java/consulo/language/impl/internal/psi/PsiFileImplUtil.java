/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package consulo.language.impl.internal.psi;

import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.language.util.IncorrectOperationException;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.fileType.FileTypeRegistry;
import consulo.virtualFileSystem.fileType.UnknownFileType;

import java.io.IOException;

public class PsiFileImplUtil {
  private PsiFileImplUtil() {
  }

  @RequiredUIAccess
  public static PsiFile setName(PsiFile file, String newName) throws IncorrectOperationException {
    VirtualFile vFile = file.getViewProvider().getVirtualFile();
    PsiManagerImpl manager = (PsiManagerImpl)file.getManager();

    try{
      FileType newFileType = FileTypeRegistry.getInstance().getFileTypeByFileName(newName);
      if (UnknownFileType.INSTANCE.equals(newFileType) || newFileType.isBinary()) {
        // before the file becomes unknown or a binary (thus, not openable in the editor), save it to prevent data loss
        FileDocumentManager fdm = FileDocumentManager.getInstance();
        Document doc = fdm.getCachedDocument(vFile);
        if (doc != null) {
          fdm.saveDocumentAsIs(doc);
        }
      }

      vFile.rename(manager, newName);
    }
    catch(IOException e){
      throw new IncorrectOperationException(e);
    }

    return file.getViewProvider().isPhysical() ? manager.findFile(vFile) : file;
  }

  @RequiredUIAccess
  public static void checkSetName(PsiFile file, String name) throws IncorrectOperationException {
    VirtualFile vFile = file.getVirtualFile();
    VirtualFile parentFile = vFile.getParent();
    if (parentFile == null) return;
    VirtualFile child = parentFile.findChild(name);
    if (child != null && !child.equals(vFile)){
      throw new IncorrectOperationException("File " + child.getPresentableUrl() + " already exists.");
    }
  }

  public static void doDelete(PsiFile file) throws IncorrectOperationException {
    PsiManagerImpl manager = (PsiManagerImpl)file.getManager();

    VirtualFile vFile = file.getVirtualFile();
    try{
      vFile.delete(manager);
    }
    catch(IOException e){
      throw new IncorrectOperationException(e);
    }
  }
}
