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
import consulo.language.editor.inspection.LocalInspectionToolSession;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.ignore.localize.LanguageIgnoreLocalize;
import consulo.language.ignore.psi.IgnoreEntry;
import consulo.language.ignore.psi.IgnoreFile;
import consulo.language.ignore.psi.IgnoreVisitor;
import consulo.language.psi.PsiElementVisitor;
import consulo.localize.LocalizeValue;

/**
 * Inspection tool that checks if entry is relative.
 */
@ExtensionImpl
public class IgnoreRelativeEntryInspection extends IgnoreInspection {
    @Override
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.ERROR;
    }

    @Override
    public LocalizeValue getDisplayName() {
        return LanguageIgnoreLocalize.codeinspectionRelativeentry();
    }

    @Override
    public LocalizeValue getDescription() {
        return LanguageIgnoreLocalize.codeinspectionRelativeentryDescription();
    }

    /**
     * Checks if entries are relative.
     */
    @Override
    public PsiElementVisitor buildVisitor(
        ProblemsHolder holder,
        boolean isOnTheFly,
        LocalInspectionToolSession session,
        Object state
    ) {
        if (!(holder.getFile() instanceof IgnoreFile)) {
            return PsiElementVisitor.EMPTY_VISITOR;
        }

        return new IgnoreVisitor() {
            @Override
            @RequiredReadAction
            public void visitEntry(IgnoreEntry entry) {
                String path = entry.getText().replaceAll("\\\\(.)", "$1");
                if (path.contains("./")) {
                    holder.newProblem(LanguageIgnoreLocalize.codeinspectionRelativeentryMessage())
                        .range(entry)
                        .withFixes(new IgnoreRelativeEntryFix(entry))
                        .create();
                }
            }
        };
    }

    /**
     * Forces checking every entry in checked file.
     *
     * @return <code>true</code>
     */
    @Override
    public boolean runForWholeFile() {
        return true;
    }
}
