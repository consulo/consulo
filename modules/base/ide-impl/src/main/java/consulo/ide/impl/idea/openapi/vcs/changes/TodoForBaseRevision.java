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
package consulo.ide.impl.idea.openapi.vcs.changes;

import consulo.virtualFileSystem.fileType.FileType;
import consulo.project.Project;
import consulo.ide.impl.idea.openapi.util.Getter;
import consulo.document.util.TextRange;
import consulo.language.psi.search.PsiTodoSearchHelper;
import consulo.language.psi.search.TodoItem;
import java.util.function.Consumer;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Irina.Chernushina
 * @since 2012-02-28
 */
public class TodoForBaseRevision extends TodoForRanges {
  private final Getter<Object> myGetter;
  private final Consumer<Object> mySaver;

  public TodoForBaseRevision(Project project,
                              List<TextRange> ranges,
                              int additionalOffset,
                              String name,
                              String text,
                              boolean revision, FileType type, final Getter<Object> cacheGetter,
                              final Consumer<Object> cacheSaver) {
    super(project, ranges, additionalOffset, name, text, revision, type);
    myGetter = cacheGetter;
    mySaver = cacheSaver;
  }

  @Override
  protected TodoItemData[] getTodoItems() {
    final TodoItemData[] items = (TodoItemData[])myGetter.get();
    if (items != null) return items;
    final TodoItem[] todoItems = getTodoForText(PsiTodoSearchHelper.getInstance(myProject));
    if (todoItems != null) {
      final TodoItemData[] arr = convertTodo(todoItems);
      mySaver.accept(arr);
      return arr;
    }
    return null;
  }

  public static TodoItemData[] convertTodo(TodoItem[] todoItems) {
    final List<TodoItemData> list = new ArrayList<TodoItemData>();
    for (TodoItem item : todoItems) {
      final TextRange range = item.getTextRange();
      final TodoItemData data = new TodoItemData(range.getStartOffset(), range.getEndOffset(), item.getPattern());
      list.add(data);
    }
    return list.toArray(new TodoItemData[list.size()]);
  }
}
