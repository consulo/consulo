/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.application.dumb.DumbAware;
import consulo.codeEditor.Editor;
import consulo.component.util.Iconable;
import consulo.language.editor.inspection.FileModifier;
import consulo.language.psi.PsiFile;
import consulo.language.util.IncorrectOperationException;
import consulo.project.Project;
import org.jetbrains.annotations.Nls;

import javax.annotation.Nonnull;

/**
 * Interface for intention actions. Intention actions are invoked by pressing
 * Alt-Enter in the code editor at the location where an intention is available,
 * and can be enabled or disabled in the "Intentions" settings dialog.
 * <p/>
 * Implement {@link Iconable Iconable} interface to
 * change icon in intention popup menu.
 * <p/>
 * Implement {@link HighPriorityAction HighPriorityAction} or
 * {@link LowPriorityAction LowPriorityAction} to change ordering.
 * <p/>
 * Can be {@link DumbAware}.
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface IntentionAction extends FileModifier {
  IntentionAction[] EMPTY_ARRAY = new IntentionAction[0];

  /**
   * Returns text to be shown in the list of available actions, if this action
   * is available.
   *
   * @return the text to show in the intention popup.
   * @see #isAvailable(Project, Editor, PsiFile)
   */
  @Nls(capitalization = Nls.Capitalization.Sentence)
  @Nonnull
  String getText();

  /**
   * Checks whether this intention is available at a caret offset in file.
   * If this method returns true, a light bulb for this intention is shown.
   *
   * @param project the project in which the availability is checked.
   * @param editor  the editor in which the intention will be invoked.
   * @param file    the file open in the editor.
   * @return true if the intention is available, false otherwise.
   */
  default boolean isAvailable(@Nonnull Project project, Editor editor, PsiFile file) {
    return true;
  }

  /**
   * Called when user invokes intention. This method is called inside command.
   * If {@link #startInWriteAction()} returns true, this method is also called
   * inside write action.
   *
   * @param project the project in which the intention is invoked.
   * @param editor  the editor in which the intention is invoked.
   * @param file    the file open in the editor.
   */
  void invoke(@Nonnull Project project, Editor editor, PsiFile file) throws IncorrectOperationException;

  /**
   * Indicate whether this action should be invoked inside write action.
   * Should return false if e.g. modal dialog is shown inside the action.
   * If false is returned the action itself is responsible for starting write action
   * when needed, by calling {@link Application#runWriteAction(Runnable)}.
   *
   * @return true if the intention requires a write action, false otherwise.
   */
  @Override
  default boolean startInWriteAction() {
    return false;
  }
}
