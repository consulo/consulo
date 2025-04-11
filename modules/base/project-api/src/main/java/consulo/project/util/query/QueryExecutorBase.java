// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.project.util.query;

import consulo.application.Application;
import consulo.application.dumb.DumbAware;
import consulo.application.util.query.QueryExecutor;
import consulo.project.DumbService;
import consulo.project.Project;
import jakarta.annotation.Nonnull;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * An adapter for {@link QueryExecutor} interface which makes it easier to write implementations. It provides a possibility to
 * automatically wrap the implementation code into a read action. During indexing, query executors that don't implement {@link DumbAware}
 * (but need to be run in a read action), are delayed until indexing is complete, given that search parameters implement {@link DumbAwareSearchParameters}.
 * <p/>
 * Besides, {@link #processQuery(Object, Predicate)} doesn't require to return a boolean value and thus it's harder to stop the whole search
 * by accidentally returning false.
 *
 * @author peter
 * @see Application#runReadAction(Supplier)
 * @see DumbService
 */
public abstract class QueryExecutorBase<Result, Params> implements QueryExecutor<Result, Params> {
    private final boolean myRequireReadAction;

    /**
     * @param requireReadAction whether {@link #processQuery(Object, Predicate)} should be wrapped into a read action.
     */
    protected QueryExecutorBase(boolean requireReadAction) {
        myRequireReadAction = requireReadAction;
    }

    /**
     * Construct an instance that executes {@link #processQuery(Object, Predicate)} as is, without wrapping into a read action.
     */
    protected QueryExecutorBase() {
        this(false);
    }

    @Override
    public final boolean execute(@Nonnull final Params queryParameters, @Nonnull final Predicate<? super Result> consumer) {
        final AtomicBoolean toContinue = new AtomicBoolean(true);
        final Predicate<Result> wrapper = result -> {
            if (!toContinue.get()) {
                return false;
            }

            if (!consumer.test(result)) {
                toContinue.set(false);
                return false;
            }
            return true;
        };

        if (myRequireReadAction && !Application.get().isReadAccessAllowed()) {
            Runnable runnable = () -> {
                if (!(queryParameters instanceof QueryParameters queryParams && !queryParams.isQueryValid())) {
                    processQuery(queryParameters, wrapper);
                }
            };

            if (!DumbService.isDumbAware(this)) {
                Project project = queryParameters instanceof QueryParameters queryParams ? queryParams.getProject() : null;
                if (project != null) {
                    DumbService.getInstance(project).runReadActionInSmartMode(runnable);
                    return toContinue.get();
                }
            }

            Application.get().runReadAction(runnable);
        }
        else {
            processQuery(queryParameters, wrapper);
        }

        return toContinue.get();
    }

    /**
     * Find some results according to queryParameters and feed them to consumer. If consumer returns false, stop.
     */
    public abstract void processQuery(@Nonnull Params queryParameters, @Nonnull Predicate<? super Result> consumer);
}
