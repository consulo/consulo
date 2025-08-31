// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.todo.nodes;

import consulo.ide.localize.IdeLocalize;
import consulo.ui.ex.tree.PresentationData;
import consulo.project.ui.view.tree.ViewSettings;
import consulo.project.ui.view.tree.PsiFileNode;
import consulo.ide.impl.idea.ide.todo.SmartTodoItemPointer;
import consulo.ide.impl.idea.ide.todo.SmartTodoItemPointerComparator;
import consulo.language.editor.todo.TodoFilter;
import consulo.ide.impl.idea.ide.todo.TodoTreeBuilder;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.document.DocumentWindow;
import consulo.language.inject.InjectedLanguageManager;
import consulo.document.Document;
import consulo.language.psi.*;
import consulo.application.dumb.IndexNotReadyException;
import consulo.project.Project;
import consulo.document.util.TextRange;
import consulo.ide.impl.psi.impl.search.TodoItemImpl;
import consulo.language.psi.search.PsiTodoSearchHelper;
import consulo.language.psi.search.TodoItem;
import consulo.ide.impl.idea.util.containers.ContainerUtil;

import jakarta.annotation.Nonnull;
import java.util.*;

public final class TodoFileNode extends PsiFileNode {
  private final TodoTreeBuilder myBuilder;
  private final boolean mySingleFileMode;

  public TodoFileNode(Project project, @Nonnull PsiFile file, TodoTreeBuilder treeBuilder, boolean singleFileMode) {
    super(project, file, ViewSettings.DEFAULT);
    myBuilder = treeBuilder;
    mySingleFileMode = singleFileMode;
  }

  @Override
  public Collection<AbstractTreeNode> getChildrenImpl() {
    try {
      if (!mySingleFileMode) {
        return createGeneralList();
      }
      else {
        return createListForSingleFile();

      }
    }
    catch (IndexNotReadyException e) {
      return Collections.emptyList();
    }
  }

  private Collection<AbstractTreeNode> createListForSingleFile() {
    PsiFile psiFile = getValue();
    TodoItem[] items = findAllTodos(psiFile, myBuilder.getTodoTreeStructure().getSearchHelper());
    ArrayList<AbstractTreeNode> children = new ArrayList<>(items.length);
    Document document = PsiDocumentManager.getInstance(getProject()).getDocument(psiFile);
    if (document != null) {
      for (TodoItem todoItem : items) {
        if (todoItem.getTextRange().getEndOffset() < document.getTextLength() + 1) {
          SmartTodoItemPointer pointer = new SmartTodoItemPointer(todoItem, document);
          TodoFilter toDoFilter = getToDoFilter();
          if (toDoFilter != null) {
            TodoItemNode itemNode = new TodoItemNode(getProject(), pointer, myBuilder);
            if (toDoFilter.contains(todoItem.getPattern())) {
              children.add(itemNode);
            }
          }
          else {
            children.add(new TodoItemNode(getProject(), pointer, myBuilder));
          }
        }
      }
    }
    Collections.sort(children, SmartTodoItemPointerComparator.ourInstance);
    return children;
  }

  public static TodoItem[] findAllTodos(final PsiFile psiFile, final PsiTodoSearchHelper helper) {
    final List<TodoItem> todoItems = new ArrayList<>(Arrays.asList(helper.findTodoItems(psiFile)));

    psiFile.accept(new PsiRecursiveElementWalkingVisitor() {
      @Override
      public void visitElement(PsiElement element) {
        if (element instanceof PsiLanguageInjectionHost) {
          InjectedLanguageManager.getInstance(psiFile.getProject()).enumerate(element, (injectedPsi, places) -> {
            if (places.size() == 1) {
              Document document = PsiDocumentManager.getInstance(injectedPsi.getProject()).getCachedDocument(injectedPsi);
              if (!(document instanceof DocumentWindow)) return;
              for (TodoItem item : helper.findTodoItems(injectedPsi)) {
                TextRange rangeInHost = ((DocumentWindow)document).injectedToHost(item.getTextRange());
                List<TextRange> additionalRanges = ContainerUtil.map(item.getAdditionalTextRanges(), ((DocumentWindow)document)::injectedToHost);
                TodoItemImpl hostItem = new TodoItemImpl(psiFile, rangeInHost.getStartOffset(), rangeInHost.getEndOffset(), item.getPattern(), additionalRanges);
                todoItems.add(hostItem);
              }
            }
          });
        }
        super.visitElement(element);
      }
    });
    return todoItems.toArray(new TodoItem[0]);
  }

  private Collection<AbstractTreeNode> createGeneralList() {
    ArrayList<AbstractTreeNode> children = new ArrayList<>();

    PsiFile psiFile = getValue();
    TodoItem[] items = findAllTodos(psiFile, myBuilder.getTodoTreeStructure().getSearchHelper());
    Document document = PsiDocumentManager.getInstance(getProject()).getDocument(psiFile);

    if (document != null) {
      for (TodoItem todoItem : items) {
        if (todoItem.getTextRange().getEndOffset() < document.getTextLength() + 1) {
          SmartTodoItemPointer pointer = new SmartTodoItemPointer(todoItem, document);
          TodoFilter todoFilter = getToDoFilter();
          if (todoFilter != null) {
            if (todoFilter.contains(todoItem.getPattern())) {
              children.add(new TodoItemNode(getProject(), pointer, myBuilder));
            }
          }
          else {
            children.add(new TodoItemNode(getProject(), pointer, myBuilder));
          }
        }
      }
    }
    Collections.sort(children, SmartTodoItemPointerComparator.ourInstance);
    return children;
  }

  private TodoFilter getToDoFilter() {
    return myBuilder.getTodoTreeStructure().getTodoFilter();
  }

  @Override
  protected void updateImpl(@Nonnull PresentationData data) {
    super.updateImpl(data);
    String newName;
    if (myBuilder.getTodoTreeStructure().isPackagesShown()) {
      newName = getValue().getName();
    }
    else {
      newName = mySingleFileMode ? getValue().getName() : getValue().getVirtualFile().getPresentableUrl();
    }

    data.setPresentableText(newName);
    int todoItemCount;
    try {
      todoItemCount = myBuilder.getTodoTreeStructure().getTodoItemCount(getValue());
    }
    catch (IndexNotReadyException e) {
      return;
    }
    if (todoItemCount > 0) {
      data.setLocationString(IdeLocalize.nodeTodoItems(todoItemCount));
    }
  }

  @Override
  public int getWeight() {
    return 4;
  }
}
