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
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 25/06/2023
 */
public interface SemanticEditorPosition {
  public interface SyntaxElement {
  }

  @Nullable Language getLanguage();

  void moveToLeftParenthesisBackwardsSkippingNested(SyntaxElement leftParenthesis, SyntaxElement rightParenthesis);

  SemanticEditorPosition beforeParentheses(SyntaxElement leftParenthesis, SyntaxElement rightParenthesis);

  SemanticEditorPosition findLeftParenthesisBackwardsSkippingNested(SyntaxElement leftParenthesis,
                                                                    SyntaxElement rightParenthesis);

  void moveToLeftParenthesisBackwardsSkippingNestedWithPredicate(SyntaxElement leftParenthesis,
                                                                 SyntaxElement rightParenthesis,
                                                                 Predicate<SemanticEditorPosition> terminationCondition);

  boolean isAt(SyntaxElement syntaxElement);

  boolean isAt(IElementType elementType);

  boolean isAtEnd();

  int getStartOffset();

  boolean isAtAnyOf(SyntaxElement... syntaxElements);

  CharSequence getChars();

  boolean isAfterOnSameLine(SyntaxElement... syntaxElements);

  @Nullable SyntaxElement elementAfterOnSameLine(SyntaxElement... syntaxElements);

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

  void moveAfterOptional(SyntaxElement syntaxElement);

  SemanticEditorPosition afterOptional(SyntaxElement syntaxElement);

  void moveAfter();

  SemanticEditorPosition after();

  void moveBeforeParentheses(SyntaxElement leftParenthesis, SyntaxElement rightParenthesis);

  SemanticEditorPosition findLeftParenthesisBackwardsSkippingNestedWithPredicate(SyntaxElement leftParenthesis,
                                                                                 SyntaxElement rightParenthesis,
                                                                                 Predicate<SemanticEditorPosition> terminationCondition);

  boolean matchesRule(Predicate<SemanticEditorPosition> rule);

  SyntaxElement map(IElementType elementType);

  @Nullable SyntaxElement getCurrElement();

  boolean hasEmptyLineAfter(int offset);

  int findStartOf(SyntaxElement element);

  boolean isAtLanguage(@Nullable Language language);

  SemanticEditorPosition copy();

  SemanticEditorPosition copyAnd(Consumer<SemanticEditorPosition> modifier);

  void moveAfterOptionalMix(SyntaxElement... elements);

  void moveBeforeOptionalMix(SyntaxElement... elements);

  void moveBeforeOptional(SyntaxElement syntaxElement);

  void moveBefore();

  SemanticEditorPosition beforeOptionalMix(SyntaxElement... elements);

  SemanticEditorPosition beforeOptional(SyntaxElement syntaxElement);

  SemanticEditorPosition afterOptionalMix(SyntaxElement... elements);
}
