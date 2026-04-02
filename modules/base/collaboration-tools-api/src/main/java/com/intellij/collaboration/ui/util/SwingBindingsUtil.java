// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.util;

import jakarta.annotation.Nonnull;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.flow.Flow;
import kotlinx.coroutines.flow.MutableStateFlow;

import javax.swing.*;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;

/**
 * Utility class for swing binding operations.
 * <p>
 * Note: Many of the original Kotlin extension functions in swingBindings.kt relied heavily
 * on Kotlin coroutines (collectScoped, launchNow, etc.) and cannot be directly converted to
 * plain Java. The binding methods here serve as stubs/placeholders for the Java API surface.
 * Callers should use the Kotlin coroutine utilities directly for the async binding patterns.
 */
@ApiStatus.Internal
public final class SwingBindingsUtil {
    private SwingBindingsUtil() {
    }

    /**
     * Binds a {@link ComboBoxWithActionsModel} to flows of items, selection, and actions.
     * This is a Java-side entry point; the actual coroutine collection must be handled externally.
     */
    public static <T> void bindComboBoxWithActionsModel(
        @Nonnull CoroutineScope scope,
        @Nonnull ComboBoxWithActionsModel<T> model,
        @Nonnull Flow<? extends Collection<T>> items,
        @Nonnull MutableStateFlow<T> selectionState,
        @Nonnull Flow<List<Action>> actions,
        @Nonnull Comparator<T> sortComparator
    ) {
        // Note: The actual coroutine-based binding (launchNow, collect, etc.) requires
        // Kotlin coroutine infrastructure. This method provides the Java API surface.
        // In practice, callers should use the Kotlin SwingBindingsKt methods.
    }

    /**
     * Binds content of a Wrapper to a data flow using a component factory.
     */
    public static <D> void bindContentIn(
        @Nonnull Wrapper wrapper,
        @Nonnull CoroutineScope scope,
        @Nonnull Flow<D> dataFlow,
        @Nonnull BiFunction<CoroutineScope, D, JComponent> componentFactory
    ) {
        // Note: Requires Kotlin coroutines for actual implementation.
        // This provides the Java API surface.
    }
}
