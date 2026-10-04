// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ui.ex.grid;

import consulo.ui.grid.GridUtilCore;
import consulo.ui.grid.ThrowableInfo;

import java.util.ArrayList;
import java.util.List;

public final class ThrowableInfoUtil {
    private ThrowableInfoUtil() {
    }

    public static String getDefaultMessage(Throwable throwable) {
        return GridUtilCore.getLongMessage(throwable);
    }

    public static Throwable getActualThrowable(ThrowableInfo info) {
        String message = info.getMessage();
        Throwable originalThrowable = info.getOriginalThrowable();
        return originalThrowable != null && getDefaultMessage(originalThrowable).equals(message)
            ? originalThrowable
            : new RuntimeException(message, originalThrowable);
    }

    public static List<ErrorInfo.Fix> getAllFixes(ErrorInfo error, boolean withCustomProviders) {
        if (!withCustomProviders) {
            return error.getFixes();
        }
        List<ErrorInfo.Fix> fixes = new ArrayList<>(error.getFixes());
        for (RuntimeErrorActionProvider provider : RuntimeErrorActionProvider.getProviders()) {
            ErrorInfo.Fix fix = provider.createAction(error);
            if (fix != null) {
                fixes.add(fix);
            }
        }
        return fixes;
    }

    public static List<ErrorInfo.Fix> getAllFixes(ErrorInfo error) {
        return getAllFixes(error, true);
    }
}
