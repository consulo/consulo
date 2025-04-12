// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.application.util.query;

import consulo.application.util.function.Processor;
import consulo.logging.Logger;
import consulo.component.ProcessCanceledException;
import consulo.application.progress.ProgressManager;
import consulo.application.dumb.IndexNotReadyException;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * @author max
 */
public final class ExecutorsQuery<Result, Parameter> extends AbstractQuery<Result> {
    private static final Logger LOG = Logger.getInstance(ExecutorsQuery.class);

    private final List<? extends QueryExecutor<Result, Parameter>> myExecutors;
    private final Parameter myParameters;

    public ExecutorsQuery(@Nonnull final Parameter params, @Nonnull List<? extends QueryExecutor<Result, Parameter>> executors) {
        myParameters = params;
        myExecutors = executors;
    }

    @Override
    protected boolean processResults(@Nonnull final Processor<? super Result> consumer) {
        for (QueryExecutor<Result, Parameter> executor : myExecutors) {
            try {
                ProgressManager.checkCanceled();
                if (!executor.execute(myParameters, consumer)) {
                    return false;
                }
            }
            catch (ProcessCanceledException | IndexNotReadyException e) {
                throw e;
            }
            catch (Exception e) {
                LOG.error(e);
            }
        }

        return true;
    }
}
