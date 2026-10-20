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
import consulo.language.ignore.Syntax;
import consulo.language.ignore.cache.PatternCache;
import consulo.language.ignore.localize.LanguageIgnoreLocalize;
import consulo.language.ignore.psi.IgnoreEntry;
import consulo.language.ignore.psi.IgnoreVisitor;
import consulo.language.psi.PsiElementVisitor;
import consulo.localize.LocalizeValue;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Inspection tool that checks if entry has correct form in specific according to the specific {@link Syntax}.
 */
@ExtensionImpl
public class IgnoreIncorrectEntryInspection extends IgnoreInspection {
    @Override
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.ERROR;
    }

    @Override
    public LocalizeValue getDisplayName() {
        return LanguageIgnoreLocalize.codeinspectionIncorrectentry();
    }

    @Override
    public LocalizeValue getDescription() {
        return LanguageIgnoreLocalize.codeinspectionIncorrectentryDescription();
    }

    /**
     * Checks if entry has correct form in specific according to the specific {@link Syntax}.
     *
     * @param holder     where visitor will register problems found.
     * @param isOnTheFly true if inspection was run in non-batch mode
     * @return not-null visitor for this inspection
     */
    @Override
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        PatternCache patternCache = PatternCache.getInstance(holder.getProject());
        return new IgnoreVisitor() {
            @Override
            @RequiredReadAction
            public void visitEntry(IgnoreEntry entry) {
                String regex = entry.getText();
                if (Syntax.GLOB.equals(entry.getSyntax())) {
                    regex = patternCache.getRegex(regex, Syntax.GLOB, false);
                }

                try {
                    Pattern.compile(regex);
                }
                catch (PatternSyntaxException e) {
                    holder.newProblem(LanguageIgnoreLocalize.codeinspectionIncorrectentryMessage(e.getDescription()))
                        .range(entry)
                        .create();
                }
            }
        };
    }
}
