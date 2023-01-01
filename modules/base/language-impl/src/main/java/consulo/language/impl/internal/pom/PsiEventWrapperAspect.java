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

package consulo.language.impl.internal.pom;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.language.ast.ASTNode;
import consulo.language.file.inject.VirtualFileWindow;
import consulo.language.file.light.LightVirtualFile;
import consulo.language.impl.internal.psi.PsiDocumentManagerBase;
import consulo.language.impl.internal.psi.PsiManagerImpl;
import consulo.language.impl.internal.psi.PsiToDocumentSynchronizer;
import consulo.language.impl.psi.DummyHolder;
import consulo.language.pom.PomModel;
import consulo.language.pom.PomModelAspect;
import consulo.language.pom.TreeAspect;
import consulo.language.pom.event.PomModelEvent;
import consulo.language.pom.event.TreeChangeEvent;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.Collections;

@Singleton
@ServiceAPI(value = ComponentScope.PROJECT, lazy = false)
@ServiceImpl
public class PsiEventWrapperAspect implements PomModelAspect {
  private final TreeAspect myTreeAspect;

  @Inject
  public PsiEventWrapperAspect(PomModel model, TreeAspect aspect) {
    myTreeAspect = aspect;
    model.registerAspect(PsiEventWrapperAspect.class, this, Collections.singleton(aspect));
  }

  @Override
  public void update(PomModelEvent event) {
    final TreeChangeEvent changeSet = (TreeChangeEvent)event.getChangeSet(myTreeAspect);
    if (changeSet == null) return;
    sendAfterEvents(changeSet);
  }

  private static void sendAfterEvents(TreeChangeEvent changeSet) {
    ASTNode rootElement = changeSet.getRootElement();
    PsiFile file = (PsiFile)rootElement.getPsi();
    if (!file.isPhysical()) {
      promoteNonPhysicalChangesToDocument(rootElement, file);
      ((PsiManagerImpl)file.getManager()).afterChange(false);
      return;
    }

    ((TreeChangeEventImpl)changeSet).fireEvents();
  }

  private static void promoteNonPhysicalChangesToDocument(ASTNode rootElement, PsiFile file) {
    if (file instanceof DummyHolder) return;
    if (((PsiDocumentManagerBase)PsiDocumentManager.getInstance(file.getProject())).isCommitInProgress()) return;

    VirtualFile vFile = file.getViewProvider().getVirtualFile();
    if (vFile instanceof LightVirtualFile && !(vFile instanceof VirtualFileWindow)) {
      Document document = FileDocumentManager.getInstance().getCachedDocument(vFile);
      if (document != null) {
        CharSequence text = rootElement.getChars();
        PsiToDocumentSynchronizer.performAtomically(file, () -> document.replaceString(0, document.getTextLength(), text));
      }
    }
  }
}
