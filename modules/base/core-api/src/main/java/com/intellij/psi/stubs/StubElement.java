/*
 * Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.psi.stubs;

import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.util.ArrayFactory;
import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import java.util.List;

/**
 * @author max
 */
public interface StubElement<T extends PsiElement> extends Stub {
  @Override
  IStubElementType getStubType();

  @Override
  StubElement getParentStub();

  @Override
  @Nonnull
  List<StubElement> getChildrenStubs();

  @Nullable
  <P extends PsiElement, S extends StubElement<P>> S findChildStubByType(@Nonnull IStubElementType<S, P> elementType);

  T getPsi();

  @Nonnull
  <E extends PsiElement> E[] getChildrenByType(@Nonnull IElementType elementType, final E[] array);

  @Nonnull
  <E extends PsiElement> E[] getChildrenByType(@Nonnull TokenSet filter, final E[] array);

  @Nonnull
  <E extends PsiElement> E[] getChildrenByType(@Nonnull IElementType elementType, @Nonnull ArrayFactory<E> f);

  @Nonnull
  <E extends PsiElement> E[] getChildrenByType(@Nonnull TokenSet filter, @Nonnull ArrayFactory<E> f);

  @Nullable
  <E extends PsiElement> E getParentStubOfType(@Nonnull Class<E> parentClass);
}