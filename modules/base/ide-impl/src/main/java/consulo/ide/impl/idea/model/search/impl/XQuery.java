package consulo.ide.impl.idea.model.search.impl;

import consulo.application.util.query.Query;

import java.util.function.Predicate;

// from kotlin
public final class XQuery<B, R> extends AbstractDecomposableQuery<R> {
    private Query<? extends B> baseQuery;

    private XTransformation<? super B, ? extends R> transformation;

    public XQuery(Query<? extends B> baseQuery, XTransformation<? super B, ? extends R> transformation) {
        super();
        this.baseQuery = baseQuery;
        this.transformation = transformation;
    }

    @Override
    protected boolean processResults(Predicate<? super R> consumer) {
        return baseQuery.forEach(baseValue -> {
            for (XResult<? extends R> result : transformation.apply(baseValue)) {
                if (!result.process(consumer)) {
                    return false;
                }
            }

            return true;
        });
    }

    
    @Override
    public Requests<R> decompose() {
        return RequestsKt.andThen(RequestsKt.decompose(baseQuery), transformation);
    }
}
