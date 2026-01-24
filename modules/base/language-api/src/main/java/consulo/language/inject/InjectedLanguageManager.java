/*
 * Copyright 2000-2017 JetBrains s.r.o.
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

package consulo.language.inject;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.document.Document;
import consulo.document.DocumentWindow;
import consulo.document.util.TextRange;
import consulo.language.file.FileViewProvider;
import consulo.language.internal.InjectedLanguageManagerHolder;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiLanguageInjectionHost;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

@ServiceAPI(ComponentScope.PROJECT)
public interface InjectedLanguageManager {
    @Nonnull
    static InjectedLanguageManager getInstance(Project project) {
        return InjectedLanguageManagerHolder.LAZY_INJECT.apply(project);
    }

    Key<Boolean> FRANKENSTEIN_INJECTION = Key.create("FRANKENSTEIN_INJECTION");

    PsiLanguageInjectionHost getInjectionHost(@Nonnull FileViewProvider injectedProvider);

    @Nullable
    PsiLanguageInjectionHost getInjectionHost(@Nonnull PsiElement injectedElement);

    @Nonnull
    TextRange injectedToHost(@Nonnull PsiElement injectedContext, @Nonnull TextRange injectedTextRange);

    int injectedToHost(@Nonnull PsiElement injectedContext, int injectedOffset);

    int injectedToHost(@Nonnull PsiElement injectedContext, int injectedOffset, boolean minHostOffset);

    @Nonnull
    String getUnescapedText(@Nonnull PsiElement injectedNode);

    @Nonnull
    List<TextRange> intersectWithAllEditableFragments(@Nonnull PsiFile injectedPsi, @Nonnull TextRange rangeToEdit);

    boolean isInjectedFragment(@Nonnull PsiFile injectedFile);

    PsiFile findInjectedPsiNoCommit(@Nonnull PsiFile host, int offset);

    /**
     * Finds PSI element in injected fragment (if any) at the given offset in the host file.<p/>
     * E.g. if you injected XML {@code "<xxx/>"} into Java string literal {@code "String s = "<xxx/>";"} and the caret is at {@code "xxx"} then
     * this method will return XmlToken(XML_TAG_START) with the text {@code "xxx"}.<br/>
     * Invocation of this method on uncommitted {@code hostFile} can lead to unexpected results, including throwing an exception!
     */
    @Nullable
    PsiElement findInjectedElementAt(@Nonnull PsiFile hostFile, int hostDocumentOffset);

    /**
     * Invocation of this method on uncommitted {@code file} can lead to unexpected results, including throwing an exception!
     */
    @Nullable
    PsiElement findElementAtNoCommit(@Nonnull PsiFile file, int offset);

    @Nullable
    List<Pair<PsiElement, TextRange>> getInjectedPsiFiles(@Nonnull PsiElement host);

    void dropFileCaches(@Nonnull PsiFile file);

    PsiFile getTopLevelFile(@Nonnull PsiElement element);

    @Nonnull
    List<DocumentWindow> getCachedInjectedDocumentsInRange(@Nonnull PsiFile hostPsiFile, @Nonnull TextRange range);

    void enumerate(@Nonnull PsiElement host, @Nonnull PsiLanguageInjectionHost.InjectedPsiVisitor visitor);

    void enumerate(@Nonnull DocumentWindow documentWindow, @Nonnull PsiFile hostPsiFile, @Nonnull PsiLanguageInjectionHost.InjectedPsiVisitor visitor);

    @RequiredReadAction
    void enumerateEx(@Nonnull PsiElement host,
                     @Nonnull PsiFile containingFile,
                     boolean probeUp,
                     @RequiredReadAction @Nonnull PsiLanguageInjectionHost.InjectedPsiVisitor visitor);

    /**
     * @return the ranges in this document window that correspond to prefix/suffix injected text fragments and thus can't be edited and are not visible in the editor.
     */
    @Nonnull
    List<TextRange> getNonEditableFragments(@Nonnull DocumentWindow window);

    /**
     * This method can be invoked on an uncommitted document, before performing commit and using other methods here
     * (which don't work for uncommitted document).
     */
    boolean mightHaveInjectedFragmentAtOffset(@Nonnull Document hostDocument, int hostOffset);

    @Nonnull
    DocumentWindow freezeWindow(@Nonnull DocumentWindow document);

    @Nullable
    PsiLanguageInjectionHost.Place getShreds(@Nonnull PsiFile injectedFile);

    @Nullable
    PsiLanguageInjectionHost.Place getShreds(@Nonnull FileViewProvider viewProvider);

    @Nonnull
    PsiLanguageInjectionHost.Place getShreds(@Nonnull DocumentWindow documentWindow);

    @RequiredReadAction
    @Nonnull
    default String getUnescapedText(@Nonnull PsiFile file, @Nullable PsiElement startElement, @Nullable PsiElement endElement) {
        int beginIndex = startElement == null ? 0 : startElement.getTextRange().getStartOffset();
        int endIndex = endElement == null ? file.getTextLength() : endElement.getTextRange().getStartOffset();
        return file.getText().substring(beginIndex, endIndex);
    }
}
