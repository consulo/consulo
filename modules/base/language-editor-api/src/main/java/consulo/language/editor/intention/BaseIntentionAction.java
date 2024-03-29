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

package consulo.language.editor.intention;

import consulo.language.scratch.ScratchFileService;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiUtilCore;
import org.jetbrains.annotations.Nls;

import jakarta.annotation.Nonnull;

/**
 * @author Mike
 */
public abstract class BaseIntentionAction implements IntentionAction {
  private String myText = "";

  @Override
  @Nonnull
  public String getText() {
    return myText;
  }

  protected void setText(@Nonnull @Nls(capitalization = Nls.Capitalization.Sentence) String text) {
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

  /**
   * @return true, if element belongs to project content root or is located in scratch files
   */
  public static boolean canModify(PsiElement element) {
    return element.getManager().isInProject(element) || ScratchFileService.isInScratchRoot(PsiUtilCore.getVirtualFile(element));
  }
}
