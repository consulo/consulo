package consulo.ide.impl.idea.model.search.impl;

import consulo.application.util.function.Processor;
import consulo.application.util.query.Query;
import jakarta.annotation.Nonnull;

import java.util.Collection;
import java.util.List;

// from kotlin
public final class QueryResult<X> extends XResult<X> {
    private Query<? extends X> query;

    public QueryResult(@Nonnull Query<? extends X> query) {
        super();
        this.query = query;
    }

    @Override
    public boolean process(@Nonnull Processor<? super X> processor) {
        return getQuery().forEach(processor);
    }

    @Nonnull
    @Override
    public <R> Collection<? extends XResult<R>> transform(@Nonnull XTransformation<? super X, ? extends R> transformation) {
        return List.of(new QueryResult<>(new XQuery<>(getQuery(), transformation)));
    }

    @Nonnull
    public final Query<? extends X> getQuery() {
        return query;
    }
}
