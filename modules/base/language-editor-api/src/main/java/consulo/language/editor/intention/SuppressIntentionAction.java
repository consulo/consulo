/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

import consulo.codeEditor.CaretModel;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.component.util.Iconable;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.util.IncorrectOperationException;
import consulo.ui.image.Image;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author anna
 * @since 2007-12-24
 */
public abstract class SuppressIntentionAction implements Iconable, SyntheticIntentionAction {
  private String myText = "";
  public static SuppressIntentionAction[] EMPTY_ARRAY = new SuppressIntentionAction[0];

  @Override
  public Image getIcon(int flags) {
    return null;
  }

  @Nonnull
  @Override
  public String getText() {
    return myText;
  }

  protected void setText(@Nonnull String text) {
    myText = text;
  }

  @Override
  public boolean startInWriteAction() {
    return true;
  }

  @Override
  public String toString() {
    return getText();
  }

  @Override
  public final void invoke(@Nonnull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    if (!file.getManager().isInProject(file)) return;
    PsiElement element = getElement(editor, file);
    if (element != null) {
      invoke(project, editor, element);
    }
  }

  /**
   * Invokes intention action for the element under caret.
   *
   * @param project the project in which the file is opened.
   * @param editor  the editor for the file.
   * @param element the element under cursor.
   * @throws IncorrectOperationException
   *
   */
  public abstract void invoke(@Nonnull Project project, Editor editor, @Nonnull PsiElement element) throws IncorrectOperationException;

  @Override
  public final boolean isAvailable(@Nonnull Project project, Editor editor, PsiFile file) {
    if (file == null) return false;
    PsiManager manager = file.getManager();
    if (manager == null) return false;
    if (!manager.isInProject(file)) return false;
    PsiElement element = getElement(editor, file);
    return element != null && isAvailable(project, editor, element);
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

  @Nullable
  private static PsiElement getElement(@Nonnull Editor editor, @Nonnull PsiFile file) {
    CaretModel caretModel = editor.getCaretModel();
    int position = caretModel.getOffset();
    return file.findElementAt(position);
  }
}
