/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.ide.impl.idea.refactoring.changeSignature;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.progress.ProgressManager;
import consulo.component.util.Iconable;
import consulo.language.editor.refactoring.localize.RefactoringLocalize;
import consulo.language.icon.IconDescriptorUpdaters;
import consulo.language.psi.PsiElement;
import consulo.project.Project;
import consulo.ui.ex.awt.tree.CheckedTreeNode;
import consulo.ui.ex.awt.tree.ColoredTreeCellRenderer;
import consulo.util.lang.ref.Ref;

import javax.swing.tree.TreeNode;
import java.util.*;

public abstract class MethodNodeBase<M extends PsiElement> extends CheckedTreeNode {
  protected final M myMethod;
  protected final Set<M> myCalled;
  protected final Project myProject;
  protected final Runnable myCancelCallback;
  private boolean myOldChecked;

  protected abstract MethodNodeBase<M> createNode(M caller, HashSet<M> called);

  protected abstract List<M> computeCallers();

  protected abstract void customizeRendererText(ColoredTreeCellRenderer renderer);

  protected MethodNodeBase(final M method, Set<M> called, Project project, Runnable cancelCallback) {
    super(method);
    myMethod = method;
    myCalled = called;
    myProject = project;
    myCancelCallback = cancelCallback;
    isChecked = false;
  }

  //IMPORTANT: do not build children in children()
  private void buildChildren() {
    if (children == null) {
      final List<M> callers = findCallers();
      children = new Vector(callers.size());
      for (M caller : callers) {
        final HashSet<M> called = new HashSet<>(myCalled);
        called.add(myMethod);
        final MethodNodeBase<M> child = createNode(caller, called);
        children.add(child);
        child.parent = this;
      }
    }
  }

  @Override
  public TreeNode getChildAt(int index) {
    buildChildren();
    return super.getChildAt(index);
  }

  @Override
  public int getChildCount() {
    buildChildren();
    return super.getChildCount();
  }

  @Override
  public int getIndex(TreeNode aChild) {
    buildChildren();
    return super.getIndex(aChild);
  }

  private List<M> findCallers() {
    if (myMethod == null) return Collections.emptyList();
    final Ref<List<M>> callers = new Ref<>();
    if (!ProgressManager.getInstance().runProcessWithProgressSynchronously(
      () -> callers.set(computeCallers()),
      RefactoringLocalize.callerChooserLookingForCallers(),
      true,
      myProject
    )) {
      myCancelCallback.run();
      return Collections.emptyList();
    }
    return callers.get();
  }

  @RequiredReadAction
  public void customizeRenderer(ColoredTreeCellRenderer renderer) {
    if (myMethod == null) return;
    int flags = Iconable.ICON_FLAG_VISIBILITY | Iconable.ICON_FLAG_READ_STATUS;
    renderer.setIcon(IconDescriptorUpdaters.getIcon(myMethod, flags));

    customizeRendererText(renderer);
  }

  @Override
  public void setEnabled(final boolean enabled) {
    super.setEnabled(enabled);
    if (!enabled) {
      myOldChecked = isChecked();
      setChecked(false);
    }
    else {
      setChecked(myOldChecked);
    }
  }

  public M getMethod() {
    return myMethod;
  }

  public PsiElement getElementToSearch() {
    return getMethod();
  }

}
