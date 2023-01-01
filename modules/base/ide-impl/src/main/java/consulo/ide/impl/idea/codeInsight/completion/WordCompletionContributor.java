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
package consulo.ide.impl.idea.codeInsight.completion;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.language.psi.stub.IdTableBuilding;
import consulo.language.Language;
import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.editor.completion.*;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.editor.impl.internal.completion.CompletionData;
import consulo.language.editor.impl.internal.completion.CompletionUtil;
import consulo.language.editor.impl.internal.completion.CompletionVariant;
import consulo.language.parser.ParserDefinition;
import consulo.language.pattern.ElementPattern;
import consulo.language.plain.ast.PlainTextTokenTypes;
import consulo.language.psi.*;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.project.DumbService;
import consulo.util.lang.StringUtil;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import static consulo.language.pattern.PlatformPatterns.psiElement;

/**
 * @author peter
 */
@ExtensionImpl(id = "wordCompletion", order = "last")
public class WordCompletionContributor extends CompletionContributor implements DumbAware {

  @RequiredReadAction
  @Override
  public void fillCompletionVariants(@Nonnull final CompletionParameters parameters, @Nonnull final CompletionResultSet result) {
    if (parameters.getCompletionType() == CompletionType.BASIC && shouldPerformWordCompletion(parameters)) {
      addWordCompletionVariants(result, parameters, Collections.emptySet());
    }
  }

  @Nonnull
  @Override
  public Language getLanguage() {
    return Language.ANY;
  }

  public static void addWordCompletionVariants(CompletionResultSet result, final CompletionParameters parameters, Set<String> excludes) {
    final Set<String> realExcludes = new HashSet<>(excludes);
    for (String exclude : excludes) {
      String[] words = exclude.split("[ \\.-]");
      if (words.length > 0 && StringUtil.isNotEmpty(words[0])) {
        realExcludes.add(words[0]);
      }
    }

    int startOffset = parameters.getOffset();
    final PsiElement position = parameters.getPosition();
    final CompletionResultSet javaResultSet = result.withPrefixMatcher(CompletionUtil.findJavaIdentifierPrefix(parameters));
    final CompletionResultSet plainResultSet = result.withPrefixMatcher(CompletionUtil.findAlphanumericPrefix(parameters));
    for (final String word : getAllWords(position, startOffset)) {
      if (!realExcludes.contains(word)) {
        final LookupElement item = LookupElementBuilder.create(word);
        javaResultSet.addElement(item);
        plainResultSet.addElement(item);
      }
    }

    addValuesFromOtherStringLiterals(result, parameters, realExcludes, position);
  }

  private static void addValuesFromOtherStringLiterals(CompletionResultSet result, CompletionParameters parameters, final Set<String> realExcludes, PsiElement position) {
    ParserDefinition definition = ParserDefinition.forLanguage(position.getLanguage());
    if (definition == null) {
      return;
    }
    final ElementPattern<PsiElement> pattern = psiElement().withElementType(definition.getStringLiteralElements(position.getLanguageVersion()));
    final PsiElement localString = PsiTreeUtil.findFirstParent(position, false, element -> pattern.accepts(element));
    if (localString == null) {
      return;
    }
    ElementManipulator<PsiElement> manipulator = ElementManipulators.getManipulator(localString);
    if (manipulator == null) {
      return;
    }
    int offset = manipulator.getRangeInElement(localString).getStartOffset();
    PsiFile file = position.getContainingFile();
    final CompletionResultSet fullStringResult = result.withPrefixMatcher(file.getText().substring(offset + localString.getTextRange().getStartOffset(), parameters.getOffset()));
    file.accept(new PsiRecursiveElementWalkingVisitor() {
      @Override
      public void visitElement(PsiElement element) {
        if (element == localString) {
          return;
        }
        if (pattern.accepts(element)) {
          element.accept(new PsiRecursiveElementWalkingVisitor() {
            @Override
            public void visitElement(PsiElement each) {
              String valueText = ElementManipulators.getValueText(each);
              if (StringUtil.isNotEmpty(valueText) && !realExcludes.contains(valueText)) {
                final LookupElement item = LookupElementBuilder.create(valueText);
                fullStringResult.addElement(item);
              }
            }
          });
          return;
        }
        super.visitElement(element);
      }
    });
  }

  @RequiredReadAction
  private static boolean shouldPerformWordCompletion(CompletionParameters parameters) {
    final PsiElement insertedElement = parameters.getPosition();
    final boolean dumb = DumbService.getInstance(insertedElement.getProject()).isDumb();
    if (dumb) {
      return true;
    }

    if (parameters.getInvocationCount() == 0) {
      return false;
    }

    final PsiFile file = insertedElement.getContainingFile();
    final CompletionData data = CompletionUtil.getCompletionDataByElement(insertedElement, file);
    if (data != null) {
      Set<CompletionVariant> toAdd = new HashSet<>();
      data.addKeywordVariants(toAdd, insertedElement, file);
      for (CompletionVariant completionVariant : toAdd) {
        if (completionVariant.hasKeywordCompletions()) {
          return false;
        }
      }
    }

    final int startOffset = parameters.getOffset();

    final PsiReference reference = file.findReferenceAt(startOffset);
    if (reference != null) {
      return false;
    }

    final PsiElement element = file.findElementAt(startOffset - 1);

    ASTNode textContainer = element != null ? element.getNode() : null;
    while (textContainer != null) {
      final IElementType elementType = textContainer.getElementType();
      if (WordCompletionElementFilter.isEnabledIn(textContainer) || elementType == PlainTextTokenTypes.PLAIN_TEXT) {
        return true;
      }
      textContainer = textContainer.getTreeParent();
    }
    return false;
  }

  public static Set<String> getAllWords(final PsiElement context, final int offset) {
    final Set<String> words = new LinkedHashSet<>();
    if (StringUtil.isEmpty(CompletionUtil.findJavaIdentifierPrefix(context, offset))) {
      return words;
    }

    final CharSequence chars = context.getContainingFile().getViewProvider().getContents(); // ??
    IdTableBuilding.scanWords((chars1, charsArray, start, end) -> {
      if (start > offset || offset > end) {
        words.add(chars1.subSequence(start, end).toString());
      }
    }, chars, 0, chars.length());
    return words;
  }
}
