// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.todo.impl.internal;

import consulo.language.editor.todo.impl.internal.node.BaseToDoNode;
import consulo.language.editor.todo.impl.internal.node.SummaryNode;
import consulo.language.editor.todo.impl.internal.node.ToDoRootNode;
import consulo.language.editor.todo.impl.internal.node.TodoTreeHelper;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.project.ui.view.tree.AbstractTreeNodeVisitor;
import consulo.project.ui.view.tree.ProjectViewNode;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import java.util.function.Supplier;

public class TodoNodeVisitor extends AbstractTreeNodeVisitor<Object> {
    private final VirtualFile myFile;

    public TodoNodeVisitor(@Nonnull Supplier<Object> supplier, VirtualFile file) {
        super(supplier, null);
        myFile = file;
    }

    @Override
    protected boolean contains(@Nonnull AbstractTreeNode node, @Nonnull Object element) {
        if (node instanceof SummaryNode || node instanceof ToDoRootNode) {
            return true;
        }
        if (node instanceof ProjectViewNode) {
            if (myFile == null) {
                return TodoTreeHelper.contains((ProjectViewNode) node, element);
            }
        }
        return node instanceof BaseToDoNode && ((BaseToDoNode) node).contains(element) || node instanceof ProjectViewNode && ((ProjectViewNode) node).contains(myFile);
    }
}
