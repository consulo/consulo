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
import consulo.ui.Component;
import consulo.ui.HtmlView;
import consulo.ui.UIAccess;
import consulo.web.internal.ui.base.FromVaadinComponentWrapper;
import consulo.web.internal.ui.base.VaadinComponentDelegate;
import org.jspecify.annotations.Nullable;

import java.net.URL;
import java.util.concurrent.CompletableFuture;

/**
 * The rendered document is given to an iframe as {@code srcdoc}, so the page styles of the document can not reach
 * the ide around it. The frame stays same origin, which is what makes the source offset lookup below possible.
 *
 * @author VISTALL
 * @since 2026-08-07
 */
public class WebHtmlViewImpl extends VaadinComponentDelegate<WebHtmlViewImpl.Vaadin> implements HtmlView {
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

    private static String buildDocument(RenderData renderData) {
        StringBuilder head = new StringBuilder();
        for (URL css : renderData.externalCsses()) {
            if (css != null) {
                head.append("<link rel=\"stylesheet\" href=\"").append(css).append("\" />\n");
            }
        }

        String inlineCss = renderData.inlineCss();
        if (!inlineCss.isEmpty()) {
            head.append("<style>\n").append(inlineCss).append("\n</style>\n");
        }

        return renderData.html().replace("<head>", "<head>" + head);
    }
}
