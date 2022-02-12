package com.intellij.psi.stubs;

import consulo.language.psi.PsiElement;
import consulo.language.psi.stub.IStubElementType;
import consulo.language.psi.stub.StubBase;
import consulo.language.psi.stub.StubElement;

/**
 * @author peter
 */
public class EmptyStub<T extends PsiElement> extends StubBase<T> {

  public EmptyStub(StubElement parent, IStubElementType elementType) {
    super(parent, elementType);
  }
}
