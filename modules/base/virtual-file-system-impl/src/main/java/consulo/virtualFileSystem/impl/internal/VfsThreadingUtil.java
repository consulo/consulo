// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.virtualFileSystem.impl.internal;

import consulo.application.Application;
import consulo.application.internal.TransferredWriteActionService;
import consulo.ui.UIAccess;

public final class VfsThreadingUtil {
    private VfsThreadingUtil() {
    }

    public static void runActionOnEdtRegardlessOfCurrentThread(Runnable action) {
        if (UIAccess.isUIThread()) {
            action.run();
        }
        else {
            Application.get().getInstance(TransferredWriteActionService.class).runOnEdtWithTransferredWriteActionAndWait(action);
        }
    }

    public static void runActionOnBackgroundRegardlessOfCurrentThread(Runnable action) {
        if (!UIAccess.isUIThread()) {
            action.run();
        }
        else {
            Application.get().getInstance(TransferredWriteActionService.class).runOnBackgroundThreadWithTransferredWriteActionAndWait(action);
        }
    }
}
