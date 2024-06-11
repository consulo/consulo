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
package consulo.ide.impl.idea.refactoring.rename.inplace;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.impl.internal.template.TemplateManagerImpl;
import consulo.language.editor.impl.internal.template.TemplateStateImpl;
import consulo.annotation.access.RequiredReadAction;
import consulo.codeEditor.Editor;
import consulo.language.Language;
import consulo.language.editor.completion.CompletionContributor;
import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.refactoring.rename.inplace.InplaceRefactoring;

import jakarta.annotation.Nonnull;

/**
 * User: anna
 * Date: 11/22/11
 */
@ExtensionImpl(order = "first")
public class CompletionContributorForInplaceRename extends CompletionContributor {

  @RequiredReadAction
  @Override
  public void fillCompletionVariants(@Nonnull CompletionParameters parameters, @Nonnull CompletionResultSet result) {
    final Editor editor = parameters.getEditor();
    final TemplateStateImpl state = TemplateManagerImpl.getTemplateStateImpl(editor);
    if (state != null) {
      if (editor.getUserData(InplaceRefactoring.INPLACE_RENAMER) != null && parameters.getInvocationCount() == 0) {
        result.stopHere();
      }
    }
  }

  @Nonnull
  @Override
  public Language getLanguage() {
    return Language.ANY;
  }
}
