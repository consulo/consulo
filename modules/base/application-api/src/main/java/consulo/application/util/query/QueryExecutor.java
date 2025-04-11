// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.application.util.query;

import jakarta.annotation.Nonnull;

import java.util.function.Predicate;

/**
 * A generic extension to enable plugging into various searches.<p/>
 * <p>
 * Consider extending {@link consulo.ide.impl.idea.openapi.application.QueryExecutorBase} instead unless you know what you're doing.
 *
 * @author max
 */
@FunctionalInterface
public interface QueryExecutor<Result, Param> {
    /**
     * Find some results according to queryParameters and feed them to consumer. If consumer returns {@code false}, stop.
     *
     * @return {@code false} if the searching should be stopped immediately. This should happen only when consumer has returned {@code false}.
     */
    boolean execute(@Nonnull Param queryParameters, @Nonnull Predicate<? super Result> consumer);
}
