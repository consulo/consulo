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

import consulo.desktop.awt.ui.popup.BalloonArrowDimensions;
import consulo.desktop.awt.ui.popup.BalloonShaper;

import java.awt.*;

/**
 * @author UNV
 * @since 2025-06-06
 */
public class BalloonAtRight extends BalloonWithArrow {
    public BalloonAtRight(Rectangle bounds, int borderRadius, BalloonArrowDimensions arrowDimensions) {
        super(
            new Rectangle(
                bounds.x + arrowDimensions.getLength(),
                bounds.y,
                bounds.width - arrowDimensions.getLength(),
                bounds.height
            ),
            borderRadius,
            arrowDimensions.copyWithNewTargetX(Math.min((int)bounds.getMinX(), arrowDimensions.getTargetX()))
        );
    }

    @Override
    public Shape getShape() {
        int halfPointerWidth = myArrowDimensions.getWidth() / 2;
        return new BalloonShaper(myBodyBounds, myArrowDimensions.getTargetPoint(), myBorderRadius)
            .lineTo((int)myBodyBounds.getMinX(), myArrowDimensions.getTargetY() - halfPointerWidth)
            .toTopCurve()
            .roundUpRight()
            .toRightCurve()
            .roundRightDown()
            .toBottomCurve()
            .roundLeftDown()
            .toLeftCurve()
            .roundLeftUp()
            .vertLineTo(myArrowDimensions.getTargetY() + halfPointerWidth)
            .lineToTargetPoint()
            .close();
    }
}
