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

package consulo.ide.impl.idea.codeInsight.unwrap;

import consulo.annotation.access.RequiredReadAction;
import consulo.codeEditor.Editor;
import consulo.language.editor.impl.action.BaseCodeInsightAction;
import consulo.language.editor.action.CodeInsightActionHandler;
import consulo.language.editor.refactoring.unwrap.UnwrapDescriptor;
import consulo.language.psi.PsiFile;
import consulo.project.Project;

import javax.annotation.Nonnull;

public class UnwrapAction extends BaseCodeInsightAction{
  public UnwrapAction() {
    super(true);
    setEnabledInModalContext(true);
  }

  @Nonnull
  @Override
  protected CodeInsightActionHandler getHandler(){
    return new UnwrapHandler();
  }

  @Override
  @RequiredReadAction
  protected boolean isValidForFile(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file) {
    return !UnwrapDescriptor.forLanguage(file.getLanguage()).isEmpty();
  }
}