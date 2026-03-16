/*
 * Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package consulo.language.psi.stub;

import consulo.language.psi.PsiElement;
import consulo.language.ast.IElementType;
import consulo.language.ast.TokenSet;

import org.jspecify.annotations.Nullable;
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

  @Nullable
  PsiFileStub<?> getContainingFileStub();

  @Override
  
  List<StubElement> getChildrenStubs();

  @Nullable
  <P extends PsiElement, S extends StubElement<P>> S findChildStubByType(IStubElementType<S, P> elementType);

  T getPsi();

  
  <E extends PsiElement> E[] getChildrenByType(IElementType elementType, E[] array);

  
  <E extends PsiElement> E[] getChildrenByType(TokenSet filter, E[] array);

  
  <E extends PsiElement> E[] getChildrenByType(IElementType elementType, IntFunction<E[]> f);

  
  <E extends PsiElement> E[] getChildrenByType(TokenSet filter, IntFunction<E[]> f);

  @Nullable
  <E extends PsiElement> E getParentStubOfType(Class<E> parentClass);
}