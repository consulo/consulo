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

package consulo.language.ignore.impl.internal.psi;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.document.util.TextRange;
import consulo.language.ignore.IgnoreFileType;
import consulo.language.ignore.IgnoreLanguage;
import consulo.language.ignore.psi.IgnoreEntry;
import consulo.language.ignore.psi.IgnoreNegation;
import consulo.language.psi.AbstractElementManipulator;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiFileFactory;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.util.IncorrectOperationException;

@ExtensionImpl
public final class IgnoreEntryManipulator extends AbstractElementManipulator<IgnoreEntry> {
    /**
     * Changes the element's text to a new value
     *
     * @param entry      element to be changed
     * @param range      range within the element
     * @param newContent new element text
     * @return changed element
     * @throws IncorrectOperationException if something goes wrong
     */
    @Override
    @RequiredWriteAction
    public IgnoreEntry handleContentChange(IgnoreEntry entry, TextRange range, String newContent)
        throws IncorrectOperationException {
        if (!(entry.getLanguage() instanceof IgnoreLanguage language)) {
            return entry;
        }
        IgnoreFileType fileType = (IgnoreFileType) language.getAssociatedFileType();
        assert fileType != null;
        PsiFile file = PsiFileFactory.getInstance(entry.getProject())
            .createFileFromText(language.getFilename(), fileType, range.replace(entry.getText(), newContent));
        IgnoreEntry newEntry = PsiTreeUtil.findChildOfType(file, IgnoreEntry.class);
        assert newEntry != null;
        return (IgnoreEntry) entry.replace(newEntry);
    }

    /**
     * Returns range of the entry. Skips negation element.
     *
     * @param element element to be changed
     * @return range
     */
    @Override
    @RequiredReadAction
    public TextRange getRangeInElement(IgnoreEntry element) {
        IgnoreNegation negation = element.getNegation();
        if (negation != null) {
            return TextRange.create(
                negation.getStartOffsetInParent() + negation.getTextLength(),
                element.getTextLength()
            );
        }
        return super.getRangeInElement(element);
    }

    @Override
    public Class<IgnoreEntry> getElementClass() {
        return IgnoreEntry.class;
    }
}
