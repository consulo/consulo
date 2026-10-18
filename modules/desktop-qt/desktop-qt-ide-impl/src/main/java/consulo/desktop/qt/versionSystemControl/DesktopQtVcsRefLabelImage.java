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
package consulo.desktop.qt.versionSystemControl;

import consulo.desktop.qt.ui.impl.TargetQt;
import consulo.desktop.qt.ui.impl.image.DesktopQtEmptyImageImpl;
import consulo.desktop.qt.ui.impl.image.DesktopQtImage;
import consulo.ui.image.Image;
import consulo.versionControlSystem.log.paint.VcsRefLabel;
import consulo.versionControlSystem.log.paint.VcsRefLabelTag;
import io.qt.core.QPointF;
import io.qt.core.QRect;
import io.qt.core.Qt;
import io.qt.gui.QBrush;
import io.qt.gui.QIcon;
import io.qt.gui.QPainter;
import io.qt.gui.QPainterPath;
import io.qt.gui.QPixmap;

/**
 * @author VISTALL
 * @since 2026-10-18
 */
final class DesktopQtVcsRefLabelImage implements Image, DesktopQtImage {
    private final VcsRefLabel myLabel;

    DesktopQtVcsRefLabelImage(VcsRefLabel label) {
        myLabel = label;
    }

    @Override
    public QPixmap toQPixmap() {
        QPixmap pixmap = DesktopQtEmptyImageImpl.createPixmap(getWidth(), getHeight());
        QPainter painter = new QPainter(pixmap);
        try {
            painter.setRenderHint(QPainter.RenderHint.Antialiasing, true);
            paint(painter);
        }
        finally {
            painter.end();
        }
        return pixmap;
    }

    @Override
    public long paintFrame(QPainter painter, QRect rect, QIcon.Mode mode) {
        painter.save();
        try {
            painter.setRenderHint(QPainter.RenderHint.Antialiasing, true);
            painter.translate(rect.left(), rect.top());
            paint(painter);
        }
        finally {
            painter.restore();
        }
        return 0;
    }

    private void paint(QPainter painter) {
        painter.setPen(Qt.PenStyle.NoPen);
        for (VcsRefLabelTag tag : myLabel.resolveTags()) {
            painter.setBrush(new QBrush(TargetQt.to(tag.color())));
            painter.drawPath(createTag(tag));
        }
    }

    private QPainterPath createTag(VcsRefLabelTag tag) {
        float[] outline = myLabel.getOutline(tag);
        QPainterPath path = new QPainterPath();
        path.setFillRule(Qt.FillRule.OddEvenFill);
        path.moveTo(outline[0], outline[1]);
        for (int i = 2; i < outline.length; i += 2) {
            path.lineTo(outline[i], outline[i + 1]);
        }
        path.closeSubpath();

        float radius = myLabel.getHoleRadius();
        path.addEllipse(new QPointF(myLabel.getHoleCenterX(tag), myLabel.getHoleCenterY()), radius, radius);
        return path;
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
