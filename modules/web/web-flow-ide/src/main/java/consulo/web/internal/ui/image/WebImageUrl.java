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
package consulo.web.internal.ui.image;

import consulo.logging.Logger;
import consulo.ui.color.RGBColor;
import consulo.ui.image.IconLibraryManager;
import consulo.ui.image.Image;
import consulo.util.io.StreamUtil;
import org.jspecify.annotations.Nullable;

import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The single way an {@link Image} reaches the browser. Everything that shows an icon - a vaadin component, a
 * gutter mark, the analyze status - asks for one url here, instead of walking the image hierarchy on its own
 * and rebuilding the composition out of several elements.
 *
 * @author VISTALL
 * @since 2026-08-01
 */
public class WebImageUrl {
    public static final String PATH = "/image";

    public static final String GROUP_ID = "groupId";
    public static final String IMAGE_ID = "imageId";
    public static final String SPEC = "spec";
    public static final String VERSION = "v";

    private static final Logger LOG = Logger.getInstance(WebImageUrl.class);

    private static final Map<String, String> ourInlinedUrls = new ConcurrentHashMap<>();

    /**
     * @return url the browser can put into {@code <img src>}, or null when the image cannot be shown at all
     */
    public static @Nullable String toURL(Image image) {
        String inlined = toInlineURL(image);
        if (inlined != null) {
            return inlined;
        }

        WebImageSpec spec = toSpec(image);
        return spec == null ? null : toURL(spec);
    }

    public static String toURL(WebImageSpec spec) {
        StringBuilder url = new StringBuilder(PATH);

        if (spec instanceof WebImageSpec.Key key) {
            url.append('?').append(GROUP_ID).append('=').append(encode(key.groupId()))
                .append('&').append(IMAGE_ID).append('=').append(encode(key.imageId()));
        }
        else {
            url.append('?').append(SPEC).append('=').append(encode(WebImageSpec.encode(spec)));
        }

        // the same group and image resolve to different bytes once the icon library changes, and the url is
        // the only thing a browser cache is keyed on
        return url.append('&').append(VERSION).append('=').append(IconLibraryManager.get().getModificationCount()).toString();
    }

    public static @Nullable WebImageSpec toSpec(Image image) {
        switch (WebDelegatingImage.unwrap(image)) {
            case WebImageKeyImpl key: {
                return new WebImageSpec.Key(key.getGroupId(), key.getImageId(), key.getWidth(), key.getHeight());
            }
            case WebEmptyImageImpl empty: {
                return new WebImageSpec.Empty(empty.getWidth(), empty.getHeight());
            }
            case WebColorizeImageImpl colorize: {
                WebImageSpec child = toSpec(colorize.getOriginal());
                if (child == null) {
                    return null;
                }
                RGBColor color = colorize.getColorValue().toRGB();
                return new WebImageSpec.Colorize(child, color.getRed() << 16 | color.getGreen() << 8 | color.getBlue());
            }
            case WebTransparentImageImpl transparent: {
                WebImageSpec child = toSpec(transparent.getOriginal());
                return child == null ? null : new WebImageSpec.Alpha(child, transparent.getAlpha());
            }
            case WebResizeImageImpl resize: {
                WebImageSpec child = toSpec(resize.getOriginal());
                return child == null ? null : new WebImageSpec.Resize(child, resize.getWidth(), resize.getHeight());
            }
            case WebLayeredImageImpl layered: {
                List<WebImageSpec> children = new ArrayList<>();
                for (Image layer : layered.getImages()) {
                    WebImageSpec child = toSpec(layer);
                    if (child != null) {
                        children.add(child);
                    }
                }
                return children.isEmpty() ? null : new WebImageSpec.Layered(children);
            }
            case WebGrayedImageImpl grayed: {
                WebImageSpec child = toSpec(grayed.getOriginal());
                return child == null ? null : new WebImageSpec.Gray(child, grayed.getPercent());
            }
            case WebAppendImageImpl appended: {
                WebImageSpec left = toSpec(appended.getLeft());
                WebImageSpec right = toSpec(appended.getRight());
                if (left == null || right == null) {
                    return null;
                }
                return new WebImageSpec.Append(left, right);
            }
            case WebTextImageImpl text: {
                WebImageSpec child = toSpec(text.getBaseImage());
                return child == null ? null : new WebImageSpec.Text(child, text.getText());
            }
            default: {
                return null;
            }
        }
    }

    /**
     * @return data uri for an image the servlet cannot rebuild from its url alone
     */
    private static @Nullable String toInlineURL(Image original) {
        Image image = WebDelegatingImage.unwrap(original);

        if (image instanceof WebCanvasImageImpl canvas) {
            WebCanvasSvgWriter writer = new WebCanvasSvgWriter(canvas.getWidth(), canvas.getHeight());
            canvas.getConsumer().accept(writer);
            return WebRenderedImage.svg(writer.toSVG()).toDataURI();
        }

        if (image instanceof WebImageImpl external) {
            URL url = external.getURL();
            String protocol = url.getProtocol();
            if ("http".equals(protocol) || "https".equals(protocol)) {
                return url.toExternalForm();
            }

            // a jar or file url means nothing to the browser, and the servlet has no safe way to open an
            // arbitrary one on request
            String inlined = ourInlinedUrls.computeIfAbsent(url.toExternalForm(), it -> inline(url));
            return inlined.isEmpty() ? null : inlined;
        }

        return null;
    }

    static @Nullable String toDataURI(Image image) {
        String inlined = toInlineURL(image);
        if (inlined != null) {
            return inlined.startsWith("data:") ? inlined : null;
        }

        WebImageSpec spec = toSpec(image);
        if (spec == null) {
            return null;
        }

        WebRenderedImage rendered = WebImageRenderer.render(spec);
        return rendered == null ? null : rendered.toDataURI();
    }

    private static String inline(URL url) {
        try (InputStream stream = url.openStream()) {
            byte[] bytes = StreamUtil.loadFromStream(stream);
            String contentType = url.getFile().endsWith(".svg") ? "image/svg+xml" : "image/png";
            return "data:" + contentType + ";base64," + Base64.getEncoder().encodeToString(bytes);
        }
        catch (Exception e) {
            LOG.warn("Failed to inline image " + url, e);
            return "";
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private WebImageUrl() {
    }
}
