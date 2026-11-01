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
package consulo.language.spellchecker.editor.impl.quickfix;

import consulo.language.spellchecker.editor.SpellCheckerManager;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.refactoring.rename.PreferrableNameSuggestionProvider;
import consulo.language.editor.refactoring.rename.SuggestedNameInfo;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiNamedElement;

import java.util.Set;

@ExtensionImpl(id = "DictionarySuggestionProvider")
public class DictionarySuggestionProvider extends PreferrableNameSuggestionProvider {
    private boolean active;

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public boolean shouldCheckOthers() {
        return !active;
    }

    @Override
    @RequiredReadAction
    public SuggestedNameInfo getSuggestedNames(PsiElement element, PsiElement nameSuggestionContext, Set<String> result) {
        assert result != null;
        if (!active || nameSuggestionContext == null) {
            return null;
        }
        String text = nameSuggestionContext.getText();
        if (nameSuggestionContext instanceof PsiNamedElement namedElem) {
            text = namedElem.getName();
        }
        if (text == null) {
            return null;
        }

        SpellCheckerManager manager = SpellCheckerManager.getInstance(element.getProject());

        result.addAll(manager.getSuggestions(text));
        return SuggestedNameInfo.NULL_INFO;
    }
}
