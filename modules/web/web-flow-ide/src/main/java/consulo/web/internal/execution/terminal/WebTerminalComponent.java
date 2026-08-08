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
package consulo.web.internal.execution.terminal;

import com.flowingcode.vaadin.addons.xterm.ConsuloPtyTerm;
import com.jediterm.terminal.model.TerminalTextBuffer;
import consulo.ui.Component;
import consulo.ui.UIAccess;
import consulo.ui.model.RangeModel;
import consulo.web.internal.ui.base.FromVaadinComponentWrapper;
import consulo.web.internal.ui.base.VaadinComponentDelegate;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2026-08-08
 */
public class WebTerminalComponent extends VaadinComponentDelegate<WebTerminalComponent.Vaadin> {
    public class Vaadin extends ConsuloPtyTerm implements FromVaadinComponentWrapper {
        @Override
        public @Nullable Component toUIComponent() {
            return WebTerminalComponent.this;
        }
    }

    private final Consumer<String> myInputConsumer;
    private final Consumer<WebTerminalConsole.TerminalSize> myResizeConsumer;

    /**
     * The process starts writing before the browser has the component, and those first characters are the
     * banner and the prompt - they are kept until there is a ui to write them into.
     */
    private final StringBuilder myPendingOutput = new StringBuilder();

    public WebTerminalComponent(Consumer<String> inputConsumer, Consumer<WebTerminalConsole.TerminalSize> resizeConsumer) {
        myInputConsumer = inputConsumer;
        myResizeConsumer = resizeConsumer;

        Vaadin xterm = getVaadinComponent();
        xterm.setSizeFull();
        xterm.setFitOnResize(true);

        xterm.getElement().addEventListener("pty-data", event -> {
            String data = event.getEventData().get("event.detail").asString();
            myInputConsumer.accept(data);
        }).addEventData("event.detail");

        xterm.getElement().addEventListener("pty-resize", event -> {
            int columns = event.getEventData().get("event.detail.cols").asInt();
            int rows = event.getEventData().get("event.detail.rows").asInt();
            myResizeConsumer.accept(new WebTerminalConsole.TerminalSize(columns, rows));
        }).addEventData("event.detail.cols").addEventData("event.detail.rows");

        // a reload builds a new client element which knows nothing of what was set on the old one, so the
        // theme is applied every time the component arrives in a ui rather than once at construction
        xterm.addAttachListener(event -> {
            applyTheme();
            flushPendingOutput();
        });
    }

    @Override
    public Vaadin createVaadinComponent() {
        return new Vaadin();
    }

    /**
     * Pushes the palette of the current scheme, so a theme switch reaches a terminal which is already running.
     */
    public void updateTheme() {
        UIAccess uiAccess = getUIAccess();
        if (uiAccess == null) {
            return;
        }
        uiAccess.give(this::applyTheme);
    }

    private void applyTheme() {
        getVaadinComponent().getElement().callJsFunction("applyTheme", WebTerminalThemeBuilder.build());
    }

    public void write(String text) {
        UIAccess uiAccess = getUIAccess();
        if (uiAccess == null) {
            synchronized (myPendingOutput) {
                myPendingOutput.append(text);
            }
            return;
        }

        uiAccess.give(() -> writeToClient(text));
    }

    /**
     * Goes straight to the client element, which holds anything written before the terminal is open. The
     * write of the addon is queued server side until the terminal reports back, and a reload leaves that
     * queue never draining.
     */
    private void writeToClient(String text) {
        getVaadinComponent().getElement().callJsFunction("writeText", text);
    }

    private void flushPendingOutput() {
        String pending;
        synchronized (myPendingOutput) {
            if (myPendingOutput.isEmpty()) {
                return;
            }
            pending = myPendingOutput.toString();
            myPendingOutput.setLength(0);
        }

        UIAccess uiAccess = getUIAccess();
        if (uiAccess != null) {
            uiAccess.give(() -> writeToClient(pending));
        }
    }

    /**
     * Zero is the bottom of the buffer, the convention the desktop console answers with.
     */
    public RangeModel getVerticalScrollModel(TerminalTextBuffer textBuffer) {
        return new RangeModel() {
            @Override
            public int getValue() {
                return 0;
            }

            @Override
            public void setValue(int value) {
                if (value == 0) {
                    getVaadinComponent().scrollToBottom();
                }
                else {
                    getVaadinComponent().scrollToLine(value);
                }
            }

            @Override
            public int getMinimum() {
                return -textBuffer.getHistoryLinesCount();
            }

            @Override
            public int getMaximum() {
                return textBuffer.getScreenLinesCount();
            }

            @Override
            public int getExtent() {
                return textBuffer.getHeight();
            }
        };
    }
}
