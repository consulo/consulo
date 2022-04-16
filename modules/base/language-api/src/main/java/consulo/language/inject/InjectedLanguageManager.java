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

import consulo.component.util.ComponentUtil;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.language.file.FileViewProvider;
import consulo.language.file.inject.DocumentWindow;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiLanguageInjectionHost;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.util.lang.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Function;

public abstract class InjectedLanguageManager {
  private static final Function<Project, InjectedLanguageManager> LAZY_INJECT = ComponentUtil.createLazyInject(InjectedLanguageManager.class);

  @Nonnull
  public static InjectedLanguageManager getInstance(Project project) {
    return LAZY_INJECT.apply(project);
  }

  public static final Key<Boolean> FRANKENSTEIN_INJECTION = Key.create("FRANKENSTEIN_INJECTION");

  public abstract PsiLanguageInjectionHost getInjectionHost(@Nonnull FileViewProvider injectedProvider);

  @Nullable
  public abstract PsiLanguageInjectionHost getInjectionHost(@Nonnull PsiElement injectedElement);

  @Nonnull
  public abstract TextRange injectedToHost(@Nonnull PsiElement injectedContext, @Nonnull TextRange injectedTextRange);

  public abstract int injectedToHost(@Nonnull PsiElement injectedContext, int injectedOffset);

  public abstract int injectedToHost(@Nonnull PsiElement injectedContext, int injectedOffset, boolean minHostOffset);

  @Nonnull
  public abstract String getUnescapedText(@Nonnull PsiElement injectedNode);

  @Nonnull
  public abstract List<TextRange> intersectWithAllEditableFragments(@Nonnull PsiFile injectedPsi, @Nonnull TextRange rangeToEdit);

  public abstract boolean isInjectedFragment(@Nonnull PsiFile injectedFile);

  /**
   * Finds PSI element in injected fragment (if any) at the given offset in the host file.<p/>
   * E.g. if you injected XML {@code "<xxx/>"} into Java string literal {@code "String s = "<xxx/>";"} and the caret is at {@code "xxx"} then
   * this method will return XmlToken(XML_TAG_START) with the text {@code "xxx"}.<br/>
   * Invocation of this method on uncommitted {@code hostFile} can lead to unexpected results, including throwing an exception!
   */
  @Nullable
  public abstract PsiElement findInjectedElementAt(@Nonnull PsiFile hostFile, int hostDocumentOffset);

  @Nullable
  public abstract List<Pair<PsiElement, TextRange>> getInjectedPsiFiles(@Nonnull PsiElement host);

  public abstract void dropFileCaches(@Nonnull PsiFile file);

  public abstract PsiFile getTopLevelFile(@Nonnull PsiElement element);

  @Nonnull
  public abstract List<DocumentWindow> getCachedInjectedDocumentsInRange(@Nonnull PsiFile hostPsiFile, @Nonnull TextRange range);

  public abstract void enumerate(@Nonnull PsiElement host, @Nonnull PsiLanguageInjectionHost.InjectedPsiVisitor visitor);

  public abstract void enumerateEx(@Nonnull PsiElement host, @Nonnull PsiFile containingFile, boolean probeUp, @Nonnull PsiLanguageInjectionHost.InjectedPsiVisitor visitor);

  /**
   * @return the ranges in this document window that correspond to prefix/suffix injected text fragments and thus can't be edited and are not visible in the editor.
   */
  @Nonnull
  public abstract List<TextRange> getNonEditableFragments(@Nonnull DocumentWindow window);

  /**
   * This method can be invoked on an uncommitted document, before performing commit and using other methods here
   * (which don't work for uncommitted document).
   */
  public abstract boolean mightHaveInjectedFragmentAtOffset(@Nonnull Document hostDocument, int hostOffset);

  @Nonnull
  public abstract DocumentWindow freezeWindow(@Nonnull DocumentWindow document);

  @Nullable
  public abstract PsiLanguageInjectionHost.Place getShreds(@Nonnull PsiFile injectedFile);

  @Nullable
  public abstract PsiLanguageInjectionHost.Place getShreds(@Nonnull FileViewProvider viewProvider);

  @Nonnull
  public abstract PsiLanguageInjectionHost.Place getShreds(@Nonnull DocumentWindow documentWindow);
}
