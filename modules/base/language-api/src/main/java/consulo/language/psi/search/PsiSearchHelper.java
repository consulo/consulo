/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.language.psi.search;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.progress.ProgressIndicator;
import consulo.content.scope.SearchScope;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiReference;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.scope.PsiSearchScopeUtil;
import consulo.project.Project;
import consulo.util.concurrent.AsyncFuture;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

/**
 * Provides low-level search and find usages services for a project, like finding references
 * to an element, finding overriding / inheriting elements, finding to do items and so on.
 * <p>
 * Use {@link PsiSearchHelper#getInstance}() to get a search helper instance.
 */
@ServiceAPI(ComponentScope.PROJECT)
public interface PsiSearchHelper {
  @Deprecated(forRemoval = true)
  class SERVICE {
    private SERVICE() {
    }

    @Deprecated(forRemoval = true)
    public static PsiSearchHelper getInstance(Project project) {
      return project.getInstance(PsiSearchHelper.class);
    }
  }

  
   static PsiSearchHelper getInstance(Project project) {
    return project.getInstance(PsiSearchHelper.class);
  }

  /**
   * Searches the specified scope for comments containing the specified identifier.
   *
   * @param identifier  the identifier to search.
   * @param searchScope the scope in which occurrences are searched.
   * @return the array of found comments.
   */
  
  PsiElement[] findCommentsContainingIdentifier(String identifier, SearchScope searchScope);

  /**
   * Processes the specified scope and hands comments containing the specified identifier over to the processor.
   *
   * @param identifier  the identifier to search.
   * @param searchScope the scope in which occurrences are searched.
   * @param processor
   * @return false if processor returned false, true otherwise
   */
  boolean processCommentsContainingIdentifier(String identifier, SearchScope searchScope, Predicate<PsiElement> processor);

  /**
   * Given a text, scope and other search flags, runs the processor on all indexed files that contain all words from the text.
   * Note that this doesn't mean the files contain the text itself.
   */
  boolean processCandidateFilesForText(GlobalSearchScope scope, short searchContext, boolean caseSensitively, String text, Predicate<? super VirtualFile> processor);

  /**
   * Returns the list of files which contain the specified word in "plain text"
   * context (for example, plain text files or attribute values in XML files).
   *
   * @param word the word to search.
   * @return the list of files containing the word.
   */
  
  PsiFile[] findFilesWithPlainTextWords(String word);

  /**
   * Passes all occurrences of the specified full-qualified class name in plain text context
   * to the specified processor.
   *
   * @param qName       the class name to search.
   * @param processor   the processor which accepts the references.
   * @param searchScope the scope in which occurrences are searched.
   */
  boolean processUsagesInNonJavaFiles(String qName, PsiNonJavaFileReferenceProcessor processor, GlobalSearchScope searchScope);

  /**
   * Passes all occurrences of the specified full-qualified class name in plain text context in the
   * use scope of the specified element to the specified processor.
   *
   * @param originalElement the element whose use scope is used to restrict the search scope,
   *                        or null if the search scope is not restricted.
   * @param qName           the class name to search.
   * @param processor       the processor which accepts the references.
   * @param searchScope     the scope in which occurrences are searched.
   */
  boolean processUsagesInNonJavaFiles(@Nullable PsiElement originalElement,
                                      String qName,
                                      PsiNonJavaFileReferenceProcessor processor,
                                      GlobalSearchScope searchScope);

  /**
   * Returns the scope in which references to the specified element are searched. This scope includes the result of
   * {@link PsiElement#getUseScope()} and also the results returned from the registered
   * com.intellij.psi.search.UseScopeEnlarger instances.
   *
   * @param element the element to return the use scope form.
   * @return the search scope instance.
   */
  
  @Deprecated
  default SearchScope getUseScope(PsiElement element) {
    return PsiSearchScopeUtil.getUseScope(element);
  }

  /**
   * Passes all files containing the specified word in {@link UsageSearchContext#IN_CODE code}
   * context to the specified processor.
   *
   * @param word            the word to search.
   * @param scope           the scope in which occurrences are searched.
   * @param processor       the processor which accepts the references.
   * @param caseSensitively if words differing in the case only should not be considered equal
   */
  boolean processAllFilesWithWord(String word, GlobalSearchScope scope, Predicate<PsiFile> processor, boolean caseSensitively);

  /**
   * Passes all files containing the specified word in {@link UsageSearchContext#IN_PLAIN_TEXT code}
   * context to the specified processor.
   *
   * @param word            the word to search.
   * @param scope           the scope in which occurrences are searched.
   * @param processor       the processor which accepts the references.
   * @param caseSensitively if words differing in the case only should not be considered equal
   */
  boolean processAllFilesWithWordInText(String word, GlobalSearchScope scope, Predicate<PsiFile> processor, boolean caseSensitively);

  /**
   * Passes all files containing the specified word in {@link UsageSearchContext#IN_COMMENTS comments}
   * context to the specified processor.
   *
   * @param word      the word to search.
   * @param scope     the scope in which occurrences are searched.
   * @param processor the processor which accepts the references.
   */
  boolean processAllFilesWithWordInComments(String word, GlobalSearchScope scope, Predicate<PsiFile> processor);

  /**
   * Passes all files containing the specified word in {@link UsageSearchContext#IN_STRINGS string literal}
   * context to the specified processor.
   *
   * @param word      the word to search.
   * @param scope     the scope in which occurrences are searched.
   * @param processor the processor which accepts the references.
   */
  boolean processAllFilesWithWordInLiterals(String word, GlobalSearchScope scope, Predicate<PsiFile> processor);

  boolean processRequests(SearchRequestCollector request, Predicate<? super PsiReference> processor);

  
  AsyncFuture<Boolean> processRequestsAsync(SearchRequestCollector request, Predicate<PsiReference> processor);

  boolean processElementsWithWord(TextOccurenceProcessor processor, SearchScope searchScope, String text, short searchContext, boolean caseSensitive);

  boolean processElementsWithWord(TextOccurenceProcessor processor,
                                  SearchScope searchScope,
                                  String text,
                                  short searchContext,
                                  boolean caseSensitive,
                                  boolean processInjectedPsi);

  
  AsyncFuture<Boolean> processElementsWithWordAsync(TextOccurenceProcessor processor, SearchScope searchScope, String text, short searchContext, boolean caseSensitive);

  default boolean hasIdentifierInFile(PsiFile file, String name) {
    throw new UnsupportedOperationException();
  }

  
  SearchCostResult isCheapEnoughToSearch(String name,
                                         GlobalSearchScope scope,
                                         @Nullable PsiFile fileToIgnoreOccurencesIn,
                                         @Nullable ProgressIndicator progress);

  public boolean processFilesWithText(GlobalSearchScope scope,
                                      short searchContext,
                                      boolean caseSensitively,
                                      String text,
                                      Predicate<VirtualFile> processor);

  enum SearchCostResult {
    ZERO_OCCURRENCES,
    FEW_OCCURRENCES,
    TOO_MANY_OCCURRENCES
  }
}
