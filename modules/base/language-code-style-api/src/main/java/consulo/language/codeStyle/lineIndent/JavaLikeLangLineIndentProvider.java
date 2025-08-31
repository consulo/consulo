/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.language.codeStyle.lineIndent;

import consulo.document.Document;
import consulo.language.Language;
import consulo.language.ast.IElementType;
import consulo.language.codeStyle.CodeStyle;
import consulo.language.codeStyle.CommonCodeStyleSettings;
import consulo.language.codeStyle.Indent;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.Contract;

import static consulo.language.codeStyle.Indent.Type;
import static consulo.language.codeStyle.Indent.Type.*;
import static consulo.language.codeStyle.lineIndent.JavaLikeLangLineIndentProvider.JavaLikeElement.*;

/**
 * A base class for Java-like language line indent provider.
 * If a LineIndentProvider is not provided, {@link FormatterBasedLineIndentProvider} is used.
 * If a registered provider is unable to calculate the indentation,
 * {@link FormatterBasedIndentAdjuster} will be used.
 */
public abstract class JavaLikeLangLineIndentProvider implements LineIndentProvider {

  public enum JavaLikeElement implements SemanticEditorPosition.SyntaxElement {
    Whitespace,
    Semicolon,
    BlockOpeningBrace,
    BlockClosingBrace,
    ArrayOpeningBracket,
    ArrayClosingBracket,
    RightParenthesis,
    LeftParenthesis,
    Colon,
    SwitchCase,
    SwitchDefault,
    ElseKeyword,
    IfKeyword,
    ForKeyword,
    TryKeyword,
    DoKeyword,
    BlockComment,
    DocBlockStart,
    DocBlockEnd,
    LineComment,
    Comma,
    LanguageStartDelimiter
  }


  @Nullable
  @Override
  public String getLineIndent(@Nonnull Project project,
                              @Nonnull Document document,
                              @Nonnull SemanticEditorPositionFactory factory,
                              Language language,
                              int offset) {
    if (offset > 0) {
      IndentCalculator indentCalculator = getIndent(project, document, factory, language, offset - 1);
      if (indentCalculator != null) {
        return indentCalculator.getIndentString(language, getPosition(factory, offset - 1));
      }
    }
    else {
      return "";
    }
    return null;
  }

