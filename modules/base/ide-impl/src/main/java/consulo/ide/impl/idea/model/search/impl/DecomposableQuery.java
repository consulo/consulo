package consulo.ide.impl.idea.model.search.impl;

import consulo.application.util.query.Query;

// from kotlin
public interface DecomposableQuery<R> extends Query<R> {
    
    Requests<R> decompose();
}

