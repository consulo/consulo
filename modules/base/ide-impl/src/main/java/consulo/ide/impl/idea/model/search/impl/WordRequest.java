package consulo.ide.impl.idea.model.search.impl;

import consulo.ide.impl.psi.impl.search.LeafOccurrence;
import consulo.ide.impl.psi.impl.search.WordRequestInfoImpl;

// from kotlin
public final class WordRequest<R> {
  private WordRequestInfoImpl searchWordRequest;

  private InjectionInfo injectionInfo;

  private XTransformation<? super LeafOccurrence, ? extends R> transformation;

  public WordRequest(WordRequestInfoImpl searchWordRequest, InjectionInfo injectionInfo, XTransformation<? super LeafOccurrence, ? extends R> transformation) {
    this.searchWordRequest = searchWordRequest;
    this.injectionInfo = injectionInfo;
    this.transformation = transformation;
  }

  
  public final WordRequestInfoImpl getSearchWordRequest() {
    return searchWordRequest;
  }

  
  public final InjectionInfo getInjectionInfo() {
    return injectionInfo;
  }

  
  public final XTransformation<? super LeafOccurrence, ? extends R> getTransformation() {
    return transformation;
  }
}
