/*
 * Copyright 2013-2026 consulo.io
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
package consulo.language.spellchecker.editor.impl.inspection;

import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.language.spellchecker.editor.localize.SpellCheckerLocalize;
import consulo.localize.LocalizeValue;
import consulo.project.Project;

/**
 * Replaces the range of a grammar or style problem with a suggestion.
 *
 * @author VISTALL
 * @since 2026-11-01
 */
class ReplaceTextFix implements LocalQuickFix {
    private final TextRange myFileRange;
    private final String mySuggestion;

    ReplaceTextFix(TextRange fileRange, String suggestion) {
        myFileRange = fileRange;
        mySuggestion = suggestion;
    }

    @Override
    public LocalizeValue getName() {
        return SpellCheckerLocalize.replaceWith(mySuggestion);
    }

    @Override
    public void applyFix(Project project, ProblemDescriptor descriptor) {
        PsiFile file = descriptor.getPsiElement().getContainingFile();
        Document document = file == null ? null : PsiDocumentManager.getInstance(project).getDocument(file);
        if (document == null || myFileRange.getEndOffset() > document.getTextLength()) {
            return;
        }
        document.replaceString(myFileRange.getStartOffset(), myFileRange.getEndOffset(), mySuggestion);
        PsiDocumentManager.getInstance(project).commitDocument(document);
    }
}
