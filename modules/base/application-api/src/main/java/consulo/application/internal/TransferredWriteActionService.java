// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.application.internal;

import consulo.annotation.access.RequiredWriteAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;

@ServiceAPI(ComponentScope.APPLICATION)
public interface TransferredWriteActionService {
    /**
     * Executes {@code action} synchronously on EDT under write action.
     * This function is semantically equivalent to {@link javax.swing.SwingUtilities#invokeAndWait}.
     * <p>
     * The difference is that raw {@code invokeAndWait} is prone to deadlocks: when a background thread runs a write
     * action and calls {@code invokeAndWait}, the EDT may be blocked on acquisition of the write-intent lock, which in
     * turn cannot be acquired because the background thread is holding the write action. This function is able to
     * overcome this limitation.
     * <p>
     * It is used for invocation of EDT-dependent code synchronously from a background write action.
     * Its use should be limited to background write action only.
     */
    @RequiredWriteAction
    void runOnEdtWithTransferredWriteActionAndWait(@RequiredWriteAction Runnable action);

    /**
     * Executes {@code action} synchronously on a background thread under write action.
     * This function is semantically equivalent to {@link consulo.application.Application#executeOnPooledThread} with
     * waiting.
     * <p>
     * This function allows transferring write access to a background thread, hence it is possible to use functions that
     * require background threads inside it. This is useful for invocation of backgroundable listeners (such as
     * {@code BulkFileListenerBackgroundable}) from EDT write actions.
     */
    @RequiredWriteAction
    void runOnBackgroundThreadWithTransferredWriteActionAndWait(@RequiredWriteAction Runnable action);
}
