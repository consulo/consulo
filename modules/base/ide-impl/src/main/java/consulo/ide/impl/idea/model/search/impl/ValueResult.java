package consulo.ide.impl.idea.model.search.impl;

import jakarta.annotation.Nonnull;

import java.util.Collection;
import java.util.function.Predicate;

// from kotlin
public final class ValueResult<X> extends XResult<X> {
    private final X value;

    public ValueResult(X value) {
        super();
        this.value = value;
    }

    @Override
    public boolean process(@Nonnull Predicate<? super X> processor) {
        return processor.test(getValue());
    }

    @Override
    @Nonnull
    public <R> Collection<? extends XResult<? extends R>> transform(@Nonnull XTransformation<? super X, ? extends R> transformation) {
        return transformation.apply(getValue());
    }

    public final X getValue() {
        return value;
    }
}
