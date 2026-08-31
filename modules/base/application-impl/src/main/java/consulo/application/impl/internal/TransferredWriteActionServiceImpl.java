// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.application.impl.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.internal.TransferredWriteActionService;
import consulo.ui.ModalityState;
import consulo.ui.UIAccess;
import jakarta.inject.Singleton;

@Singleton
@ServiceImpl
public class TransferredWriteActionServiceImpl implements TransferredWriteActionService {
    @Override
    public void runOnEdtWithTransferredWriteActionAndWait(Runnable action) {
        BaseApplication application = (BaseApplication) Application.get();
        if (!application.isWriteAccessAllowed()) {
            throw new IllegalStateException("Transferring of write action to EDT is permitted only if write lock is acquired");
        }
        if (UIAccess.isUIThread()) {
            action.run();
            return;
        }

        RWLock lock = application.myLock;
        if (!(lock instanceof ReadMostlyRWLock rwLock)) {
            application.getLastUIAccess().giveAndWait(action);
            return;
        }

        rwLock.transferWriteAction(true, action, edtRunnable -> application.invokeLater(edtRunnable, ModalityState.any()));
    }

    @Override
    public void runOnBackgroundThreadWithTransferredWriteActionAndWait(Runnable action) {
        BaseApplication application = (BaseApplication) Application.get();
        if (!application.isWriteAccessAllowed()) {
            throw new IllegalStateException("Transferring of write action to a background thread is permitted only if write lock is acquired");
        }
        if (!UIAccess.isUIThread()) {
            action.run();
            return;
        }

        RWLock lock = application.myLock;
        if (!(lock instanceof ReadMostlyRWLock rwLock)) {
            application.executeOnPooledThread(action);
            return;
        }

        rwLock.transferWriteAction(false, action, application::executeOnPooledThread);
    }
}
