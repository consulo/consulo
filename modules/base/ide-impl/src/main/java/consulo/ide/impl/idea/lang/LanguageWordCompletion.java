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

/*
 * @author max
 */
package consulo.ide.impl.idea.lang;

import consulo.language.OldLanguageExtension;
import consulo.language.ast.ASTNode;
import consulo.language.psi.PsiElement;
import consulo.language.ast.IElementType;
import consulo.container.plugin.PluginIds;
import consulo.language.version.LanguageVersionUtil;

import javax.annotation.Nonnull;

public class LanguageWordCompletion extends OldLanguageExtension<WordCompletionElementFilter> {
  public static final LanguageWordCompletion INSTANCE = new LanguageWordCompletion();

  private LanguageWordCompletion() {
    super(PluginIds.CONSULO_BASE + ".codeInsight.wordCompletionFilter", new DefaultWordCompletionFilter());
  }

  public boolean isEnabledIn(@Nonnull ASTNode astNode) {
    final PsiElement psi = astNode.getPsi();
    if (psi == null) {
      return false;
    }
    IElementType elementType = astNode.getElementType();
    return forLanguage(psi.getLanguage()).isWordCompletionEnabledIn(elementType, LanguageVersionUtil.findLanguageVersion(elementType.getLanguage(), psi));
  }
}