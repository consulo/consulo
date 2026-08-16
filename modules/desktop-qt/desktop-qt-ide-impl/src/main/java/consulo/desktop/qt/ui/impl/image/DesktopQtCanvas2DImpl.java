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
package consulo.desktop.qt.ui.impl.image;

import consulo.ui.color.ColorValue;
import consulo.ui.color.RGBColor;
import consulo.ui.font.Font;
import consulo.ui.image.Image;
import consulo.ui.image.canvas.Canvas2D;
import io.qt.core.QRectF;
import io.qt.core.Qt;
import io.qt.gui.*;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtCanvas2DImpl implements Canvas2D {
    private static class State {
        private float myGlobalAlpha = 1f;
        private @Nullable QColor myFillColor = new QColor(0, 0, 0, 255);
        private @Nullable QColor myStrokeColor;
        private @Nullable QGradient myGradient;
        private TextAlign myTextAlign = TextAlign.left;
        private TextBaseline myTextBaseline = TextBaseline.bottom;
        private double myStrokeWidth = 1;
        private boolean myDashed;
        private boolean myFixDash;
        private @Nullable List<Double> myDashPattern;
        private Qt.PenCapStyle myCapStyle = Qt.PenCapStyle.FlatCap;
        private Qt.PenJoinStyle myJoinStyle = Qt.PenJoinStyle.MiterJoin;
        private double myMiterLimit = 10;
        private boolean myShadow;
        private QColor myShadowColor = new QColor(128, 128, 128, 255);
        private double myShadowAlpha = 1;
        private double myShadowOffsetX = 2;
        private double myShadowOffsetY = 3;

        private State copy() {
            State state = new State();
            state.myGlobalAlpha = myGlobalAlpha;
            state.myFillColor = myFillColor;
            state.myStrokeColor = myStrokeColor;
            state.myGradient = myGradient;
            state.myTextAlign = myTextAlign;
            state.myTextBaseline = myTextBaseline;
            state.myStrokeWidth = myStrokeWidth;
            state.myDashed = myDashed;
            state.myFixDash = myFixDash;
            state.myDashPattern = myDashPattern;
            state.myCapStyle = myCapStyle;
            state.myJoinStyle = myJoinStyle;
            state.myMiterLimit = myMiterLimit;
            state.myShadow = myShadow;
            state.myShadowColor = myShadowColor;
            state.myShadowAlpha = myShadowAlpha;
            state.myShadowOffsetX = myShadowOffsetX;
            state.myShadowOffsetY = myShadowOffsetY;
            return state;
        }
    }

    private final QPainter myPainter;
    private final Deque<State> myStack = new LinkedList<>();

    private State myState = new State();
    private QPainterPath myPath = new QPainterPath();

    public DesktopQtCanvas2DImpl(QPainter painter) {
        myPainter = painter;
    }

    @Override
    public void setGlobalAlpha(float value) {
        myState.myGlobalAlpha = value;
        myPainter.setOpacity(value);
    }

    @Override
    public void setFont(Font font) {
        QFont qFont = new QFont(font.getFamily());
        qFont.setPixelSize(Math.max(font.getFontSize(), 1));
        qFont.setBold((font.getFontStyle() & Font.BOLD) != 0);
        qFont.setItalic((font.getFontStyle() & Font.ITALIC) != 0);
        myPainter.setFont(qFont);
    }

    @Override
    public void setFillStyle(@Nullable ColorValue value) {
        myState.myFillColor = toQColor(value);
        myState.myGradient = null;
    }

    @Override
    public void setStrokeStyle(@Nullable ColorValue value) {
        myState.myStrokeColor = toQColor(value);
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
        myPainter.save();
        myStack.push(myState);
        myState = myState.copy();
    }

    @Override
    public void restore() {
        if (myStack.isEmpty()) {
            return;
        }

        myPainter.restore();
        myState = myStack.pop();
    }

    @Override
    public void rect(double x, double y, double w, double h) {
        myPath.addRect(x, y, w, h);
    }

    @Override
    public void fillRect(double x, double y, double w, double h) {
        QPainterPath path = new QPainterPath();
        path.addRect(x, y, w, h);
        fillPath(path);
    }

    @Override
    public void strokeRect(double x, double y, double w, double h) {
        QPainterPath path = new QPainterPath();
        path.addRect(x, y, w, h);
        strokePath(path);
    }

    @Override
    public void arc(double x, double y, double r, double sAngle, double eAngle) {
        // html canvas angles grow clockwise from 3 o'clock, qt angles grow counter clockwise
        double startAngle = -Math.toDegrees(sAngle);
        double sweepAngle = -Math.toDegrees(eAngle - sAngle);

        if (myPath.isEmpty()) {
            myPath.arcMoveTo(x - r, y - r, r * 2, r * 2, startAngle);
        }

        myPath.arcTo(x - r, y - r, r * 2, r * 2, startAngle, sweepAngle);
    }

    @Override
    public void drawImage(Image image, double x, double y, double w, double h) {
        if (w <= 0 || h <= 0 || !(image instanceof DesktopQtImage qtImage)) {
            return;
        }

        try {
            QPixmap pixmap = qtImage.toQPixmap();
            if (pixmap == null || pixmap.isNull()) {
                return;
            }

            // the target rect is logical while the source rect is the physical extent of the pixmap
            myPainter.drawPixmap(new QRectF(x, y, w, h), pixmap, new QRectF(0, 0, pixmap.width(), pixmap.height()));
        }
        catch (Throwable ignored) {
        }
    }

    @Override
    public void fillText(String text, double x, double y, double maxWidth) {
        if (text.isEmpty() || myState.myFillColor == null) {
            return;
        }

        QFontMetrics metrics = myPainter.fontMetrics();

        String[] lines = text.split("\n");

        int[] lineWidths = new int[lines.length];
        int textWidth = 0;
        for (int i = 0; i < lines.length; i++) {
            lineWidths[i] = metrics.horizontalAdvance(lines[i]);
            textWidth = Math.max(textWidth, lineWidths[i]);
        }

        int lineHeight = metrics.height();
        int textHeight = lineHeight * lines.length;

        double baseX = x;
        if (myState.myTextAlign == TextAlign.center) {
            baseX -= textWidth / 2.0;
        }
        else if (myState.myTextAlign == TextAlign.right) {
            baseX -= textWidth;
        }

        double baseY = y;
        if (myState.myTextBaseline == TextBaseline.middle) {
            baseY -= textHeight / 2.0;
        }
        else if (myState.myTextBaseline == TextBaseline.bottom) {
            baseY -= textHeight;
        }

        baseY += metrics.ascent();

        myPainter.save();
        try {
            myPainter.setPen(myState.myFillColor);

            for (int i = 0; i < lines.length; i++) {
                if (!lines[i].isEmpty()) {
                    double lineX = baseX;
                    if (myState.myTextAlign == TextAlign.center) {
                        lineX += (textWidth - lineWidths[i]) / 2.0;
                    }
                    else if (myState.myTextAlign == TextAlign.right) {
                        lineX += textWidth - lineWidths[i];
                    }

                    myPainter.drawText((int) Math.round(lineX), (int) Math.round(baseY), lines[i]);
                }

                baseY += lineHeight;
            }
        }
        finally {
            myPainter.restore();
        }
    }

    @Override
    public void beginPath() {
        myPath = new QPainterPath();
    }

    @Override
    public void moveTo(double x, double y) {
        myPath.moveTo(x, y);
    }

    @Override
    public void lineTo(double x, double y) {
        myPath.lineTo(x, y);
    }

    @Override
    public void quadraticCurveTo(double x1, double y1, double x2, double y2) {
        myPath.quadTo(x1, y1, x2, y2);
    }

    @Override
    public void curveTo(double x1, double y1, double x2, double y2, double x3, double y3) {
        myPath.cubicTo(x1, y1, x2, y2, x3, y3);
    }

    @Override
    public void closePath() {
        if (!myPath.isEmpty()) {
            myPath.closeSubpath();
        }
    }

    @Override
    public void stroke() {
        strokePath(myPath);
    }

    @Override
    public void fill() {
        fillPath(myPath);
    }

    @Override
    public void scale(double value) {
        myPainter.scale(value, value);
    }

    @Override
    public void translate(double dx, double dy) {
        myPainter.translate(dx, dy);
    }

    @Override
    public void rotate(double theta, boolean flipH, boolean flipV, double cx, double cy) {
        myPainter.translate(cx, cy);
        myPainter.rotate(theta);
        if (flipH || flipV) {
            myPainter.scale(flipH ? -1 : 1, flipV ? -1 : 1);
        }
        myPainter.translate(-cx, -cy);
    }

    @Override
    public void setStrokeWidth(double value) {
        myState.myStrokeWidth = value;
    }

    @Override
    public void setDashed(boolean value) {
        setDashed(value, myState.myFixDash);
    }

    @Override
    public void setDashed(boolean value, boolean fixDash) {
        myState.myDashed = value;
        myState.myFixDash = fixDash;
    }

    @Override
    public void setDashPattern(String value) {
        myState.myDashPattern = parseDashPattern(value);
    }

    @Override
    public void setLineCap(String value) {
        myState.myCapStyle = switch (value) {
            case "round" -> Qt.PenCapStyle.RoundCap;
            case "square" -> Qt.PenCapStyle.SquareCap;
            default -> Qt.PenCapStyle.FlatCap;
        };
    }

    @Override
    public void setLineJoin(String value) {
        myState.myJoinStyle = switch (value) {
            case "round" -> Qt.PenJoinStyle.RoundJoin;
            case "bevel" -> Qt.PenJoinStyle.BevelJoin;
            default -> Qt.PenJoinStyle.MiterJoin;
        };
    }

    @Override
    public void setMiterLimit(double value) {
        myState.myMiterLimit = value;
    }

    @Override
    public void setGradient(
        ColorValue color1,
        ColorValue color2,
        double x,
        double y,
        double w,
        double h,
        String direction,
        double alpha1,
        double alpha2
    ) {
        double x1 = x;
        double y1 = y;
        double x2 = x;
        double y2 = y;

        if (direction == null || direction.isEmpty() || "south".equals(direction)) {
            y2 = y + h;
        }
        else if ("east".equals(direction)) {
            x2 = x + w;
        }
        else if ("north".equals(direction)) {
            y1 = y + h;
        }
        else if ("west".equals(direction)) {
            x1 = x + w;
        }

        QLinearGradient gradient = new QLinearGradient(x1, y1, x2, y2);
        gradient.setColorAt(0, withAlpha(toQColor(color1), alpha1));
        gradient.setColorAt(1, withAlpha(toQColor(color2), alpha2));

        myState.myGradient = gradient;
        myState.myFillColor = null;
    }

    @Override
    public void setShadow(boolean enabled) {
        myState.myShadow = enabled;
    }

    @Override
    public void setShadowColor(ColorValue value) {
        QColor color = toQColor(value);
        if (color != null) {
            myState.myShadowColor = color;
        }
    }

    @Override
    public void setShadowAlpha(double value) {
        myState.myShadowAlpha = value;
    }

    @Override
    public void setShadowOffset(double dx, double dy) {
        myState.myShadowOffsetX = dx;
        myState.myShadowOffsetY = dy;
    }

    private void fillPath(QPainterPath path) {
        if (path.isEmpty()) {
            return;
        }

        if (myState.myShadow) {
            paintShadow(path, true, false);
        }

        if (myState.myGradient != null) {
            myPainter.fillPath(path, myState.myGradient);
        }
        else if (myState.myFillColor != null) {
            myPainter.fillPath(path, myState.myFillColor);
        }
    }

    private void strokePath(QPainterPath path) {
        if (path.isEmpty() || myState.myStrokeColor == null) {
            return;
        }

        if (myState.myShadow) {
            paintShadow(path, false, true);
        }

        myPainter.strokePath(path, createPen(myState.myStrokeColor));
    }

    private void paintShadow(QPainterPath path, boolean filled, boolean stroked) {
        myPainter.save();
        try {
            myPainter.translate(myState.myShadowOffsetX, myState.myShadowOffsetY);
            myPainter.setOpacity(myState.myGlobalAlpha * myState.myShadowAlpha);

            if (filled) {
                myPainter.fillPath(path, myState.myShadowColor);
            }

            if (stroked) {
                myPainter.strokePath(path, createPen(myState.myShadowColor));
            }
        }
        finally {
            myPainter.restore();
        }
    }

    private QPen createPen(QColor color) {
        double width = Math.max(myState.myStrokeWidth, 0.01);

        QPen pen = new QPen(color, width);
        pen.setCapStyle(myState.myCapStyle);
        pen.setJoinStyle(myState.myJoinStyle);
        pen.setMiterLimit(myState.myMiterLimit);

        List<Double> dashPattern = myState.myDashPattern;
        if (myState.myDashed && dashPattern != null) {
            // qt dash lengths are multiples of the pen width, the incoming pattern is in absolute units
            List<Double> scaled = new ArrayList<>(dashPattern.size());
            for (Double dash : dashPattern) {
                scaled.add(Math.max(dash / width, 0.01));
            }

            pen.setStyle(Qt.PenStyle.CustomDashLine);
            pen.setDashPattern(scaled);
        }

        return pen;
    }

    private static @Nullable List<Double> parseDashPattern(@Nullable String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        List<Double> result = new ArrayList<>();
        for (String token : value.split("[\\s,]+")) {
            if (token.isEmpty()) {
                continue;
            }

            try {
                double dash = Double.parseDouble(token);
                if (dash > 0) {
                    result.add(dash);
                }
            }
            catch (NumberFormatException ignored) {
            }
        }

        if (result.isEmpty()) {
            return null;
        }

        // qt requires an even number of dash lengths
        if (result.size() % 2 != 0) {
            result.add(result.get(result.size() - 1));
        }

        return result;
    }

    private static QColor withAlpha(@Nullable QColor color, double alpha) {
        if (color == null) {
            return new QColor(0, 0, 0, 0);
        }

        if (alpha >= 1) {
            return color;
        }

        return new QColor(color.red(), color.green(), color.blue(), clamp((int) Math.round(color.alpha() * alpha)));
    }

    private static @Nullable QColor toQColor(@Nullable ColorValue value) {
        if (value == null) {
            return null;
        }

        RGBColor color = value.toRGB();
        return new QColor(clamp(color.getRed()), clamp(color.getGreen()), clamp(color.getBlue()), clamp(color.getAlpha()));
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
