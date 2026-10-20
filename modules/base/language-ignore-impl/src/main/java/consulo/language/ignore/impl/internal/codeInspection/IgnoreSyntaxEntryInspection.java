/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2018 hsz Jakub Chrzanowski <jakub@hsz.mobi>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package consulo.language.ignore.impl.internal.codeInspection;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.ignore.IgnoreLanguage;
import consulo.language.ignore.Syntax;
import consulo.language.ignore.localize.LanguageIgnoreLocalize;
import consulo.language.ignore.psi.IgnoreSyntax;
import consulo.language.ignore.psi.IgnoreVisitor;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;

/**
 * Inspection tool that checks if syntax entry has correct value.
 */
@ExtensionImpl
public class IgnoreSyntaxEntryInspection extends IgnoreInspection {
    @Override
    public LocalizeValue getDisplayName() {
        return LanguageIgnoreLocalize.codeinspectionSyntaxentry();
    }

    @Override
    public LocalizeValue getDescription() {
        return LanguageIgnoreLocalize.codeinspectionSyntaxentryDescription();
    }

    @Override
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.ERROR;
    }

    /**
     * Checks if syntax entry has correct value.
     *
     * @param holder     where visitor will register problems found.
     * @param isOnTheFly true if inspection was run in non-batch mode
     * @return not-null visitor for this inspection
     */
    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        return new IgnoreVisitor() {
            @Override
            @RequiredReadAction
            public void visitSyntax(IgnoreSyntax syntax) {
                PsiFile file = syntax.getContainingFile();
                if (file == null || !(file.getLanguage() instanceof IgnoreLanguage language) || !language.isSyntaxSupported()) {
                    return;
                }

                String value = syntax.getValue().getText();
                for (Syntax s : Syntax.values()) {
                    if (s.toString().equals(value)) {
                        return;
                    }
                }

                holder.newProblem(LanguageIgnoreLocalize.codeinspectionSyntaxentryMessage())
                    .range(syntax)
                    .withFixes(new IgnoreSyntaxEntryFix(syntax))
                    .create();
            }
        };
    }
}
