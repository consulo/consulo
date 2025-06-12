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

import consulo.desktop.awt.ui.popup.BalloonPointerSize;
import consulo.ui.Point2D;
import consulo.ui.Rectangle2D;

/**
 * @author UNV
 * @since 2025-06-06
 */
public abstract class BalloonWithArrow extends BalloonForm {
    protected final BalloonPointerSize myPointerSize;
    protected final Point2D myPointerTarget;

    public BalloonWithArrow(Rectangle2D bounds, int borderRadius, BalloonPointerSize pointerSize, Point2D pointerTarget) {
        super(bounds, borderRadius);
        myPointerSize = pointerSize;
        myPointerTarget = pointerTarget;
    }
}
