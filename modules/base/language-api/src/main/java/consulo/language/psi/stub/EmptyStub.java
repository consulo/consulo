package consulo.language.psi.stub;

import consulo.language.psi.PsiElement;

/**
 * @author peter
 */
public class EmptyStub<T extends PsiElement> extends StubBase<T> {

  public EmptyStub(StubElement parent, IStubElementType elementType) {
    super(parent, elementType);
  }
}
