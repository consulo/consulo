/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

package consulo.language.editor.refactoring.util;

import consulo.application.ApplicationManager;
import consulo.application.util.function.Processor;
import consulo.component.ProcessCanceledException;
import consulo.content.scope.SearchScope;
import consulo.document.util.TextRange;
import consulo.find.FindManager;
import consulo.find.FindUsagesHandler;
import consulo.find.FindUsagesUtil;
import consulo.language.ast.ASTNode;
import consulo.language.parser.ParserDefinition;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiPolyVariantReference;
import consulo.language.psi.PsiReference;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.scope.PsiSearchScopeUtil;
import consulo.language.psi.search.PsiNonJavaFileReferenceProcessor;
import consulo.language.psi.search.PsiSearchHelper;
import consulo.language.psi.search.TextOccurenceProcessor;
import consulo.language.psi.search.UsageSearchContext;
import consulo.logging.Logger;
import consulo.usage.NonCodeUsageInfo;
import consulo.usage.UsageInfo;
import consulo.usage.UsageInfoFactory;
import jakarta.annotation.Nonnull;

import java.util.Collection;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

public class TextOccurrencesUtil {
  private static final Logger LOG = Logger.getInstance(TextOccurrencesUtil.class);

  private TextOccurrencesUtil() {
  }

  public static void addTextOccurences(@Nonnull PsiElement element,
                                       @Nonnull String stringToSearch,
                                       @Nonnull GlobalSearchScope searchScope,
                                       @Nonnull final Collection<UsageInfo> results,
                                       @Nonnull UsageInfoFactory factory) {
    processTextOccurences(element, stringToSearch, searchScope, new Processor<UsageInfo>() {
      @Override
      public boolean process(UsageInfo t) {
        results.add(t);
        return true;
      }
    }, factory);
  }

  public static boolean processTextOccurences(@Nonnull PsiElement element,
                                              @Nonnull String stringToSearch,
                                              @Nonnull GlobalSearchScope searchScope,
                                              @Nonnull final Processor<UsageInfo> processor,
                                              @Nonnull final UsageInfoFactory factory) {
    PsiSearchHelper helper = PsiSearchHelper.getInstance(element.getProject());

    return helper.processUsagesInNonJavaFiles(element, stringToSearch, new PsiNonJavaFileReferenceProcessor() {
      @Override
      public boolean process(PsiFile psiFile, int startOffset, int endOffset) {
        try {
          UsageInfo usageInfo = ApplicationManager.getApplication()
                                                  .runReadAction((Supplier<UsageInfo>)() -> factory.createUsageInfo(psiFile,
                                                                                                                    startOffset,
                                                                                                                    endOffset));
          return usageInfo == null || processor.process(usageInfo);
        }
        catch (ProcessCanceledException e) {
          throw e;
        }
        catch (Exception e) {
          LOG.error(e);
          return true;
        }
      }
    }, searchScope);
  }

  private static boolean processStringLiteralsContainingIdentifier(@Nonnull String identifier,
                                                                   @Nonnull SearchScope searchScope,
                                                                   PsiSearchHelper helper,
                                                                   final Processor<PsiElement> processor) {
    TextOccurenceProcessor occurenceProcessor = new TextOccurenceProcessor() {
      @Override
      public boolean execute(PsiElement element, int offsetInElement) {
        ParserDefinition definition = ParserDefinition.forLanguage(element.getLanguage());
        ASTNode node = element.getNode();
        if (definition != null && node != null && definition.getStringLiteralElements(element.getLanguageVersion())
                                                            .contains(node.getElementType())) {
          return processor.process(element);
        }
        return true;
      }
    };

    return helper.processElementsWithWord(occurenceProcessor, searchScope, identifier, UsageSearchContext.IN_STRINGS, true);
  }

