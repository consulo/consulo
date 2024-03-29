package consulo.ide.impl.idea.model.search.impl;

import consulo.project.util.query.SearchParameters;
import jakarta.annotation.Nonnull;

// from kotlin
public final class ParametersRequest<B, R> {
  private SearchParameters<B> params;

  private XTransformation<? super B, ? extends R> transformation;

  public ParametersRequest(@Nonnull SearchParameters<B> params, @Nonnull XTransformation<? super B, ? extends R> transformation) {
    this.params = params;
    this.transformation = transformation;
  }

  @Nonnull
  public final SearchParameters<B> getParams() {
    return params;
  }

  @Nonnull
  public final XTransformation<? super B, ? extends R> getTransformation() {
    return transformation;
  }
}

