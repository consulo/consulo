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

package consulo.language.impl.ast;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.language.ast.ASTNode;
import consulo.language.Language;
import consulo.language.psi.*;
import consulo.language.psi.resolve.ResolveState;
import consulo.language.version.LanguageVersion;
import consulo.project.Project;
import consulo.language.impl.psi.CheckUtil;
import consulo.language.impl.psi.ResolveScopeManager;
import consulo.language.impl.internal.psi.SharedPsiElementImplUtil;
import consulo.language.impl.psi.SourceTreeToPsiMap;
import consulo.language.psi.resolve.PsiScopeProcessor;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.content.scope.SearchScope;
import consulo.language.ast.IElementType;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.util.IncorrectOperationException;
import consulo.logging.Logger;

import jakarta.annotation.Nonnull;

public class OwnBufferLeafPsiElement extends LeafElement implements PsiElement {
  private static final Logger LOG = Logger.getInstance(OwnBufferLeafPsiElement.class);

  public OwnBufferLeafPsiElement(IElementType type, CharSequence text) {
    super(type, text);
  }

  @RequiredReadAction
  @Override
  @Nonnull
  public PsiElement[] getChildren() {
    return PsiElement.EMPTY_ARRAY;
  }

  @RequiredReadAction
  @Override
  public PsiElement getFirstChild() {
    return null;
  }

  @RequiredReadAction
  @Override
  public PsiElement getLastChild() {
    return null;
  }

  @Override
  public void acceptChildren(@Nonnull PsiElementVisitor visitor) {
  }

  @Override
  public PsiElement getParent() {
    return SharedImplUtil.getParent(this);
  }

  @RequiredReadAction
  @Override
  public PsiElement getNextSibling() {
    return SharedImplUtil.getNextSibling(this);
  }

  @RequiredReadAction
  @Override
  public PsiElement getPrevSibling() {
    return SharedImplUtil.getPrevSibling(this);
  }

  @Override
  public PsiFile getContainingFile() {
    PsiFile file = SharedImplUtil.getContainingFile(this);
    if (file == null || !file.isValid()) throw new PsiInvalidElementAccessException(this);
    return file;
  }

  @RequiredReadAction
  @Override
  public PsiElement findElementAt(int offset) {
    return this;
  }

  @RequiredReadAction
  @Override
  public PsiReference findReferenceAt(int offset) {
    return SharedPsiElementImplUtil.findReferenceAt(this, offset);
  }

  @Override
  public PsiElement copy() {
    ASTNode elementCopy = copyElement();
    return SourceTreeToPsiMap.treeElementToPsi(elementCopy);
  }

  @Override
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
  @Nonnull
  public PsiReference[] getReferences() {
    return SharedPsiElementImplUtil.getReferences(this);
  }

  @Override
  public PsiElement add(@Nonnull PsiElement element) throws IncorrectOperationException {
    throw new IncorrectOperationException();
  }

  @RequiredWriteAction
  @Override
  public PsiElement addBefore(@Nonnull PsiElement element, PsiElement anchor) throws IncorrectOperationException {
    throw new IncorrectOperationException();
  }

  @RequiredWriteAction
  @Override
  public PsiElement addAfter(@Nonnull PsiElement element, PsiElement anchor) throws IncorrectOperationException {
    throw new IncorrectOperationException();
  }

  @Override
  public void checkAdd(@Nonnull PsiElement element) throws IncorrectOperationException {
    throw new IncorrectOperationException();
  }

  @RequiredWriteAction
  @Override
  public PsiElement addRange(PsiElement first, PsiElement last) throws IncorrectOperationException {
    throw new IncorrectOperationException();
  }

  @RequiredWriteAction
  @Override
  public PsiElement addRangeBefore(@Nonnull PsiElement first, @Nonnull PsiElement last, PsiElement anchor)
    throws IncorrectOperationException {
    throw new IncorrectOperationException();
  }

  @RequiredWriteAction
  @Override
  public PsiElement addRangeAfter(PsiElement first, PsiElement last, PsiElement anchor)
    throws IncorrectOperationException {
    throw new IncorrectOperationException();
  }

  @RequiredWriteAction
  @Override
  public void delete() throws IncorrectOperationException {
    LOG.assertTrue(getTreeParent() != null);
    CheckUtil.checkWritable(this);
    getTreeParent().deleteChildInternal(this);
    this.invalidate();
  }

  @Override
  public void checkDelete() throws IncorrectOperationException {
    CheckUtil.checkWritable(this);
  }

  @RequiredWriteAction
  @Override
  public void deleteChildRange(PsiElement first, PsiElement last) throws IncorrectOperationException {
    throw new IncorrectOperationException();
  }

  @RequiredWriteAction
  @Override
  public PsiElement replace(@Nonnull PsiElement newElement) throws IncorrectOperationException {
    LOG.assertTrue(getTreeParent() != null);
    CheckUtil.checkWritable(this);
    TreeElement elementCopy = ChangeUtil.copyToElement(newElement);
    getTreeParent().replaceChildInternal(this, elementCopy);
    elementCopy = ChangeUtil.decodeInformation(elementCopy);
    PsiElement result = SourceTreeToPsiMap.treeElementToPsi(elementCopy);

    this.invalidate();
    return result;
  }

  @Override
  public String toString() {
    return "PsiElement" + "(" + getElementType().toString() + ")";
  }

  @Override
  public void accept(@Nonnull PsiElementVisitor visitor) {
    visitor.visitElement(this);
  }

  @Override
  public boolean processDeclarations(@Nonnull PsiScopeProcessor processor,
                                     @Nonnull ResolveState state,
                                     PsiElement lastParent,
                                     @Nonnull PsiElement place) {
    return true;
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
  @Nonnull
  public GlobalSearchScope getResolveScope() {
    return ResolveScopeManager.getElementResolveScope(this);
  }

  @Override
  @Nonnull
  public SearchScope getUseScope() {
    return ResolveScopeManager.getElementUseScope(this);
  }

  @Override
  @Nonnull
  public Project getProject() {
    PsiManager manager = getManager();
    if (manager == null) throw new PsiInvalidElementAccessException(this);

    return manager.getProject();
  }

  @RequiredReadAction
  @Override
  @Nonnull
  public Language getLanguage() {
    return getElementType().getLanguage();
  }

  @Override
  public ASTNode getNode() {
    return this;
  }

  @Override
  public PsiElement getPsi() {
    return this;
  }

  @Override
  public boolean isEquivalentTo(PsiElement another) {
    return this == another;
  }

  @RequiredReadAction
  @Nonnull
  @Override
  public LanguageVersion getLanguageVersion() {
    return PsiTreeUtil.getLanguageVersion(this);
  }
}
