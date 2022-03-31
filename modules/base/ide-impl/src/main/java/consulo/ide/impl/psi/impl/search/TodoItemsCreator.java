// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.psi.impl.search;

import com.intellij.ide.todo.TodoConfiguration;
import consulo.logging.Logger;
import consulo.document.util.TextRange;
import consulo.language.psi.search.IndexPattern;
import consulo.language.psi.search.IndexPatternOccurrence;
import consulo.language.psi.search.TodoItem;
import consulo.language.psi.search.TodoPattern;
import javax.annotation.Nonnull;

/**
 * @author irengrig
 * moved from PsiSearchHelperImpl
 */
public class TodoItemsCreator {
  private static final Logger LOG = Logger.getInstance(TodoItemsCreator.class);
  private final TodoPattern[] myTodoPatterns;

  public TodoItemsCreator() {
    myTodoPatterns = TodoConfiguration.getInstance().getTodoPatterns();
  }

  public TodoItem createTodo(IndexPatternOccurrence occurrence) {
    final TextRange occurrenceRange = occurrence.getTextRange();
    return new TodoItemImpl(occurrence.getFile(), occurrenceRange.getStartOffset(), occurrenceRange.getEndOffset(), mapPattern(occurrence.getPattern()), occurrence.getAdditionalTextRanges());
  }

  @Nonnull
  private TodoPattern mapPattern(@Nonnull IndexPattern pattern) {
    for (TodoPattern todoPattern : myTodoPatterns) {
      if (todoPattern.getIndexPattern() == pattern) {
        return todoPattern;
      }
    }
    LOG.error("Could not find matching TODO pattern for index pattern " + pattern.getPatternString());
    return null;
  }
}
