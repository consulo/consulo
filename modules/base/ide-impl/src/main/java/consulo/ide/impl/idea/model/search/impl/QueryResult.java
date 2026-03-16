package consulo.ide.impl.idea.model.search.impl;

import consulo.application.util.query.Query;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

// from kotlin
public final class QueryResult<X> extends XResult<X> {
    private Query<? extends X> query;

    public QueryResult(Query<? extends X> query) {
        super();
        this.query = query;
    }

    @Override
    public boolean process(Predicate<? super X> processor) {
        return getQuery().forEach(processor);
    }

    
    @Override
    public <R> Collection<? extends XResult<R>> transform(XTransformation<? super X, ? extends R> transformation) {
        return List.of(new QueryResult<>(new XQuery<>(getQuery(), transformation)));
    }

    
    public final Query<? extends X> getQuery() {
        return query;
    }
}
