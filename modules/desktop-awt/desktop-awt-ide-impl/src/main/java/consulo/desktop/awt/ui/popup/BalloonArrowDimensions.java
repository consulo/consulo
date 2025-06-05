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
package consulo.desktop.awt.ui.popup;

import java.awt.*;

/**
 * @author UNV
 * @since 2025-06-06
 */
public class BalloonArrowDimensions {
    private final Point myTarget;
    private final int myWidth;
    private final int myLength;

    public BalloonArrowDimensions(Point target, int width, int length) {
        myTarget = new Point(target);
        myWidth = width;
        myLength = length;
    }

    public BalloonArrowDimensions copyWithNewTargetX(int x) {
        return new BalloonArrowDimensions(new Point(x, myTarget.y), myWidth, myLength);
    }

    public BalloonArrowDimensions copyWithNewTargetY(int y) {
        return new BalloonArrowDimensions(new Point(myTarget.x, y), myWidth, myLength);
    }

    public int getTargetX() {
        return myTarget.x;
    }

    public int getTargetY() {
        return myTarget.y;
    }

    public int getWidth() {
        return myWidth;
    }

    public int getLength() {
        return myLength;
    }

    public Point getTargetPoint() {
        return new Point(myTarget);
    }
}
