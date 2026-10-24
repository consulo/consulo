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
package consulo.desktop.qt.ui.impl.avatar;

import consulo.desktop.qt.ui.impl.DesktopQtStyleApplier;
import consulo.desktop.qt.ui.impl.image.DesktopQtImage;
import consulo.ui.AvatarStyle;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.ui.impl.avatar.AvatarPresentation;
import consulo.ui.style.ComponentColors;
import consulo.ui.style.StyleManager;
import io.qt.core.QRectF;
import io.qt.core.Qt;
import io.qt.gui.QColor;
import io.qt.gui.QFont;
import io.qt.gui.QPainter;
import io.qt.gui.QPainterPath;
import io.qt.gui.QPalette;
import io.qt.gui.QPen;
import io.qt.gui.QPixmap;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-18
 */
public final class DesktopQtAvatarPainter {
    private DesktopQtAvatarPainter() {
    }

    public static QPixmap paint(String name, @Nullable Image avatarImage, int size, double ratio, QPalette palette) {
        return paint(name, avatarImage, size, null, ratio, palette);
    }

    /**
     * @param outline an outline style, whose colour the border of the avatar is drawn in
     */
    public static QPixmap paint(String name,
                                @Nullable Image avatarImage,
                                int size,
                                @Nullable AvatarStyle outline,
                                double ratio,
                                QPalette palette) {
        Image image = AvatarPresentation.imageOf(name, avatarImage);
        ComponentColors outlineColor = AvatarPresentation.outlineColorOf(outline);
        int pixels = (int) Math.ceil(size * ratio);
        QPixmap pixmap = new QPixmap(pixels, pixels);
        pixmap.setDevicePixelRatio(ratio);
        pixmap.fill(new QColor(Qt.GlobalColor.transparent));

        boolean dark = StyleManager.get().getCurrentStyle().isDark();
        QColor initialsColor = QColor.fromHsvF(
            AvatarPresentation.hueOf(name),
            AvatarPresentation.saturation(dark),
            AvatarPresentation.brightness(dark)
        );
        QColor background = DesktopQtStyleApplier.themeColor(ComponentColors.COMPONENT_BACKGROUND, palette.color(QPalette.ColorRole.Base));
        QColor border = DesktopQtStyleApplier.themeColor(ComponentColors.BORDER, palette.color(QPalette.ColorRole.Mid));

        QPainter painter = new QPainter(pixmap);
        try {
            painter.setRenderHint(QPainter.RenderHint.Antialiasing, true);
            painter.setRenderHint(QPainter.RenderHint.SmoothPixmapTransform, true);

            QRectF rect = new QRectF(0.5, 0.5, size - 1, size - 1);
            painter.setPen(Qt.PenStyle.NoPen);
            painter.setBrush(background);
            painter.drawEllipse(rect);

            if (image != null) {
                QPixmap icon = DesktopQtImage.toQPixmap(ImageEffects.resize(image, size, size));
                QPainterPath clip = new QPainterPath();
                clip.addEllipse(new QRectF(0, 0, size, size));
                painter.save();
                painter.setClipPath(clip);
                painter.drawPixmap(new QRectF(0, 0, size, size), icon, new QRectF(0, 0, icon.width(), icon.height()));
                painter.restore();
            }
            else {
                QFont font = painter.font();
                font.setPixelSize((int) (size * 0.42));
                painter.setFont(font);
                painter.setPen(initialsColor);
                painter.drawText(rect, Qt.AlignmentFlag.AlignCenter.value(), AvatarPresentation.initialsOf(name));
            }

            painter.setBrush(Qt.BrushStyle.NoBrush);
            if (outlineColor != null) {
                double width = AvatarPresentation.OUTLINE_WIDTH;
                QPen outlinePen = new QPen(DesktopQtStyleApplier.themeColor(outlineColor, border));
                outlinePen.setWidthF(width);
                painter.setPen(outlinePen);
                painter.drawEllipse(new QRectF(width / 2, width / 2, size - width, size - width));
            }
            else {
                painter.setPen(border);
                painter.drawEllipse(rect);
            }
        }
        finally {
            painter.end();
        }
        return pixmap;
    }
}
