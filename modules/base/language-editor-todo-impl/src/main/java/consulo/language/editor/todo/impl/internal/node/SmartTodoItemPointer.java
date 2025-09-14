// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.language.editor.todo.impl.internal.node;

import consulo.document.Document;
import consulo.document.RangeMarker;
import consulo.document.util.TextRange;
import consulo.language.psi.search.TodoItem;
import consulo.util.collection.ContainerUtil;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * @author Vladimir Kondratyev
 */
public final class SmartTodoItemPointer {
  private final TodoItem myTodoItem;
  private final Document myDocument;
  private final RangeMarker myRangeMarker;
  private final List<RangeMarker> myAdditionalRangeMarkers;

  public SmartTodoItemPointer(@Nonnull TodoItem todoItem, @Nonnull Document document) {
    myTodoItem = todoItem;
    myDocument = document;
    TextRange textRange = myTodoItem.getTextRange();
    myRangeMarker = document.createRangeMarker(textRange);
    myAdditionalRangeMarkers = ContainerUtil.map(todoItem.getAdditionalTextRanges(), document::createRangeMarker);
  }

  public TodoItem getTodoItem() {
    return myTodoItem;
  }

  public Document getDocument() {
    return myDocument;
  }

  public RangeMarker getRangeMarker() {
    return myRangeMarker;
  }

  @Nonnull
  public List<RangeMarker> getAdditionalRangeMarkers() {
    return myAdditionalRangeMarkers;
  }

  public boolean equals(Object obj) {
    if (!(obj instanceof SmartTodoItemPointer)) {
      return false;
    }
    SmartTodoItemPointer pointer = (SmartTodoItemPointer)obj;
    if (!(myTodoItem.getFile().equals(pointer.myTodoItem.getFile()) &&
          myRangeMarker.getStartOffset() == pointer.myRangeMarker.getStartOffset() &&
          myRangeMarker.getEndOffset() == pointer.myRangeMarker.getEndOffset() &&
          myTodoItem.getPattern().equals(pointer.myTodoItem.getPattern()) &&
          myAdditionalRangeMarkers.size() == pointer.myAdditionalRangeMarkers.size())) {
      return false;
    }
    for (int i = 0; i < myAdditionalRangeMarkers.size(); i++) {
      RangeMarker m1 = myAdditionalRangeMarkers.get(i);
      RangeMarker m2 = pointer.myAdditionalRangeMarkers.get(i);
      if (m1.getStartOffset() != m2.getStartOffset() || m1.getEndOffset() != m2.getEndOffset()) {
        return false;
      }
    }
    return true;
  }

  public int hashCode() {
    return myTodoItem.getFile().hashCode();
  }
}
