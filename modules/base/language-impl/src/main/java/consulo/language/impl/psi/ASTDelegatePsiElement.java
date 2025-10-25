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
package consulo.language.impl.psi;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.ast.TokenSet;
import consulo.project.internal.SingleProjectHolder;
import consulo.language.impl.ast.ChangeUtil;
import consulo.language.impl.ast.CompositeElement;
import consulo.language.impl.ast.TreeElement;
import consulo.language.impl.ast.SharedImplUtil;
import consulo.language.psi.*;
import consulo.language.util.IncorrectOperationException;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import consulo.util.dataholder.Key;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author max
 */
public abstract class ASTDelegatePsiElement extends PsiElementBase implements PsiElementWithSubtreeChangeNotifier {
    private static final Logger LOG = Logger.getInstance(ASTDelegatePsiElement.class);

    @Nonnull
    @Override
    public PsiManager getManager() {
        Project project = SingleProjectHolder.theOnlyOpenProject();
        if (project != null) {
            return PsiManager.getInstance(project);
        }
        PsiElement parent = this;

        while (parent instanceof ASTDelegatePsiElement) {
            parent = parent.getParent();
        }

        if (parent == null) {
            throw new PsiInvalidElementAccessException(this);
        }

        return parent.getManager();
    }

    @Nonnull
    @Override
    @RequiredReadAction
    public PsiElement[] getChildren() {
        PsiElement psiChild = getFirstChild();
        if (psiChild == null) {
            return PsiElement.EMPTY_ARRAY;
        }

        List<PsiElement> result = new ArrayList<>();
        while (psiChild != null) {
            if (psiChild.getNode() instanceof CompositeElement) {
                result.add(psiChild);
            }
            psiChild = psiChild.getNextSibling();
        }
        return PsiUtilCore.toPsiElementArray(result);
    }

    @Override
    @RequiredReadAction
    @Nullable
    public PsiElement getFirstChild() {
        return SharedImplUtil.getFirstChild(getNode());
    }

    @Nullable
    @Override
    @RequiredReadAction
    public PsiElement getLastChild() {
        return SharedImplUtil.getLastChild(getNode());
    }

    @Nullable
    @Override
    @RequiredReadAction
    public PsiElement getNextSibling() {
        return SharedImplUtil.getNextSibling(getNode());
    }

    @Nullable
    @Override
    @RequiredReadAction
    public PsiElement getPrevSibling() {
        return SharedImplUtil.getPrevSibling(getNode());
    }

    @Nonnull
    @Override
    @RequiredReadAction
    public TextRange getTextRange() {
        return getNode().getTextRange();
    }

    @Override
    @RequiredReadAction
    public int getStartOffsetInParent() {
        return getNode().getStartOffset() - getNode().getTreeParent().getStartOffset();
    }

    @Override
    @RequiredReadAction
    public int getTextLength() {
        return getNode().getTextLength();
    }

    @Override
    @RequiredReadAction
    public PsiElement findElementAt(int offset) {
        ASTNode treeElement = getNode().findLeafElementAt(offset);
        return SourceTreeToPsiMap.treeElementToPsi(treeElement);
    }

    @Override
    @RequiredReadAction
    public int getTextOffset() {
        return getNode().getStartOffset();
    }

    @Override
    @RequiredReadAction
    public String getText() {
        return getNode().getText();
    }

    @Override
    @Nonnull
    @RequiredReadAction
    public char[] textToCharArray() {
        return getNode().getText().toCharArray();
    }

    @Override
    @RequiredReadAction
    public boolean textContains(char c) {
        return getNode().textContains(c);
    }

    @Override
    @RequiredReadAction
    public <T> T getCopyableUserData(Key<T> key) {
        return getNode().getCopyableUserData(key);
    }

    @Override
    @RequiredReadAction
    public <T> void putCopyableUserData(Key<T> key, T value) {
        getNode().putCopyableUserData(key, value);
    }

    @Override
    @Nonnull
    @RequiredReadAction
    public abstract ASTNode getNode();

    @Override
    public void subtreeChanged() {
    }

    @Override
    @Nonnull
    @RequiredReadAction
    public Language getLanguage() {
        return getNode().getElementType().getLanguage();
    }

    @Nullable
    @RequiredReadAction
    @SuppressWarnings("unchecked")
    protected <T extends PsiElement> T findChildByType(IElementType type) {
        ASTNode node = getNode().findChildByType(type);
        return node == null ? null : (T) node.getPsi();
    }