  @Nullable
  protected IndentCalculator getIndent(@Nonnull Project project,
                                       @Nonnull Document document,
                                       @Nonnull SemanticEditorPositionFactory factory,
                                       @Nullable Language language,
                                       int offset) {
    IndentCalculatorFactory myFactory = new IndentCalculatorFactory(project, document);
    if (getPosition(factory, offset).matchesRule(position -> position.isAt(Whitespace) && position.isAtMultiline())) {
      if (getPosition(factory, offset).before().isAt(Comma)) {
        SemanticEditorPosition position = getPosition(factory, offset);
        if (position.hasEmptyLineAfter(offset) &&
          !position.after()
                   .matchesRule(p -> p.isAtAnyOf(ArrayClosingBracket,
                                                 BlockOpeningBrace,
                                                 BlockClosingBrace,
                                                 RightParenthesis) || p.isAtEnd()) &&
          position.findLeftParenthesisBackwardsSkippingNestedWithPredicate(LeftParenthesis,
                                                                           RightParenthesis,
                                                                           self -> self.isAtAnyOf(BlockClosingBrace,
                                                                                                  BlockOpeningBrace,
                                                                                                  Semicolon))
                  .isAt(LeftParenthesis)) {
          return myFactory.createIndentCalculator(NONE, IndentCalculator.LINE_AFTER);
        }
      }
      else if (afterOptionalWhitespaceOnSameLine(factory,
                                                 offset).matchesRule(position -> position.isAt(BlockClosingBrace) && !position.after()
                                                                                                                              .afterOptional(
                                                                                                                                Whitespace)
                                                                                                                              .isAt(Comma))) {
        return myFactory.createIndentCalculator(NONE, position -> {
          position.moveToLeftParenthesisBackwardsSkippingNested(BlockOpeningBrace, BlockClosingBrace);
          if (!position.isAtEnd()) {
            return getBlockStatementStartOffset(factory, position);
          }
          return -1;
        });
      }
      else if (getPosition(factory, offset).beforeOptional(Whitespace).isAt(BlockClosingBrace)) {
        return myFactory.createIndentCalculator(getBlockIndentType(project, document, language), IndentCalculator.LINE_BEFORE);
      }
      else if (getPosition(factory, offset).before().isAt(Semicolon)) {
        SemanticEditorPosition beforeSemicolon = getPosition(factory, offset).before().beforeOptional(Semicolon);
        if (beforeSemicolon.isAt(BlockClosingBrace)) {
          beforeSemicolon.moveBeforeParentheses(BlockOpeningBrace, BlockClosingBrace);
        }
        int statementStart = getStatementStartOffset(factory, beforeSemicolon, dropIndentAfterReturnLike(beforeSemicolon));
        SemanticEditorPosition atStatementStart = getPosition(factory, statementStart);
        if (atStatementStart.isAt(BlockOpeningBrace)) {
          return myFactory.createIndentCalculator(getIndentInBlock(project, document, language, atStatementStart),
                                                  (position) -> getDeepBlockStatementStartOffset(factory, position));
        }
        if (!isInsideForLikeConstruction(atStatementStart)) {
          return myFactory.createIndentCalculator(NONE, position -> statementStart);
        }
      }
      else if (isInArray(factory, offset)) {
        return myFactory.createIndentCalculator(getIndentInBrackets(), IndentCalculator.LINE_BEFORE);
      }
      else if (getPosition(factory, offset).before().isAt(LeftParenthesis)) {
        return myFactory.createIndentCalculator(CONTINUATION, IndentCalculator.LINE_BEFORE);
      }
      else if (getPosition(factory, offset).matchesRule(position -> {
        moveBeforeEndLineComments(position);
        if (position.isAt(BlockOpeningBrace)) {
          return !position.before().beforeOptionalMix(LineComment, BlockComment, Whitespace).isAt(LeftParenthesis);
        }
        return false;
      })) {
        SemanticEditorPosition position = getPosition(factory, offset).before().beforeOptionalMix(LineComment, BlockComment, Whitespace);
        return myFactory.createIndentCalculator(getIndentInBlock(project, document, language, position),
                                                it -> getBlockStatementStartOffset(factory, it));
      }
      else if (getPosition(factory, offset).before()
                                           .matchesRule(position -> isColonAfterLabelOrCase(position, factory) || position.isAtAnyOf(
                                             ElseKeyword,
                                             DoKeyword))) {
        return myFactory.createIndentCalculator(NORMAL, IndentCalculator.LINE_BEFORE);
      }
      else if (getPosition(factory, offset).matchesRule(position -> {
        position.moveBefore();
        if (position.isAt(BlockComment)) {
          return position.before().isAt(Whitespace) && position.isAtMultiline();
        }
        return false;
      })) {
        return myFactory.createIndentCalculator(NONE, position -> position.findStartOf(BlockComment));
      }
      else if (getPosition(factory, offset).before().isAt(DocBlockEnd)) {
        return myFactory.createIndentCalculator(NONE, position -> position.findStartOf(DocBlockStart));
      }
      else {
        SemanticEditorPosition position = getPosition(factory, offset);
        position = position.before().beforeOptionalMix(LineComment, BlockComment, Whitespace);
        if (position.isAt(RightParenthesis)) {
          int offsetAfterParen = position.getStartOffset() + 1;
          position.moveBeforeParentheses(LeftParenthesis, RightParenthesis);
          if (!position.isAtEnd()) {
            position.moveBeforeOptional(Whitespace);
            if (position.isAt(IfKeyword) || position.isAt(ForKeyword)) {
              SemanticEditorPosition.SyntaxElement element = position.getCurrElement();
              assert element != null;
              int controlKeywordOffset = position.getStartOffset();
              Type indentType = getPosition(factory, offsetAfterParen).afterOptional(Whitespace).isAt(BlockOpeningBrace) ? NONE : NORMAL;
              return myFactory.createIndentCalculator(indentType, baseLineOffset -> controlKeywordOffset);
            }
          }
        }
      }
    }
    //return myFactory.createIndentCalculator(NONE, IndentCalculator.LINE_BEFORE); /* TO CHECK UNCOVERED CASES */
    return null;
  }

  private SemanticEditorPosition afterOptionalWhitespaceOnSameLine(@Nonnull SemanticEditorPositionFactory factory, int offset) {
    SemanticEditorPosition position = getPosition(factory, offset);
    if (position.isAt(Whitespace)) {
      if (position.hasLineBreaksAfter(offset)) return position;
      position.moveAfter();
    }
    return position;
  }

  /**
   * Checks that the current offset is inside array. By default it is assumed to be after opening array bracket
   * but can be overridden for more complicated logic, for example, the following case in Java: []{&lt;caret&gt;}.
   *
   * @param editor The editor.
   * @param offset The current offset in the editor.
   * @return {@code true} if the position is inside array.
   */
  protected boolean isInArray(@Nonnull SemanticEditorPositionFactory factory, int offset) {
    return getPosition(factory, offset).before().isAt(ArrayOpeningBracket);
  }

  /**
   * Checking the document context in position for return-like token (i.e. {@code return}, {@code break}, {@code continue}),
   * after that we need to reduce the indent (for example after {@code break;} in {@code switch} statement).
   *
   * @param statementBeforeSemicolon position in the document context
   * @return true, if need to reduce the indent
   */
  protected boolean dropIndentAfterReturnLike(@Nonnull SemanticEditorPosition statementBeforeSemicolon) {
    return false;
  }

