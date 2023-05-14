/*
 * Copyright 2013-2023 consulo.io
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
package consulo.language.editor.documentation;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.codeEditor.Editor;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.ui.ex.popup.JBPopup;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 15/01/2023
 */
@ServiceAPI(ComponentScope.PROJECT)
public interface DocumentationManager {
  static DocumentationManager getInstance(@Nonnull Project project) {
    return project.getInstance(DocumentationManager.class);
  }

  default void showJavaDocInfo(@Nonnull PsiElement element, PsiElement original) {
    showJavaDocInfo(element, original, null);
  }

  default void showJavaDocInfo(@Nonnull PsiElement element, PsiElement original, @Nullable Runnable closeCallback) {
    showJavaDocInfo(element, original, false, closeCallback);
  }

  default void showJavaDocInfo(@Nonnull PsiElement element, PsiElement original, boolean requestFocus, @Nullable Runnable closeCallback) {
    showJavaDocInfo(element, original, requestFocus, closeCallback, null, true);
  }

  void showJavaDocInfo(@Nonnull PsiElement element, PsiElement original, boolean requestFocus, @Nullable Runnable closeCallback, @Nullable String documentation, boolean useStoredPopupSize);

  /**
   * Asks to show quick doc for the target element.
   *
   * @param editor             editor with an element for which quick do should be shown
   * @param element            target element which documentation should be shown
   * @param original           element that was used as a quick doc anchor. Example: consider a code like {@code Runnable task;}.
   *                           A user wants to see javadoc for the {@code Runnable}, so, original element is a class name from the variable
   *                           declaration but {@code 'element'} argument is a {@code Runnable} descriptor
   * @param closeCallback      callback to be notified on target hint close (if any)
   * @param documentation      precalculated documentation
   * @param closeOnSneeze      flag that defines whether quick doc control should be as non-obtrusive as possible. E.g. there are at least
   *                           two possible situations - the quick doc is shown automatically on mouse over element; the quick doc is shown
   *                           on explicit action call (Ctrl+Q). We want to close the doc on, say, editor viewport position change
   *                           at the first situation but don't want to do that at the second
   * @param useStoredPopupSize whether popup size previously set by user (via mouse-dragging) should be used, or default one should be used
   */
  void showJavaDocInfo(@Nonnull Editor editor,
                       @Nonnull PsiElement element,
                       @Nonnull PsiElement original,
                       @Nullable Runnable closeCallback,
                       @Nullable String documentation,
                       boolean closeOnSneeze,
                       boolean useStoredPopupSize);

  void showJavaDocInfoAtToolWindow(@Nonnull PsiElement element, @Nonnull PsiElement original);

  default void showJavaDocInfo(Editor editor, @Nullable PsiFile file, boolean requestFocus) {
    showJavaDocInfo(editor, file, requestFocus, null);
  }

  void showJavaDocInfo(Editor editor, @Nullable PsiFile file, boolean requestFocus, @Nullable Runnable closeCallback);

  @Nullable
  PsiElement getElementFromLookup(Editor editor, @Nullable PsiFile file);

  @Nullable
  JBPopup getDocInfoHint();

  boolean hasActiveDockedDocWindow();

  void setAllowContentUpdateFromContext(boolean allow);

  void updateToolwindowContext();

  /**
   * @return {@code true} if quick doc control is configured to not prevent user-IDE interaction (e.g. should be closed if
   * the user presses a key);
   * {@code false} otherwise
   */
  boolean isCloseOnSneeze();

  @Nonnull
  Project getProject(@Nullable PsiElement element);

  Editor getEditor();

  @Nullable
  default PsiElement findTargetElement(@Nonnull Editor editor, @Nullable PsiFile file, PsiElement contextElement) {
    return findTargetElement(editor, editor.getCaretModel().getOffset(), file, contextElement);
  }

  @Nullable
  PsiElement findTargetElement(Editor editor, int offset, @Nullable PsiFile file, PsiElement contextElement);

  String generateDocumentation(@Nonnull PsiElement element, @Nullable PsiElement originalElement, boolean onHover);

  void createToolWindow(PsiElement element, PsiElement originalElement);
}
