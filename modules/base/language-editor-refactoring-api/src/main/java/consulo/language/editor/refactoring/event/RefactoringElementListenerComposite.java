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

package consulo.language.editor.refactoring.event;

import consulo.language.psi.PsiElement;
import consulo.util.collection.Lists;

import jakarta.annotation.Nonnull;
import java.util.List;

public class RefactoringElementListenerComposite implements RefactoringElementListener, UndoRefactoringElementListener {
  private final List<RefactoringElementListener> myListeners = Lists.newLockFreeCopyOnWriteList();

  public void addListener(RefactoringElementListener listener){
    myListeners.add(listener);
  }

  @Override
  public void elementMoved(@Nonnull PsiElement newElement){
    for (RefactoringElementListener myListener : myListeners) {
      myListener.elementMoved(newElement);
    }
  }

  @Override
  public void elementRenamed(@Nonnull PsiElement newElement){
    for (RefactoringElementListener myListener : myListeners) {
      myListener.elementRenamed(newElement);
    }
  }

  @Override
  public void undoElementMovedOrRenamed(@Nonnull PsiElement newElement, @Nonnull String oldQualifiedName) {
    for (RefactoringElementListener listener : myListeners) {
      if (listener instanceof UndoRefactoringElementListener) {
        ((UndoRefactoringElementListener)listener).undoElementMovedOrRenamed(newElement, oldQualifiedName);
      }
    }
  }
}
