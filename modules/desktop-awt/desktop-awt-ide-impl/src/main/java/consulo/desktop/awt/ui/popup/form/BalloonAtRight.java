/*
 * Copyright 2013-2025 consulo.io
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
package consulo.desktop.awt.ui.popup.form;

import consulo.desktop.awt.ui.ImmutableInsets;
import consulo.desktop.awt.ui.popup.BalloonPointerSize;
import consulo.desktop.awt.ui.popup.BalloonShaper;
import consulo.ui.Point2D;
import consulo.ui.Rectangle2D;

import java.awt.*;

/**
 * @author UNV
 * @since 2025-06-06
 */
public class BalloonAtRight extends BalloonWithArrow {
    public BalloonAtRight(Rectangle2D bounds, int borderRadius, BalloonPointerSize pointerSize, Point2D pointerTarget) {
        super(
            ImmutableInsets.left(pointerSize.getLength()).stepIn(bounds),
            borderRadius,
            pointerSize,
            pointerTarget.withX(Math.min(bounds.minX(), pointerTarget.x()))
        );
    }

    @Override
    public Shape getShape() {
        int halfPointerWidth = myPointerSize.getWidth() / 2;
        return new BalloonShaper(myBodyBounds, myPointerTarget, myBorderRadius)
            .lineTo(myBodyBounds.minX(), myPointerTarget.y() - halfPointerWidth)
            .toTopCurve()
            .roundUpRight()
            .toRightCurve()
            .roundRightDown()
            .toBottomCurve()
            .roundLeftDown()
            .toLeftCurve()
            .roundLeftUp()
            .vertLineTo(myPointerTarget.y() + halfPointerWidth)
            .lineToTargetPoint()
            .close();
    }
}
