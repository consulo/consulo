package consulo.ide.impl.idea.model.search.impl;

import java.util.Collection;
import java.util.function.Predicate;

// from kotlin
public abstract class XResult<X> {
    public abstract boolean process(Predicate<? super X> processor);

    
    public abstract <R> Collection<? extends XResult<? extends R>> transform(XTransformation<? super X, ? extends R> transformation);
}

