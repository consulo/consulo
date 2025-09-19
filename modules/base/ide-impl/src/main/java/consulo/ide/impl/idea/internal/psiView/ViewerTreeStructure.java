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
package consulo.ide.impl.idea.internal.psiView;

import consulo.language.ast.ASTNode;
import consulo.language.ast.TokenType;
import consulo.language.impl.psi.SourceTreeToPsiMap;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.psi.*;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.tree.AbstractTreeStructure;
import consulo.ui.ex.tree.NodeDescriptor;
import consulo.util.collection.ArrayUtil;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jeka
 * @since 2001-08-25
 */
public class ViewerTreeStructure extends AbstractTreeStructure {
    private boolean myShowWhiteSpaces = true;
    private boolean myShowTreeNodes = true;

    private final Project myProject;
    private PsiElement myRootPsiElement = null;
    private final Object myRootElement = new Object();

    public ViewerTreeStructure(Project project) {
        myProject = project;
    }

    public void setRootPsiElement(PsiElement rootPsiElement) {
        myRootPsiElement = rootPsiElement;
    }

    public PsiElement getRootPsiElement() {
        return myRootPsiElement;
    }

    @Nonnull
    @Override
    public Object getRootElement() {
        return myRootElement;
    }

    @Nonnull
    @Override
    public Object[] getChildElements(@Nonnull Object element) {
        if (myRootElement == element) {
            if (myRootPsiElement == null) {
                return ArrayUtil.EMPTY_OBJECT_ARRAY;
            }
            if (!(myRootPsiElement instanceof PsiFile file)) {
                return new Object[]{myRootPsiElement};
            }
            List<PsiFile> files = file.getViewProvider().getAllFiles();
            return PsiUtilCore.toPsiFileArray(files);
        }
        Object[][] children = new Object[1][];
        children[0] = ArrayUtil.EMPTY_OBJECT_ARRAY;
        myProject.getApplication().runReadAction(() -> {
            Object[] result;
            if (myShowTreeNodes) {
                List<Object> list = new ArrayList<>();
                ASTNode root = element instanceof PsiElement psiElement
                    ? SourceTreeToPsiMap.psiElementToTree(psiElement)
                    : element instanceof ASTNode astNode ? astNode : null;
                if (element instanceof Inject inject) {
                    root = SourceTreeToPsiMap.psiElementToTree(inject.getPsi());
                }

                if (root != null) {
                    ASTNode child = root.getFirstChildNode();
                    while (child != null) {
                        if (myShowWhiteSpaces || child.getElementType() != TokenType.WHITE_SPACE) {
                            PsiElement childElement = child.getPsi();
                            list.add(childElement == null ? child : childElement);
                        }
                        child = child.getTreeNext();
                    }
                    PsiElement psi = root.getPsi();
                    if (psi instanceof PsiLanguageInjectionHost) {
                        InjectedLanguageManager.getInstance(myProject).enumerate(
                            psi,
                            (injectedPsi, places) -> list.add(new Inject(psi, injectedPsi))
                        );
                    }
                }
                result = ArrayUtil.toObjectArray(list);
            }
            else {
                PsiElement[] elementChildren = ((PsiElement) element).getChildren();
                if (!myShowWhiteSpaces) {
                    List<PsiElement> childrenList = new ArrayList<>(elementChildren.length);
                    for (PsiElement psiElement : elementChildren) {
                        if (!myShowWhiteSpaces && psiElement instanceof PsiWhiteSpace) {
                            continue;
                        }
                        childrenList.add(psiElement);
                    }
                    result = PsiUtilCore.toPsiElementArray(childrenList);
                }
                else {
                    result = elementChildren;
                }
            }
            children[0] = result;
        });
        return children[0];
    }

    @Override
    public Object getParentElement(@Nonnull Object element) {
        if (element == myRootElement) {
            return null;
        }
        if (element == myRootPsiElement) {
            return myRootElement;
        }
        if (element instanceof PsiFile file
            && InjectedLanguageManager.getInstance(file.getProject()).getInjectionHost(file) != null) {
            return new Inject(
                InjectedLanguageManager.getInstance(file.getProject()).getInjectionHost(file),
                (PsiElement) element
            );
        }
        return element instanceof Inject inject ? inject.getParent() : ((PsiElement) element).getContext();
    }

    @Override
    public void commit() {
    }

    @Override
    public boolean hasSomethingToCommit() {
        return false;
    }

    @Override
    @Nonnull
    public NodeDescriptor createDescriptor(@Nonnull Object element, NodeDescriptor parentDescriptor) {
        if (element == myRootElement) {
            return new NodeDescriptor(null) {
                @RequiredUIAccess
                @Override
                public boolean update() {
                    return false;
                }

                @Override
                public Object getElement() {
                    return myRootElement;
                }
            };
        }
        return new ViewerNodeDescriptor(myProject, element, parentDescriptor);
    }

    public boolean isShowWhiteSpaces() {
        return myShowWhiteSpaces;
    }

    public void setShowWhiteSpaces(boolean showWhiteSpaces) {
        myShowWhiteSpaces = showWhiteSpaces;
    }

    public boolean isShowTreeNodes() {
        return myShowTreeNodes;
    }

    public void setShowTreeNodes(boolean showTreeNodes) {
        myShowTreeNodes = showTreeNodes;
    }

    static class Inject {
        private final PsiElement myParent;
        private final PsiElement myPsi;

        Inject(PsiElement parent, PsiElement psi) {
            myParent = parent;
            myPsi = psi;
        }

        public PsiElement getParent() {
            return myParent;
        }

        public PsiElement getPsi() {
            return myPsi;
        }

        @Override
        public String toString() {
            return "INJECTION " + myPsi.getLanguage();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Inject inject = (Inject) o;

            return myParent.equals(inject.myParent) && myPsi.equals(inject.myPsi);
        }

        @Override
        public int hashCode() {
            return 31 * myParent.hashCode() + myPsi.hashCode();
        }
    }
}
