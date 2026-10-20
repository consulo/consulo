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
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.util.collection.MultiMap;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * Inspection tool that checks if entries are duplicated by others.
 */
@ExtensionImpl
public class IgnoreFileDuplicateEntryInspection extends IgnoreInspection {
    @Override
    public LocalizeValue getDisplayName() {
        return LanguageIgnoreLocalize.codeinspectionDuplicateentry();
    }

    @Override
    public LocalizeValue getDescription() {
        return LanguageIgnoreLocalize.codeinspectionDuplicateentryDescription();
    }

    @Override
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.WARNING;
    }

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
        return new PsiElementVisitor() {
            @Override
            @RequiredReadAction
            public void visitFile(PsiFile file) {
                if (file instanceof IgnoreFile) {
                    checkFile(holder, file);
                }
            }
        };
    }

    /**
     * Reports problems at file level. Checks if entries are duplicated by other entries.
     *
     * @param problemsHolder where problems are registered
     * @param file           current working file to check
     */
    @RequiredReadAction
    private static void checkFile(ProblemsHolder problemsHolder, PsiFile file) {
        MultiMap<String, IgnoreEntry> entries = MultiMap.create();

        file.acceptChildren(new IgnoreVisitor() {
            @Override
            @RequiredReadAction
            public void visitEntry(IgnoreEntry entry) {
                entries.putValue(entry.getText(), entry);
                super.visitEntry(entry);
            }
        });

        for (Map.Entry<String, Collection<IgnoreEntry>> stringCollectionEntry : entries.entrySet()) {
            Iterator<IgnoreEntry> iterator = stringCollectionEntry.getValue().iterator();
            iterator.next();
            while (iterator.hasNext()) {
                IgnoreEntry entry = iterator.next();
                problemsHolder.newProblem(LanguageIgnoreLocalize.codeinspectionDuplicateentryMessage())
                    .range(entry)
                    .withFixes(new IgnoreFileRemoveEntryFix(entry))
                    .create();
            }
        }
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
