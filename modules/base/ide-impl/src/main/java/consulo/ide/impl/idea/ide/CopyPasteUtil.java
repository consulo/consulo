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

import consulo.ui.ex.awt.tree.AbstractTreeUpdater;
import consulo.disposer.Disposable;
import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.ui.ex.awt.CopyPasteManager;
import consulo.language.psi.PsiElement;
import javax.annotation.Nonnull;

import java.awt.datatransfer.Transferable;
import java.util.function.Consumer;

/**
 * @author max
 */
public class CopyPasteUtil {
  private CopyPasteUtil() { }

  public static PsiElement[] getElementsInTransferable(Transferable t) {
    final PsiElement[] elts = PsiCopyPasteManager.getElements(t);
    return elts != null ? elts : PsiElement.EMPTY_ARRAY;
  }

  public static void addDefaultListener(@Nonnull Disposable parent, @Nonnull Consumer<? super PsiElement> consumer) {
    CopyPasteManager.getInstance().addContentChangedListener(new DefaultCopyPasteListener(consumer), parent);
  }

  public static class DefaultCopyPasteListener implements CopyPasteManager.ContentChangedListener {
    private final Consumer<? super PsiElement> consumer;

    @Deprecated
    public DefaultCopyPasteListener(AbstractTreeUpdater updater) {
      this(element -> updater.addSubtreeToUpdateByElement(element));
    }

    private DefaultCopyPasteListener(@Nonnull Consumer<? super PsiElement> consumer) {
      this.consumer = consumer;
    }

    @Override
    public void contentChanged(final Transferable oldTransferable, final Transferable newTransferable) {
      Application application = ApplicationManager.getApplication();
      if (application == null || application.isReadAccessAllowed()) {
        updateByTransferable(oldTransferable);
        updateByTransferable(newTransferable);
      }
      else {
        application.runReadAction(() -> {
          updateByTransferable(oldTransferable);
          updateByTransferable(newTransferable);
        });
      }
    }

    private void updateByTransferable(final Transferable t) {
      PsiElement[] psiElements = getElementsInTransferable(t);
      for (PsiElement psiElement : psiElements) {
        if (!psiElement.getProject().isDisposed()) {
          consumer.accept(psiElement);
        }
      }
    }
  }
}
