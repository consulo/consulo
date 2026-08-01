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

import consulo.ui.color.ColorValue;
import consulo.ui.color.RGBColor;
import consulo.ui.font.Font;
import consulo.ui.image.Image;
import consulo.ui.image.canvas.Canvas2D;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;

/**
 * Replays canvas calls into an svg document, so a drawn image can be handed to the browser as a plain
 * url like every other image instead of needing a live {@code <canvas>} element to paint itself into.
 *
 * @author VISTALL
 * @since 2026-08-01
 */
public class WebCanvasSvgWriter implements Canvas2D {
    private static class State implements Cloneable {
        private @Nullable ColorValue myFill;
        private @Nullable ColorValue myStroke;
        private float myGlobalAlpha = 1f;
        private double myStrokeWidth = 1;
        private @Nullable Font myFont;
        private TextAlign myTextAlign = TextAlign.left;
        private TextBaseline myTextBaseline = TextBaseline.bottom;
        private double myScale = 1;
        private double myTranslateX;
        private double myTranslateY;

        @Override
        protected State clone() {
            try {
                return (State)super.clone();
            }
            catch (CloneNotSupportedException e) {
                throw new AssertionError(e);
            }
        }
    }

    private final StringBuilder myBody = new StringBuilder();
    private final StringBuilder myPath = new StringBuilder();
    private final Deque<State> myStack = new ArrayDeque<>();
    private final int myWidth;
    private final int myHeight;

    private State myState = new State();

    public WebCanvasSvgWriter(int width, int height) {
        myWidth = width;
        myHeight = height;
    }

    public String toSVG() {
        return "<svg xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\""
            + " width=\"" + myWidth + "\" height=\"" + myHeight + "\""
            + " viewBox=\"0 0 " + myWidth + " " + myHeight + "\">"
            + myBody
            + "</svg>";
    }

    @Override
    public void setGlobalAlpha(float value) {
        myState.myGlobalAlpha = value;
    }

    @Override
    public void setFont(Font font) {
        myState.myFont = font;
    }

    @Override
    public void setFillStyle(@Nullable ColorValue value) {
        myState.myFill = value;
    }

    @Override
    public void setStrokeStyle(@Nullable ColorValue value) {
        myState.myStroke = value;
    }

    @Override
    public void setTextAlign(TextAlign textAlign) {
        myState.myTextAlign = textAlign;
    }

    @Override
    public void setTextBaseline(TextBaseline baseline) {
        myState.myTextBaseline = baseline;
    }

    @Override
    public void save() {
        myStack.push(myState.clone());
    }

    @Override
    public void restore() {
        State state = myStack.poll();
        if (state != null) {
            myState = state;
        }
    }

    @Override
    public void beginPath() {
        myPath.setLength(0);
    }

    @Override
    public void moveTo(double x, double y) {
        myPath.append('M').append(number(x)).append(' ').append(number(y));
    }

    @Override
    public void lineTo(double x, double y) {
        myPath.append('L').append(number(x)).append(' ').append(number(y));
    }

    @Override
    public void quadraticCurveTo(double x1, double y1, double x2, double y2) {
        myPath.append('Q').append(number(x1)).append(' ').append(number(y1))
            .append(' ').append(number(x2)).append(' ').append(number(y2));
    }

    @Override
    public void curveTo(double x1, double y1, double x2, double y2, double x3, double y3) {
        myPath.append('C').append(number(x1)).append(' ').append(number(y1))
            .append(' ').append(number(x2)).append(' ').append(number(y2))
            .append(' ').append(number(x3)).append(' ').append(number(y3));
    }

    @Override
    public void closePath() {
        myPath.append('Z');
    }

    @Override
    public void rect(double x, double y, double w, double h) {
        myPath.append('M').append(number(x)).append(' ').append(number(y))
            .append('H').append(number(x + w))
            .append('V').append(number(y + h))
            .append('H').append(number(x))
            .append('Z');
    }

    @Override
    public void arc(double x, double y, double r, double sAngle, double eAngle) {
        double sweep = eAngle - sAngle;

        // an svg elliptical arc whose end lands on its start draws nothing, so a full turn is cut in halves
        if (Math.abs(sweep) >= Math.PI * 2) {
            myPath.append('M').append(number(x - r)).append(' ').append(number(y))
                .append('A').append(number(r)).append(' ').append(number(r)).append(" 0 1 1 ")
                .append(number(x + r)).append(' ').append(number(y))
                .append('A').append(number(r)).append(' ').append(number(r)).append(" 0 1 1 ")
                .append(number(x - r)).append(' ').append(number(y))
                .append('Z');
            return;
        }

        myPath.append('M').append(number(x + r * Math.cos(sAngle))).append(' ').append(number(y + r * Math.sin(sAngle)))
            .append('A').append(number(r)).append(' ').append(number(r)).append(" 0 ")
            .append(Math.abs(sweep) > Math.PI ? '1' : '0').append(' ')
            .append(sweep < 0 ? '0' : '1').append(' ')
            .append(number(x + r * Math.cos(eAngle))).append(' ').append(number(y + r * Math.sin(eAngle)));
    }

