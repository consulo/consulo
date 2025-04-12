package consulo.ide.impl.idea.model.search.impl;

import consulo.application.util.function.Processor;
import jakarta.annotation.Nonnull;

import java.util.Collection;

// from kotlin
public abstract class XResult<X> {
    public abstract boolean process(@Nonnull Processor<? super X> processor);

    @Nonnull
    public abstract <R> Collection<? extends XResult<? extends R>> transform(@Nonnull XTransformation<? super X, ? extends R> transformation);
}