    @Nullable
    @RequiredReadAction
    @SuppressWarnings("unchecked")
    protected <T extends PsiElement> T findLastChildByType(IElementType type) {
        PsiElement child = getLastChild();
        while (child != null) {
            ASTNode node = child.getNode();
            if (node != null && node.getElementType() == type) {
                return (T) child;
            }
            child = child.getPrevSibling();
        }
        return null;
    }

    @Nonnull
    @RequiredReadAction
    protected <T extends PsiElement> T findNotNullChildByType(IElementType type) {
        return notNullChild(findChildByType(type));
    }

    @Nullable
    @RequiredReadAction
    @SuppressWarnings("unchecked")
    protected <T extends PsiElement> T findChildByType(TokenSet type) {
        ASTNode node = getNode().findChildByType(type);
        return node == null ? null : (T) node.getPsi();
    }

    @Nonnull
    @RequiredReadAction
    @SuppressWarnings("unchecked")
    protected <T extends PsiElement> T findNotNullChildByType(TokenSet type) {
        return notNullChild(findChildByType(type));
    }

    @Nullable
    @RequiredReadAction
    @SuppressWarnings("unchecked")
    protected <T extends PsiElement> T findChildByFilter(TokenSet tokenSet) {
        ASTNode[] nodes = getNode().getChildren(tokenSet);
        return nodes.length == 0 ? null : (T) nodes[0].getPsi();
    }

    @Nonnull
    @RequiredReadAction
    protected <T extends PsiElement> T findNotNullChildByFilter(TokenSet tokenSet) {
        return notNullChild(findChildByFilter(tokenSet));
    }

    @Nonnull
    @RequiredReadAction
    @SuppressWarnings("unchecked")
    protected <T extends PsiElement> T[] findChildrenByType(IElementType elementType, Class<T> arrayClass) {
        return ContainerUtil.map2Array(SharedImplUtil.getChildrenOfType(getNode(), elementType), arrayClass, s -> (T) s.getPsi());
    }

    @RequiredReadAction
    @Nonnull
    @SuppressWarnings("unchecked")
    protected <T extends PsiElement> List<T> findChildrenByType(TokenSet elementType) {
        List<T> result = List.of();
        ASTNode child = getNode().getFirstChildNode();
        while (child != null) {
            IElementType tt = child.getElementType();
            if (elementType.contains(tt)) {
                if (result == List.<T>of()) {
                    result = new ArrayList<>();
                }
                result.add((T) child.getPsi());
            }
            child = child.getTreeNext();
        }
        return result;
    }

    @Nonnull
    @RequiredReadAction
    @SuppressWarnings("unchecked")
    protected <T extends PsiElement> List<T> findChildrenByType(IElementType elementType) {
        List<T> result = List.of();
        ASTNode child = getNode().getFirstChildNode();
        while (child != null) {
            if (elementType == child.getElementType()) {
                if (result == List.<T>of()) {
                    result = new ArrayList<>();
                }
                result.add((T) child.getPsi());
            }
            child = child.getTreeNext();
        }
        return result;
    }

    @Nonnull
    @RequiredReadAction
    @SuppressWarnings("unchecked")
    protected <T extends PsiElement> T[] findChildrenByType(TokenSet elementType, Class<T> arrayClass) {
        return ContainerUtil.map2Array(getNode().getChildren(elementType), arrayClass, s -> (T) s.getPsi());
    }

    @Override
    @RequiredReadAction
    public PsiElement copy() {
        return getNode().copyElement().getPsi();
    }

    @Override
    @RequiredWriteAction
    public PsiElement add(@Nonnull PsiElement element) throws IncorrectOperationException {
        return addInnerBefore(element, null);
    }

    @Override
    @RequiredWriteAction
    public PsiElement addBefore(@Nonnull PsiElement element, PsiElement anchor) throws IncorrectOperationException {
        return addInnerBefore(element, anchor);
    }

    @RequiredWriteAction
    private PsiElement addInnerBefore(PsiElement element, PsiElement anchor) throws IncorrectOperationException {
        CheckUtil.checkWritable(this);
        TreeElement elementCopy = ChangeUtil.copyToElement(element);
        ASTNode treeElement = addInternal(elementCopy, elementCopy, SourceTreeToPsiMap.psiElementToTree(anchor), Boolean.TRUE);
        if (treeElement != null) {
            if (treeElement instanceof TreeElement treeElem) {
                return ChangeUtil.decodeInformation(treeElem).getPsi();
            }
            return treeElement.getPsi();
        }
        throw new IncorrectOperationException("Element cannot be added");
    }

