/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.language.editor.refactoring.unwrap;

import consulo.codeEditor.Editor;
import consulo.codeEditor.SelectionModel;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.util.lang.Pair;

import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Konstantin Bulenkov
 */
public abstract class UnwrapDescriptorBase implements UnwrapDescriptor {
  private Unwrapper[] myUnwrappers;

  public final Unwrapper[] getUnwrappers() {
    if (myUnwrappers == null) {
      myUnwrappers = createUnwrappers();
    }

    return myUnwrappers;
  }

  @Override
  public List<Pair<PsiElement, Unwrapper>> collectUnwrappers(Project project, Editor editor, PsiFile file) {
    PsiElement e = findTargetElement(editor, file);

     List<Pair<PsiElement, Unwrapper>> result = new ArrayList<Pair<PsiElement, Unwrapper>>();
     Set<PsiElement> ignored = new HashSet<PsiElement>();
     while (e != null) {
       for (Unwrapper u : getUnwrappers()) {
         if (u.isApplicableTo(e) && !ignored.contains(e)) {
           result.add(new Pair<>(e, u));
           u.collectElementsToIgnore(e, ignored);
         }
       }
       e = e.getParent();
     }

     return result;
  }

  protected abstract Unwrapper[] createUnwrappers();

  @Nullable
  protected PsiElement findTargetElement(Editor editor, PsiFile file) {
    int offset = editor.getCaretModel().getOffset();
    PsiElement endElement = file.findElementAt(offset);
    SelectionModel selectionModel = editor.getSelectionModel();
    if (selectionModel.hasSelection() && selectionModel.getSelectionStart() < offset) {
      PsiElement startElement = file.findElementAt(selectionModel.getSelectionStart());
      if (startElement != null && startElement != endElement && startElement.getTextRange().getEndOffset() == offset) {
        return startElement;
      }
    }
    return endElement;
  }

  @Override
  public boolean showOptionsDialog() {
    return true;
  }

  @Override
  public boolean shouldTryToRestoreCaretPosition() {
    return true;
  }
}
