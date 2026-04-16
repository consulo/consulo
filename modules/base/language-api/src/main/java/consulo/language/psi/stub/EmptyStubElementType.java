package consulo.language.psi.stub;

import consulo.annotation.access.RequiredReadAction;
import consulo.language.Language;
import consulo.language.psi.PsiElement;

import org.jspecify.annotations.Nullable;
import java.io.IOException;
import java.util.Objects;

/**
 * @author peter
 */
public abstract class EmptyStubElementType<T extends PsiElement> extends IStubElementType<EmptyStub, T> {
  protected EmptyStubElementType(String debugName, @Nullable Language language) {
    super(debugName, language);
  }

  @RequiredReadAction
  @Override
  public final EmptyStub createStub(T psi, StubElement parentStub) {
    return createStub(parentStub);
  }

  protected EmptyStub createStub(StubElement parentStub) {
    return new EmptyStub(parentStub, this);
  }

  @Override
  public String getExternalId() {
    return getLanguage().getID() + toString();
  }

  @Override
  public final void serialize(EmptyStub stub, StubOutputStream dataStream) throws IOException {
  }

  @Override
  public final EmptyStub deserialize(StubInputStream dataStream, @Nullable StubElement parentStub) throws IOException {
    return createStub(Objects.requireNonNull(parentStub));
  }

  @Override
  public final void indexStub(EmptyStub stub, IndexSink sink) {
  }
}
