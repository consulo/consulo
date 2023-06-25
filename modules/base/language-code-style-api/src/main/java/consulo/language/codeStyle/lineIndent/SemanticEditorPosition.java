/*
 * Copyright 2013-2023 consulo.io
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

import consulo.language.Language;
import consulo.language.ast.IElementType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 25/06/2023
 */
public interface SemanticEditorPosition {
  public interface SyntaxElement {
  }

  @Nullable
  Language getLanguage();

  void moveToLeftParenthesisBackwardsSkippingNested(@Nonnull SyntaxElement leftParenthesis, @Nonnull SyntaxElement rightParenthesis);

  SemanticEditorPosition beforeParentheses(@Nonnull SyntaxElement leftParenthesis, @Nonnull SyntaxElement rightParenthesis);

  SemanticEditorPosition findLeftParenthesisBackwardsSkippingNested(@Nonnull SyntaxElement leftParenthesis,
                                                                    @Nonnull SyntaxElement rightParenthesis);

  void moveToLeftParenthesisBackwardsSkippingNestedWithPredicate(@Nonnull SyntaxElement leftParenthesis,
                                                                 @Nonnull SyntaxElement rightParenthesis,
                                                                 @Nonnull Predicate<SemanticEditorPosition> terminationCondition);

  boolean isAt(@Nonnull SyntaxElement syntaxElement);

  boolean isAt(@Nonnull IElementType elementType);

  boolean isAtEnd();

  int getStartOffset();

  boolean isAtAnyOf(@Nonnull SyntaxElement... syntaxElements);

  CharSequence getChars();

  boolean isAfterOnSameLine(@Nonnull SyntaxElement... syntaxElements);

  @Nullable
  SyntaxElement elementAfterOnSameLine(@Nonnull SyntaxElement... syntaxElements);

  boolean isAtMultiline(SyntaxElement... elements);

  boolean isAtMultiline();

  /**
   * Checks if there are line breaks strictly after the given offset till the end of the current element.
   *
   * @param offset The offset to search line breaks after.
   * @return True if there are line breaks after the given offset.
   */
  boolean hasLineBreaksAfter(int offset);

  SemanticEditorPosition before();

  void moveAfterOptional(@Nonnull SyntaxElement syntaxElement);

  SemanticEditorPosition afterOptional(@Nonnull SyntaxElement syntaxElement);

  void moveAfter();

  SemanticEditorPosition after();

  void moveBeforeParentheses(@Nonnull SyntaxElement leftParenthesis, @Nonnull SyntaxElement rightParenthesis);

  SemanticEditorPosition findLeftParenthesisBackwardsSkippingNestedWithPredicate(@Nonnull SyntaxElement leftParenthesis,
                                                                                 @Nonnull SyntaxElement rightParenthesis,
                                                                                 @Nonnull Predicate<SemanticEditorPosition> terminationCondition);

  boolean matchesRule(@Nonnull Predicate<SemanticEditorPosition> rule);

  SyntaxElement map(@Nonnull IElementType elementType);

  @Nullable
  SyntaxElement getCurrElement();

  boolean hasEmptyLineAfter(int offset);

  int findStartOf(@Nonnull SyntaxElement element);

  boolean isAtLanguage(@Nullable Language language);

  SemanticEditorPosition copy();

  SemanticEditorPosition copyAnd(@Nonnull Consumer<SemanticEditorPosition> modifier);

  void moveAfterOptionalMix(@Nonnull SyntaxElement... elements);

  void moveBeforeOptionalMix(@Nonnull SyntaxElement... elements);

  void moveBeforeOptional(@Nonnull SyntaxElement syntaxElement);

  void moveBefore();

  SemanticEditorPosition beforeOptionalMix(@Nonnull SyntaxElement... elements);

  SemanticEditorPosition beforeOptional(@Nonnull SyntaxElement syntaxElement);

  SemanticEditorPosition afterOptionalMix(@Nonnull SyntaxElement... elements);
}
