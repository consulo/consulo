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

package consulo.ide.impl.idea.ide.todo.nodes;

import consulo.ide.impl.idea.ide.todo.ToDoSettings;
import consulo.ide.impl.idea.ide.todo.TodoTreeBuilder;
import consulo.ide.impl.idea.ide.todo.TodoTreeStructure;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

public abstract class BaseToDoNode<Value> extends AbstractTreeNode<Value> {
  protected final ToDoSettings myToDoSettings;
  protected final TodoTreeBuilder myBuilder;

  protected BaseToDoNode(Project project, @Nonnull Value value, TodoTreeBuilder builder) {
    super(project, value);
    myBuilder = builder;
    myToDoSettings = myBuilder.getTodoTreeStructure();
  }

  public boolean contains(VirtualFile file) {
    return false;
  }

  public boolean contains(Object element) {
    return false;
  }

  protected TodoTreeStructure getTreeStructure() {
    return myBuilder.getTodoTreeStructure();
  }

  public abstract int getFileCount(Value val);

  public abstract int getTodoItemCount(Value val);
}
