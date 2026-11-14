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
package consulo.web.ui.impl.internal.clipboard;

import com.vaadin.flow.component.UI;
import consulo.disposer.Disposable;
import consulo.ui.clipboard.ClipboardAccessException;
import consulo.ui.clipboard.ClipboardFeature;
import consulo.ui.clipboard.DataTransfer;
import consulo.ui.clipboard.DataTransferType;
import consulo.ui.impl.clipboard.BaseClipboard;

import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2026-08-07
 */
public class WebClipboardImpl extends BaseClipboard {
    private static final String PASTED_TEXT = "event.clipboardData.getData('text/plain')";
    private static final String PASTED_HTML = "event.clipboardData.getData('text/html')";

    private final UI myUI;

    /**
     * An image of a paste gesture is read in the browser and comes back as an event of its own - the paste event
     * carries only what can be read at once.
     */
    private static final String IMAGE_PASTE_SCRIPT = """
        if (!window.__consuloImagePaste) {
            window.__consuloImagePaste = true;
            document.addEventListener('paste', event => {
                const items = event.clipboardData ? event.clipboardData.items : [];
                for (const item of items) {
                    if (item.kind === 'file' && item.type.startsWith('image/')) {
                        const reader = new FileReader();
                        reader.onload = () => document.body.dispatchEvent(new CustomEvent('consulo-image-paste', {detail: reader.result}));
                        reader.readAsDataURL(item.getAsFile());
                        break;
                    }
                }
            }, true);
        }
        """;
    private static final String PASTED_IMAGE = "event.detail";

    // a paste gesture is the only payload a browser hands over without a permission prompt
    private volatile DataTransfer myPasted = DataTransfer.EMPTY;
    private final List<Consumer<DataTransfer>> myPasteListeners = new CopyOnWriteArrayList<>();

    public WebClipboardImpl(UI ui) {
        myUI = ui;

        ui.getElement()
            .addEventListener("paste", event -> stagePasted(
                event.getEventData().path(PASTED_TEXT).asString(""),
                event.getEventData().path(PASTED_HTML).asString("")
            ))
            .addEventData(PASTED_TEXT)
            .addEventData(PASTED_HTML);
        ui.getElement()
            .addEventListener("consulo-image-paste", event -> stageImage(event.getEventData().path(PASTED_IMAGE).asString("")))
            .addEventData(PASTED_IMAGE);
        ui.getPage().executeJs(IMAGE_PASTE_SCRIPT);
    }

    /**
     * An image pasted in the browser, as a data url - what the paste handed over, an image only.
     */
    private void stageImage(String dataUrl) {
        int comma = dataUrl.indexOf(',');
        if (!dataUrl.startsWith("data:image/") || comma < 0) {
            return;
        }
        byte[] png;
        try {
            png = Base64.getDecoder().decode(dataUrl.substring(comma + 1));
        }
        catch (IllegalArgumentException e) {
            return;
        }
        DataTransfer pasted = DataTransfer.builder().put(DataTransferType.IMAGE, png).build();
        myPasted = pasted;
        firePasted(pasted);
    }

    private void firePasted(DataTransfer pasted) {
        for (Consumer<DataTransfer> listener : myPasteListeners) {
            listener.accept(pasted);
        }
    }

    @Override
    public Disposable addPasteListener(Consumer<DataTransfer> listener) {
        myPasteListeners.add(listener);
        return () -> myPasteListeners.remove(listener);
    }

    /**
     * What the browser handed over with a paste gesture - the only payload it gives a page without a permission
     * prompt, and the moment the session learns what the system clipboard holds.
     */
    public void stagePasted(String text, String html) {
        DataTransfer.Builder builder = DataTransfer.builder();
        if (!text.isEmpty()) {
            builder.put(DataTransferType.TEXT, text);
        }
        if (!html.isEmpty()) {
            builder.put(DataTransferType.HTML, html);
        }

        DataTransfer pasted = builder.build();
        if (pasted.isEmpty()) {
            return;
        }

        myPasted = pasted;

        // the gesture is the one moment this session learns what the system clipboard really holds. if that is
        // not what this process copied last, another application has taken it and the rich half we kept no
        // longer describes what would be pasted
        if (!Objects.equals(getLocalContents().get(DataTransferType.TEXT), text)) {
            fireForeignChange();
        }
    }

    @Override
    public boolean isSupported(ClipboardFeature feature) {
        return feature == ClipboardFeature.PASTE_LISTENER;
    }

    /**
     * What a paste gesture handed over is preferred over asking the browser. It is the same clipboard and it is
     * already here, so asking costs a round trip which has to come back before anything can be pasted - and the
     * answer to that request needs a permission the gesture did not.
     */
    @Override
    protected CompletableFuture<DataTransfer> readNative() {
        DataTransfer pasted = myPasted;
        if (!pasted.isEmpty()) {
            return CompletableFuture.completedFuture(pasted);
        }

        return execute("return navigator.clipboard.readText();", String.class)
            .thenApply(text -> text == null || text.isEmpty() ? DataTransfer.EMPTY : DataTransfer.of(text));
    }

    @Override
    protected CompletableFuture<Set<DataTransferType<?>>> readNativeTypes() {
        return CompletableFuture.completedFuture(Set.of());
    }

    /**
     * Html goes as a second representation of the same item, so a rich editor pastes it while a plain one takes the text. A
     * browser offers no rtf, and one without {@code ClipboardItem} gets the text only.
     */
    @Override
    protected CompletableFuture<Void> writeNative(DataTransfer transfer) {
        String text = transfer.get(DataTransferType.TEXT);
        String html = transfer.get(DataTransferType.HTML);
        if (text == null && html == null) {
            return CompletableFuture.completedFuture(null);
        }

        if (html == null) {
            return execute("return navigator.clipboard.writeText($0).then(() => true);", Boolean.class, text)
                .thenApply(written -> null);
        }

        return execute(
            """
                if (typeof ClipboardItem === 'undefined' || !navigator.clipboard.write) {
                    return navigator.clipboard.writeText($0 !== null ? $0 : $1).then(() => true);
                }
                const entries = {'text/html': new Blob([$1], {type: 'text/html'})};
                if ($0 !== null) {
                    entries['text/plain'] = new Blob([$0], {type: 'text/plain'});
                }
                return navigator.clipboard.write([new ClipboardItem(entries)]).then(() => true);
                """,
            Boolean.class,
            text,
            html
        ).thenApply(written -> null);
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
