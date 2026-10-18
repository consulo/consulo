// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.review.toolwindow;

/**
 * Represents a single closable tab in review toolwindow (e.g. review details tab).
 */
public interface ReviewTab {
    /**
     * Unique id used to distinguish tabs that can be reused,
     * so if tab with the same {@link #getId()} is requested to be opened
     * it will be reused if {@link #isReuseTabOnRequest()} is {@code true}
     * or closed and new one will be opened.
     */
    String getId();

    /**
     * If {@code true} open requests will select opened tabs if tabs with the same {@link #getId()} exists,
     * otherwise existing tab with the {@link #getId()} will be closed and new tab opened
     */
    boolean isReuseTabOnRequest();
}
