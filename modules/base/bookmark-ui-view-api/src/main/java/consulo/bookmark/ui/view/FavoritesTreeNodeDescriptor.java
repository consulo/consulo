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

package consulo.bookmark.ui.view;

import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.SmartPsiElementPointer;
import consulo.module.Module;
import consulo.project.Project;
import consulo.project.ui.view.internal.node.LibraryGroupElement;
import consulo.project.ui.view.internal.node.NamedLibraryElement;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.ui.ex.tree.NodeDescriptor;
import consulo.ui.ex.tree.PresentableNodeDescriptor;
import consulo.ui.ex.tree.PresentationData;
import consulo.virtualFileSystem.VirtualFile;

/**
 * @author anna
 * @since 2005-02-15
 */
public class FavoritesTreeNodeDescriptor extends PresentableNodeDescriptor<AbstractTreeNode> {
  public static final FavoritesTreeNodeDescriptor[] EMPTY_ARRAY = new FavoritesTreeNodeDescriptor[0];

  private final Project myProject;
  private final AbstractTreeNode myElement;

  public FavoritesTreeNodeDescriptor(final Project project, final NodeDescriptor parentDescriptor, final AbstractTreeNode element) {
    super(parentDescriptor);
    myProject = project;
    myElement = element;
  }

  @Override
  protected void update(PresentationData presentation) {
    myElement.update();
    presentation.copyFrom(myElement.getPresentation());
  }

 /* protected boolean isMarkReadOnly() {
    final Object parentValue = myElement.getParent() == null ? null : myElement.getParent().getValue();
    return parentValue instanceof PsiDirectory || parentValue instanceof PackageElement;
  }*/

  public String getLocation() {
    return getLocation(myElement, myProject);
  }

  public static String getLocation(final AbstractTreeNode element, final Project project) {
    Object nodeElement = element.getValue();
    if (nodeElement instanceof SmartPsiElementPointer) {
      nodeElement = ((SmartPsiElementPointer)nodeElement).getElement();
    }
    if (nodeElement instanceof PsiElement) {
      if (nodeElement instanceof PsiDirectory) {
        return ((PsiDirectory)nodeElement).getVirtualFile().getPresentableUrl();
      }
      if (nodeElement instanceof PsiFile) {
        final PsiFile containingFile = (PsiFile)nodeElement;
        final VirtualFile virtualFile = containingFile.getVirtualFile();
        return virtualFile != null ? virtualFile.getPresentableUrl() : "";
      }
    }

    if (nodeElement instanceof LibraryGroupElement) {
      return ((LibraryGroupElement)nodeElement).getModule().getName();
    }
    if (nodeElement instanceof NamedLibraryElement) {
      final NamedLibraryElement namedLibraryElement = ((NamedLibraryElement)nodeElement);
      final Module module = namedLibraryElement.getModule();
      return (module != null ? module.getName() : "") + ":" + namedLibraryElement.getOrderEntry().getPresentableName();
    }

    for (BookmarkNodeProvider provider : project.getExtensionList(BookmarkNodeProvider.class)) {
      String location = provider.getElementLocation(nodeElement);
      if (location != null) return location;
    }
    return null;
  }

  @Override
  public AbstractTreeNode getElement() {
    return myElement;
  }

  public boolean equals(Object object) {
    if (!(object instanceof FavoritesTreeNodeDescriptor)) return false;
    return ((FavoritesTreeNodeDescriptor)object).getElement().equals(myElement);
  }

  public int hashCode() {
    return myElement.hashCode();
  }

  public FavoritesTreeNodeDescriptor getFavoritesRoot() {
    FavoritesTreeNodeDescriptor descriptor = this;
    while (descriptor.getParentDescriptor() instanceof FavoritesTreeNodeDescriptor) {
      FavoritesTreeNodeDescriptor parent = (FavoritesTreeNodeDescriptor)descriptor.getParentDescriptor();
      if (parent != null && parent.getParentDescriptor() == null) {
        return descriptor;
      }
      descriptor = parent;
    }
    return descriptor;
  }

  @Override
  public PresentableNodeDescriptor getChildToHighlightAt(int index) {
    return myElement.getChildToHighlightAt(index);
  }
}
