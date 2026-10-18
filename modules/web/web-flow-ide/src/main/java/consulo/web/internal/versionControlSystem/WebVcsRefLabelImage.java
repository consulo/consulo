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
package consulo.web.internal.versionControlSystem;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Svg;
import consulo.ui.color.ColorValue;
import consulo.versionControlSystem.log.paint.VcsRefLabel;
import consulo.versionControlSystem.log.paint.VcsRefLabelTag;
import consulo.web.ui.impl.internal.WebColors;
import consulo.web.ui.impl.internal.image.WebComponentImage;

import java.util.Locale;

/**
 * Inline svg in the page, so the colours of the style follow a change of the style on the client.
 *
 * @author VISTALL
 * @since 2026-10-18
 */
final class WebVcsRefLabelImage implements WebComponentImage {
    private final VcsRefLabel myLabel;

    WebVcsRefLabelImage(VcsRefLabel label) {
        myLabel = label;
    }

    @Override
    public Component createComponent() {
        int width = myLabel.getWidth();
        int height = myLabel.getHeight();

        StringBuilder svg = new StringBuilder();
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"").append(width)
            .append("\" height=\"").append(height)
            .append("\" viewBox=\"0 0 ").append(width).append(' ').append(height)
            .append("\" style=\"display:block\">");
        for (VcsRefLabelTag tag : myLabel.resolveTags()) {
            svg.append("<path fill-rule=\"evenodd\" d=\"").append(path(tag))
                .append("\" style=\"fill:").append(color(tag.color())).append("\"/>");
        }
        svg.append("</svg>");

        Svg component = new Svg(svg.toString());
        component.getStyle()
            .set("width", width + "px")
            .set("height", height + "px")
            .set("flex", "none");
        return component;
    }

    private String path(VcsRefLabelTag tag) {
        float[] outline = myLabel.getOutline(tag);
        StringBuilder path = new StringBuilder();
        path.append('M').append(number(outline[0])).append(' ').append(number(outline[1]));
        for (int i = 2; i < outline.length; i += 2) {
            path.append('L').append(number(outline[i])).append(' ').append(number(outline[i + 1]));
        }
        path.append('Z');

        float radius = myLabel.getHoleRadius();
        float centerX = myLabel.getHoleCenterX(tag);
        String centerY = number(myLabel.getHoleCenterY());
        String arc = "A" + number(radius) + " " + number(radius) + " 0 1 0 ";
        String right = number(centerX + radius) + " " + centerY;
        String left = number(centerX - radius) + " " + centerY;
        path.append('M').append(right).append(arc).append(left).append(arc).append(right).append('Z');
        return path.toString();
    }

    private static String color(ColorValue color) {
        String css = WebColors.toStyleCssColor(color);
        return css == null ? "currentColor" : css;
    }

    private static String number(float value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    @Override
    public int getWidth() {
        return myLabel.getWidth();
    }

    @Override
    public int getHeight() {
        return myLabel.getHeight();
    }
}
