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
package consulo.web.internal.ui.htmlView;

import com.vaadin.flow.component.html.IFrame;
import consulo.logging.Logger;
import consulo.util.io.StreamUtil;
import consulo.ui.Component;
import consulo.ui.HtmlView;
import consulo.ui.UIAccess;
import consulo.web.internal.ui.WebColors;
import consulo.ui.style.StyleManager;
import consulo.ui.style.Style;
import consulo.ui.style.ComponentColors;
import consulo.web.internal.ui.base.FromVaadinComponentWrapper;
import consulo.web.internal.ui.base.VaadinComponentDelegate;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

/**
 * The rendered document is given to an iframe as {@code srcdoc}, so the page styles of the document can not reach
 * the ide around it. The frame stays same origin, which is what makes the source offset lookup below possible.
 *
 * @author VISTALL
 * @since 2026-08-07
 */
public class WebHtmlViewImpl extends VaadinComponentDelegate<WebHtmlViewImpl.Vaadin> implements HtmlView {
    private static final Logger LOG = Logger.getInstance(WebHtmlViewImpl.class);

    private static final String SCROLL_TO_SRC_OFFSET_SCRIPT = """
        var doc = this.contentDocument;
        if (doc) {
            var offset = $0;
            var best = null;
            var bestDistance = Number.MAX_VALUE;
            var nodes = doc.querySelectorAll('[src]');
            for (var i = 0; i < nodes.length; i++) {
                var parts = nodes[i].getAttribute('src').split('..');
                if (parts.length !== 2) {
                    continue;
                }
                var from = parseInt(parts[0], 10);
                var to = parseInt(parts[1], 10);
                if (isNaN(from) || isNaN(to)) {
                    continue;
                }
                var distance = Math.min(Math.abs(from - offset), Math.abs(to - 1 - offset));
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = nodes[i];
                }
            }
            if (best) {
                best.scrollIntoView({block: 'start'});
            }
        }
        """;

    public class Vaadin extends IFrame implements FromVaadinComponentWrapper {
        @Override
        public @Nullable Component toUIComponent() {
            return WebHtmlViewImpl.this;
        }
    }

    public WebHtmlViewImpl() {
        getVaadinComponent().setSizeFull();
        getVaadinComponent().getElement().setAttribute("frameborder", "0");
    }

    @Override
    public Vaadin createVaadinComponent() {
        return new Vaadin();
    }

    @Override
    public CompletableFuture<?> render(RenderData renderData) {
        String document = buildDocument(renderData);

        UIAccess uiAccess = getUIAccess();
        if (uiAccess == null) {
            getVaadinComponent().getElement().setAttribute("srcdoc", document);
            return CompletableFuture.completedFuture(null);
        }

        return uiAccess.giveAsync(() -> getVaadinComponent().getElement().setAttribute("srcdoc", document));
    }

    @Override
    public void scrollToMarkdownSrcOffset(int offset) {
        UIAccess uiAccess = getUIAccess();
        if (uiAccess == null) {
            return;
        }

        uiAccess.give(() -> getVaadinComponent().getElement().executeJs(SCROLL_TO_SRC_OFFSET_SCRIPT, offset));
    }

    /**
     * The colours a stylesheet of this view is written against. The awt backend resolves them out of the look
     * and feel while it parses, which a frame cannot do - custom properties are resolved in the tree they are
     * declared in, and an iframe shares nothing with the page holding it. So they are declared again inside.
     */
    private static void appendThemeVariables(StringBuilder head) {
        Style style = StyleManager.get().getCurrentStyle();

        head.append("<style>\n:root {\n");
        appendVariable(head, "--cobra-bg", style, ComponentColors.LAYOUT);
        appendVariable(head, "--cobra-fg", style, ComponentColors.TEXT_FOREGROUND);
        appendVariable(head, "--cobra-disabled-fg", style, ComponentColors.DISABLED_TEXT);
        appendVariable(head, "--cobra-separator-fg", style, ComponentColors.SEPARATOR);
        appendVariable(head, "--cobra-input-bg", style, ComponentColors.COMPONENT_BACKGROUND);
        appendVariable(head, "--cobra-link", style, ComponentColors.LINK_FOREGROUND);
        head.append("}\n");

        // the scrollbar of the frame is its own, the sheet of the ide does not reach in here either. same shape
        // as consulo/scrollbar.css - no arrow buttons, transparent track, the thumb of the theme
        appendVariable(head.append(":root {\n"), "--scrollbar-thumb", style, ComponentColors.SCROLL_BAR_THUMB);
        appendVariable(head, "--scrollbar-hover-thumb", style, ComponentColors.SCROLL_BAR_HOVER_THUMB);
        head.append("}\n")
            .append("::-webkit-scrollbar { width: 8px; height: 8px; background-color: transparent; }\n")
            .append("::-webkit-scrollbar-button { display: none; width: 0; height: 0; }\n")
            .append("::-webkit-scrollbar-track,\n")
            .append("::-webkit-scrollbar-track-piece,\n")
            .append("::-webkit-scrollbar-corner { background-color: transparent; }\n")
            .append("::-webkit-scrollbar-thumb { background-color: var(--scrollbar-thumb); border-radius: 5px; }\n")
            .append("::-webkit-scrollbar-thumb:hover { background-color: var(--scrollbar-hover-thumb); }\n")
            .append("</style>\n");
    }

    private static void appendVariable(StringBuilder head, String name, Style style, ComponentColors color) {
        head.append("  ").append(name).append(": ").append(WebColors.toCssColor(style.getColorValue(color))).append(";\n");
    }

    /**
     * Every stylesheet is inlined rather than linked. The urls a caller hands over point into the running
     * platform - a {@code jar:file:} inside a plugin - and a browser has no idea what those are, so the frame
     * has to be given the text itself.
     */
    private static String buildDocument(RenderData renderData) {
        StringBuilder head = new StringBuilder();
        appendThemeVariables(head);

        for (URL css : renderData.externalCsses()) {
            if (css == null) {
                continue;
            }

            try (InputStream stream = css.openStream()) {
                head.append("<style>\n").append(StreamUtil.readText(stream, StandardCharsets.UTF_8)).append("\n</style>\n");
            }
            catch (IOException e) {
                LOG.warn("Failed to read the stylesheet " + css + " of an html view", e);
            }
        }

        String inlineCss = renderData.inlineCss();
        if (!inlineCss.isEmpty()) {
            head.append("<style>\n").append(inlineCss).append("\n</style>\n");
        }

        return renderData.html().replace("<head>", "<head>" + head);
    }
}
