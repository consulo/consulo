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
package consulo.web.internal.ui.clipboard;

import com.vaadin.flow.component.UI;
import consulo.ui.clipboard.ClipboardAccessException;
import consulo.ui.clipboard.ClipboardFeature;
import consulo.ui.clipboard.DataTransfer;
import consulo.ui.clipboard.DataTransferType;
import consulo.ui.impl.clipboard.BaseClipboard;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2026-08-07
 */
public class WebClipboardImpl extends BaseClipboard {
    private static final String PASTED_TEXT = "event.clipboardData.getData('text/plain')";
    private static final String PASTED_HTML = "event.clipboardData.getData('text/html')";

    private final UI myUI;

    // a paste gesture is the only payload a browser hands over without a permission prompt
    private volatile DataTransfer myPasted = DataTransfer.EMPTY;

    public WebClipboardImpl(UI ui) {
        myUI = ui;

        ui.getElement()
            .addEventListener("paste", event -> myPasted = DataTransfer.builder()
                .put(DataTransferType.TEXT, event.getEventData().path(PASTED_TEXT).asString(""))
                .put(DataTransferType.HTML, event.getEventData().path(PASTED_HTML).asString(""))
                .build())
            .addEventData(PASTED_TEXT)
            .addEventData(PASTED_HTML);
    }

    @Override
    public boolean isSupported(ClipboardFeature feature) {
        return false;
    }

    @Override
    protected CompletableFuture<DataTransfer> readNative() {
        return execute("return navigator.clipboard.readText();", String.class)
            .thenApply(text -> text == null ? DataTransfer.EMPTY : DataTransfer.of(text))
            .exceptionallyCompose(e -> {
                DataTransfer pasted = myPasted;
                return pasted.isEmpty() ? CompletableFuture.failedFuture(e) : CompletableFuture.completedFuture(pasted);
            });
    }

    @Override
    protected CompletableFuture<Set<DataTransferType<?>>> readNativeTypes() {
        return CompletableFuture.completedFuture(Set.of());
    }

    @Override
    protected CompletableFuture<Void> writeNative(DataTransfer transfer) {
        String text = transfer.get(DataTransferType.TEXT);
        if (text == null) {
            return CompletableFuture.completedFuture(null);
        }

        return execute("return navigator.clipboard.writeText($0).then(() => true);", Boolean.class, text)
            .thenApply(written -> null);
    }

    @Override
    protected CompletableFuture<Void> clearNative() {
        return writeNative(DataTransfer.of(""));
    }

    private <T> CompletableFuture<T> execute(String expression, Class<T> resultClass, Object... parameters) {
        return myUI.getPage()
            .executeJs(expression, parameters)
            .toCompletableFuture(resultClass)
            .exceptionallyCompose(e -> CompletableFuture.failedFuture(new ClipboardAccessException("Browser refused clipboard access", e)));
    }
}
