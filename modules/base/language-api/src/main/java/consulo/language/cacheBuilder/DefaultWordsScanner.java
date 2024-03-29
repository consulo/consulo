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
package consulo.language.cacheBuilder;

import consulo.application.util.function.Processor;
import consulo.language.ast.IElementType;
import consulo.language.ast.TokenSet;
import consulo.language.lexer.Lexer;

import jakarta.annotation.Nonnull;

/**
 * The default implementation of a words scanner based on a custom language lexer.
 *
 * @author max
 */

public class DefaultWordsScanner implements WordsScanner {
  private final Lexer myLexer;
  private final TokenSet myIdentifierTokenSet;
  private final TokenSet myCommentTokenSet;
  private final TokenSet myLiteralTokenSet;
  private final TokenSet mySkipCodeContextTokenSet;
  private boolean myMayHaveFileRefsInLiterals;

  /**
   * Creates a new instance of the words scanner.
   *
   * @param lexer              the lexer used for breaking the text into tokens.
   * @param identifierTokenSet the set of token types which represent identifiers.
   * @param commentTokenSet    the set of token types which represent comments.
   * @param literalTokenSet    the set of token types which represent literals.
   */
  public DefaultWordsScanner(final Lexer lexer, final TokenSet identifierTokenSet, final TokenSet commentTokenSet,
                             final TokenSet literalTokenSet) {
    this(lexer, identifierTokenSet, commentTokenSet, literalTokenSet, TokenSet.EMPTY);
  }

  /**
   * Creates a new instance of the words scanner.
   *
   * @param lexer              the lexer used for breaking the text into tokens.
   * @param identifierTokenSet the set of token types which represent identifiers.
   * @param commentTokenSet    the set of token types which represent comments.
   * @param literalTokenSet    the set of token types which represent literals.
   * @param SkipCodeContextTokenSet the set of token types which should not be considered as code context.
   */
  public DefaultWordsScanner(final Lexer lexer, final TokenSet identifierTokenSet, final TokenSet commentTokenSet,
                             final TokenSet literalTokenSet, @Nonnull TokenSet skipCodeContextTokenSet) {
    myLexer = lexer;
    myIdentifierTokenSet = identifierTokenSet;
    myCommentTokenSet = commentTokenSet;
    myLiteralTokenSet = literalTokenSet;
    mySkipCodeContextTokenSet = skipCodeContextTokenSet;
  }

  public void processWords(CharSequence fileText, Processor<WordOccurrence> processor) {
    myLexer.start(fileText);
    WordOccurrence occurrence = null; // shared occurrence

    IElementType type;
    while ((type = myLexer.getTokenType()) != null) {
      if (myIdentifierTokenSet.contains(type)) {
        if (occurrence == null) {
          occurrence = new WordOccurrence(fileText, myLexer.getTokenStart(), myLexer.getTokenEnd(), WordOccurrence.Kind.CODE);
        }
        else {
          occurrence.init(fileText, myLexer.getTokenStart(), myLexer.getTokenEnd(), WordOccurrence.Kind.CODE);
        }
        if (!processor.process(occurrence)) return;
      }
      else if (myCommentTokenSet.contains(type)) {
        if (!stripWords(processor, fileText,myLexer.getTokenStart(),myLexer.getTokenEnd(), WordOccurrence.Kind.COMMENTS,occurrence, false)) return;
      }
      else if (myLiteralTokenSet.contains(type)) {
        if (!stripWords(processor, fileText, myLexer.getTokenStart(),myLexer.getTokenEnd(),WordOccurrence.Kind.LITERALS,occurrence, myMayHaveFileRefsInLiterals)) return;
      }
      else if (!mySkipCodeContextTokenSet.contains(type)) {
        if (!stripWords(processor, fileText, myLexer.getTokenStart(), myLexer.getTokenEnd(), WordOccurrence.Kind.CODE, occurrence, false)) return;
      }
      myLexer.advance();
    }
  }

  protected static boolean stripWords(final Processor<WordOccurrence> processor,
                                    final CharSequence tokenText,
                                    int from,
                                    int to,
                                    final WordOccurrence.Kind kind,
                                    WordOccurrence occurence,
                                    boolean mayHaveFileRefs
  ) {
    // This code seems strange but it is more effective as Character.isJavaIdentifier_xxx_ is quite costly operation due to unicode
    int index = from;

    ScanWordsLoop:
    while (true) {
      while (true) {
        if (index == to) break ScanWordsLoop;
        char c = tokenText.charAt(index);
        if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') ||
            (c != '$' && Character.isJavaIdentifierStart(c))) {
          break;
        }
        index++;
      }
      int wordStart = index;
      while (true) {
        index++;
        if (index == to) break;
        char c = tokenText.charAt(index);
        if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')) continue;
        if (c == '$' || !Character.isJavaIdentifierPart(c)) break;
      }
      int wordEnd = index;
      if (occurence == null) {
        occurence = new WordOccurrence(tokenText, wordStart, wordEnd, kind);
      }
      else {
        occurence.init(tokenText, wordStart, wordEnd, kind);
      }

      if (!processor.process(occurence)) return false;

      if (mayHaveFileRefs) {
        occurence.init(tokenText,wordStart, wordEnd, WordOccurrence.Kind.FOREIGN_LANGUAGE);
        if (!processor.process(occurence)) return false;
      }
    }
    return true;
  }

  public void setMayHaveFileRefsInLiterals(final boolean mayHaveFileRefsInLiterals) {
    myMayHaveFileRefsInLiterals = mayHaveFileRefsInLiterals;
  }
}
