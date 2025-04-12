package consulo.ide.impl.idea.model.search.impl;

import jakarta.annotation.Nonnull;

import java.util.Collection;
import java.util.function.Predicate;

// from kotlin
public abstract class XResult<X> {
    public abstract boolean process(@Nonnull Predicate<? super X> processor);

    @Nonnull
    public abstract <R> Collection<? extends XResult<? extends R>> transform(@Nonnull XTransformation<? super X, ? extends R> transformation);
}