    @Override
    public void fill() {
        emitPath(myState.myFill, false);
    }

    @Override
    public void stroke() {
        emitPath(myState.myStroke, true);
    }

    private void emitPath(@Nullable ColorValue color, boolean stroked) {
        if (myPath.isEmpty() || color == null) {
            return;
        }

        RGBColor rgb = color.toRGB();

        myBody.append("<path d=\"").append(myPath).append('"');
        if (stroked) {
            myBody.append(" fill=\"none\" stroke=\"").append(hex(rgb)).append('"')
                .append(" stroke-width=\"").append(number(myState.myStrokeWidth)).append('"')
                .append(" stroke-opacity=\"").append(number(opacity(rgb))).append('"');
        }
        else {
            myBody.append(" fill=\"").append(hex(rgb)).append('"')
                .append(" fill-opacity=\"").append(number(opacity(rgb))).append('"');
        }
        appendTransform();
        myBody.append("/>");
    }

    @Override
    public void fillText(String text, double x, double y, double maxWidth) {
        ColorValue fill = myState.myFill;
        if (fill == null) {
            return;
        }

        RGBColor rgb = fill.toRGB();
        Font font = myState.myFont;

        myBody.append("<text x=\"").append(number(x)).append("\" y=\"").append(number(y)).append('"')
            .append(" fill=\"").append(hex(rgb)).append('"')
            .append(" fill-opacity=\"").append(number(opacity(rgb))).append('"');

        if (font != null) {
            myBody.append(" font-family=\"").append(escape(font.getFamily())).append('"')
                .append(" font-size=\"").append(font.getFontSize()).append('"');
            if ((font.getFontStyle() & Font.STYLE_BOLD) != 0) {
                myBody.append(" font-weight=\"bold\"");
            }
            if ((font.getFontStyle() & Font.STYLE_ITALIC) != 0) {
                myBody.append(" font-style=\"italic\"");
            }
        }

        myBody.append(" text-anchor=\"").append(switch (myState.myTextAlign) {
            case left -> "start";
            case center -> "middle";
            case right -> "end";
        }).append('"');

        myBody.append(" dominant-baseline=\"").append(switch (myState.myTextBaseline) {
            case top -> "text-before-edge";
            case middle -> "central";
            case bottom -> "alphabetic";
        }).append('"');

        appendTransform();

        myBody.append('>').append(escape(text)).append("</text>");
    }

    @Override
    public void drawImage(Image image, double x, double y, double w, double h) {
        String dataURI = WebImageUrl.toDataURI(image);
        if (dataURI == null) {
            return;
        }

        myBody.append("<image x=\"").append(number(x)).append("\" y=\"").append(number(y))
            .append("\" width=\"").append(number(w)).append("\" height=\"").append(number(h)).append('"')
            .append(" href=\"").append(dataURI).append('"')
            .append(" xlink:href=\"").append(dataURI).append('"');
        if (myState.myGlobalAlpha != 1f) {
            myBody.append(" opacity=\"").append(number(myState.myGlobalAlpha)).append('"');
        }
        appendTransform();
        myBody.append("/>");
    }

    private void appendTransform() {
        if (myState.myTranslateX != 0 || myState.myTranslateY != 0 || myState.myScale != 1) {
            myBody.append(" transform=\"translate(").append(number(myState.myTranslateX)).append(' ')
                .append(number(myState.myTranslateY)).append(") scale(").append(number(myState.myScale)).append(")\"");
        }
    }

    private double opacity(RGBColor color) {
        return myState.myGlobalAlpha * color.getAlpha() / 255f;
    }

    private static String hex(RGBColor color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    private static String number(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long)value);
        }
        return String.format(Locale.ROOT, "%.3f", value);
    }

    static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    @Override
    public void scale(double value) {
        myState.myScale *= value;
    }

    @Override
    public void translate(double dx, double dy) {
        myState.myTranslateX += myState.myScale * dx;
        myState.myTranslateY += myState.myScale * dy;
    }

    @Override
    public void setStrokeWidth(double value) {
        myState.myStrokeWidth = value;
    }

    @Override
    public void rotate(double theta, boolean flipH, boolean flipV, double cx, double cy) {
    }

    @Override
    public void setDashed(boolean value) {
    }

    @Override
    public void setDashed(boolean value, boolean fixDash) {
    }

    @Override
    public void setDashPattern(String value) {
    }

    @Override
    public void setLineCap(String value) {
    }

    @Override
    public void setLineJoin(String value) {
    }

    @Override
    public void setMiterLimit(double value) {
    }

    @Override
    public void setGradient(ColorValue color1,
                            ColorValue color2,
                            double x,
                            double y,
                            double w,
                            double h,
                            String direction,
                            double alpha1,
                            double alpha2) {
    }

    @Override
    public void setShadow(boolean enabled) {
    }

    @Override
    public void setShadowColor(ColorValue value) {
    }

    @Override
    public void setShadowAlpha(double value) {
    }

    @Override
    public void setShadowOffset(double dx, double dy) {
    }
}
