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

package consulo.ide.impl.idea.ide.highlighter.custom.impl;

import consulo.codeEditor.Editor;
import consulo.codeEditor.HighlighterIterator;
import consulo.document.Document;
import consulo.ide.impl.idea.openapi.fileTypes.impl.AbstractFileType;
import consulo.language.internal.custom.CustomHighlighterTokenType;
import consulo.language.ast.IElementType;
import consulo.language.editor.action.FileQuoteHandler;
import consulo.virtualFileSystem.fileType.FileType;

import jakarta.annotation.Nonnull;

/**
 * @author Maxim.Mossienko
 */
class CustomFileTypeQuoteHandler implements FileQuoteHandler {
  private final AbstractFileType myFileType;

  CustomFileTypeQuoteHandler(AbstractFileType fileType) {
    myFileType = fileType;
  }

  @Override
  public boolean isClosingQuote(HighlighterIterator iterator, int offset) {
    IElementType tokenType = (IElementType)iterator.getTokenType();

    if (tokenType == CustomHighlighterTokenType.STRING || tokenType == CustomHighlighterTokenType.SINGLE_QUOTED_STRING || tokenType == CustomHighlighterTokenType.CHARACTER) {
      int start = iterator.getStart();
      int end = iterator.getEnd();
      return end - start >= 1 && offset == end - 1;
    }
    return false;
  }

  @Override
  public boolean isOpeningQuote(HighlighterIterator iterator, int offset) {
    IElementType tokenType = (IElementType)iterator.getTokenType();

    if (tokenType == CustomHighlighterTokenType.STRING || tokenType == CustomHighlighterTokenType.SINGLE_QUOTED_STRING || tokenType == CustomHighlighterTokenType.CHARACTER) {
      int start = iterator.getStart();
      return offset == start;
    }
    return false;
  }

  @Override
  public boolean hasNonClosedLiteral(Editor editor, HighlighterIterator iterator, int offset) {
    try {
      Document doc = editor.getDocument();
      CharSequence chars = doc.getCharsSequence();
      int lineEnd = doc.getLineEndOffset(doc.getLineNumber(offset));

      while (!iterator.atEnd() && iterator.getStart() < lineEnd) {
        IElementType tokenType = (IElementType)iterator.getTokenType();

        if (tokenType == CustomHighlighterTokenType.STRING || tokenType == CustomHighlighterTokenType.SINGLE_QUOTED_STRING || tokenType == CustomHighlighterTokenType.CHARACTER) {

          if (iterator.getStart() >= iterator.getEnd() - 1 || chars.charAt(iterator.getEnd() - 1) != '\"' && chars.charAt(iterator.getEnd() - 1) != '\'') {
            return true;
          }
        }
        iterator.advance();
      }
    }
    finally {
      while (!iterator.atEnd() && iterator.getStart() != offset) iterator.retreat();
    }

    return false;
  }

  @Override
  public boolean isInsideLiteral(HighlighterIterator iterator) {
    IElementType tokenType = (IElementType)iterator.getTokenType();

    return tokenType == CustomHighlighterTokenType.STRING || tokenType == CustomHighlighterTokenType.SINGLE_QUOTED_STRING || tokenType == CustomHighlighterTokenType.CHARACTER;
  }

  @Nonnull
  @Override
  public FileType getFileType() {
    return myFileType;
  }
}
