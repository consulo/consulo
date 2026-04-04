// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.application.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.AppUIExecutor;
import consulo.application.Application;
import consulo.application.NonBlockingReadAction;
import consulo.ui.ModalityState;

import java.util.concurrent.Callable;

/**
 * An internal service not supposed to be used directly
 */
@ServiceAPI(ComponentScope.APPLICATION)
public abstract class AsyncExecutionService {
    public abstract AppUIExecutor createUIExecutor();

    /**
     * @deprecated Modality state is no longer used. Use {@link #createUIExecutor()} instead.
     */
    @Deprecated
    public AppUIExecutor createUIExecutor(ModalityState modalityState) {
        return createUIExecutor();
    }

    public abstract <T> NonBlockingReadAction<T> buildNonBlockingReadAction(Callable<T> computation);

    public static AsyncExecutionService getService() {
        return Application.get().getInstance(AsyncExecutionService.class);
    }
}
