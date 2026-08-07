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

package consulo.ide.impl.idea.ide;

import consulo.application.Application;
import consulo.disposer.Disposable;
import consulo.language.psi.PsiElement;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.clipboard.DataTransfer;
import consulo.ui.ex.CopyPasteManager;

import java.util.function.Consumer;

/**
 * @author max
 */
public class CopyPasteUtil {
  private CopyPasteUtil() { }

  public static PsiElement[] getElementsInTransfer(DataTransfer transfer) {
    PsiElement[] elements = PsiCopyPasteManagerImpl.getElements(transfer);
    return elements != null ? elements : PsiElement.EMPTY_ARRAY;
  }

  @RequiredUIAccess
  public static void addDefaultListener(Disposable parent, Consumer<? super PsiElement> consumer) {
    CopyPasteManager.getInstance().addContentListener((oldTransfer, newTransfer) -> {
      Application application = Application.get();
      if (application.isReadAccessAllowed()) {
        updateByTransfer(oldTransfer, consumer);
        updateByTransfer(newTransfer, consumer);
      }
      else {
        application.runReadAction(() -> {
          updateByTransfer(oldTransfer, consumer);
          updateByTransfer(newTransfer, consumer);
        });
      }
    }, parent);
  }

  private static void updateByTransfer(DataTransfer transfer, Consumer<? super PsiElement> consumer) {
    for (PsiElement psiElement : getElementsInTransfer(transfer)) {
      if (!psiElement.getProject().isDisposed()) {
        consumer.accept(psiElement);
      }
    }
  }
}
