/*
 * Copyright 2000-2017 JetBrains s.r.o.
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

package com.intellij.psi.impl;

import com.intellij.ide.util.PsiNavigationSupport;
import com.intellij.navigation.ItemPresentation;
import consulo.logging.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import consulo.util.dataholder.UserDataHolderBase;
import com.intellij.pom.Navigatable;
import com.intellij.psi.*;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import consulo.annotation.access.RequiredReadAction;
import consulo.lang.LanguageVersion;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public abstract class PsiElementBase extends UserDataHolderBase implements NavigatablePsiElement {
  private static final Logger LOG = Logger.getInstance(PsiElementBase.class);

  @RequiredReadAction
  @Override
  public PsiElement getFirstChild() {
    PsiElement[] children = getChildren();
    if (children.length == 0) return null;
    return children[0];
  }

  @RequiredReadAction
  @Override
  public PsiElement getLastChild() {
    PsiElement[] children = getChildren();
    if (children.length == 0) return null;
    return children[children.length - 1];
  }

  @RequiredReadAction
  @Override
  public PsiElement getNextSibling() {
    return SharedPsiElementImplUtil.getNextSibling(this);
  }

  @RequiredReadAction
  @Override
  public PsiElement getPrevSibling() {
    return SharedPsiElementImplUtil.getPrevSibling(this);
  }

  @Override
  public void acceptChildren(@Nonnull PsiElementVisitor visitor) {
    PsiElement child = getFirstChild();
    while (child != null) {
      child.accept(visitor);
      child = child.getNextSibling();
    }
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

  @RequiredReadAction
  @Override
  public PsiReference findReferenceAt(int offset) {
    return SharedPsiElementImplUtil.findReferenceAt(this, offset);
  }

  @Override
  public PsiElement addRange(PsiElement first, PsiElement last) throws IncorrectOperationException {
    throw new IncorrectOperationException("Operation not supported in: " + getClass());
  }

  @Override
  public PsiElement addRangeBefore(@Nonnull PsiElement first, @Nonnull PsiElement last, PsiElement anchor) throws IncorrectOperationException {
    throw new IncorrectOperationException("Operation not supported in: " + getClass());
  }

  @Override
  public PsiElement addRangeAfter(PsiElement first, PsiElement last, PsiElement anchor) throws IncorrectOperationException {
    throw new IncorrectOperationException("Operation not supported in: " + getClass());
  }

  @Override
  public void deleteChildRange(PsiElement first, PsiElement last) throws IncorrectOperationException {
    throw new IncorrectOperationException("Operation not supported in: " + getClass());
  }

  @Override
  public PsiElement copy() {
    return (PsiElement)clone();
  }

  @Override
  public PsiElement add(@Nonnull PsiElement element) throws IncorrectOperationException {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public PsiElement addBefore(@Nonnull PsiElement element, PsiElement anchor) throws IncorrectOperationException {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public PsiElement addAfter(@Nonnull PsiElement element, PsiElement anchor) throws IncorrectOperationException {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public void checkAdd(@Nonnull PsiElement element) throws IncorrectOperationException {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public void delete() throws IncorrectOperationException {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public void checkDelete() throws IncorrectOperationException {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public PsiElement replace(@Nonnull PsiElement newElement) throws IncorrectOperationException {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @RequiredReadAction
  @Override
  public boolean textContains(char c) {
    return getText().indexOf(c) >= 0;
  }

  @Override
  public boolean processDeclarations(@Nonnull PsiScopeProcessor processor, @Nonnull ResolveState state, PsiElement lastParent, @Nonnull PsiElement place) {
    return true;
  }

  @Override
  public PsiElement getContext() {
    return getParent();
  }

  @Override
  @Nonnull
  public PsiElement getNavigationElement() {
    return this;
  }

  @Override
  public PsiElement getOriginalElement() {
    return this;
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
  public void navigate(boolean requestFocus) {
    final Navigatable descriptor = PsiNavigationSupport.getInstance().getDescriptor(this);
    if (descriptor != null) {
      descriptor.navigate(requestFocus);
    }
  }

  @Override
  public boolean canNavigate() {
    return PsiNavigationSupport.getInstance().canNavigate(this);
  }

  @Override
  public boolean canNavigateToSource() {
    return canNavigate();
  }

  @Override
  @Nonnull
  public Project getProject() {
    final PsiManager manager = getManager();
    return manager.getProject();
  }

  //default implementations of methods from NavigationItem
  @Override
  public ItemPresentation getPresentation() {
    return null;
  }

  @Override
  public boolean isEquivalentTo(final PsiElement another) {
    return this == another;
  }

  @Override
  public PsiFile getContainingFile() {
    final PsiElement parent = getParent();
    if (parent == null) throw new PsiInvalidElementAccessException(this);
    return parent.getContainingFile();
  }

  @Override
  public boolean isPhysical() {
    final PsiElement parent = getParent();
    return parent != null && parent.isPhysical();
  }

  @Override
  public boolean isWritable() {
    final PsiElement parent = getParent();
    return parent != null && parent.isWritable();
  }

  @Override
  public boolean isValid() {
    PsiElement parent = getParent();
    while (parent != null && parent.getClass() == this.getClass()) {
      parent = parent.getParent();
    }
    return parent != null && parent.isValid();
  }

  //Q: get rid of these methods?
  @Override
  public boolean textMatches(@Nonnull CharSequence text) {
    return Comparing.equal(getText(), text, true);
  }

  @Override
  public boolean textMatches(@Nonnull PsiElement element) {
    return getText().equals(element.getText());
  }

  @Override
  public void accept(@Nonnull PsiElementVisitor visitor) {
    visitor.visitElement(this);
  }

  @Override
  public String getName() {
    return null;
  }

  @Nonnull
  protected <T> T notNullChild(T child) {
    if (child == null) {
      LOG.error(getText() + "\n parent=" + getParent().getText());
    }
    return child;
  }

  @Nonnull
  protected <T> T[] findChildrenByClass(Class<T> aClass) {
    List<T> result = new ArrayList<>();
    for (PsiElement cur = getFirstChild(); cur != null; cur = cur.getNextSibling()) {
      if (aClass.isInstance(cur)) result.add((T)cur);
    }
    return result.toArray((T[])Array.newInstance(aClass, result.size()));
  }

  @Nullable
  protected <T> T findChildByClass(Class<T> aClass) {
    for (PsiElement cur = getFirstChild(); cur != null; cur = cur.getNextSibling()) {
      if (aClass.isInstance(cur)) return (T)cur;
    }
    return null;
  }

  @Nonnull
  protected <T> T findNotNullChildByClass(Class<T> aClass) {
    return notNullChild(findChildByClass(aClass));
  }

  @Nonnull
  public LanguageVersion getLanguageVersion() {
    return PsiTreeUtil.getLanguageVersion(this);
  }

  @Nonnull
  @Override
  public PsiManager getManager() {
    try {
      return PsiManager.getInstance(getProject());
    }
    catch (StackOverflowError e) {
      throw new IllegalArgumentException("Implementation conflict getProject() + getManager(): Class: " + getClass().getName() , e);
    }
  }
}
