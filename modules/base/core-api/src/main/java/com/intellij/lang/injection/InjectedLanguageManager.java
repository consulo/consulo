/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

/*
 * Created by IntelliJ IDEA.
 * User: cdr
 * Date: Jun 8, 2007
 * Time: 8:41:25 PM
 */
package com.intellij.lang.injection;

import com.intellij.injected.editor.DocumentWindow;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NotNullLazyKey;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLanguageInjectionHost;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.List;

public abstract class InjectedLanguageManager {
  protected static final NotNullLazyKey<InjectedLanguageManager, Project> INSTANCE_CACHE = ServiceManager.createLazyKey(InjectedLanguageManager.class);

  public static InjectedLanguageManager getInstance(Project project) {
    return INSTANCE_CACHE.getValue(project);
  }

  @Nullable
  public abstract PsiLanguageInjectionHost getInjectionHost(@Nonnull PsiElement element);

  @Nonnull
  public abstract TextRange injectedToHost(@Nonnull PsiElement injectedContext, @Nonnull TextRange injectedTextRange);
  public abstract int injectedToHost(@Nonnull PsiElement injectedContext, int injectedOffset);

  /**
   * Test-only method.
   * @see com.intellij.lang.injection.MultiHostInjector#EP_NAME
   */
  @TestOnly
  public abstract void registerMultiHostInjector(@Nonnull MultiHostInjector injector, @Nonnull Class<? extends PsiElement>... elements);

  /**
   * Test-only method.
   * @see com.intellij.lang.injection.MultiHostInjector#EP_NAME
   */
  @TestOnly
  public abstract boolean unregisterMultiHostInjector(@Nonnull MultiHostInjector injector);

  public abstract String getUnescapedText(@Nonnull PsiElement injectedNode);

  @Nonnull
  public abstract List<TextRange> intersectWithAllEditableFragments(@Nonnull PsiFile injectedPsi, @Nonnull TextRange rangeToEdit);

  public abstract boolean isInjectedFragment(PsiFile file);

  @Nullable
  public abstract PsiElement findInjectedElementAt(@Nonnull PsiFile hostFile, int hostDocumentOffset);

  @Nullable
  public abstract List<Pair<PsiElement, TextRange>> getInjectedPsiFiles(@Nonnull PsiElement host);

  public abstract void dropFileCaches(@Nonnull PsiFile file);

  public abstract PsiFile getTopLevelFile(@Nonnull PsiElement element);

  @Nonnull
  public abstract List<DocumentWindow> getCachedInjectedDocuments(@Nonnull PsiFile hostPsiFile);

  public abstract void startRunInjectors(@Nonnull Document hostDocument, boolean synchronously);

  public abstract void enumerate(@Nonnull PsiElement host, @Nonnull PsiLanguageInjectionHost.InjectedPsiVisitor visitor);

  public abstract void enumerateEx(@Nonnull PsiElement host, @Nonnull PsiFile containingFile, boolean probeUp, @Nonnull PsiLanguageInjectionHost.InjectedPsiVisitor visitor);

  /**
   * @return the ranges in this document window that correspond to prefix/suffix injected text fragments and thus can't be edited and are not visible in the editor.
   */
  @Nonnull
  public abstract List<TextRange> getNonEditableFragments(@Nonnull DocumentWindow window);
}
