// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.util;

import consulo.application.Application;
import consulo.project.Project;
import jakarta.annotation.Nonnull;

import java.util.function.Supplier;

public final class ServiceUtil {
    private ServiceUtil() {
    }

    @Nonnull
    public static <T> Supplier<T> serviceGet(@Nonnull Project project, @Nonnull Class<T> serviceClass) {
        return () -> project.getService(serviceClass);
    }

    @Nonnull
    public static <T> Supplier<T> serviceGet(@Nonnull Class<T> serviceClass) {
        return () -> Application.get().getService(serviceClass);
    }
}
