// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal;

public final class OrphanDirtyFilesQueueDiscardReason {
    private final String myMessage;

    public OrphanDirtyFilesQueueDiscardReason(String message) {
        myMessage = message;
    }

    public String getMessage() {
        return myMessage;
    }
}
