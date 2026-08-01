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
package consulo.web.internal.ui.image;

import consulo.ui.color.ColorValue;
import consulo.ui.image.Image;

/**
 * @author VISTALL
 * @since 2026-08-01
 */
public class WebColorizeImageImpl implements Image {
    private final Image myOriginal;
    private final ColorValue myColorValue;

    public WebColorizeImageImpl(Image original, ColorValue colorValue) {
        myOriginal = original;
        myColorValue = colorValue;
    }

    public Image getOriginal() {
        return myOriginal;
    }

    public ColorValue getColorValue() {
        return myColorValue;
    }

    @Override
    public int getHeight() {
        return myOriginal.getHeight();
    }

    @Override
    public int getWidth() {
        return myOriginal.getWidth();
    }
}
