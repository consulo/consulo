/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package consulo.language.editor.intention;

import consulo.codeEditor.Editor;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.util.IncorrectOperationException;
import consulo.project.Project;

import jakarta.annotation.Nonnull;

/**
 * @author Danila Ponomarenko
 */
public abstract class BaseElementAtCaretIntentionAction extends BaseIntentionAction {
  private volatile boolean useElementToTheLeft = false;

  @Override
  public final boolean isAvailable(@Nonnull Project project, Editor editor, PsiFile file) {
    if (!file.getManager().isInProject(file)) return false;

    useElementToTheLeft = false;
    final PsiElement elementToTheRight = getElementToTheRight(editor, file);
    if (elementToTheRight == null) {
      return false;
    }

    if (isAvailable(project, editor, elementToTheRight)) {
      return true;
    }

    final PsiElement elementToTheLeft = getElementToTheLeft(editor, file);
    if (elementToTheLeft != null && isAvailable(project, editor, elementToTheLeft)) {
      useElementToTheLeft = true;
      return true;
    }

    return false;
  }

  /**
   * Checks whether this intention is available at a caret offset in file.
   * If this method returns true, a light bulb for this intention is shown.
   *
   * @param project the project in which the availability is checked.
   * @param editor  the editor in which the intention will be invoked.
   * @param element the element under caret.
   * @return true if the intention is available, false otherwise.
   */
  public abstract boolean isAvailable(@Nonnull Project project, Editor editor, @Nonnull PsiElement element);

  @Override
  public final void invoke(@Nonnull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    final PsiElement element = useElementToTheLeft ? getElementToTheLeft(editor, file) : getElementToTheRight(editor,file);
    if (element == null){
      return;
    }

    invoke(project, editor, element);
  }

  /**
   * Invokes intention action for the element under cursor.
   *
   * @param project the project in which the file is opened.
   * @param editor  the editor for the file.
   * @param element the element under cursor.
   * @throws IncorrectOperationException
   *
   */
  public abstract void invoke(@Nonnull Project project, Editor editor, @Nonnull PsiElement element) throws IncorrectOperationException;

  @jakarta.annotation.Nullable
  protected static PsiElement getElementToTheRight(Editor editor, @Nonnull PsiFile file) {
    return file.findElementAt(editor.getCaretModel().getOffset());
  }

  @jakarta.annotation.Nullable
  protected static PsiElement getElementToTheLeft(Editor editor, @Nonnull PsiFile file) {
    return file.findElementAt(editor.getCaretModel().getOffset() - 1);
  }


}