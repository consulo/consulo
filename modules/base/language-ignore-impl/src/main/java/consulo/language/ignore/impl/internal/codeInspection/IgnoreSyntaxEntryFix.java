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
import consulo.codeEditor.Editor;
import consulo.language.editor.AutoPopupController;
import consulo.language.editor.completion.CompletionType;
import consulo.language.editor.inspection.LocalQuickFixAndIntentionActionOnPsiElement;
import consulo.language.ignore.localize.LanguageIgnoreLocalize;
import consulo.language.ignore.psi.IgnoreSyntax;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

/**
 * QuickFix action that invokes {@link consulo.language.ignore.impl.internal.codeInsight.SyntaxCompletionContributor}
 * on the given {@link IgnoreSyntax} element.
 */
public class IgnoreSyntaxEntryFix extends LocalQuickFixAndIntentionActionOnPsiElement {
    /**
     * Builds a new instance of {@link IgnoreSyntaxEntryFix}.
     *
     * @param syntax an element that will be handled with QuickFix
     */
    public IgnoreSyntaxEntryFix(IgnoreSyntax syntax) {
        super(syntax);
    }

    /**
     * Gets QuickFix name.
     *
     * @return QuickFix action name
     */
    @Override
    public LocalizeValue getText() {
        return LanguageIgnoreLocalize.quickFixSyntaxEntry();
    }

    /**
     * Handles QuickFix action invoked on {@link IgnoreSyntax}.
     *
     * @param project      the {@link Project} containing the working file
     * @param file         the {@link PsiFile} containing handled entry
     * @param editor       is null when called from inspection
     * @param startElement the {@link IgnoreSyntax} that will be selected and replaced
     * @param endElement   the {@link PsiElement} which is ignored in invoked action
     */
    @Override
    @RequiredUIAccess
    @RequiredReadAction
    public void invoke(
        Project project,
        PsiFile file,
        @Nullable Editor editor,
        PsiElement startElement,
        PsiElement endElement
    ) {
        if (startElement instanceof IgnoreSyntax ignoreSyntax && editor != null) {
            PsiElement value = ignoreSyntax.getValue();
            editor.getSelectionModel().setSelection(value.getTextOffset(), value.getTextOffset() + value.getTextLength());
            AutoPopupController.getInstance(project).scheduleAutoPopup(editor, CompletionType.BASIC, null);
        }
    }

    /**
     * Run in read action because of completion invoking.
     *
     * @return <code>false</code>
     */
    @Override
    public boolean startInWriteAction() {
        return false;
    }
}
