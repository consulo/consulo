/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.language.editor.todo.impl.internal;

import consulo.document.util.TextRange;
import consulo.language.editor.todo.TodoFilter;
import consulo.language.editor.todo.impl.internal.versionSystemControl.TodoCheckinHandlerWorker;
import consulo.language.psi.PsiFile;
import consulo.language.psi.search.PsiTodoSearchHelper;
import consulo.language.psi.search.TodoItem;
import consulo.language.psi.search.TodoPattern;
import consulo.project.Project;
import consulo.util.collection.MultiMap;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.ChangeListManager;
import consulo.versionControlSystem.change.LocalChangeList;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.util.*;

/**
 * @author irengrig
 */
public class CustomChangelistTodosTreeBuilder extends TodoTreeBuilder {
  public static final TodoItem[] EMPTY_ITEMS = new TodoItem[0];
  private final Project myProject;
  private final String myTitle;
  private final MultiMap<PsiFile, TodoItem> myMap;
  private final Set<PsiFile> myIncludedFiles;
  private PsiTodoSearchHelper myPsiTodoSearchHelper;
  private final ChangeListManager myChangeListManager;

  public CustomChangelistTodosTreeBuilder(JTree tree, Project project, String title, Collection<? extends TodoItem> todoItems) {
    super(tree, project);
    myProject = project;
    myTitle = title;
    myMap = new MultiMap<>();
    myIncludedFiles = new HashSet<>();
    myChangeListManager = ChangeListManager.getInstance(myProject);
    initMap(todoItems);
    initHelper();
  }

  private void initMap(Collection<? extends TodoItem> todoItems) {
    buildMap(todoItems);
    myIncludedFiles.addAll(myMap.keySet());
  }

  private void buildMap(Collection<? extends TodoItem> todoItems) {
    myMap.clear();
    for (TodoItem todoItem : todoItems) {
      myMap.putValue(todoItem.getFile(), todoItem);
    }
  }

  private void initHelper() {
    myPsiTodoSearchHelper = new PsiTodoSearchHelper() {
      @Nonnull
      @Override
      public PsiFile[] findFilesWithTodoItems() {
        List<Change> changes = new ArrayList<>();
        List<LocalChangeList> changeLists = myChangeListManager.getChangeLists();
        Map<VirtualFile, Change> allChanges = new HashMap<>();
        for (LocalChangeList changeList : changeLists) {
          Collection<Change> currChanges = changeList.getChanges();
          for (Change currChange : currChanges) {
            if (currChange.getAfterRevision() != null && currChange.getAfterRevision().getFile().getVirtualFile() != null) {
              allChanges.put(currChange.getAfterRevision().getFile().getVirtualFile(), currChange);
            }
          }
        }
        for (PsiFile next : myIncludedFiles) {
          Change change = allChanges.get(next.getVirtualFile());
          if (change != null) {
            changes.add(change);
          }
        }
        // a hack here with _todo filter
        TodoCheckinHandlerWorker worker = new TodoCheckinHandlerWorker(myProject, changes, getTodoTreeStructure().getTodoFilter(), true);
        worker.execute();
        buildMap(worker.inOneList());

        Set<PsiFile> files = myMap.keySet();
        return files.toArray(PsiFile.EMPTY_ARRAY);
      }

      @Nonnull
      @Override
      public TodoItem[] findTodoItems(@Nonnull PsiFile file) {
        return findPatternedTodoItems(file, getTodoTreeStructure().getTodoFilter());
      }

      @Nonnull
      @Override
      public TodoItem[] findTodoItemsLight(@Nonnull PsiFile file) {
        return findTodoItems(file);
      }

      @Nonnull
      @Override
      public TodoItem[] findTodoItemsLight(@Nonnull PsiFile file, int startOffset, int endOffset) {
        return findTodoItems(file, startOffset, endOffset);
      }

      @Nonnull
      @Override
      public TodoItem[] findTodoItems(@Nonnull PsiFile file, int startOffset, int endOffset) {
        TodoItem[] todoItems = findTodoItems(file);
        if (todoItems.length == 0) {
          return todoItems;
        }
        TextRange textRange = new TextRange(startOffset, endOffset);
        List<TodoItem> result = new ArrayList<>();
        for (TodoItem todoItem : todoItems) {
          if (todoItem.getTextRange().contains(textRange)) {
            result.add(todoItem);
          }
        }
        return result.isEmpty() ? EMPTY_ITEMS : result.toArray(new TodoItem[0]);
      }

      @Override
      public int getTodoItemsCount(@Nonnull PsiFile file) {
        return findTodoItems(file).length;
      }

      @Override
      public int getTodoItemsCount(@Nonnull PsiFile file, @Nonnull TodoPattern pattern) {
        TodoFilter filter = new TodoFilter();
        filter.addTodoPattern(pattern);
        return findPatternedTodoItems(file, filter).length;
      }
    };
  }

  private TodoItem[] findPatternedTodoItems(PsiFile file, TodoFilter todoFilter) {
    if (!myIncludedFiles.contains(file)) return EMPTY_ITEMS;
    if (myDirtyFileSet.contains(file.getVirtualFile())) {
      myMap.remove(file);
      Change change = myChangeListManager.getChange(file.getVirtualFile());
      if (change != null) {
        TodoCheckinHandlerWorker worker = new TodoCheckinHandlerWorker(myProject, Collections.singletonList(change), todoFilter, true);
        worker.execute();
        Collection<TodoItem> todoItems = worker.inOneList();
        if (todoItems != null && !todoItems.isEmpty()) {
          for (TodoItem todoItem : todoItems) {
            myMap.putValue(file, todoItem);
          }
        }
      }
    }
    Collection<TodoItem> todoItems = myMap.get(file);
    return todoItems.isEmpty() ? EMPTY_ITEMS : todoItems.toArray(new TodoItem[0]);
  }

  @Nonnull
  @Override
  protected TodoTreeStructure createTreeStructure() {
    return new CustomChangelistTodoTreeStructure(myProject, myPsiTodoSearchHelper);
  }

  @Override
  void rebuildCache() {
    Set<VirtualFile> files = new HashSet<>();
    TodoTreeStructure treeStructure = getTodoTreeStructure();
    PsiFile[] psiFiles = myPsiTodoSearchHelper.findFilesWithTodoItems();
    for (PsiFile psiFile : psiFiles) {
      if (myPsiTodoSearchHelper.getTodoItemsCount(psiFile) > 0 && treeStructure.accept(psiFile)) {
        files.add(psiFile.getVirtualFile());
      }
    }

    super.rebuildCache(files);
  }
}
