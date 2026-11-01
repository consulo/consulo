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

import consulo.codeEditor.Editor;
import consulo.component.util.Iconable;
import consulo.document.util.TextRange;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.editor.completion.lookup.LookupManager;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.util.PsiEditorUtil;
import consulo.language.psi.PsiElement;
import consulo.language.psi.SmartPointerManager;
import consulo.language.psi.SmartPsiElementPointer;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.language.spellchecker.editor.localize.SpellCheckerLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.lang.StringUtil;

import java.util.ArrayList;
import java.util.List;

public class ChangeTo extends ShowSuggestions implements LocalQuickFix, Iconable {
    private final TextRange myRange;
    private final SmartPsiElementPointer<PsiElement> myElementPointer;

    public ChangeTo(String wordWithTypo, PsiElement element, TextRange range) {
        super(wordWithTypo);
        myRange = range;
        myElementPointer = SmartPointerManager.createPointer(element);
    }

    @Override
    public LocalizeValue getName() {
        return SpellCheckerLocalize.changeTo();
    }

    @Override
    @RequiredUIAccess
    public void applyFix(Project project, ProblemDescriptor descriptor) {
        PsiElement element = myElementPointer.getElement();
        if (element == null) {
            return;
        }
        Editor editor = PsiEditorUtil.findEditor(element);

        if (editor == null) {
            return;
        }

        TextRange textRange = myRange;
        editor.getSelectionModel().setSelection(textRange.getStartOffset(), textRange.getEndOffset());

        String word = editor.getSelectionModel().getSelectedText();

        if (word == null || StringUtil.isEmpty(word)) {
            return;
        }

        List<LookupElement> lookupItems = new ArrayList<>();
        for (String variant : getSuggestions(project)) {
            lookupItems.add(LookupElementBuilder.create(variant));
        }
        LookupElement[] items = new LookupElement[lookupItems.size()];
        items = lookupItems.toArray(items);
        LookupManager lookupManager = LookupManager.getInstance(project);
        lookupManager.showLookup(editor, items);
    }
}