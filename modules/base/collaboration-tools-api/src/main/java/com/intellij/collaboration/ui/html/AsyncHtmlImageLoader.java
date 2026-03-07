// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.html;

import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.*;
import java.net.URL;

@FunctionalInterface
public interface AsyncHtmlImageLoader {
    Key<AsyncHtmlImageLoader> KEY = Key.create("Async.Html.Image.Loader");

    /**
     * Suspend function - kept as a regular method since the Kotlin coroutine continuation
     * parameter style is used at call sites.
     */
    @Nullable
    Image load(@Nullable URL baseUrl, @Nonnull String src) throws Exception;
}
