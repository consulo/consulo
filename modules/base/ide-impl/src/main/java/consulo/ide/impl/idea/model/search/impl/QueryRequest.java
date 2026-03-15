package consulo.ide.impl.idea.model.search.impl;

import consulo.application.util.query.Query;

// from kotlin
public final class QueryRequest<B, R> {
  private Query<? extends B> query;

  private XTransformation<? super B, ? extends R> transformation;

  public QueryRequest(Query<? extends B> query, XTransformation<? super B, ? extends R> transformation) {
    this.query = query;
    this.transformation = transformation;
  }

  
  public final Query<? extends B> getQuery() {
    return query;
  }

  
  public final XTransformation<? super B, ? extends R> getTransformation() {
    return transformation;
  }
}
