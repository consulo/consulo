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
package consulo.web.ui.impl.internal.htmlView;

import com.vaadin.flow.component.html.Div;
import consulo.logging.Logger;
import consulo.util.io.StreamUtil;
import consulo.util.lang.StringUtil;
import consulo.ui.Component;
import consulo.ui.HtmlView;
import consulo.ui.UIAccess;
import consulo.web.ui.impl.internal.WebColors;
import consulo.ui.style.StyleManager;
import consulo.ui.style.Style;
import consulo.ui.style.ComponentColors;
import consulo.ui.image.Image;
import consulo.web.ui.impl.internal.base.FromVaadinComponentWrapper;
import consulo.web.ui.impl.internal.base.VaadinComponentDelegate;
import consulo.web.ui.impl.internal.image.WebImageElement;
import consulo.web.ui.impl.internal.image.WebImageUrl;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The document is rendered into a shadow root of a plain element of the page. Not an iframe: a frame has a
 * registry and a connection of its own, and nothing of the platform - the image tags among them - would work
 * inside one. The shadow root keeps the stylesheet of the document off the ide around it, which is the only
 * thing the frame was there for, and custom elements still upgrade because it is the same document.
 *
 * @author VISTALL
 * @since 2026-08-07
 */
public class WebHtmlViewImpl extends VaadinComponentDelegate<WebHtmlViewImpl.Vaadin> implements HtmlView {
    private static final Logger LOG = Logger.getInstance(WebHtmlViewImpl.class);

    private static final String RENDER_SCRIPT = """
        const root = this.shadowRoot || this.attachShadow({mode: 'open'});
        root.innerHTML = $0;
        """;

    /**
     * An {@code img} of the document which names an image of the platform. The whole tag is taken over rather
     * than only the {@code src} - what stands in its place is a tree of custom elements, not a picture at a
     * url, so there is nothing left of the original to keep.
     */
    private static final Pattern PLATFORM_IMAGE = Pattern.compile(
        "<img\\b[^>]*\\bsrc\\s*=\\s*\"" + Pattern.quote(IMAGE_SRC_PREFIX) + "([^\"]*)\"[^>]*>",
        Pattern.CASE_INSENSITIVE
    );

    private static final String SCROLL_TO_SRC_OFFSET_SCRIPT = """
        var doc = this.shadowRoot;
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

    public class Vaadin extends Div implements FromVaadinComponentWrapper {
        @Override
        public @Nullable Component toUIComponent() {
            return WebHtmlViewImpl.this;
        }
    }

    private volatile @Nullable Function<String, Image> myImageResolver;

    public WebHtmlViewImpl() {
        getVaadinComponent().setSizeFull();
        // the document scrolls inside this element rather than growing it
        getVaadinComponent().getStyle().set("overflow", "auto");
    }

    @Override
    public Vaadin createVaadinComponent() {
        return new Vaadin();
    }

    @Override
    public void setImageResolver(@Nullable Function<String, Image> imageResolver) {
        myImageResolver = imageResolver;
    }

    /**
     * Resolved while the document is still a string on the server. The browser is never told about an id it
     * could not fetch anyway, so nothing asks for a url which is not there and no image loads twice.
     * <p>
     * An id nobody resolves, or an image which has no form on the web, leaves the tag out rather than drawing a
     * broken picture - a document names images of the platform, and not every build has every one of them.
     */
    private String resolveImages(String html) {
        Function<String, Image> resolver = myImageResolver;
        if (resolver == null) {
            return html;
        }

        Matcher matcher = PLATFORM_IMAGE.matcher(html);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String id = matcher.group(1);

            String replacement = "";
            try {
                Image image = resolver.apply(id);
                if (image != null) {
                    replacement = toImageHtml(image);
                    if (replacement.isEmpty()) {
                        LOG.warn("The image " + id + " of an html view has no form the browser could show");
                    }
                }
            }
            catch (Exception e) {
                LOG.warn("Failed to resolve the image " + id + " of an html view", e);
            }

            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * The custom elements first - they follow the theme and the servlet serves each icon by its id. Not every
     * image is a composition of those though: one painted onto a canvas, or read out of a plugin jar, only ever
     * becomes a url, and an {@code img} of that url is the whole of what the browser needs.
     */
    private static String toImageHtml(Image image) {
        String elements = WebImageElement.toHtml(image);
        if (elements != null) {
            return elements;
        }

        String url = WebImageUrl.toURL(image);
        return url == null ? "" : "<img src=\"" + StringUtil.escapeXmlEntities(url) + "\">";
    }

    @Override
    public CompletableFuture<?> render(RenderData renderData) {
        String document = resolveImages(buildDocument(renderData));

        UIAccess uiAccess = getUIAccess();
        if (uiAccess == null) {
            getVaadinComponent().getElement().executeJs(RENDER_SCRIPT, document);
            return CompletableFuture.completedFuture(null);
        }

        return uiAccess.giveAsync(() -> getVaadinComponent().getElement().executeJs(RENDER_SCRIPT, document));
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
     * A stylesheet of a document is written against a document - it styles {@code body}, which a shadow root
     * does not have. The host of the root stands in for it, so those rules keep applying to what they were
     * written for instead of matching nothing.
     */
    private static String toShadowCss(String css) {
        return css.replaceAll("(^|[},;]|\\*/)(\\s*)body\\b", "$1$2:host");
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
                head.append("<style>\n")
                    .append(toShadowCss(StreamUtil.readText(stream, StandardCharsets.UTF_8)))
                    .append("\n</style>\n");
            }
            catch (IOException e) {
                LOG.warn("Failed to read the stylesheet " + css + " of an html view", e);
            }
        }

        String inlineCss = renderData.inlineCss();
        if (!inlineCss.isEmpty()) {
            head.append("<style>\n").append(toShadowCss(inlineCss)).append("\n</style>\n");
        }

        return renderData.html().replace("<head>", "<head>" + head);
    }
}
