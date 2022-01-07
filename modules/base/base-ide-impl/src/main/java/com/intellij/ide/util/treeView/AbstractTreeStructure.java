/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

package com.intellij.ide.util.treeView;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ActionCallback;
import com.intellij.openapi.util.AsyncResult;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.ui.tree.LeafState;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class AbstractTreeStructure {
  @Nonnull
  public abstract Object getRootElement();

  @Nonnull
  public abstract Object[] getChildElements(@Nonnull Object element);

  @Nullable
  public abstract Object getParentElement(@Nonnull Object element);

  @Nonnull
  public abstract NodeDescriptor createDescriptor(@Nonnull Object element, @Nullable NodeDescriptor parentDescriptor);

  public abstract void commit();

  public abstract boolean hasSomethingToCommit();

  @Nonnull
  public static ActionCallback asyncCommitDocuments(@Nonnull Project project) {
    if (project.isDisposed()) return ActionCallback.DONE;
    PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
    if (!documentManager.hasUncommitedDocuments()) {
      return ActionCallback.DONE;
    }
    final ActionCallback callback = new ActionCallback();
    documentManager.performWhenAllCommitted(callback.createSetDoneRunnable());
    return callback;
  }

  /**
   * @return callback which is set to {@link ActionCallback#setDone()} when the tree structure is committed.
   * By default it just calls {@link #commit()} synchronously but it is desirable to override it
   * to provide asynchronous commit to the tree structure to make it more responsible.
   * E.g. when you should commit all documents during the {@link #commit()},
   * you can use {@link #asyncCommitDocuments(Project)} to do it asynchronously.
   */
  @Nonnull
  public ActionCallback asyncCommit() {
    if (hasSomethingToCommit()) commit();
    return ActionCallback.DONE;
  }

  public boolean isToBuildChildrenInBackground(@Nonnull Object element) {
    return false;
  }

  public boolean isValid(@Nonnull Object element) {
    return true;
  }

  /**
   * @param element an object that represents a node in this tree structure
   * @return a leaf state for the given element
   * @see LeafState.Supplier#getLeafState()
   */
  @Nonnull
  public LeafState getLeafState(@Nonnull Object element) {
    if (isAlwaysLeaf(element)) return LeafState.ALWAYS;
    if (element instanceof LeafState.Supplier) {
      LeafState.Supplier supplier = (LeafState.Supplier)element;
      return supplier.getLeafState();
    }
    return LeafState.DEFAULT;
  }

  public boolean isAlwaysLeaf(@Nonnull Object element) {
    return false;
  }

  @Nonnull
  public AsyncResult<Object> revalidateElement(@Nonnull Object element) {
    return AsyncResult.resolved(element);
  }
}