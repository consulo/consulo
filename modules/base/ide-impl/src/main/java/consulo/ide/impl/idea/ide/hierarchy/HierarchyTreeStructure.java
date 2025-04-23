/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ide.hierarchy;

import consulo.content.scope.SearchScope;
import consulo.ide.impl.idea.ide.hierarchy.scope.HierarchyScope;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.tree.AbstractTreeStructure;
import consulo.ui.ex.tree.NodeDescriptor;
import consulo.ui.style.StandardColors;
import consulo.util.collection.ArrayUtil;
import consulo.util.concurrent.ActionCallback;
import jakarta.annotation.Nonnull;

public abstract class HierarchyTreeStructure extends AbstractTreeStructure {
    protected HierarchyNodeDescriptor myBaseDescriptor;
    private HierarchyNodeDescriptor myRoot;
    protected final Project myProject;

    protected HierarchyTreeStructure(@Nonnull Project project, HierarchyNodeDescriptor baseDescriptor) {
        myBaseDescriptor = baseDescriptor;
        myProject = project;
        myRoot = baseDescriptor;
    }

    public final HierarchyNodeDescriptor getBaseDescriptor() {
        return myBaseDescriptor;
    }

    protected final void setBaseElement(@Nonnull HierarchyNodeDescriptor baseElement) {
        myBaseDescriptor = baseElement;
        myRoot = baseElement;
        while (myRoot.getParentDescriptor() != null) {
            myRoot = (HierarchyNodeDescriptor)myRoot.getParentDescriptor();
        }
    }

    @Override
    @Nonnull
    public final NodeDescriptor createDescriptor(@Nonnull Object element, NodeDescriptor parentDescriptor) {
        if (element instanceof HierarchyNodeDescriptor descriptor) {
            return descriptor;
        }
        if (element instanceof String text) {
            return new TextInfoNodeDescriptor(parentDescriptor, text, myProject);
        }
        throw new IllegalArgumentException("Unknown element type: " + element);
    }

    @Override
    public final boolean isToBuildChildrenInBackground(@Nonnull Object element) {
        if (element instanceof HierarchyNodeDescriptor descriptor) {
            Object[] cachedChildren = descriptor.getCachedChildren();
            if (cachedChildren == null && descriptor.isValid()) {
                return true;
            }
        }
        return false;
    }

    @Nonnull
    @Override
    public final Object[] getChildElements(@Nonnull Object element) {
        if (element instanceof HierarchyNodeDescriptor descriptor) {
            Object[] cachedChildren = descriptor.getCachedChildren();
            if (cachedChildren == null) {
                if (!descriptor.isValid()) { //invalid
                    descriptor.setCachedChildren(ArrayUtil.EMPTY_OBJECT_ARRAY);
                }
                else {
                    descriptor.setCachedChildren(buildChildren(descriptor));
                }
            }
            return descriptor.getCachedChildren();
        }
        return ArrayUtil.EMPTY_OBJECT_ARRAY;
    }

    @Override
    public final Object getParentElement(@Nonnull Object element) {
        if (element instanceof HierarchyNodeDescriptor descriptor) {
            return descriptor.getParentDescriptor();
        }

        return null;
    }

    @Override
    public final void commit() {
        PsiDocumentManager.getInstance(myProject).commitAllDocuments();
    }

    @Override
    public final boolean hasSomethingToCommit() {
        return PsiDocumentManager.getInstance(myProject).hasUncommitedDocuments();
    }

    @Nonnull
    @Override
    public ActionCallback asyncCommit() {
        return PsiDocumentManager.asyncCommitDocuments(myProject);
    }

    protected abstract Object[] buildChildren(HierarchyNodeDescriptor descriptor);

    @Nonnull
    @Override
    public final Object getRootElement() {
        return myRoot;
    }

    protected SearchScope getSearchScope(String scopeId, PsiElement thisClass) {
        return HierarchyScope.find(myProject, scopeId).getSearchScope(thisClass);
    }

    protected boolean isInScope(PsiElement baseClass, PsiElement srcElement, String scopeId) {
        return HierarchyScope.find(myProject, scopeId).isInScope(baseClass, srcElement);
    }

    private static final class TextInfoNodeDescriptor extends NodeDescriptor {
        public TextInfoNodeDescriptor(NodeDescriptor parentDescriptor, String text, Project project) {
            super(parentDescriptor);
            myName = text;
            myColor = StandardColors.RED;
        }

        @Override
        public final Object getElement() {
            return myName;
        }

        @RequiredUIAccess
        @Override
        public final boolean update() {
            return true;
        }
    }

    public boolean isAlwaysShowPlus() {
        return false;
    }
}
