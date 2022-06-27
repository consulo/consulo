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
package consulo.ide.impl.idea.codeInsight.completion;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.ide.impl.idea.ide.highlighter.custom.SyntaxTable;
import consulo.codeEditor.EditorEx;
import consulo.ide.impl.idea.openapi.fileTypes.impl.CustomSyntaxTableFileType;
import consulo.ide.impl.psi.CustomHighlighterTokenType;
import consulo.annotation.access.RequiredReadAction;
import consulo.application.dumb.DumbAware;
import consulo.language.editor.completion.*;
import consulo.codeEditor.HighlighterIterator;
import consulo.language.ast.IElementType;
import consulo.language.psi.PsiElement;
import consulo.language.util.ProcessingContext;
import consulo.virtualFileSystem.fileType.FileType;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Set;

import static consulo.language.pattern.PlatformPatterns.psiElement;
import static consulo.language.pattern.PlatformPatterns.psiFile;
import static consulo.language.pattern.StandardPatterns.instanceOf;

/**
 * @author yole
 */
@ExtensionImpl
public class CustomFileTypeCompletionContributor extends CompletionContributor implements DumbAware {
  public CustomFileTypeCompletionContributor() {
    extend(CompletionType.BASIC, psiElement().inFile(psiFile().withFileType(instanceOf(CustomSyntaxTableFileType.class))), new CompletionProvider() {
      @RequiredReadAction
      @Override
      public void addCompletions(@Nonnull CompletionParameters parameters, ProcessingContext context, @Nonnull CompletionResultSet result) {
        if (inCommentOrLiteral(parameters)) {
          return;
        }

        FileType fileType = parameters.getOriginalFile().getFileType();
        if (!(fileType instanceof CustomSyntaxTableFileType)) {
          return;
        }

        SyntaxTable syntaxTable = ((CustomSyntaxTableFileType)fileType).getSyntaxTable();
        String prefix = findPrefix(parameters.getPosition(), parameters.getOffset());
        CompletionResultSet resultSetWithPrefix = result.withPrefixMatcher(prefix);

        addVariants(resultSetWithPrefix, syntaxTable.getKeywords1());
        addVariants(resultSetWithPrefix, syntaxTable.getKeywords2());
        addVariants(resultSetWithPrefix, syntaxTable.getKeywords3());
        addVariants(resultSetWithPrefix, syntaxTable.getKeywords4());

        WordCompletionContributor.addWordCompletionVariants(resultSetWithPrefix, parameters, Collections.<String>emptySet());
      }
    });
  }

  private static boolean inCommentOrLiteral(CompletionParameters parameters) {
    HighlighterIterator iterator = ((EditorEx)parameters.getEditor()).getHighlighter().createIterator(parameters.getOffset());
    IElementType elementType = (IElementType)iterator.getTokenType();
    if (elementType == CustomHighlighterTokenType.WHITESPACE) {
      iterator.retreat();
      elementType = (IElementType)iterator.getTokenType();
    }
    return elementType == CustomHighlighterTokenType.LINE_COMMENT ||
           elementType == CustomHighlighterTokenType.MULTI_LINE_COMMENT ||
           elementType == CustomHighlighterTokenType.STRING ||
           elementType == CustomHighlighterTokenType.SINGLE_QUOTED_STRING;
  }

  private static void addVariants(CompletionResultSet resultSet, Set<String> keywords) {
    for (String keyword : keywords) {
      resultSet.addElement(LookupElementBuilder.create(keyword).bold());
    }
  }

  @RequiredReadAction
  private static String findPrefix(PsiElement insertedElement, int offset) {
    String text = insertedElement.getText();
    int offsetInElement = offset - insertedElement.getTextOffset();
    int start = offsetInElement - 1;
    while (start >= 0) {
      if (!Character.isJavaIdentifierStart(text.charAt(start))) break;
      --start;
    }
    return text.substring(start + 1, offsetInElement).trim();
  }

  @Nonnull
  @Override
  public Language getLanguage() {
    return Language.ANY;
  }
}
