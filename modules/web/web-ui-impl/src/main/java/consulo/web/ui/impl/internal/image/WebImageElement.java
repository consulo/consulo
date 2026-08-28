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
package consulo.web.ui.impl.internal.image;

import com.vaadin.flow.dom.Element;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Builds the custom element tree of {@code frontend/webImage.js} out of a {@link WebImageSpec}. The effects are
 * applied by the browser, so the servlet is left serving one icon at a time and nothing has to rebuild a
 * composition on the server.
 * <p/>
 * The same tree is produced twice, as flow elements for a component and as markup for the editor, because the
 * editor pushes its icons inside a json payload and never gets a component to attach.
 *
 * @author VISTALL
 * @since 2026-08-01
 */
public class WebImageElement {
    private static final String IMAGE = "web-image";
    private static final String EMPTY = "web-image-empty";
    private static final String LAYERED = "web-image-layered";
    private static final String COLORIZE = "web-image-colorize";
    private static final String TRANSPARENT = "web-image-transparent";
    private static final String GRAYED = "web-image-grayed";
    private static final String APPEND = "web-image-append";
    private static final String TEXT = "web-image-text";

    public static @Nullable Element toElement(Image image) {
        WebImageSpec spec = WebImageUrl.toSpec(image);
        if (spec != null) {
            return toElement(spec);
        }

        String url = WebImageUrl.toURL(image);
        if (url == null) {
            return null;
        }

        Element element = new Element(IMAGE);
        element.setAttribute("width", String.valueOf(image.getWidth()));
        element.setAttribute("height", String.valueOf(image.getHeight()));
        element.setAttribute("src", url);
        return element;
    }

    public static Element toElement(WebImageSpec spec) {
        Element element = new Element(tagOf(spec));

        element.setAttribute("width", String.valueOf(WebImageSpec.widthOrDefault(spec)));
        element.setAttribute("height", String.valueOf(WebImageSpec.heightOrDefault(spec)));

        switch (spec) {
            case WebImageSpec.Key key -> element.setAttribute("src", WebImageUrl.toURL(key));
            case WebImageSpec.Empty ignored -> {
            }
            case WebImageSpec.Colorize colorize -> {
                element.setAttribute("color", toCssColor(colorize.rgb()));
                element.appendChild(toElement(colorize.child()));
            }
            case WebImageSpec.Alpha alpha -> {
                element.setAttribute("alpha", String.valueOf(alpha.alpha()));
                element.appendChild(toElement(alpha.child()));
            }
            // a resize is the box of its child at another size, there is no effect of its own to apply
            case WebImageSpec.Resize resize -> {
                Element child = toElement(resize.child());
                child.setAttribute("width", String.valueOf(resize.width()));
                child.setAttribute("height", String.valueOf(resize.height()));
                return child;
            }
            case WebImageSpec.Layered layered -> {
                for (WebImageSpec child : layered.children()) {
                    element.appendChild(toElement(child));
                }
            }
            case WebImageSpec.Gray gray -> {
                element.setAttribute("percent", String.valueOf(gray.percent()));
                element.appendChild(toElement(gray.child()));
            }
            case WebImageSpec.Append append -> {
                element.appendChild(toElement(append.left()));
                element.appendChild(toElement(append.right()));
            }
            case WebImageSpec.Text text -> {
                element.setAttribute("text", text.text());
                element.appendChild(toElement(text.child()));
            }
        }

        return element;
    }

    public static @Nullable String toHtml(Image image) {
        WebImageSpec spec = WebImageUrl.toSpec(image);
        if (spec != null) {
            return toHtml(spec);
        }

        String url = WebImageUrl.toURL(image);
        if (url == null) {
            return null;
        }

        return "<" + IMAGE + " width=\"" + image.getWidth() + "\" height=\"" + image.getHeight()
            + "\" src=\"" + escape(url) + "\"></" + IMAGE + ">";
    }

    public static String toHtml(WebImageSpec spec) {
        StringBuilder html = new StringBuilder();
        appendHtml(html, spec);
        return html.toString();
    }

    private static void appendHtml(StringBuilder html, WebImageSpec spec) {
        if (spec instanceof WebImageSpec.Resize resize) {
            appendHtml(html, resize.child(), resize.width(), resize.height());
            return;
        }

        appendHtml(html, spec, WebImageSpec.widthOrDefault(spec), WebImageSpec.heightOrDefault(spec));
    }

    private static void appendHtml(StringBuilder html, WebImageSpec spec, int width, int height) {
        String tag = tagOf(spec);

        html.append('<').append(tag)
            .append(" width=\"").append(width).append('"')
            .append(" height=\"").append(height).append('"');

        List<WebImageSpec> children = List.of();

        switch (spec) {
            case WebImageSpec.Key key -> html.append(" src=\"").append(escape(WebImageUrl.toURL(key))).append('"');
            case WebImageSpec.Empty ignored -> {
            }
            case WebImageSpec.Colorize colorize -> {
                html.append(" color=\"").append(toCssColor(colorize.rgb())).append('"');
                children = List.of(colorize.child());
            }
            case WebImageSpec.Alpha alpha -> {
                html.append(" alpha=\"").append(alpha.alpha()).append('"');
                children = List.of(alpha.child());
            }
            case WebImageSpec.Resize resize -> children = List.of(resize.child());
            case WebImageSpec.Layered layered -> children = layered.children();
            case WebImageSpec.Gray gray -> {
                html.append(" percent=\"").append(gray.percent()).append('"');
                children = List.of(gray.child());
            }
            case WebImageSpec.Append append -> children = List.of(append.left(), append.right());
            case WebImageSpec.Text text -> {
                html.append(" text=\"").append(escape(text.text())).append('"');
                children = List.of(text.child());
            }
        }

        html.append('>');

        for (WebImageSpec child : children) {
            appendHtml(html, child);
        }

        html.append("</").append(tag).append('>');
    }

    private static String tagOf(WebImageSpec spec) {
        return switch (spec) {
            case WebImageSpec.Key ignored -> IMAGE;
            case WebImageSpec.Empty ignored -> EMPTY;
            case WebImageSpec.Colorize ignored -> COLORIZE;
            case WebImageSpec.Alpha ignored -> TRANSPARENT;
            case WebImageSpec.Resize ignored -> IMAGE;
            case WebImageSpec.Layered ignored -> LAYERED;
            case WebImageSpec.Gray ignored -> GRAYED;
            case WebImageSpec.Append ignored -> APPEND;
            case WebImageSpec.Text ignored -> TEXT;
        };
    }

    private static String toCssColor(int rgb) {
        return String.format("#%06x", rgb & 0xFFFFFF);
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;");
    }

    private WebImageElement() {
    }
}
