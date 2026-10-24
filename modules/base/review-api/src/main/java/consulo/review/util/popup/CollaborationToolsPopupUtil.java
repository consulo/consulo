// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.review.util.popup;

import consulo.review.list.search.ShowDirection;
import consulo.ui.PopupPosition;
import consulo.ui.RelativePoint2D;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.event.JBPopupListener;
import consulo.ui.ex.popup.event.LightweightWindowEvent;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public final class CollaborationToolsPopupUtil {
    private CollaborationToolsPopupUtil() {
    }

    /**
     * Shows the popup and completes when it is closed; cancelling the result closes the popup.
     */
    @RequiredUIAccess
    public static CompletableFuture<?> showAndAwait(JBPopup popup, RelativePoint2D point, ShowDirection showDirection) {
        CompletableFuture<Object> result = new CompletableFuture<>();
        completeOnClose(popup, result, () -> null);
        showPopup(popup, point, showDirection);
        return result;
    }

    /**
     * Completes {@code result} with {@code valueOnClose} when the popup is closed, and closes the popup when {@code result}
     * is completed or cancelled first.
     */
    @RequiredUIAccess
    public static <R extends @Nullable Object> void completeOnClose(JBPopup popup, CompletableFuture<R> result, Supplier<R> valueOnClose) {
        popup.addListener(new JBPopupListener() {
            @Override
            public void onClosed(LightweightWindowEvent event) {
                result.complete(valueOnClose.get());
            }
        });
        UIAccess uiAccess = UIAccess.current();
        result.whenComplete((ignored, error) -> uiAccess.give(() -> {
            if (!popup.isDisposed()) {
                popup.cancel();
            }
        }));
    }

    @RequiredUIAccess
    public static void showPopup(JBPopup popup, RelativePoint2D point, ShowDirection showDirection) {
        if (showDirection == ShowDirection.ABOVE) {
            popup.showBy(point.getUIComponent(), null, PopupPosition.TOP);
        }
        else {
            popup.show(point);
        }
    }
}
