// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.list.error;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.Nls;

import javax.swing.*;
import java.util.function.Function;

public sealed interface ErrorStatusPresenter<T> permits ErrorStatusPresenter.Text, ErrorStatusPresenter.HTML {
    String ERROR_ACTION_HREF = "ERROR_ACTION";

    /**
     * @deprecated Moved to ErrorStatusPresenter.Text
     */
    @Deprecated
    @Nonnull
    @Nls
    String getErrorTitle(@Nonnull T error);

    @Nullable
    Action getErrorAction(@Nonnull T error);

    non-sealed interface Text<T> extends ErrorStatusPresenter<T> {
        @Override
        @Nonnull
        @Nls
        String getErrorTitle(@Nonnull T error);

        @Nullable
        @Nls
        String getErrorDescription(@Nonnull T error);
    }

    non-sealed interface HTML<T> extends ErrorStatusPresenter<T> {
        @Nonnull
        String getHTMLBody(@Nonnull T error);
    }

    static <T extends Throwable> @Nonnull Text<T> simple(@Nonnull @Nls String title) {
        return simple(title, null, error -> null);
    }

    static <T extends Throwable> @Nonnull Text<T> simple(
        @Nonnull @Nls String title,
        @Nullable Function<T, @Nls String> descriptionProvider
    ) {
        return simple(title, descriptionProvider, error -> null);
    }

    static <T extends Throwable> @Nonnull Text<T> simple(
        @Nonnull @Nls String title,
        @Nullable Function<T, @Nls String> descriptionProvider,
        @Nonnull Function<T, @Nullable Action> actionProvider
    ) {
        return new Text<>() {
            @Override
            public @Nonnull String getErrorTitle(@Nonnull T error) {
                return title;
            }

            @Override
            public @Nullable Action getErrorAction(@Nonnull T error) {
                return actionProvider.apply(error);
            }

            @Override
            public @Nullable String getErrorDescription(@Nonnull T error) {
                if (descriptionProvider != null) {
                    return descriptionProvider.apply(error);
                }
                return error.getLocalizedMessage();
            }
        };
    }
}
