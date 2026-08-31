/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.language.impl.psi;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.content.scope.SearchScope;
import consulo.language.Language;
import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.ast.TokenSet;
import consulo.project.internal.SingleProjectHolder;
import consulo.language.impl.ast.ChangeUtil;
import consulo.language.impl.ast.CompositeElement;
import consulo.language.impl.ast.LazyParseableElement;
import consulo.language.impl.ast.TreeElement;
import consulo.language.impl.ast.SharedImplUtil;
import consulo.language.impl.internal.psi.SharedPsiElementImplUtil;
import consulo.language.psi.*;
import consulo.language.psi.resolve.PsiScopeProcessor;
import consulo.language.psi.resolve.ResolveState;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.util.IncorrectOperationException;
import consulo.language.version.LanguageVersion;
import consulo.logging.Logger;
import consulo.navigation.ItemPresentation;
import consulo.navigation.NavigationItem;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.collection.ArrayUtil;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author max
 */
public class LazyParseablePsiElement extends LazyParseableElement implements PsiElement, NavigationItem {
    private static final Logger LOG = Logger.getInstance(LazyParseablePsiElement.class);

    public LazyParseablePsiElement(IElementType type, @Nullable CharSequence buffer) {
        super(type, buffer);
        setPsi(this);
    }

    @Override
    public LazyParseablePsiElement clone() {
        LazyParseablePsiElement clone = (LazyParseablePsiElement) super.clone();
        clone.setPsi(clone);
        return clone;
    }

    @Override
    @RequiredReadAction
    public PsiElement[] getChildren() {
        return getChildrenAsPsiElements((TokenSet) null, PsiElement.ARRAY_FACTORY);
    }

    @RequiredReadAction
    @SuppressWarnings("unchecked")
    protected @Nullable <T> T findChildByClass(Class<T> aClass) {
        for (PsiElement cur = getFirstChild(); cur != null; cur = cur.getNextSibling()) {
            if (aClass.isInstance(cur)) {
                return (T) cur;
            }
        }
        return null;
    }

    @RequiredReadAction
    @SuppressWarnings("unchecked")
    protected <T> T[] findChildrenByClass(Class<T> aClass) {
        List<T> result = new ArrayList<>();
        for (PsiElement cur = getFirstChild(); cur != null; cur = cur.getNextSibling()) {
            if (aClass.isInstance(cur)) {
                result.add((T) cur);
            }
        }
        return result.toArray(ArrayUtil.newArray(aClass, result.size()));
    }

    @Override
    @RequiredReadAction
    public PsiElement getFirstChild() {
        TreeElement child = getFirstChildNode();
        if (child == null) {
            return null;
        }
        return child.getPsi();
    }

    @Override
    @RequiredReadAction
    public PsiElement getLastChild() {
        TreeElement child = getLastChildNode();
        if (child == null) {
            return null;
        }
        return child.getPsi();
    }

    @Override
    @RequiredReadAction
    public void acceptChildren(PsiElementVisitor visitor) {
        PsiElement child = getFirstChild();
        while (child != null) {
            child.accept(visitor);
            child = child.getNextSibling();
        }
    }

    @Override
    @RequiredReadAction
    public PsiElement getParent() {
        CompositeElement treeParent = getTreeParent();
        if (treeParent == null) {
            return null;
        }
        if (treeParent instanceof PsiElement) {
            return (PsiElement) treeParent;
        }
        return treeParent.getPsi();
    }

    @Override
    @RequiredReadAction
    public PsiElement getNextSibling() {
        return SharedImplUtil.getNextSibling(this);
    }

    @Override
    @RequiredReadAction
    public PsiElement getPrevSibling() {
        return SharedImplUtil.getPrevSibling(this);
    }

    @Override
    public PsiFile getContainingFile() {
        PsiFile file = SharedImplUtil.getContainingFile(this);
        if (file == null) {
            throw new PsiInvalidElementAccessException(this);
        }
        return file;
    }

    @Override
    public PsiElement findElementAt(int offset) {
        ASTNode leaf = findLeafElementAt(offset);
        return SourceTreeToPsiMap.treeElementToPsi(leaf);
    }

    @Override
    @RequiredReadAction
    public PsiReference findReferenceAt(int offset) {
        return SharedPsiElementImplUtil.findReferenceAt(this, offset);
    }

    @Override
    public PsiElement copy() {
        ASTNode elementCopy = copyElement();
        return SourceTreeToPsiMap.treeElementToPsi(elementCopy);
    }

    @Override
    @RequiredReadAction
    public boolean isValid() {
        return SharedImplUtil.isValid(this);
    }

    @Override
    public boolean isWritable() {
        return SharedImplUtil.isWritable(this);
    }

    @Override
    public PsiReference getReference() {
        return null;
    }

    @Override

    public PsiReference[] getReferences() {
        return SharedPsiElementImplUtil.getReferences(this);
    }

    @Override
    @RequiredWriteAction
    public PsiElement add(PsiElement element) throws IncorrectOperationException {
        return addInnerBefore(element, null);
    }

    @Override
    @RequiredWriteAction
    public PsiElement addBefore(PsiElement element, PsiElement anchor) throws IncorrectOperationException {
        return addInnerBefore(element, anchor);
    }

