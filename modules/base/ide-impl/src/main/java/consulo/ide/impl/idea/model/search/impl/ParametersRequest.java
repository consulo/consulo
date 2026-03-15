package consulo.ide.impl.idea.model.search.impl;

import consulo.project.util.query.SearchParameters;

// from kotlin
public final class ParametersRequest<B, R> {
  private SearchParameters<B> params;

  private XTransformation<? super B, ? extends R> transformation;

  public ParametersRequest(SearchParameters<B> params, XTransformation<? super B, ? extends R> transformation) {
    this.params = params;
    this.transformation = transformation;
  }

  
  public final SearchParameters<B> getParams() {
    return params;
  }

  
  public final XTransformation<? super B, ? extends R> getTransformation() {
    return transformation;
  }
}

