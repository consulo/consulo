// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.collaboration.ui;

import consulo.collaboration.localize.CollaborationToolsLocalize;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.Nls;

import java.net.ConnectException;
import java.nio.channels.UnresolvedAddressException;

public final class ExceptionUtil {
    private ExceptionUtil() {
    }

    public static @Nls @Nonnull String getPresentableMessage(@Nonnull Throwable exception) {
        if (exception.getLocalizedMessage() != null) {
            return exception.getLocalizedMessage();
        }

        if (exception instanceof ConnectException) {
            if (exception.getCause() instanceof UnresolvedAddressException) {
                return CollaborationToolsLocalize.errorAddressUnresolved().get();
            }
            return CollaborationToolsLocalize.errorConnectionError().get();
        }
        return CollaborationToolsLocalize.errorUnknown().get();
    }
}
