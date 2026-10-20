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
import consulo.document.Document;
import consulo.language.editor.inspection.LocalQuickFixOnPsiElement;
import consulo.language.ignore.localize.LanguageIgnoreLocalize;
import consulo.language.ignore.psi.IgnoreEntry;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * QuickFix action that removes relative parts of the entry
 * {@link IgnoreRelativeEntryInspection}.
 */
public class IgnoreRelativeEntryFix extends LocalQuickFixOnPsiElement {
    private static final Logger LOG = Logger.getInstance(IgnoreRelativeEntryFix.class);

    /**
     * Builds a new instance of {@link IgnoreRelativeEntryFix}.
     *
     * @param entry an element that will be handled with QuickFix
     */
    public IgnoreRelativeEntryFix(IgnoreEntry entry) {
        super(entry);
    }

    /**
     * Gets QuickFix name.
     *
     * @return QuickFix action name
     */
    @Override
    public LocalizeValue getText() {
        return LanguageIgnoreLocalize.quickFixRelativeEntry();
    }

    /**
     * Handles QuickFix action invoked on {@link IgnoreEntry}.
     *
     * @param project      the {@link Project} containing the working file
     * @param psiFile      the {@link PsiFile} containing handled entry
     * @param startElement the {@link IgnoreEntry} that will be removed
     * @param endElement   the {@link PsiElement} which is ignored in invoked action
     */
    @Override
    @RequiredUIAccess
    @RequiredReadAction
    public void invoke(Project project, PsiFile psiFile, PsiElement startElement, PsiElement endElement) {
        if (!(startElement instanceof IgnoreEntry)) {
            return;
        }

        Document document = PsiDocumentManager.getInstance(project).getDocument(psiFile);
        if (document == null) {
            return;
        }

        int start = startElement.getStartOffsetInParent();
        String text = startElement.getText();
        String fixed = getFixedPath(text);
        document.replaceString(start, start + text.length(), fixed);
    }

    /**
     * Removes relative parts from the given path.
     *
     * @param path element
     * @return fixed path
     */
    private static String getFixedPath(String path) {
        String fixed = path.replaceAll("\\/", "/").replaceAll("\\\\\\.", ".");
        try {
            String normalized = new URI(fixed).normalize().getPath();
            if (normalized != null) {
                fixed = normalized;
            }
        }
        catch (URISyntaxException e) {
            LOG.error(e);
        }
        return fixed.replaceAll("/\\.{1,2}/", "/").replaceAll("^\\.{0,2}/", "");
    }
}
