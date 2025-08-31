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

package consulo.language.editor.highlight;

import consulo.codeEditor.HighlighterIterator;
import consulo.language.PairedBraceMatcher;
import consulo.language.ast.IElementType;
import consulo.language.psi.PsiFile;
import consulo.virtualFileSystem.fileType.FileType;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @see PairedBraceMatcher simple implementation of paired tokens per language
 * @see LanguageBraceMatcher for registration brace matcher per language
 * @see VirtualFileBraceMatcher for registration brace matcher per file by file type
 * @see XmlAwareBraceMatcher
 * @see NontrivialBraceMatcher
 */
public interface BraceMatcher {
  int getBraceTokenGroupId(IElementType tokenType);

  boolean isLBraceToken(HighlighterIterator iterator, CharSequence fileText, FileType fileType);

  boolean isRBraceToken(HighlighterIterator iterator, CharSequence fileText, FileType fileType);

  boolean isPairBraces(IElementType tokenType, IElementType tokenType2);

  boolean isStructuralBrace(HighlighterIterator iterator, CharSequence text, FileType fileType);

  @Nullable
  IElementType getOppositeBraceTokenType(@Nonnull IElementType type);

  boolean isPairedBracesAllowedBeforeType(@Nonnull IElementType lbraceType, @Nullable IElementType contextType);

  /**
   * Returns the start offset of the code construct which owns the opening structural brace at the specified offset. For example,
   * if the opening brace belongs to an 'if' statement, returns the start offset of the 'if' statement.
   *
   * @param file               the file in which brace matching is performed.
   * @param openingBraceOffset the offset of an opening structural brace.
   * @return the offset of corresponding code construct, or the same offset if not defined.
   */
  int getCodeConstructStart(PsiFile file, int openingBraceOffset);
}
