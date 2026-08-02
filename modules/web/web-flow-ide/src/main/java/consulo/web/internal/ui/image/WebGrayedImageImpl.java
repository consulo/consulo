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

import consulo.ui.image.Image;
import consulo.ui.style.StyleManager;

/**
 * @author VISTALL
 * @since 2026-08-02
 */
public class WebGrayedImageImpl implements Image {
    /**
     * The percentages the awt frontend hands to its gray filter - how far a pixel is pulled back towards
     * white once its luminance is spent, which a dark theme needs much less of.
     */
    private static final int LIGHT_PERCENT = 65;
    private static final int DARK_PERCENT = 30;

    private final Image myOriginal;

    public WebGrayedImageImpl(Image original) {
        myOriginal = original;
    }

    public Image getOriginal() {
        return myOriginal;
    }

    public int getPercent() {
        return StyleManager.get().getCurrentStyle().isDark() ? DARK_PERCENT : LIGHT_PERCENT;
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
