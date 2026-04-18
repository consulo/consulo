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
import org.jspecify.annotations.Nullable;

import java.util.List;

@ServiceAPI(ComponentScope.PROJECT)
public interface InjectedLanguageManager {
    static InjectedLanguageManager getInstance(Project project) {
        return InjectedLanguageManagerHolder.LAZY_INJECT.apply(project);
    }

    Key<Boolean> FRANKENSTEIN_INJECTION = Key.create("FRANKENSTEIN_INJECTION");

    PsiLanguageInjectionHost getInjectionHost(FileViewProvider injectedProvider);

    @Nullable PsiLanguageInjectionHost getInjectionHost(PsiElement injectedElement);

    TextRange injectedToHost(PsiElement injectedContext, TextRange injectedTextRange);

    int injectedToHost(PsiElement injectedContext, int injectedOffset);

    int injectedToHost(PsiElement injectedContext, int injectedOffset, boolean minHostOffset);

    @Nullable String getUnescapedLeafText(PsiElement element, boolean strict);

    String getUnescapedText(PsiElement injectedNode);

    List<TextRange> intersectWithAllEditableFragments(PsiFile injectedPsi, TextRange rangeToEdit);

    boolean isInjectedFragment(PsiFile injectedFile);

    PsiFile findInjectedPsiNoCommit(PsiFile host, int offset);

    /**
     * Finds PSI element in injected fragment (if any) at the given offset in the host file.<p/>
     * E.g. if you injected XML {@code "<xxx/>"} into Java string literal {@code "String s = "<xxx/>";"} and the caret is at {@code "xxx"}
     * then this method will return XmlToken(XML_TAG_START) with the text {@code "xxx"}.<br/>
     * Invocation of this method on uncommitted {@code hostFile} can lead to unexpected results, including throwing an exception!
     */
    @Nullable PsiElement findInjectedElementAt(PsiFile hostFile, int hostDocumentOffset);

    /**
     * Invocation of this method on uncommitted {@code file} can lead to unexpected results, including throwing an exception!
     */
    @Nullable PsiElement findElementAtNoCommit(PsiFile file, int offset);

    @Nullable List<Pair<PsiElement, TextRange>> getInjectedPsiFiles(PsiElement host);

    void dropFileCaches(PsiFile file);

    @Nullable PsiFile getTopLevelFile(PsiElement element);

    List<DocumentWindow> getCachedInjectedDocumentsInRange(PsiFile hostPsiFile, TextRange range);

    void enumerate(PsiElement host, PsiLanguageInjectionHost.InjectedPsiVisitor visitor);

    void enumerate(DocumentWindow documentWindow, PsiFile hostPsiFile, PsiLanguageInjectionHost.InjectedPsiVisitor visitor);

    @RequiredReadAction
    void enumerateEx(PsiElement host,
                     PsiFile containingFile,
                     boolean probeUp,
                     @RequiredReadAction PsiLanguageInjectionHost.InjectedPsiVisitor visitor);

    /**
     * @return the ranges in this document window that correspond to prefix/suffix injected text fragments and thus can't be edited
     *         and are not visible in the editor.
     */
    List<TextRange> getNonEditableFragments(DocumentWindow window);

    /**
     * This method can be invoked on an uncommitted document, before performing commit and using other methods here
     * (which don't work for uncommitted document).
     */
    boolean mightHaveInjectedFragmentAtOffset(Document hostDocument, int hostOffset);

    DocumentWindow freezeWindow(DocumentWindow document);

    PsiLanguageInjectionHost.@Nullable Place getShreds(PsiFile injectedFile);

    PsiLanguageInjectionHost.@Nullable Place getShreds(FileViewProvider viewProvider);

    PsiLanguageInjectionHost.Place getShreds(DocumentWindow documentWindow);
    @RequiredReadAction
   
    default String getUnescapedText(PsiFile file, @Nullable PsiElement startElement, @Nullable PsiElement endElement) {
        int beginIndex = startElement == null ? 0 : startElement.getTextRange().getStartOffset();
        int endIndex = endElement == null ? file.getTextLength() : endElement.getTextRange().getStartOffset();
        return file.getText().substring(beginIndex, endIndex);
    }
}
