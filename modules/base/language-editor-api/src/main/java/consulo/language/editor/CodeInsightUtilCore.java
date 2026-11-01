/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.language.editor;

import consulo.annotation.access.RequiredReadAction;
import consulo.document.Document;
import consulo.document.RangeMarker;
import consulo.language.Language;
import consulo.language.psi.*;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.util.lang.CharArrayUtil;
import consulo.util.lang.StringCharactersParser;
import consulo.util.lang.reflect.ReflectionUtil;
import org.jspecify.annotations.Nullable;


public abstract class CodeInsightUtilCore extends FileModificationService {

    @RequiredReadAction
    public static <T extends PsiElement> T findElementInRange(
        PsiFile file,
        int startOffset,
        int endOffset,
        Class<T> clazz,
        Language language
    ) {
        return findElementInRange(file, startOffset, endOffset, clazz, language, null);
    }

    @RequiredReadAction
    @SuppressWarnings("unchecked")
    private static <T extends PsiElement> @Nullable T findElementInRange(
        PsiFile file,
        int startOffset,
        int endOffset,
        Class<T> clazz,
        Language language,
        @Nullable PsiElement initialElement
    ) {
        PsiElement element1 = file.getViewProvider().findElementAt(startOffset, language);
        PsiElement element2 = file.getViewProvider().findElementAt(endOffset - 1, language);
        if (element1 instanceof PsiWhiteSpace ws1) {
            startOffset = ws1.getTextRange().getEndOffset();
            element1 = file.getViewProvider().findElementAt(startOffset, language);
        }
        if (element2 instanceof PsiWhiteSpace ws2) {
            endOffset = ws2.getTextRange().getStartOffset();
            element2 = file.getViewProvider().findElementAt(endOffset - 1, language);
        }
        if (element2 == null || element1 == null) {
            return null;
        }
        PsiElement commonParent = PsiTreeUtil.findCommonParent(element1, element2);
        T element = ReflectionUtil.isAssignable(clazz, commonParent.getClass())
            ? (T) commonParent
            : PsiTreeUtil.getParentOfType(commonParent, clazz);

        if (element == initialElement) {
            return element;
        }

        if (element == null
            || element.getTextRange().getStartOffset() != startOffset
            || element.getTextRange().getEndOffset() != endOffset) {
            return null;
        }
        return element;
    }

    @RequiredReadAction
    public static <T extends PsiElement> @Nullable T forcePsiPostprocessAndRestoreElement(T element) {
        return forcePsiPostprocessAndRestoreElement(element, false);
    }

    @RequiredReadAction
    @SuppressWarnings("unchecked")
    public static <T extends PsiElement> @Nullable T forcePsiPostprocessAndRestoreElement(T element, boolean useFileLanguage) {
        PsiFile psiFile = element.getContainingFile();
        Document document = psiFile.getViewProvider().getDocument();
        //if (document == null) return element;
        Language language = useFileLanguage ? psiFile.getLanguage() : PsiUtilCore.getDialect(element);
        PsiDocumentManager documentManager = PsiDocumentManager.getInstance(psiFile.getProject());
        RangeMarker rangeMarker = document.createRangeMarker(element.getTextRange());
        documentManager.doPostponedOperationsAndUnblockDocument(document);
        documentManager.commitDocument(document);

        T elementInRange = findElementInRange(
            psiFile,
            rangeMarker.getStartOffset(),
            rangeMarker.getEndOffset(),
            (Class<? extends T>) element.getClass(),
            language,
            element
        );
        rangeMarker.dispose();
        return elementInRange;
    }

    public static boolean parseStringCharacters(String chars, StringBuilder out, @Nullable int[] sourceOffsets) {
        return StringCharactersParser.parseStringCharacters(chars, out, sourceOffsets);
    }

    public static boolean parseStringCharacters(String chars, StringBuilder out, @Nullable int[] sourceOffsets, boolean textBlock) {
        return StringCharactersParser.parseStringCharacters(chars, out, sourceOffsets, textBlock);
    }

    /**
     * @see StringCharactersParser#parseStringCharacters(String, int[])
     */
    public static @Nullable CharSequence parseStringCharacters(String chars, @Nullable int[] sourceOffsets) {
        return StringCharactersParser.parseStringCharacters(chars, sourceOffsets);
    }
}
