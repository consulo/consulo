// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.language.editor.todo.impl.internal.search;

import consulo.document.util.TextRange;
import consulo.language.psi.PsiFile;
import consulo.language.psi.search.TodoItem;
import consulo.language.psi.search.TodoPattern;
import jakarta.annotation.Nonnull;

import java.util.List;

public class TodoItemImpl implements TodoItem {
  private final PsiFile myFile;
  private final int myStartOffset;
  private final int myEndOffset;
  private final TodoPattern myPattern;
  private final List<TextRange> myAdditionalRanges;

  public TodoItemImpl(@Nonnull PsiFile file, int startOffset, int endOffset, @Nonnull TodoPattern pattern, @Nonnull List<TextRange> additionalRanges) {
    myFile = file;
    myStartOffset = startOffset;
    myEndOffset = endOffset;
    myPattern = pattern;
    myAdditionalRanges = additionalRanges;
  }

  @Nonnull
  @Override
  public PsiFile getFile() {
    return myFile;
  }

  @Nonnull
  @Override
  public TextRange getTextRange() {
    return new TextRange(myStartOffset, myEndOffset);
  }

  @Nonnull
  @Override
  public List<TextRange> getAdditionalTextRanges() {
    return myAdditionalRanges;
  }

  @Nonnull
  @Override
  public TodoPattern getPattern() {
    return myPattern;
  }

  public int hashCode() {
    return myFile.hashCode() + myStartOffset + myEndOffset + myPattern.hashCode();
  }

  public boolean equals(Object obj) {
    if (!(obj instanceof TodoItemImpl)) {
      return false;
    }
    TodoItemImpl todoItem = (TodoItemImpl)obj;
    return myFile.equals(todoItem.myFile) && myStartOffset == todoItem.myStartOffset && myEndOffset == todoItem.myEndOffset && myPattern.equals(todoItem.myPattern);
  }
}
