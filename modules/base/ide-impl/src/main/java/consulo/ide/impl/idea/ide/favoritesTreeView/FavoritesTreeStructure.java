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

package consulo.ide.impl.idea.ide.favoritesTreeView;

import consulo.annotation.access.RequiredReadAction;
import consulo.bookmark.ui.view.BookmarkNodeProvider;
import consulo.bookmark.ui.view.FavoritesListNode;
import consulo.bookmark.ui.view.FavoritesTreeNodeDescriptor;
import consulo.ide.impl.idea.ide.projectView.impl.ProjectTreeStructure;
import consulo.ide.localize.IdeLocalize;
import consulo.language.psi.PsiElement;
import consulo.language.psi.SmartPsiElementPointer;
import consulo.project.Project;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.project.ui.view.tree.ViewSettings;
import consulo.ui.ex.tree.NodeDescriptor;
import consulo.ui.ex.tree.PresentationData;
import consulo.util.collection.ArrayUtil;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Konstantin Bulenkov
 */
public class FavoritesTreeStructure extends ProjectTreeStructure {
    public FavoritesTreeStructure(Project project) {
        super(project, FavoritesViewTreeBuilder.ID);
    }

    @Override
    protected AbstractTreeNode createRoot(Project project, ViewSettings settings) {
        return new FavoritesRootNode(project);
    }

    public void rootsChanged() {
        ((FavoritesRootNode) getRootElement()).rootsChanged();
    }

    @Nonnull
    @Override
    @RequiredReadAction
    public Object[] getChildElements(@Nonnull Object element) {
        if (!(element instanceof AbstractTreeNode favTreeElement)) {
            return ArrayUtil.EMPTY_OBJECT_ARRAY;
        }

        try {
            if (!(element instanceof FavoritesListNode listNode)) {
                return super.getChildElements(favTreeElement);
            }

            List<AbstractTreeNode> result = new ArrayList<>();
            if (listNode.getProvider() != null) {
                return ArrayUtil.toObjectArray(listNode.getChildren());
            }
            Collection<AbstractTreeNode> roots = FavoritesListNode.getFavoritesRoots(myProject, listNode.getName(), listNode);
            for (AbstractTreeNode<?> abstractTreeNode : roots) {
                Object value = abstractTreeNode.getValue();

                if (value == null) {
                    continue;
                }
                if (value instanceof PsiElement elem && !elem.isValid()) {
                    continue;
                }
                if (value instanceof SmartPsiElementPointer smartPointer && smartPointer.getElement() == null) {
                    continue;
                }

                boolean invalid = false;
                for (BookmarkNodeProvider nodeProvider : myProject.getExtensionList(BookmarkNodeProvider.class)) {
                    if (nodeProvider.isInvalidElement(value)) {
                        invalid = true;
                        break;
                    }
                }
                if (invalid) {
                    continue;
                }

                result.add(abstractTreeNode);
            }
            //myFavoritesRoots = result;
            //if (result.isEmpty()) {
            //  result.add(getEmptyScreen());
            //}
            return ArrayUtil.toObjectArray(result);
        }
        catch (Exception ignore) {
        }

        return ArrayUtil.EMPTY_OBJECT_ARRAY;
    }

    private AbstractTreeNode<String> getEmptyScreen() {
        return new AbstractTreeNode<>(myProject, IdeLocalize.favoritesEmptyScreen().get()) {
            @RequiredReadAction
            @Override
            @Nonnull
            public Collection<AbstractTreeNode> getChildren() {
                return Collections.emptyList();
            }

            @Override
            public void update(PresentationData presentation) {
                presentation.setPresentableText(getValue());
            }
        };
    }

    @Override
    public Object getParentElement(@Nonnull Object element) {
        AbstractTreeNode parent = null;
        if (element == getRootElement()) {
            return null;
        }
        if (element instanceof AbstractTreeNode node) {
            parent = (AbstractTreeNode) node.getParent();
        }
        if (parent == null) {
            return getRootElement();
        }
        return parent;
    }

    @Nonnull
    @Override
    public NodeDescriptor createDescriptor(@Nonnull Object element, NodeDescriptor parentDescriptor) {
        return new FavoritesTreeNodeDescriptor(myProject, parentDescriptor, (AbstractTreeNode) element);
    }
}
