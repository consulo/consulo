package consulo.ide.impl.idea.model.search.impl;

import consulo.ide.impl.psi.impl.search.LeafOccurrence;
import consulo.application.util.query.Query;
import consulo.util.collection.ContainerUtil;

import java.util.List;

// from kotlin
public class RequestsKt {
    
    public static <T> Requests<T> decompose(Query<T> query) {
        return query instanceof DecomposableQuery
            ? ((DecomposableQuery<T>) query).decompose()
            : new Requests<>(List.of(), List.of(new QueryRequest<>(query, TransformationKt.<T>idTransform())), List.of());
    }

    
    public static <B, R> Requests<R> andThen(
        Requests<? extends B> thisRequest,
        XTransformation<? super B, ? extends R> transformation
    ) {
        List<ParametersRequest<?, ? extends R>> params =
            ContainerUtil.map(thisRequest.getParametersRequests(), it -> RequestsKt.andThen(it, transformation));
        List<QueryRequest<?, ? extends R>> queries =
            ContainerUtil.map(thisRequest.getQueryRequests(), it -> RequestsKt.andThen(it, transformation));
        List<WordRequest<? extends R>> wordRequests =
            ContainerUtil.map(thisRequest.getWordRequests(), it -> RequestsKt.andThen(it, transformation));
        return new Requests<>(params, queries, wordRequests);
    }

    
    public static <B, R> WordRequest<R> andThen(
        WordRequest<? extends B> thisWordRequest,
        XTransformation<? super B, ? extends R> t
    ) {
        return new WordRequest<>(
            thisWordRequest.getSearchWordRequest(),
            thisWordRequest.getInjectionInfo(),
            TransformationKt.<LeafOccurrence, B, R>karasique(thisWordRequest.getTransformation(), t)
        );
    }

    
    public static <B, R, I> QueryRequest<B, R> andThen(
        QueryRequest<B, ? extends I> thisQuery,
        XTransformation<? super I, ? extends R> t
    ) {
        return new QueryRequest<>(thisQuery.getQuery(), TransformationKt.<B, I, R>karasique(thisQuery.getTransformation(), t));
    }

    
    public static <B, R, I> ParametersRequest<B, R> andThen(
        ParametersRequest<B, ? extends I> thisParameters,
        XTransformation<? super I, ? extends R> t
    ) {
        return new ParametersRequest<>(
            thisParameters.getParams(),
            TransformationKt.<B, I, R>karasique(thisParameters.getTransformation(), t)
        );
    }
}
