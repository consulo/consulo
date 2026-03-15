/*
 * Copyright 2013-2022 consulo.io
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
package consulo.language.psi.resolve;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.progress.ProgressIndicatorProvider;
import consulo.language.psi.*;
import consulo.project.Project;

import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 26-Mar-22
 */
@ServiceAPI(ComponentScope.PROJECT)
public interface ResolveCache {
  @FunctionalInterface
  public interface AbstractResolver<TRef extends PsiReference, TResult> {
    TResult resolve(TRef ref, boolean incompleteCode);
  }

  /**
   * Resolver which returns array of possible resolved variants instead of just one
   */
  @FunctionalInterface
  public interface PolyVariantResolver<T extends PsiPolyVariantReference> extends AbstractResolver<T, ResolveResult[]> {
    @Override
    
    ResolveResult[] resolve(T t, boolean incompleteCode);
  }

  /**
   * Poly variant resolver with additional containingFile parameter, which helps to avoid costly tree up traversal
   */
  @FunctionalInterface
  public interface PolyVariantContextResolver<T extends PsiPolyVariantReference> {
    
    ResolveResult[] resolve(T ref, PsiFile containingFile, boolean incompleteCode);
  }

  /**
   * Resolver specialized to resolve PsiReference to PsiElement
   */
  @FunctionalInterface
  public interface Resolver extends AbstractResolver<PsiReference, PsiElement> {
  }

  public static ResolveCache getInstance(Project project) {
    ProgressIndicatorProvider.checkCanceled(); // We hope this method is being called often enough to cancel daemon processes smoothly
    return project.getInstance(ResolveCache.class);
  }

  
  default <T extends PsiPolyVariantReference> ResolveResult[] resolveWithCaching(T ref, PolyVariantResolver<T> resolver, boolean needToPreventRecursion, boolean incompleteCode) {
    return resolveWithCaching(ref, resolver, needToPreventRecursion, incompleteCode, ref.getElement().getContainingFile());
  }

  
  public <T extends PsiPolyVariantReference> ResolveResult[] resolveWithCaching(T ref,
                                                                                PolyVariantResolver<T> resolver,
                                                                                boolean needToPreventRecursion,
                                                                                boolean incompleteCode,
                                                                                PsiFile containingFile);

  
  public <T extends PsiPolyVariantReference> ResolveResult[] resolveWithCaching(T ref,
                                                                                PolyVariantContextResolver<T> resolver,
                                                                                boolean needToPreventRecursion,
                                                                                boolean incompleteCode,
                                                                                PsiFile containingFile);

  <TRef extends PsiReference, TResult> TResult resolveWithCaching(TRef ref, AbstractResolver<TRef, TResult> resolver, boolean needToPreventRecursion, boolean incompleteCode);

  @Nullable
    // null means not cached
  <T extends PsiPolyVariantReference> ResolveResult[] getCachedResults(T ref, boolean physical, boolean incompleteCode, boolean isPoly);
}
