/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package consulo.ide.impl.psi.impl.search;

import consulo.annotation.component.ServiceImpl;
import consulo.document.util.TextRange;
import consulo.ide.impl.idea.ide.todo.TodoIndexPatternProvider;
import consulo.language.psi.stub.todo.TodoCacheManager;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.search.*;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Singleton
@ServiceImpl
public class PsiTodoSearchHelperImpl extends PsiTodoSearchHelper {
  private final PsiManager myManager;
  private static final TodoItem[] EMPTY_TODO_ITEMS = new TodoItem[0];

  @Inject
  public PsiTodoSearchHelperImpl(PsiManager manager) {
    myManager = manager;
  }

  @Override
  @Nonnull
  public PsiFile[] findFilesWithTodoItems() {
    return TodoCacheManager.getInstance(myManager.getProject()).getFilesWithTodoItems();
  }

  @Override
  @Nonnull
  public TodoItem[] findTodoItems(@Nonnull PsiFile file) {
    return findTodoItems(file, 0, file.getTextLength());
  }

  @Override
  @Nonnull
  public TodoItem[] findTodoItems(@Nonnull PsiFile file, int startOffset, int endOffset) {
    Collection<IndexPatternOccurrence> occurrences = IndexPatternSearch.search(file, TodoIndexPatternProvider.getInstance()).findAll();
    if (occurrences.isEmpty()) {
      return EMPTY_TODO_ITEMS;
    }

    return processTodoOccurences(startOffset, endOffset, occurrences);
  }

  @Nonnull
  private static TodoItem[] processTodoOccurences(int startOffset, int endOffset, Collection<IndexPatternOccurrence> occurrences) {
    List<TodoItem> items = new ArrayList<>(occurrences.size());
    TextRange textRange = new TextRange(startOffset, endOffset);
    TodoItemsCreator todoItemsCreator = new TodoItemsCreator();
    for(IndexPatternOccurrence occurrence: occurrences) {
      TextRange occurrenceRange = occurrence.getTextRange();
      if (textRange.contains(occurrenceRange)) {
        items.add(todoItemsCreator.createTodo(occurrence));
      }
    }

    return items.toArray(new TodoItem[items.size()]);
  }

  @Nonnull
  @Override
  public TodoItem[] findTodoItemsLight(@Nonnull PsiFile file) {
    return findTodoItemsLight(file, 0, file.getTextLength());
  }

  @Nonnull
  @Override
  public TodoItem[] findTodoItemsLight(@Nonnull PsiFile file, int startOffset, int endOffset) {
    Collection<IndexPatternOccurrence> occurrences =
            LightIndexPatternSearch.SEARCH.createQuery(new IndexPatternSearch.SearchParameters(file, TodoIndexPatternProvider.getInstance())).findAll();

    if (occurrences.isEmpty()) {
      return EMPTY_TODO_ITEMS;
    }

    return processTodoOccurences(startOffset, endOffset, occurrences);
  }

  @Override
  public int getTodoItemsCount(@Nonnull PsiFile file) {
    int count = TodoCacheManager.getInstance(myManager.getProject()).getTodoCount(file.getVirtualFile(), TodoIndexPatternProvider.getInstance());
    if (count != -1) return count;
    return findTodoItems(file).length;
  }

  @Override
  public int getTodoItemsCount(@Nonnull PsiFile file, @Nonnull TodoPattern pattern) {
    int count = TodoCacheManager.getInstance(myManager.getProject()).getTodoCount(file.getVirtualFile(), pattern.getIndexPattern());
    if (count != -1) return count;
    TodoItem[] items = findTodoItems(file);
    count = 0;
    for (TodoItem item : items) {
      if (item.getPattern().equals(pattern)) count++;
    }
    return count;
  }
}
