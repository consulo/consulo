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
import consulo.ui.image.ImageKey;
import consulo.ui.impl.image.ImageReference;
import org.jspecify.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Turns a {@link WebImageSpec} into the bytes of a single image. Effects stay in the vector domain while the
 * source is svg - an icon is scaled by the browser after this point, and a rasterized effect would be scaled
 * with it.
 *
 * @author VISTALL
 * @since 2026-08-01
 */
public class WebImageRenderer {
    private static final Logger LOG = Logger.getInstance(WebImageRenderer.class);

    private static final String SVG_HEADER = "<svg xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\"";

    public static @Nullable WebRenderedImage render(WebImageSpec spec) {
        return switch (spec) {
            case WebImageSpec.Key key -> renderKey(key);
            case WebImageSpec.Empty empty -> WebRenderedImage.svg(SVG_HEADER
                + " width=\"" + Math.max(empty.width(), 0) + "\" height=\"" + Math.max(empty.height(), 0) + "\"/>");
            case WebImageSpec.Colorize colorize -> colorize(render(colorize.child()), colorize.rgb());
            case WebImageSpec.Alpha alpha -> alpha(render(alpha.child()), alpha.alpha());
            // the browser scales the element the image is served into, so a resize needs no new bytes
            case WebImageSpec.Resize resize -> render(resize.child());
            case WebImageSpec.Layered layered -> layered(layered);
        };
    }

    private static @Nullable WebRenderedImage renderKey(WebImageSpec.Key key) {
        WebImageKeyImpl imageKey = (WebImageKeyImpl)ImageKey.of(key.groupId(), key.imageId(), key.width(), key.height());

        ImageReference reference = imageKey.calcImage();
        if (!(reference instanceof WebImageReference webReference)) {
            return null;
        }

        return new WebRenderedImage(webReference.getData(), webReference.isSVG());
    }

    private static @Nullable WebRenderedImage layered(WebImageSpec.Layered layered) {
        int width = WebImageSpec.widthOrDefault(layered);
        int height = WebImageSpec.heightOrDefault(layered);

        StringBuilder builder = new StringBuilder(SVG_HEADER)
            .append(" width=\"").append(width).append("\" height=\"").append(height).append('"')
            .append(" viewBox=\"0 0 ").append(width).append(' ').append(height).append("\">");

        boolean any = false;

        for (WebImageSpec child : layered.children()) {
            WebRenderedImage rendered = render(child);
            if (rendered == null) {
                continue;
            }

            any = true;

            int childWidth = WebImageSpec.width(child);
            int childHeight = WebImageSpec.height(child);

            // an external reference is refused while an svg is loaded as an image, so the layers travel inline
            String dataURI = rendered.toDataURI();

            builder.append("<image x=\"0\" y=\"0\"")
                .append(" width=\"").append(childWidth > 0 ? childWidth : width).append('"')
                .append(" height=\"").append(childHeight > 0 ? childHeight : height).append('"')
                .append(" href=\"").append(dataURI).append('"')
                .append(" xlink:href=\"").append(dataURI).append("\"/>");
        }

        if (!any) {
            return null;
        }

        return WebRenderedImage.svg(builder.append("</svg>").toString());
    }

