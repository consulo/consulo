package consulo.ide.impl.idea.model.search.impl;

import consulo.application.util.query.Query;
import javax.annotation.Nonnull;

// from kotlin
public interface DecomposableQuery<R> extends Query<R> {
  @Nonnull
  Requests<R> decompose();
}

