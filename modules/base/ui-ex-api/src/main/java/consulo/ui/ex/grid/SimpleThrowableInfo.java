// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ui.ex.grid;

import consulo.ui.grid.ThrowableInfo;
import org.jspecify.annotations.Nullable;

public class SimpleThrowableInfo implements ThrowableInfo {
    private final String myMessage;
    private final @Nullable Throwable myOriginalThrowable;

    SimpleThrowableInfo(String message, @Nullable Throwable originalThrowable) {
        myMessage = message;
        myOriginalThrowable = originalThrowable;
    }

    SimpleThrowableInfo(String message) {
        this(message, null);
    }

    SimpleThrowableInfo(Throwable originalThrowable) {
        this(ThrowableInfoUtil.getDefaultMessage(originalThrowable), originalThrowable);
    }

    @Override
    public String getMessage() {
        return myMessage;
    }

    @Override
    public @Nullable Throwable getOriginalThrowable() {
        return myOriginalThrowable;
    }
}
