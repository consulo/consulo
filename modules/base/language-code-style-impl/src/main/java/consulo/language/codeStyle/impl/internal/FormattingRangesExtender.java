// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.codeStyle.impl.internal;

import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiWhiteSpace;
import consulo.logging.Logger;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.CharArrayUtil;
import consulo.util.lang.ref.Ref;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

@SuppressWarnings("SameParameterValue")
class FormattingRangesExtender {
  private final static Logger LOG = Logger.getInstance(FormattingRangesExtender.class);

  private final static int MAX_EXTENSION_LINES = 10;

  private final Document myDocument;
  private final PsiFile myFile;

  FormattingRangesExtender(@Nonnull Document document, PsiFile file) {
    myDocument = document;
    myFile = file;
  }

  public List<TextRange> getExtendedRanges(@Nonnull List<TextRange> ranges) {
    return ContainerUtil.map(ranges, range -> processRange(range));
  }

  private TextRange processRange(@Nonnull TextRange originalRange) {
    TextRange validRange = ensureRangeIsValid(originalRange);
    ASTNode containingNode = CodeFormatterFacade.findContainingNode(myFile, expandToLine(validRange));
    if (containingNode != null && !validRange.isEmpty()) {
      return narrowToMaxExtensionLines(validRange, getRangeWithSiblings(containingNode));
    }
    return validRange;
  }

  private TextRange narrowToMaxExtensionLines(@Nonnull TextRange original, @Nonnull TextRange result) {
    int startLine = Math.max(myDocument.getLineNumber(result.getStartOffset()), myDocument.getLineNumber(original.getStartOffset()) - MAX_EXTENSION_LINES);
    int endLine = Math.min(myDocument.getLineNumber(result.getEndOffset() - 1), myDocument.getLineNumber(original.getEndOffset() - 1) + MAX_EXTENSION_LINES);
    int rangeStart = Math.max(result.getStartOffset(), myDocument.getLineStartOffset(startLine));
    int rangeEnd = Math.min(result.getEndOffset(), myDocument.getLineEndOffset(endLine));
    return new TextRange(rangeStart, rangeEnd);
  }

  private TextRange ensureRangeIsValid(@Nonnull TextRange range) {
    int startOffset = range.getStartOffset();
    int endOffset = range.getEndOffset();
    final int docLength = myDocument.getTextLength();
    if (endOffset > docLength) {
      LOG.warn("The given range " + endOffset + " exceeds the document length " + docLength);
      return new TextRange(Math.min(startOffset, docLength), docLength);
    }
    return range;
  }

  @Nullable
  private TextRange trimSpaces(@Nonnull TextRange range) {
    int startOffset = range.getStartOffset();
    int endOffset = range.getEndOffset();
    startOffset = CharArrayUtil.shiftForward(myDocument.getCharsSequence(), startOffset, endOffset, " /t");
    if (startOffset == endOffset) return null;
    endOffset = CharArrayUtil.shiftBackward(myDocument.getCharsSequence(), startOffset, endOffset, " /t");
    return new TextRange(startOffset, endOffset);
  }

  private TextRange expandToLine(@Nonnull TextRange range) {
    int line = myDocument.getLineNumber(range.getStartOffset());
    if (line == myDocument.getLineNumber(Math.min(range.getEndOffset(), myDocument.getTextLength()))) {
      int lineStart = myDocument.getLineStartOffset(line);
      int lineEnd = myDocument.getLineEndOffset(line);
      TextRange lineRange = trimSpaces(new TextRange(lineStart, lineEnd));
      if (lineRange != null) {
        return lineRange;
      }
    }
    return range;
  }

  private static TextRange getRangeWithSiblings(@Nonnull ASTNode astNode) {
    Ref<TextRange> result = Ref.create(astNode.getTextRange());
    IElementType elementType = astNode.getElementType();
    ASTNode sibling = astNode.getTreePrev();
    while (sibling != null && processSibling(sibling, result, elementType)) {
      sibling = sibling.getTreePrev();
    }
    sibling = astNode.getTreeNext();
    while (sibling != null && processSibling(sibling, result, elementType)) {
      sibling = sibling.getTreeNext();
    }
    return result.get();
  }

  private static boolean processSibling(@Nonnull ASTNode node, @Nonnull Ref<TextRange> rangeRef, @Nonnull IElementType siblingType) {
    if (node.getPsi() instanceof PsiWhiteSpace) {
      return !hasMinLineBreaks(node, 2);
    }
    else if (node.getElementType() == siblingType) {
      rangeRef.set(rangeRef.get().union(node.getTextRange()));
    }
    return false;
  }

  private static boolean hasMinLineBreaks(@Nonnull ASTNode node, int lineBreaks) {
    int count = 0;
    for (int i = 0; i < node.getChars().length(); i++) {
      if (node.getChars().charAt(i) == '\n') count++;
      if (count >= lineBreaks) return true;
    }
    return false;
  }

}