  protected boolean isColonAfterLabelOrCase(@Nonnull SemanticEditorPosition position, SemanticEditorPositionFactory factory) {
    return position.isAt(Colon) && getPosition(factory, position.getStartOffset()).isAfterOnSameLine(SwitchCase,
                                                                                                     SwitchDefault);
  }

  protected boolean isInsideForLikeConstruction(SemanticEditorPosition position) {
    return position.isAfterOnSameLine(ForKeyword);
  }

  /**
   * Returns the start offset of the statement or new-line-'{' that owns the code block in {@code position}.
   * <p>
   * Custom implementation for language can overwrite the default behavior for multi-lines statements like
   * <pre>{@code
   *    template<class T>
   *    class A {};
   * }</pre>
   * or check indentation after new-line-'{' vs the brace style.
   *
   * @param factory
   * @param position the position in the code block
   */
  protected int getBlockStatementStartOffset(@Nonnull SemanticEditorPositionFactory factory,
                                             @Nonnull SemanticEditorPosition position) {
    moveBeforeEndLineComments(position);
    position.moveBeforeOptional(BlockOpeningBrace);
    if (position.isAt(Whitespace)) {
      if (position.isAtMultiline()) {
        return position.after().getStartOffset();
      }
      position.moveBefore();
    }
    return getStatementStartOffset(factory, position, false);
  }

  private static void moveBeforeEndLineComments(@Nonnull SemanticEditorPosition position) {
    position.moveBefore();
    while (!position.isAtMultiline() && position.isAtAnyOf(LineComment, BlockComment, Whitespace)) {
      position.moveBefore();
    }
  }

  /**
   * Returns the start offset of the statement that owns the code block in {@code position}
   *
   * @param factory
   * @param position the position in the code block
   */
  protected int getDeepBlockStatementStartOffset(SemanticEditorPositionFactory factory, @Nonnull SemanticEditorPosition position) {
    position.moveToLeftParenthesisBackwardsSkippingNested(BlockOpeningBrace, BlockClosingBrace);
    return getBlockStatementStartOffset(factory, position);
  }

  private int getStatementStartOffset(@Nonnull SemanticEditorPositionFactory factory,
                                      @Nonnull SemanticEditorPosition position,
                                      boolean ignoreLabels) {
    Language currLanguage = position.getLanguage();
    while (!position.isAtEnd()) {
      if (currLanguage == Language.ANY || currLanguage == null) currLanguage = position.getLanguage();
      if (!ignoreLabels && isColonAfterLabelOrCase(position, factory)) {
        SemanticEditorPosition afterColon =
          getPosition(factory, position.getStartOffset()).afterOptionalMix(Whitespace, BlockComment)
                                                         .after()
                                                         .afterOptionalMix(Whitespace, LineComment);
        return afterColon.getStartOffset();
      }
      else if (position.isAt(RightParenthesis)) {
        position.moveBeforeParentheses(LeftParenthesis, RightParenthesis);
        continue;
      }
      else if (position.isAt(BlockClosingBrace)) {
        position.moveBeforeParentheses(BlockOpeningBrace, BlockClosingBrace);
        continue;
      }
      else if (position.isAt(ArrayClosingBracket)) {
        position.moveBeforeParentheses(ArrayOpeningBracket, ArrayClosingBracket);
        continue;
      }
      else if (isStartOfStatementWithOptionalBlock(position)) {
        return position.getStartOffset();
      }
      else if (position.isAtAnyOf(Semicolon, BlockOpeningBrace, BlockComment, DocBlockEnd, LeftParenthesis, LanguageStartDelimiter) ||
        (position.getLanguage() != Language.ANY) && !position.isAtLanguage(currLanguage)) {
        SemanticEditorPosition statementStart = position.copy();
        statementStart = statementStart.after().afterOptionalMix(Whitespace, LineComment);
        if (!isIndentProvider(statementStart, ignoreLabels)) {
          SemanticEditorPosition maybeColon = statementStart.afterOptionalMix(Whitespace, BlockComment).after();
          SemanticEditorPosition afterColonStatement = maybeColon.after().after();
          if (atColonWithNewLineAfterColonStatement(maybeColon, afterColonStatement)) {
            return afterColonStatement.getStartOffset();
          }
          if (atBlockStartAndNeedBlockIndent(position)) {
            return position.getStartOffset();
          }
        }
        else if (!statementStart.isAtEnd()) {
          return statementStart.getStartOffset();
        }
      }
      position.moveBefore();
    }
    return 0;
  }

