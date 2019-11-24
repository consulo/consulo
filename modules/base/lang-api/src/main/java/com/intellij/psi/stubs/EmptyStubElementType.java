package com.intellij.psi.stubs;

import com.intellij.lang.Language;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;

import consulo.annotation.access.RequiredReadAction;

import java.io.IOException;

/**
 * @author peter
 */
public abstract class EmptyStubElementType<T extends PsiElement> extends IStubElementType<EmptyStub, T> {
  protected EmptyStubElementType(@Nonnull @NonNls String debugName, @javax.annotation.Nullable Language language) {
    super(debugName, language);
  }

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