    @Override
    @RequiredWriteAction
    public PsiElement addAfter(@Nonnull PsiElement element, PsiElement anchor) throws IncorrectOperationException {
        CheckUtil.checkWritable(this);
        TreeElement elementCopy = ChangeUtil.copyToElement(element);
        ASTNode treeElement = addInternal(elementCopy, elementCopy, SourceTreeToPsiMap.psiElementToTree(anchor), Boolean.FALSE);
        if (treeElement instanceof TreeElement treeElem) {
            return ChangeUtil.decodeInformation(treeElem).getPsi();
        }
        return treeElement.getPsi();
    }

    @Override
    public void checkAdd(@Nonnull PsiElement element) throws IncorrectOperationException {
        CheckUtil.checkWritable(this);
    }

    @RequiredWriteAction
    public ASTNode addInternal(ASTNode first, ASTNode last, ASTNode anchor, Boolean before) {
        return CodeEditUtil.addChildren(getNode(), first, last, getAnchorNode(anchor, before));
    }

    @Override
    @RequiredWriteAction
    public PsiElement addRange(PsiElement first, PsiElement last) throws IncorrectOperationException {
        return SharedImplUtil.addRange(this, first, last, null, null);
    }

    @Override
    @RequiredWriteAction
    public PsiElement addRangeBefore(@Nonnull PsiElement first, @Nonnull PsiElement last, PsiElement anchor)
        throws IncorrectOperationException {
        return SharedImplUtil.addRange(this, first, last, SourceTreeToPsiMap.psiElementToTree(anchor), Boolean.TRUE);
    }

    @Override
    @RequiredWriteAction
    public PsiElement addRangeAfter(PsiElement first, PsiElement last, PsiElement anchor) throws IncorrectOperationException {
        return SharedImplUtil.addRange(this, first, last, SourceTreeToPsiMap.psiElementToTree(anchor), Boolean.FALSE);
    }

    @Override
    @RequiredWriteAction
    public void delete() throws IncorrectOperationException {
        PsiElement parent = getParent();
        if (parent instanceof ASTDelegatePsiElement delegatePsiElement) {
            CheckUtil.checkWritable(this);
            delegatePsiElement.deleteChildInternal(getNode());
        }
        else if (parent instanceof CompositePsiElement compositePsiElement) {
            CheckUtil.checkWritable(this);
            compositePsiElement.deleteChildInternal(getNode());
        }
        else if (parent instanceof PsiFile) {
            CheckUtil.checkWritable(this);
            parent.deleteChildRange(this, this);
        }
        else {
            throw new UnsupportedOperationException(
                getClass().getName() + " under " + (parent == null ? "null" : parent.getClass().getName())
            );
        }
    }

    @RequiredWriteAction
    public void deleteChildInternal(@Nonnull ASTNode child) {
        CodeEditUtil.removeChild(getNode(), child);
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

        LOG.assertTrue(firstElement.getTreeParent() == getNode());
        LOG.assertTrue(lastElement.getTreeParent() == getNode());
        CodeEditUtil.removeChildren(getNode(), firstElement, lastElement);
    }

    @Override
    @RequiredWriteAction
    public PsiElement replace(@Nonnull PsiElement newElement) throws IncorrectOperationException {
        CheckUtil.checkWritable(this);
        TreeElement elementCopy = ChangeUtil.copyToElement(newElement);
        if (getParent() instanceof ASTDelegatePsiElement) {
            ASTDelegatePsiElement parentElement = (ASTDelegatePsiElement) getParent();
            parentElement.replaceChildInternal(this, elementCopy);
        }
        else {
            CodeEditUtil.replaceChild(getParent().getNode(), getNode(), elementCopy);
        }
        elementCopy = ChangeUtil.decodeInformation(elementCopy);
        return SourceTreeToPsiMap.treeElementToPsi(elementCopy);
    }

    @RequiredWriteAction
    public void replaceChildInternal(PsiElement child, TreeElement newElement) {
        CodeEditUtil.replaceChild(getNode(), child.getNode(), newElement);
    }

    @RequiredReadAction
    private ASTNode getAnchorNode(ASTNode anchor, Boolean before) {
        ASTNode anchorBefore;
        if (anchor != null) {
            anchorBefore = before ? anchor : anchor.getTreeNext();
        }
        else if (before != null && !before) {
            anchorBefore = getNode().getFirstChildNode();
        }
        else {
            anchorBefore = null;
        }
        return anchorBefore;
    }
}
