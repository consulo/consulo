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
import com.vaadin.flow.dom.DebouncePhase;
import com.vaadin.flow.dom.Element;
import consulo.ui.color.ColorValue;
import consulo.versionControlSystem.log.graph.paint.VcsGraphDot;
import consulo.versionControlSystem.log.graph.paint.VcsGraphMouseListener;
import consulo.versionControlSystem.log.graph.paint.VcsGraphRow;
import consulo.versionControlSystem.log.graph.paint.VcsGraphSegment;
import consulo.web.ui.impl.internal.WebColors;
import consulo.web.ui.impl.internal.image.WebComponentImage;
import tools.jackson.databind.JsonNode;

import java.util.Locale;

/**
 * The lines are laid out at the reference row height and stretched to the real one by the browser, their strokes kept
 * unscaled; the dots sit on the real middle of the row, so they stay round.
 *
 * @author VISTALL
 * @since 2026-10-18
 */
final class WebVcsGraphRowImage implements WebComponentImage {
    private static final String X_EXPRESSION = "event.clientX - element.getBoundingClientRect().left";
    private static final String Y_EXPRESSION = "event.clientY - element.getBoundingClientRect().top";
    private static final String HEIGHT_EXPRESSION = "element.getBoundingClientRect().height";
    private static final int MOUSE_MOVE_DEBOUNCE_MS = 50;

    private final VcsGraphRow myRow;

    WebVcsGraphRowImage(VcsGraphRow row) {
        myRow = row;
    }

    @Override
    public Component createComponent() {
        int width = myRow.getWidth();
        int height = VcsGraphRow.REFERENCE_ROW_HEIGHT;

        StringBuilder svg = new StringBuilder();
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"").append(width)
            .append("\" height=\"100%\" style=\"display:block;overflow:visible\">");
        svg.append("<svg width=\"").append(width).append("\" height=\"100%\" viewBox=\"0 0 ").append(width).append(' ').append(height)
            .append("\" preserveAspectRatio=\"none\" overflow=\"visible\">");
        for (VcsGraphSegment segment : myRow.resolveSegments(height)) {
            svg.append("<line x1=\"").append(number(segment.x1()))
                .append("\" y1=\"").append(number(segment.y1()))
                .append("\" x2=\"").append(number(segment.x2()))
                .append("\" y2=\"").append(number(segment.y2()))
                .append("\" vector-effect=\"non-scaling-stroke\" style=\"stroke:").append(color(segment.color()))
                .append(";stroke-width:").append(number(segment.width()))
                .append(";stroke-linecap:round");
            float[] dash = segment.dash();
            if (dash != null) {
                svg.append(";stroke-dasharray:").append(number(dash[0])).append(' ').append(number(dash[1]))
                    .append(";stroke-dashoffset:").append(number(dash[0] / 2));
            }
            svg.append("\"/>");
        }
        svg.append("</svg>");
        for (VcsGraphDot dot : myRow.resolveDots(height)) {
            svg.append("<circle cx=\"").append(number(dot.x()))
                .append("\" cy=\"50%\" r=\"").append(number(dot.radius()))
                .append("\" style=\"fill:").append(color(dot.color())).append("\"/>");
        }
        svg.append("</svg>");

        Svg component = new Svg(svg.toString());
        component.addClassName(FILL_CELL_CLASS);
        component.getStyle().set("width", width + "px");

        VcsGraphMouseListener listener = myRow.getMouseListener();
        if (listener != null) {
            listenMouse(component.getElement(), listener);
        }
        return component;
    }

    private static void listenMouse(Element element, VcsGraphMouseListener listener) {
        element.addEventListener("mousemove", event -> {
                JsonNode data = event.getEventData();
                listener.mouseMoved(x(data), y(data), VcsGraphRow.REFERENCE_ROW_HEIGHT);
            })
            .addEventData(X_EXPRESSION)
            .addEventData(Y_EXPRESSION)
            .addEventData(HEIGHT_EXPRESSION)
            .debounce(MOUSE_MOVE_DEBOUNCE_MS, DebouncePhase.LEADING, DebouncePhase.TRAILING);

        element.addEventListener("click", event -> {
                JsonNode data = event.getEventData();
                listener.mouseClicked(x(data), y(data), VcsGraphRow.REFERENCE_ROW_HEIGHT);
            })
            .addEventData(X_EXPRESSION)
            .addEventData(Y_EXPRESSION)
            .addEventData(HEIGHT_EXPRESSION);

        element.addEventListener("mouseleave", event -> listener.mouseExited());
    }

    private static float x(JsonNode data) {
        return (float) data.path(X_EXPRESSION).asDouble(0);
    }

    private static float y(JsonNode data) {
        double height = data.path(HEIGHT_EXPRESSION).asDouble(0);
        double y = data.path(Y_EXPRESSION).asDouble(0);
        return height <= 0 ? 0 : (float) (y * VcsGraphRow.REFERENCE_ROW_HEIGHT / height);
    }

    private static String color(ColorValue color) {
        String css = WebColors.toCssColor(color);
        return css == null ? "currentColor" : css;
    }

    private static String number(float value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    @Override
    public int getWidth() {
        return myRow.getWidth();
    }

    @Override
    public int getHeight() {
        return 1;
    }
}