  public static boolean processUsagesInStringsAndComments(@Nonnull PsiElement element,
                                                          @Nonnull final String stringToSearch,
                                                          final boolean ignoreReferences,
                                                          @Nonnull final BiPredicate<PsiElement, TextRange> processor) {
    PsiSearchHelper helper = PsiSearchHelper.getInstance(element.getProject());
    SearchScope scope = PsiSearchScopeUtil.getUseScope(element);
    scope = GlobalSearchScope.projectScope(element.getProject()).intersectWith(scope);
    Processor<PsiElement> commentOrLiteralProcessor = new Processor<PsiElement>() {
      @Override
      public boolean process(PsiElement literal) {
        return processTextIn(literal, stringToSearch, ignoreReferences, processor);
      }
    };
    return processStringLiteralsContainingIdentifier(stringToSearch, scope, helper, commentOrLiteralProcessor) &&
      helper.processCommentsContainingIdentifier(stringToSearch, scope, commentOrLiteralProcessor);
  }

  public static void addUsagesInStringsAndComments(@Nonnull PsiElement element,
                                                   @Nonnull String stringToSearch,
                                                   @Nonnull final Collection<UsageInfo> results,
                                                   @Nonnull final UsageInfoFactory factory) {
    final Object lock = new Object();
    processUsagesInStringsAndComments(element, stringToSearch, false, new BiPredicate<PsiElement, TextRange>() {
      @Override
      public boolean test(PsiElement commentOrLiteral, TextRange textRange) {
        UsageInfo usageInfo = factory.createUsageInfo(commentOrLiteral, textRange.getStartOffset(), textRange.getEndOffset());
        if (usageInfo != null) {
          synchronized (lock) {
            results.add(usageInfo);
          }
        }
        return true;
      }
    });
  }

  private static boolean processTextIn(PsiElement scope,
                                       String stringToSearch,
                                       boolean ignoreReferences,
                                       BiPredicate<PsiElement, TextRange> processor) {
    String text = scope.getText();
    for (int offset = 0; offset < text.length(); offset++) {
      offset = text.indexOf(stringToSearch, offset);
      if (offset < 0) break;
      PsiReference referenceAt = scope.findReferenceAt(offset);
      if (!ignoreReferences &&
        referenceAt != null &&
        (referenceAt.resolve() != null || referenceAt instanceof PsiPolyVariantReference && ((PsiPolyVariantReference)referenceAt).multiResolve(
          true).length > 0)) {
        continue;
      }

      if (offset > 0) {
        char c = text.charAt(offset - 1);
        if (Character.isJavaIdentifierPart(c) && c != '$') {
          if (offset < 2 || text.charAt(offset - 2) != '\\') continue;  //escape sequence
        }
      }

      if (offset + stringToSearch.length() < text.length()) {
        char c = text.charAt(offset + stringToSearch.length());
        if (Character.isJavaIdentifierPart(c) && c != '$') {
          continue;
        }
      }

      TextRange textRange = new TextRange(offset, offset + stringToSearch.length());
      if (!processor.test(scope, textRange)) {
        return false;
      }

      offset += stringToSearch.length();
    }
    return true;
  }

  public static boolean isSearchTextOccurencesEnabled(@Nonnull PsiElement element) {
    FindUsagesHandler handler = FindManager.getInstance(element.getProject()).getFindUsagesHandler(element, true);
    return FindUsagesUtil.isSearchForTextOccurrencesAvailable(element, false, handler);
  }

  public static void findNonCodeUsages(PsiElement element,
                                       String stringToSearch,
                                       boolean searchInStringsAndComments,
                                       boolean searchInNonJavaFiles,
                                       String newQName,
                                       Collection<UsageInfo> results) {
    if (searchInStringsAndComments || searchInNonJavaFiles) {
      UsageInfoFactory factory = createUsageInfoFactory(element, newQName);

      if (searchInStringsAndComments) {
        addUsagesInStringsAndComments(element, stringToSearch, results, factory);
      }

      if (searchInNonJavaFiles) {
        GlobalSearchScope projectScope = GlobalSearchScope.projectScope(element.getProject());
        addTextOccurences(element, stringToSearch, projectScope, results, factory);
      }
    }
  }

  private static UsageInfoFactory createUsageInfoFactory(final PsiElement element, final String newQName) {
    return new UsageInfoFactory() {
      @Override
      public UsageInfo createUsageInfo(@Nonnull PsiElement usage, int startOffset, int endOffset) {
        int start = usage.getTextRange().getStartOffset();
        return NonCodeUsageInfo.create(usage.getContainingFile(), start + startOffset, start + endOffset, element, newQName);
      }
    };
  }

}
