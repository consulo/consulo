// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ui.messageList;

import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

import java.net.URL;
import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface AsyncHtmlImageLoader {
    CompletableFuture<? extends @Nullable Image> load(@Nullable URL baseUrl, String src);
}
