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

package com.intellij.lang.injection;

import com.intellij.injected.editor.DocumentWindow;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import consulo.disposer.Disposable;
import consulo.util.dataholder.Key;
import com.intellij.openapi.util.NotNullLazyKey;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLanguageInjectionHost;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public abstract class InjectedLanguageManager {

  public static InjectedLanguageManager getInstance(Project project) {
    return INSTANCE_CACHE.getValue(project);
  }

  protected static final NotNullLazyKey<InjectedLanguageManager, Project> INSTANCE_CACHE = ServiceManager.createLazyKey(InjectedLanguageManager.class);

  public static final Key<Boolean> FRANKENSTEIN_INJECTION = Key.create("FRANKENSTEIN_INJECTION");

  public abstract PsiLanguageInjectionHost getInjectionHost(@Nonnull FileViewProvider injectedProvider);

  @Nullable
  public abstract PsiLanguageInjectionHost getInjectionHost(@Nonnull PsiElement injectedElement);

  @Nonnull
  public abstract TextRange injectedToHost(@Nonnull PsiElement injectedContext, @Nonnull TextRange injectedTextRange);

  public abstract int injectedToHost(@Nonnull PsiElement injectedContext, int injectedOffset);

  public abstract int injectedToHost(@Nonnull PsiElement injectedContext, int injectedOffset, boolean minHostOffset);

  /**
   * @deprecated use {@link MultiHostInjector#MULTIHOST_INJECTOR_EP_NAME extension point} for production and
   * {@link #registerMultiHostInjector(MultiHostInjector, Disposable)} for tests
   */
  public void registerMultiHostInjector(@Nonnull MultiHostInjector injector) {
    throw new UnsupportedOperationException();
  }

  public void registerMultiHostInjector(@Nonnull MultiHostInjector injector, @Nonnull Disposable parentDisposable) {
    throw new UnsupportedOperationException();
  }

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
}
