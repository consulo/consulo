package consulo.ide.impl.idea.model.search.impl;

import consulo.application.util.function.Processor;
import jakarta.annotation.Nonnull;

import java.util.Collection;

// from kotlin
public final class ValueResult<X> extends XResult<X> {
    private final X value;

    public ValueResult(X value) {
        super();
        this.value = value;
    }

    @Override
    public boolean process(@Nonnull Processor<? super X> processor) {
        return processor.process(getValue());
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
