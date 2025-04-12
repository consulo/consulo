package consulo.ide.impl.idea.model.search.impl;

import consulo.application.util.query.Query;
import jakarta.annotation.Nonnull;

// from kotlin
public interface DecomposableQuery<R> extends Query<R> {
    @Nonnull
    Requests<R> decompose();
}

