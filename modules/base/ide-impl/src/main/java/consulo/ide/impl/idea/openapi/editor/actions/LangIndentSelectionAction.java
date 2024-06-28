/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.editor.actions;

import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.ide.impl.idea.codeInsight.completion.NextPrevParameterAction;
import consulo.language.editor.completion.lookup.LookupManager;
import consulo.language.psi.PsiFile;

/**
 * @author peter
 */
public class LangIndentSelectionAction extends IndentSelectionAction {

  @Override
  protected boolean isEnabled(Editor editor, DataContext dataContext) {
    if (!originalIsEnabled(editor, wantSelection())) return false;
    if (LookupManager.getActiveLookup(editor) != null) return false;

    PsiFile psiFile = dataContext.getData(PsiFile.KEY);
    return psiFile == null || !NextPrevParameterAction.hasSutablePolicy(editor, psiFile);
  }

  protected boolean wantSelection() {
    return true;
  }
}
