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

package consulo.ide.impl.idea.codeInsight.editorActions.wordSelection;

import consulo.annotation.component.ExtensionImpl;
import consulo.component.extension.ExtensionPointName;
import consulo.language.psi.PsiComment;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiWhiteSpace;

@ExtensionImpl
public class WordSelectioner extends AbstractWordSelectioner {
  private static final ExtensionPointName<WordSelectionerFilter> EP_NAME = ExtensionPointName.create(WordSelectionerFilter.class);

  @Override
  public boolean canSelect(PsiElement e) {
    if (e instanceof PsiComment || e instanceof PsiWhiteSpace) {
      return false;

    }
    for (WordSelectionerFilter filter : EP_NAME.getExtensionList()) {
      if (!filter.canSelect(e)) {
        return false;
      }
    }
    return true;
  }
}
