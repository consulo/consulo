// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.index.io.internal;

public final class IOCancellationCallbackHolder {
    public static final IOCancellationCallbackHolder INSTANCE = new IOCancellationCallbackHolder();

    // not volatile - ok without it
    private IOCancellationCallback myUsedIoCallback = new IOCancellationCallback() {
        @Override
        public void checkCancelled() {
        }

        @Override
        public void interactWithUI() {
        }
    };

    private IOCancellationCallbackHolder() {
    }

    public void setIoCancellationCallback(IOCancellationCallback callback) {
        myUsedIoCallback = callback;
    }

    public static void checkCancelled() {
        INSTANCE.myUsedIoCallback.checkCancelled();
    }

    public void interactWithUI() {
        myUsedIoCallback.interactWithUI();
    }
}
