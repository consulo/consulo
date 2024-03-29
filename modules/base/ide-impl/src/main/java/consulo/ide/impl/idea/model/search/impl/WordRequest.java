package consulo.ide.impl.idea.model.search.impl;

import consulo.ide.impl.psi.impl.search.LeafOccurrence;
import consulo.ide.impl.psi.impl.search.WordRequestInfoImpl;
import jakarta.annotation.Nonnull;

// from kotlin
public final class WordRequest<R> {
  private WordRequestInfoImpl searchWordRequest;

  private InjectionInfo injectionInfo;

  private XTransformation<? super LeafOccurrence, ? extends R> transformation;

  public WordRequest(@Nonnull WordRequestInfoImpl searchWordRequest, @Nonnull InjectionInfo injectionInfo, @Nonnull XTransformation<? super LeafOccurrence, ? extends R> transformation) {
    this.searchWordRequest = searchWordRequest;
    this.injectionInfo = injectionInfo;
    this.transformation = transformation;
  }

  @Nonnull
  public final WordRequestInfoImpl getSearchWordRequest() {
    return searchWordRequest;
  }

  @Nonnull
  public final InjectionInfo getInjectionInfo() {
    return injectionInfo;
  }

  @Nonnull
  public final XTransformation<? super LeafOccurrence, ? extends R> getTransformation() {
    return transformation;
  }
}
