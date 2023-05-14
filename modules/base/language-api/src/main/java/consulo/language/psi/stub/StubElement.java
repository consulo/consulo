/*
 * Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package consulo.language.psi.stub;

import consulo.language.psi.PsiElement;
import consulo.language.ast.IElementType;
import consulo.language.ast.TokenSet;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.function.IntFunction;

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
  <E extends PsiElement> E[] getChildrenByType(@Nonnull IElementType elementType, @Nonnull IntFunction<E[]> f);

  @Nonnull
  <E extends PsiElement> E[] getChildrenByType(@Nonnull TokenSet filter, @Nonnull IntFunction<E[]> f);

  @Nullable
  <E extends PsiElement> E getParentStubOfType(@Nonnull Class<E> parentClass);
}