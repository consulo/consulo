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

import consulo.annotation.access.RequiredReadAction;
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
import org.jspecify.annotations.Nullable;

/**
 * @author anna
 * @since 2005-02-15
 */
public class FavoritesTreeNodeDescriptor extends PresentableNodeDescriptor<AbstractTreeNode> {
    public static final FavoritesTreeNodeDescriptor[] EMPTY_ARRAY = new FavoritesTreeNodeDescriptor[0];

    private final Project myProject;
    private final AbstractTreeNode myElement;

    public FavoritesTreeNodeDescriptor(Project project, NodeDescriptor parentDescriptor, AbstractTreeNode element) {
        super(parentDescriptor);
        myProject = project;
        myElement = element;
    }

    @Override
    protected void update(PresentationData presentation) {
        myElement.update();
        presentation.copyFrom(myElement.getPresentation());
    }

    /*protected boolean isMarkReadOnly() {
        Object parentValue = myElement.getParent() == null ? null : myElement.getParent().getValue();
        return parentValue instanceof PsiDirectory || parentValue instanceof PackageElement;
    }*/

    @RequiredReadAction
    public @Nullable String getLocation() {
        return getLocation(myElement, myProject);
    }

    @RequiredReadAction
    public static @Nullable String getLocation(AbstractTreeNode element, Project project) {
        Object nodeElement = element.getValue();
        if (nodeElement instanceof SmartPsiElementPointer smartPtr) {
            nodeElement = smartPtr.getElement();
        }
        if (nodeElement instanceof PsiElement) {
            if (nodeElement instanceof PsiDirectory dir) {
                return dir.getVirtualFile().getPresentableUrl();
            }
            if (nodeElement instanceof PsiFile containingFile) {
                VirtualFile virtualFile = containingFile.getVirtualFile();
                return virtualFile != null ? virtualFile.getPresentableUrl() : "";
            }
        }

        if (nodeElement instanceof LibraryGroupElement libGroup) {
            return libGroup.getModule().getName();
        }
        if (nodeElement instanceof NamedLibraryElement namedLib) {
            Module module = namedLib.getModule();
            return (module != null ? module.getName() : "") + ":" + namedLib.getOrderEntry().getPresentableName();
        }

        for (BookmarkNodeProvider provider : project.getExtensionList(BookmarkNodeProvider.class)) {
            String location = provider.getElementLocation(nodeElement);
            if (location != null) {
                return location;
            }
        }
        return null;
    }

    @Override
    public AbstractTreeNode getElement() {
        return myElement;
    }

    @Override
    public boolean equals(@Nullable Object object) {
        return object == this
            || object instanceof FavoritesTreeNodeDescriptor that && that.getElement().equals(myElement);
    }

    @Override
    public int hashCode() {
        return myElement.hashCode();
    }

    public FavoritesTreeNodeDescriptor getFavoritesRoot() {
        FavoritesTreeNodeDescriptor descriptor = this;
        while (descriptor.getParentDescriptor() instanceof FavoritesTreeNodeDescriptor favDecr) {
            if (favDecr.getParentDescriptor() == null) {
                return descriptor;
            }
            descriptor = favDecr;
        }
        return descriptor;
    }

    @Override
    public PresentableNodeDescriptor getChildToHighlightAt(int index) {
        return myElement.getChildToHighlightAt(index);
    }
}
