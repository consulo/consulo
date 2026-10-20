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

package consulo.language.ignore.impl.internal.reference;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.ignore.IgnoreLanguage;
import consulo.language.ignore.impl.internal.codeInsight.FileExtensionCompletionContributor;
import consulo.language.ignore.psi.IgnoreEntry;
import consulo.language.ignore.psi.IgnoreFile;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFileSystemItem;
import consulo.language.psi.PsiReference;
import consulo.language.psi.PsiReferenceContributor;
import consulo.language.psi.PsiReferenceProvider;
import consulo.language.psi.PsiReferenceRegistrar;
import consulo.language.util.ProcessingContext;

import static consulo.language.pattern.PlatformPatterns.psiElement;
import static consulo.language.pattern.PlatformPatterns.psiFile;

/**
 * PSI elements references contributor.
 */
@ExtensionImpl
public final class IgnoreReferenceContributor extends PsiReferenceContributor {
    /**
     * Registers new references provider for PSI element.
     *
     * @param psiReferenceRegistrar reference provider
     */
    @Override
    public void registerReferenceProviders(PsiReferenceRegistrar psiReferenceRegistrar) {
        psiReferenceRegistrar.registerReferenceProvider(
            psiElement().inFile(psiFile(IgnoreFile.class)),
            new IgnoreReferenceProvider()
        );
    }

    @Override
    public Language getLanguage() {
        return IgnoreLanguage.INSTANCE;
    }

    private static class IgnoreReferenceProvider extends PsiReferenceProvider {
        @Override
        public boolean acceptsTarget(PsiElement target) {
            return target instanceof PsiFileSystemItem;
        }

        /**
         * Returns references for given {@link PsiElement}.
         *
         * @param psiElement        current element
         * @param processingContext context
         * @return {@link PsiReference} list
         */
        @Override
        public PsiReference[] getReferencesByElement(PsiElement psiElement, ProcessingContext processingContext) {
            String text = psiElement.getText();
            if (psiElement instanceof IgnoreEntry entry && !shouldSkipCompletion(text)) {
                return new IgnoreReferenceSet(entry).getAllReferences();
            }
            return PsiReference.EMPTY_ARRAY;
        }

        private static boolean shouldSkipCompletion(String text) {
            return FileExtensionCompletionContributor.fileExtensionCompletionSupported(text);
        }
    }
}
