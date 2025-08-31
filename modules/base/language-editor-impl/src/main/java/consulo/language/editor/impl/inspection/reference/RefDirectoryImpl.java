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
import consulo.application.ReadAction;
import consulo.language.editor.inspection.reference.*;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiElement;
import consulo.language.util.ModuleUtilCore;
import consulo.module.content.ProjectFileIndex;
import consulo.util.lang.ObjectUtil;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class RefDirectoryImpl extends RefElementImpl implements RefDirectory {
  private volatile RefModule myRefModule; // it's guaranteed that getModule() used after initialize()

  protected RefDirectoryImpl(PsiDirectory psiElement, RefManager refManager) {
    super(psiElement.getName(), psiElement, refManager);
  }

  @Override
  public void initialize() {
    PsiDirectory psiElement = ObjectUtil.tryCast(getPsiElement(), PsiDirectory.class);
    LOG.assertTrue(psiElement != null);
    PsiDirectory parentDirectory = psiElement.getParentDirectory();
    if (parentDirectory != null && ProjectFileIndex.getInstance(psiElement.getProject()).isInSourceContent(parentDirectory.getVirtualFile())) {
      WritableRefElement refElement = (WritableRefElement)getRefManager().getReference(parentDirectory);
      if (refElement != null) {
        refElement.add(this);
        return;
      }
    }
    myRefModule = getRefManager().getRefModule(ModuleUtilCore.findModuleForPsiElement(psiElement));
    if (myRefModule != null) {
      ((WritableRefEntity)myRefModule).add(this);
      return;
    }
    ((WritableRefEntity)myManager.getRefProject()).add(this);
  }

  @Override
  public void accept(@Nonnull RefVisitor visitor) {
    ApplicationManager.getApplication().runReadAction(() -> visitor.visitDirectory(this));
  }

  @Override
  public boolean isValid() {
    if (isDeleted()) return false;
    return ReadAction.compute(() -> {
      if (getRefManager().getProject().isDisposed()) return false;

      VirtualFile directory = getVirtualFile();
      return directory != null && directory.isValid();
    });
  }

  @Nullable
  @Override
  public RefModule getModule() {
    return myRefModule != null ? myRefModule : super.getModule();
  }


  @Nonnull
  @Override
  public String getQualifiedName() {
    return getName(); //todo relative name
  }

  @Override
  public String getExternalName() {
    PsiElement element = getPsiElement();
    assert element != null;
    return ((PsiDirectory)element).getVirtualFile().getPath();
  }
}
