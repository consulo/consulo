package consulo.ide.impl.idea.model.search.impl;

import consulo.application.util.query.Query;
import jakarta.annotation.Nonnull;

// from kotlin
public final class QueryRequest<B, R> {
  private Query<? extends B> query;

  private XTransformation<? super B, ? extends R> transformation;

  public QueryRequest(@Nonnull Query<? extends B> query, @Nonnull XTransformation<? super B, ? extends R> transformation) {
    this.query = query;
    this.transformation = transformation;
  }

  @Nonnull
  public final Query<? extends B> getQuery() {
    return query;
  }

  @Nonnull
  public final XTransformation<? super B, ? extends R> getTransformation() {
    return transformation;
  }
}
