// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ui.ex.grid;

import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class SimpleErrorInfo extends SimpleThrowableInfo implements ErrorInfo {
    private final List<Fix> myFixes;

    public SimpleErrorInfo(@Nullable String message, @Nullable Throwable throwable, List<Fix> fixes) {
        super(message == null ? ThrowableInfoUtil.getDefaultMessage(Objects.requireNonNull(throwable)) : message, throwable);
        myFixes = fixes;
    }

    @Override
    public List<Fix> getFixes() {
        return myFixes;
    }

    public static ErrorInfo create(String message, @Nullable Throwable throwable, List<Fix> fixes) {
        return new SimpleErrorInfo(message, throwable, fixes);
    }

    public static ErrorInfo create(String message, @Nullable Throwable throwable) {
        return create(message, throwable, Collections.emptyList());
    }

    public static ErrorInfo create(String message) {
        return create(message, null);
    }

    public static ErrorInfo create(Throwable throwable) {
        return new SimpleErrorInfo(null, throwable, Collections.emptyList());
    }
}
