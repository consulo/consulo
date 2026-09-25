// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.index.io.internal;

/**
 * Check whether the current process should be terminated to avoid long IO-operation
 * and throws cancellation exception if it does.
 */
public interface IOCancellationCallback {
    void checkCancelled();

    void interactWithUI();
}
