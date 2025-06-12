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

import consulo.ui.Rectangle2D;
import consulo.ui.ex.awt.JBUIScale;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * @author UNV
 * @since 2025-06-06
 */
public class BalloonWithoutArrow extends BalloonForm {
    public BalloonWithoutArrow(Rectangle2D bounds, int borderRadius) {
        super(bounds, borderRadius);
    }

    @Override
    public Shape getShape() {
        return new RoundRectangle2D.Double(
            myBodyBounds.minX(),
            myBodyBounds.minY(),
            myBodyBounds.width() - JBUIScale.scale(1),
            myBodyBounds.height() - JBUIScale.scale(1),
            myBorderRadius,
            myBorderRadius
        );
    }
}
