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

import java.awt.*;

/**
 * @author UNV
 * @since 2025-06-06
 */
public abstract class BalloonWithArrow extends BalloonForm {
    protected final BalloonArrowDimensions myArrowDimensions;

    public BalloonWithArrow(Rectangle bounds, int borderRadius, BalloonArrowDimensions arrowDimensions) {
        super(bounds, borderRadius);
        myArrowDimensions = arrowDimensions;
    }
}
