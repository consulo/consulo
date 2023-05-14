package consulo.language.psi.stub;

import consulo.annotation.access.RequiredReadAction;
import consulo.language.Language;
import consulo.language.psi.PsiElement;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.IOException;

/**
 * @author peter
 */
public abstract class EmptyStubElementType<T extends PsiElement> extends IStubElementType<EmptyStub, T> {
  protected EmptyStubElementType(@Nonnull @NonNls String debugName, @Nullable Language language) {
    super(debugName, language);
  }

  @Nonnull
  @RequiredReadAction
  @Override
  public final EmptyStub createStub(@Nonnull T psi, StubElement parentStub) {
    return createStub(parentStub);
  }

  protected EmptyStub createStub(StubElement parentStub) {
    return new EmptyStub(parentStub, this);
  }

  @Nonnull
  @Override
  public String getExternalId() {
    return getLanguage().getID() + toString();
  }

  @Override
  public final void serialize(@Nonnull EmptyStub stub, @Nonnull StubOutputStream dataStream) throws IOException {
  }

  @Nonnull
  @Override
  public final EmptyStub deserialize(@Nonnull StubInputStream dataStream, StubElement parentStub) throws IOException {
    return createStub(parentStub);
  }

  @Override
  public final void indexStub(@Nonnull EmptyStub stub, @Nonnull IndexSink sink) {
  }
}
