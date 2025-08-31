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

package consulo.ide.impl.idea.packageDependencies.ui;

import consulo.language.editor.scope.AnalysisScopeBundle;
import consulo.codeEditor.Editor;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.impl.internal.OpenFileDescriptorImpl;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.navigation.Navigatable;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.ui.color.ColorValue;
import consulo.ui.image.Image;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import java.util.*;

public class PackageDependenciesNode extends DefaultMutableTreeNode implements Navigatable {
  private static final Image EMPTY_ICON = Image.empty(0, Image.DEFAULT_ICON_SIZE);

  protected static final ColorValue NOT_CHANGED = ColorValue.dummy("must be never called");

  private Set<VirtualFile> myRegisteredFiles = null;
  private boolean myHasUnmarked = false;
  private boolean myHasMarked = false;
  private boolean myEquals;
  protected ColorValue myColor = null;
  protected Project myProject;
  private boolean mySorted;

  public PackageDependenciesNode(@Nonnull Project project) {
    myProject = project;
  }

  public void setEquals(boolean equals) {
    myEquals = equals;
  }

  public boolean isEquals() {
    return myEquals;
  }

  public void fillFiles(Set<PsiFile> set, boolean recursively) {
    PsiManager psiManager = PsiManager.getInstance(myProject);
    for (VirtualFile vFile : getRegisteredFiles()) {
      PsiFile psiFile = psiManager.findFile(vFile);
      if (psiFile != null && psiFile.isValid()) {
        set.add(psiFile);
      }
    }
  }

  public void addFile(VirtualFile file, boolean isMarked) {
    getRegisteredFiles().add(file);
    updateMarked(!isMarked, isMarked);
  }

  public Image getIcon() {
    return EMPTY_ICON;
  }

  public int getWeight() {
    return 0;
  }

  public boolean hasUnmarked() {
    return myHasUnmarked;
  }

  public boolean hasMarked() {
    return myHasMarked;
  }

  @Nullable
  public PsiElement getPsiElement() {
    return null;
  }

  @Nullable
  public ColorValue getColor() {
    return myColor;
  }

  public void updateColor() {
    myColor = null;
  }

  public int getContainingFiles() {
    int result = 0;
    for (int i = 0; i < getChildCount(); i++) {
      result += ((PackageDependenciesNode)getChildAt(i)).getContainingFiles();
    }
    return result;
  }

  public String getPresentableFilesCount() {
    int filesCount = getContainingFiles();
    return filesCount > 0 ? " (" + AnalysisScopeBundle.message("package.dependencies.node.items.count", filesCount) + ")" : "";
  }

  @Override
  public void add(MutableTreeNode newChild) {
    super.add(newChild);
    boolean hasUnmarked = ((PackageDependenciesNode)newChild).hasUnmarked();
    boolean hasMarked = ((PackageDependenciesNode)newChild).hasMarked();
    updateMarked(hasUnmarked, hasMarked);
  }

  private void updateMarked(boolean hasUnmarked, boolean hasMarked) {
    if (hasUnmarked && !myHasUnmarked || hasMarked && !myHasMarked) {
      myHasUnmarked |= hasUnmarked;
      myHasMarked |= hasMarked;
      PackageDependenciesNode parent = (PackageDependenciesNode)getParent();
      if (parent != null) {
        parent.updateMarked(myHasUnmarked, myHasMarked);
      }
    }
  }

  @Override
  public void navigate(boolean focus) {
    if (canNavigate()) {
      openTextEditor(focus);
    }
  }

  @Nullable
  private Editor openTextEditor(boolean focus) {
    OpenFileDescriptorImpl descriptor = getDescriptor();
    if (descriptor != null) {
      return FileEditorManager.getInstance(getProject()).openTextEditor(descriptor, focus);
    }
    return null;
  }

  @Override
  public boolean canNavigate() {
    if (getProject() == null) return false;
    PsiElement psiElement = getPsiElement();
    if (psiElement == null) return false;
    VirtualFile virtualFile = psiElement.getContainingFile().getVirtualFile();
    return virtualFile != null && virtualFile.isValid();
  }

  @Override
  public boolean canNavigateToSource() {
    return canNavigate();
  }

  @Nullable
  private Project getProject() {
    PsiElement psiElement = getPsiElement();
    if (psiElement == null || psiElement.getContainingFile() == null) {
      return null;
    }
    return psiElement.getContainingFile().getProject();
  }

  @Nullable
  private OpenFileDescriptorImpl getDescriptor() {
    if (getProject() == null) return null;
    PsiElement psiElement = getPsiElement();
    if (psiElement == null) return null;
    VirtualFile virtualFile = psiElement.getContainingFile().getVirtualFile();
    if (virtualFile == null || !virtualFile.isValid()) return null;
    return new OpenFileDescriptorImpl(getProject(), virtualFile, psiElement.getTextOffset());
  }

  @Override
  public Object getUserObject() {
    return toString();
  }

  public boolean isValid() {
    return true;
  }

  public Set<VirtualFile> getRegisteredFiles() {
    if (myRegisteredFiles == null) {
      myRegisteredFiles = new HashSet<VirtualFile>();
    }
    return myRegisteredFiles;
  }

  @Nullable
  public String getComment() {
    return null;
  }

  public boolean canSelectInLeftTree(Map<PsiFile, Set<PsiFile>> deps) {
    return false;
  }

  public boolean isSorted() {
    return mySorted;
  }

  public void setSorted(boolean sorted) {
    mySorted = sorted;
  }

  public void sortChildren() {
    if (isSorted()) return;
    List children = TreeUtil.listChildren(this);
    Collections.sort(children, new DependencyNodeComparator());
    removeAllChildren();
    TreeUtil.addChildrenTo(this, children);
    setSorted(true);
  }
}
