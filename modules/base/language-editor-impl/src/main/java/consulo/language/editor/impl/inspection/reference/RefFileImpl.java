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
package consulo.language.editor.impl.inspection.reference;

import consulo.application.ApplicationManager;
import consulo.language.editor.inspection.reference.RefElement;
import consulo.language.editor.inspection.reference.RefFile;
import consulo.language.editor.inspection.reference.RefManager;
import consulo.language.editor.inspection.reference.RefVisitor;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.project.macro.ProjectPathMacroManager;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class RefFileImpl extends RefElementImpl implements RefFile {
  public RefFileImpl(PsiFile elem, RefManager manager) {
    super(elem, manager);
  }

  @Override
  public PsiFile getPsiElement() {
    return (PsiFile)super.getPsiElement();
  }

  @Override
  public void accept(@Nonnull RefVisitor visitor) {
    ApplicationManager.getApplication().runReadAction(() -> visitor.visitFile(this));
  }

  @Override
  public String getExternalName() {
    PsiFile psiFile = getPsiElement();
    VirtualFile virtualFile = psiFile != null ? psiFile.getVirtualFile() : null;
    return virtualFile != null ? virtualFile.getUrl() : getName();
  }

  @Override
  public void initialize() {
    VirtualFile vFile = getVirtualFile();
    if (vFile == null) return;
    VirtualFile parentDirectory = vFile.getParent();
    if (parentDirectory == null) return;
    PsiDirectory psiDirectory = getRefManager().getPsiManager().findDirectory(parentDirectory);
    if (psiDirectory != null) {
      RefElement element = getRefManager().getReference(psiDirectory);
      if (element != null) {
        ((RefElementImpl)element).add(this);
      }
    }
  }

  @Nullable
  static RefElement fileFromExternalName(RefManager manager, String fqName) {
    VirtualFile virtualFile = VirtualFileManager.getInstance().findFileByUrl(ProjectPathMacroManager.getInstance(manager.getProject()).expandPath(fqName));
    if (virtualFile != null) {
      PsiFile psiFile = PsiManager.getInstance(manager.getProject()).findFile(virtualFile);
      if (psiFile != null) {
        return manager.getReference(psiFile);
      }
    }
    return null;
  }
}
