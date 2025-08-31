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

package consulo.language.psi.path;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import consulo.module.Module;
import consulo.virtualFileSystem.fileType.FileType;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.ArrayList;

/**
 * @author Dmitry Avdeev
 * @see PathReferenceProvider
 */
@ServiceAPI(ComponentScope.APPLICATION)
public abstract class PathReferenceManager {
  @Nonnull
  public static PathReferenceManager getInstance() {
    return Application.get().getInstance(PathReferenceManager.class);
  }

  /**
   * Create web path references for given PsiElement.
   * The same as {@link #createReferences(PsiElement, boolean, boolean, boolean, PathReferenceProvider[])} with
   * endingSlashNotAllowed = true and relativePathsAllowed = true.
   *
   * @param psiElement          the underlying PSI element.
   * @param soft                set this to true to create soft references (see {@link PsiReference#isSoft()}).
   * @param additionalProviders additional providers to process.
   * @return created references or an empty array.
   */
  @Nonnull
  public abstract PsiReference[] createReferences(@Nonnull PsiElement psiElement, boolean soft, PathReferenceProvider... additionalProviders);

  /**
   * Create web path references for given PsiElement.
   *
   * @param psiElement            the underlying PSI element.
   * @param soft                  set this to true to create soft references (see {@link PsiReference#isSoft()}).
   * @param endingSlashNotAllowed true if paths like "/foo/" should not be resolved.
   * @param relativePathsAllowed  true if the folder of the file containing the PsiElement should be used as "root".
   *                              Otherwise, web application root will be used.
   * @param additionalProviders   additional providers to process.  @return created references or an empty array.
   */
  @Nonnull
  public abstract PsiReference[] createReferences(@Nonnull PsiElement psiElement,
                                                  boolean soft,
                                                  boolean endingSlashNotAllowed,
                                                  boolean relativePathsAllowed,
                                                  PathReferenceProvider... additionalProviders);

  public abstract PsiReference[] createReferences(@Nonnull PsiElement psiElement,
                                                  boolean soft,
                                                  boolean endingSlashNotAllowed,
                                                  boolean relativePathsAllowed,
                                                  FileType[] suitableFileTypes,
                                                  PathReferenceProvider... additionalProviders);

  @Nonnull
  public abstract PsiReference[] createCustomReferences(@Nonnull PsiElement psiElement, boolean soft, PathReferenceProvider... providers);


  @Nullable
  public abstract PathReference getPathReference(@Nonnull String path, @Nonnull PsiElement element, PathReferenceProvider... additionalProviders);

  @Nullable
  public abstract PathReference getCustomPathReference(@Nonnull String path, @Nonnull Module module, @Nonnull PsiElement element, PathReferenceProvider... providers);

  @Nonnull
  public abstract PathReferenceProvider getGlobalWebPathReferenceProvider();

  @Nonnull
  public abstract PathReferenceProvider createStaticPathReferenceProvider(boolean relativePathsAllowed);

  public static PsiReference[] getReferencesFromProvider(@Nonnull PathReferenceProvider provider, @Nonnull PsiElement psiElement, boolean soft) {
    ArrayList<PsiReference> references = new ArrayList<PsiReference>();
    provider.createReferences(psiElement, references, soft);
    return references.toArray(new PsiReference[references.size()]);
  }
}
