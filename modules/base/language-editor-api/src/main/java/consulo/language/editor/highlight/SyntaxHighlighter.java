/*
 * Copyright 2000-2010 JetBrains s.r.o.
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

import consulo.colorScheme.TextAttributesKey;
import consulo.language.Language;
import consulo.language.ast.IElementType;
import consulo.language.lexer.Lexer;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;

/**
 * Controls the syntax highlighting of a file.
 *
 * @see SyntaxHighlighterFactory#getSyntaxHighlighter(Project, VirtualFile)
 * @see SyntaxHighlighterFactory#getSyntaxHighlighter(Language, Project, VirtualFile)
 */
public interface SyntaxHighlighter {
  /**
   * Returns the lexer used for highlighting the file. The lexer is invoked incrementally when the file is changed, so it must be
   * capable of saving/restoring state and resuming lexing from the middle of the file.
   *
   * @return The lexer implementation.
   */
  @Nonnull
  Lexer getHighlightingLexer();

  /**
   * Returns the list of text attribute keys used for highlighting the specified token type. The attributes of all attribute keys
   * returned for the token type are successively merged to obtain the color and attributes of the token.
   *
   * @param tokenType The token type for which the highlighting is requested.
   * @return The array of text attribute keys.
   */
  @Nonnull
  TextAttributesKey[] getTokenHighlights(@Nonnull IElementType tokenType);
}