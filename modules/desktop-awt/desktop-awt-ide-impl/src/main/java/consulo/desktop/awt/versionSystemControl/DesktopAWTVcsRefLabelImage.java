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
package consulo.desktop.awt.versionSystemControl;

import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import consulo.versionControlSystem.log.paint.VcsRefLabel;
import consulo.versionControlSystem.log.paint.VcsRefLabelTag;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;

/**
 * @author VISTALL
 * @since 2026-10-18
 */
final class DesktopAWTVcsRefLabelImage implements Image, Icon {
    private final VcsRefLabel myLabel;

    DesktopAWTVcsRefLabelImage(VcsRefLabel label) {
        myLabel = label;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.translate(x, y);
            for (VcsRefLabelTag tag : myLabel.resolveTags()) {
                g2.setColor(TargetAWT.to(tag.color()));
                g2.fill(createTag(tag));
            }
        }
        finally {
            g2.dispose();
        }
    }

    private Area createTag(VcsRefLabelTag tag) {
        float[] outline = myLabel.getOutline(tag);
        Path2D.Float path = new Path2D.Float();
        path.moveTo(outline[0], outline[1]);
        for (int i = 2; i < outline.length; i += 2) {
            path.lineTo(outline[i], outline[i + 1]);
        }
        path.closePath();

        float radius = myLabel.getHoleRadius();
        Area area = new Area(path);
        area.subtract(new Area(new Ellipse2D.Float(
            myLabel.getHoleCenterX(tag) - radius,
            myLabel.getHoleCenterY() - radius,
            2 * radius,
            2 * radius
        )));
        return area;
    }

    @Override
    public int getIconWidth() {
        return myLabel.getWidth();
    }

    @Override
    public int getIconHeight() {
        return myLabel.getHeight();
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
