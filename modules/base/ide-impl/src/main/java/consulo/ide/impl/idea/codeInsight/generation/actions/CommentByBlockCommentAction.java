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

package consulo.ide.impl.idea.codeInsight.generation.actions;

import consulo.application.dumb.DumbAware;
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.ide.impl.idea.codeInsight.actions.MultiCaretCodeInsightAction;
import consulo.ide.impl.idea.codeInsight.actions.MultiCaretCodeInsightActionHandler;
import consulo.ide.impl.idea.codeInsight.generation.CommentByBlockCommentHandler;
import consulo.ide.impl.idea.openapi.fileTypes.impl.AbstractFileType;
import consulo.language.Commenter;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.virtualFileSystem.fileType.FileType;

import javax.annotation.Nonnull;

public class CommentByBlockCommentAction extends MultiCaretCodeInsightAction implements DumbAware {
  public CommentByBlockCommentAction() {
    setEnabledInModalContext(true);
  }

  @Nonnull
  @Override
  protected MultiCaretCodeInsightActionHandler getHandler() {
    return new CommentByBlockCommentHandler();
  }

  @Override
  protected boolean isValidFor(@Nonnull Project project, @Nonnull Editor editor, @Nonnull Caret caret, @Nonnull final PsiFile file) {
    final FileType fileType = file.getFileType();
    if (fileType instanceof AbstractFileType) {
      return ((AbstractFileType)fileType).getCommenter() != null;
    }

    Commenter commenter = Commenter.forLanguage(file.getLanguage());
    if (commenter == null) commenter = Commenter.forLanguage(file.getViewProvider().getBaseLanguage());
    if (commenter == null) return false;
    return commenter.getBlockCommentPrefix() != null && commenter.getBlockCommentSuffix() != null;
  }
}