  /**
   * Returns {@code true} if the {@code position} starts a statement that <i>can</i> have a code block and the statement
   * is the first in the code line.
   * In C-like languages it is one of {@code if, else, for, while, do, try}.
   *
   * @param position
   */
  protected boolean isStartOfStatementWithOptionalBlock(@Nonnull SemanticEditorPosition position) {
    return position.matchesRule(self -> {
      SemanticEditorPosition before = self.before();
      return before.isAt(Whitespace) && before.isAtMultiline() && self.isAtAnyOf(ElseKeyword, IfKeyword, ForKeyword, TryKeyword, DoKeyword);
    });
  }

  private static boolean atBlockStartAndNeedBlockIndent(@Nonnull SemanticEditorPosition position) {
    return position.isAt(BlockOpeningBrace);
  }

  private static boolean atColonWithNewLineAfterColonStatement(@Nonnull SemanticEditorPosition maybeColon,
                                                               @Nonnull SemanticEditorPosition afterColonStatement) {
    return maybeColon.isAt(Colon) && maybeColon.after().isAtMultiline(Whitespace) && !afterColonStatement.isAtEnd();
  }

  /**
   * Checking the document context in position as indent-provider.
   *
   * @param statementStartPosition position is the document
   * @param ignoreLabels           {@code true}, if labels cannot be used as indent-providers in the context.
   * @return {@code true}, if statement is indent-provider (by default)
   */
  protected boolean isIndentProvider(@Nonnull SemanticEditorPosition statementStartPosition, boolean ignoreLabels) {
    return true;
  }

  /**
   * Returns abstract semantic position in {@code editor} for indent calculation.
   *
   * @param factory factory for position
   * @param offset  the offset in the {@code editor}
   */
  @Nonnull
  public SemanticEditorPosition getPosition(@Nonnull SemanticEditorPositionFactory factory, int offset) {
    return factory.create(offset, this::mapType);
  }

  @Nullable
  protected abstract SemanticEditorPosition.SyntaxElement mapType(@Nonnull IElementType tokenType);


  @Nullable
  protected Indent getIndentInBlock(@Nonnull Project project,
                                    @Nonnull Document document, @Nullable Language language,
                                    @Nonnull SemanticEditorPosition blockStartPosition) {
    if (language != null) {
      CommonCodeStyleSettings settings =
        CodeStyle.getSettings(project, document).getCommonSettings(language);
      if (settings.BRACE_STYLE == CommonCodeStyleSettings.NEXT_LINE_SHIFTED) {
        return getDefaultIndentFromType(settings.METHOD_BRACE_STYLE == CommonCodeStyleSettings.NEXT_LINE_SHIFTED ? NONE : null);
      }
    }
    return getDefaultIndentFromType(NORMAL);
  }

  @Contract("_, null -> null")
  private static Type getBlockIndentType(@Nonnull Project project, @Nonnull Document document, @Nullable Language language) {
    if (language != null) {
      CommonCodeStyleSettings settings = CodeStyle.getSettings(project, document).getCommonSettings(language);
      if (settings.BRACE_STYLE == CommonCodeStyleSettings.NEXT_LINE || settings.BRACE_STYLE == CommonCodeStyleSettings.END_OF_LINE) {
        return NONE;
      }
    }
    return null;
  }

  @Contract("null -> null")
  protected static Indent getDefaultIndentFromType(@Nullable Type type) {
    return type == null ? null : Indent.getIndent(type, 0, false, false);
  }

  public static class IndentCalculatorFactory {
    private final Project myProject;
    private final Document myDocument;

    public IndentCalculatorFactory(Project project, Document document) {
      myProject = project;
      myDocument = document;
    }

    @Nullable
    public IndentCalculator createIndentCalculator(@Nullable Type indentType,
                                                   @Nullable IndentCalculator.BaseLineOffsetCalculator baseLineOffsetCalculator) {
      return createIndentCalculator(getDefaultIndentFromType(indentType), baseLineOffsetCalculator);
    }

    @Nullable
    public IndentCalculator createIndentCalculator(@Nullable Indent indent,
                                                   @Nullable IndentCalculator.BaseLineOffsetCalculator baseLineOffsetCalculator) {
      return indent != null ? new IndentCalculator(myProject,
                                                   myDocument,
                                                   baseLineOffsetCalculator != null ? baseLineOffsetCalculator : IndentCalculator.LINE_BEFORE,
                                                   indent) : null;
    }
  }

  @Override
  @Contract("null -> false")
  public final boolean isSuitableFor(@Nullable Language language) {
    return language != null && isSuitableForLanguage(language);
  }

  public abstract boolean isSuitableForLanguage(@Nonnull Language language);

  protected Type getIndentTypeInBrackets() {
    return CONTINUATION;
  }

  protected Indent getIndentInBrackets() {
    return getDefaultIndentFromType(getIndentTypeInBrackets());
  }
}