    private static @Nullable WebRenderedImage alpha(@Nullable WebRenderedImage source, float alpha) {
        if (source == null) {
            return null;
        }

        if (source.svg()) {
            return WebRenderedImage.svg(wrapInGroup(text(source), "", "opacity=\"" + alpha + "\""));
        }

        BufferedImage image = readPng(source);
        if (image == null) {
            return source;
        }

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                int newAlpha = Math.round(((argb >> 24) & 0xFF) * alpha);
                image.setRGB(x, y, (newAlpha << 24) | (argb & 0xFFFFFF));
            }
        }

        return writePng(image, source);
    }

    /**
     * Mirrors the awt effect: hue and saturation come from the target color, the brightness of the source is
     * kept as a factor, transparent pixels stay transparent.
     */
    private static @Nullable WebRenderedImage colorize(@Nullable WebRenderedImage source, int rgb) {
        if (source == null) {
            return null;
        }

        if (source.svg()) {
            return WebRenderedImage.svg(colorizeSvg(text(source), rgb));
        }

        BufferedImage image = readPng(source);
        if (image == null) {
            return source;
        }

        float[] base = Color.RGBtoHSB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, null);
        float[] hsb = new float[3];

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                int alpha = (argb >> 24) & 0xFF;
                if (alpha == 0) {
                    continue;
                }

                Color.RGBtoHSB((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, hsb);

                int colored = Color.HSBtoRGB(base[0], base[1], base[2] * hsb[2]);

                image.setRGB(x, y, (alpha << 24) | (colored & 0xFFFFFF));
            }
        }

        return writePng(image, source);
    }

    /**
     * The paint of an icon can sit in an attribute, in a style or in a class, and a gradient or an embedded
     * raster has no single paint at all - a filter recolors whatever the document draws without reading it.
     * Unlike the awt effect the source contribution is its luminance, since a filter has no way to compute
     * the hsb brightness of a pixel.
     */
    private static String colorizeSvg(String svg, int rgb) {
        String id = "consulo-colorize";

        String defs = "<defs><filter id=\"" + id + "\" color-interpolation-filters=\"sRGB\">"
            + "<feColorMatrix type=\"matrix\" result=\"luminance\" values=\""
            + "0.2126 0.7152 0.0722 0 0 "
            + "0.2126 0.7152 0.0722 0 0 "
            + "0.2126 0.7152 0.0722 0 0 "
            + "0 0 0 1 0\"/>"
            + "<feFlood flood-color=\"" + String.format("#%06x", rgb & 0xFFFFFF) + "\" result=\"tint\"/>"
            + "<feComposite in=\"tint\" in2=\"luminance\" operator=\"arithmetic\" k1=\"1\" k2=\"0\" k3=\"0\" k4=\"0\" result=\"colored\"/>"
            + "<feComposite in=\"colored\" in2=\"SourceAlpha\" operator=\"in\"/>"
            + "</filter></defs>";

        return wrapInGroup(svg, defs, "filter=\"url(#" + id + ")\"");
    }

    /**
     * Moves everything the document draws into a group carrying the effect. Rewriting the root element itself
     * would drop the width, height and viewBox the browser sizes the image by.
     */
    private static String wrapInGroup(String svg, String defs, String groupAttributes) {
        int rootStart = svg.indexOf("<svg");
        if (rootStart < 0) {
            return svg;
        }

        int rootEnd = findTagEnd(svg, rootStart);
        int close = svg.lastIndexOf("</svg>");
        if (rootEnd < 0 || close < rootEnd) {
            return svg;
        }

        return svg.substring(0, rootEnd + 1)
            + defs
            + "<g " + groupAttributes + ">"
            + svg.substring(rootEnd + 1, close)
            + "</g>"
            + svg.substring(close);
    }

    private static int findTagEnd(String text, int from) {
        char quote = 0;
        for (int i = from; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quote != 0) {
                if (c == quote) {
                    quote = 0;
                }
            }
            else if (c == '"' || c == '\'') {
                quote = c;
            }
            else if (c == '>') {
                return i;
            }
        }
        return -1;
    }

    private static String text(WebRenderedImage image) {
        return new String(image.data(), StandardCharsets.UTF_8);
    }

    private static @Nullable BufferedImage readPng(WebRenderedImage source) {
        try {
            BufferedImage read = ImageIO.read(new ByteArrayInputStream(source.data()));
            if (read == null) {
                return null;
            }

            BufferedImage argb = new BufferedImage(read.getWidth(), read.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = argb.createGraphics();
            graphics.drawImage(read, 0, 0, null);
            graphics.dispose();
            return argb;
        }
        catch (IOException e) {
            LOG.warn("Failed to read raster icon", e);
            return null;
        }
    }

    private static WebRenderedImage writePng(BufferedImage image, WebRenderedImage fallback) {
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            ImageIO.write(image, "png", stream);
            return new WebRenderedImage(stream.toByteArray(), false);
        }
        catch (IOException e) {
            LOG.warn("Failed to write raster icon", e);
            return fallback;
        }
    }

    private WebImageRenderer() {
    }
}