    @Override
    @RequiredWriteAction
    public PsiElement addAfter(PsiElement element, PsiElement anchor) throws IncorrectOperationException {
        CheckUtil.checkWritable(this);
        TreeElement elementCopy = ChangeUtil.copyToElement(element);
        TreeElement treeElement = addInternal(elementCopy, elementCopy, SourceTreeToPsiMap.psiElementToTree(anchor), Boolean.FALSE);
        return ChangeUtil.decodeInformation(treeElement).getPsi();
    }

    @Override
    public final void checkAdd(PsiElement element) throws IncorrectOperationException {
        CheckUtil.checkWritable(this);
    }

    @Override
    @RequiredWriteAction
    public final PsiElement addRange(PsiElement first, PsiElement last) throws IncorrectOperationException {
        return SharedImplUtil.addRange(this, first, last, null, null);
    }

    @Override
    @RequiredWriteAction
    public final PsiElement addRangeBefore(PsiElement first, PsiElement last, PsiElement anchor) throws IncorrectOperationException {
        return SharedImplUtil.addRange(this, first, last, SourceTreeToPsiMap.psiElementToTree(anchor), Boolean.TRUE);
    }

    @Override
    @RequiredWriteAction
    public final PsiElement addRangeAfter(PsiElement first, PsiElement last, PsiElement anchor) throws IncorrectOperationException {
        return SharedImplUtil.addRange(this, first, last, SourceTreeToPsiMap.psiElementToTree(anchor), Boolean.FALSE);
    }

    @Override
    @RequiredWriteAction
    public void delete() throws IncorrectOperationException {
        LOG.assertTrue(getTreeParent() != null, "Parent not found for " + this);
        CheckUtil.checkWritable(this);
        getTreeParent().deleteChildInternal(this);
        invalidate();
    }

    @Override
    public void checkDelete() throws IncorrectOperationException {
        CheckUtil.checkWritable(this);
    }

    @Override
    @RequiredWriteAction
    public void deleteChildRange(PsiElement first, PsiElement last) throws IncorrectOperationException {
        CheckUtil.checkWritable(this);
        ASTNode firstElement = SourceTreeToPsiMap.psiElementToTree(first);
        ASTNode lastElement = SourceTreeToPsiMap.psiElementToTree(last);
        LOG.assertTrue(firstElement.getTreeParent() == this);
        LOG.assertTrue(lastElement.getTreeParent() == this);
        CodeEditUtil.removeChildren(this, firstElement, lastElement);
    }

    @Override
    @RequiredWriteAction
    public PsiElement replace(PsiElement newElement) throws IncorrectOperationException {
        return SharedImplUtil.doReplace(this, this, newElement);
    }

    @Override
    public void accept(PsiElementVisitor visitor) { //TODO: remove this method!!
        visitor.visitElement(this);
    }

    @Override
    public boolean processDeclarations(PsiScopeProcessor processor, ResolveState state, @Nullable PsiElement lastParent, PsiElement place) {
        return true;
    }

    @Override
    public String toString() {
        return "PsiElement" + "(" + getElementType().toString() + ")";
    }

    @Override
    public PsiElement getContext() {
        return getParent();
    }

    @Override
    public PsiElement getNavigationElement() {
        return this;
    }

    @Override
    public PsiElement getOriginalElement() {
        return this;
    }

    @Override
    public boolean isPhysical() {
        PsiFile file = getContainingFile();
        return file != null && file.isPhysical();
    }

    @Override
    public GlobalSearchScope getResolveScope() {
        assert isValid();
        return ResolveScopeManager.getElementResolveScope(this);
    }

    @Override
    public SearchScope getUseScope() {
        return ResolveScopeManager.getElementUseScope(this);
    }

    @Override
    public ItemPresentation getPresentation() {
        return null;
    }

    @Override
    public @Nullable String getName() {
        return null;
    }

    @Override
    @RequiredUIAccess
    public void navigate(boolean requestFocus) {
        PsiNavigationSupport.getInstance().getDescriptor(this).navigate(requestFocus);
    }

    @Override
    @RequiredReadAction
    public boolean canNavigate() {
        return PsiNavigationSupport.getInstance().canNavigate(this);
    }

    @Override
    @RequiredReadAction
    public boolean canNavigateToSource() {
        return canNavigate();
    }

    @Override
    public Project getProject() {
        Project project = SingleProjectHolder.theOnlyOpenProject();
        if (project != null) {
            return project;
        }
        return getManager().getProject();
    }

    @Override
    @RequiredReadAction
    public Language getLanguage() {
        return getElementType().getLanguage();
    }

    @Override
    @RequiredReadAction
    public LanguageVersion getLanguageVersion() {
        return PsiTreeUtil.getLanguageVersion(this);
    }

    @Override
    public ASTNode getNode() {
        return this;
    }

    @RequiredWriteAction
    private PsiElement addInnerBefore(PsiElement element, PsiElement anchor) throws IncorrectOperationException {
        CheckUtil.checkWritable(this);
        TreeElement elementCopy = ChangeUtil.copyToElement(element);
        TreeElement treeElement = addInternal(elementCopy, elementCopy, SourceTreeToPsiMap.psiElementToTree(anchor), Boolean.TRUE);
        if (treeElement != null) {
            return ChangeUtil.decodeInformation(treeElement).getPsi();
        }
        throw new IncorrectOperationException("Element cannot be added");
    }

    @Override
    public boolean isEquivalentTo(PsiElement another) {
        return this == another;
    }
}

