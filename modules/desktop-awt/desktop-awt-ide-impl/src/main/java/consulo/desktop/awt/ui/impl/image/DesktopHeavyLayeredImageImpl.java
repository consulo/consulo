/*
 * Copyright 2013-2017 consulo.io
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
package consulo.desktop.awt.ui.impl.image;

import consulo.desktop.awt.uiOld.LayeredIcon;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;

import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.util.Arrays;

/**
 * @author VISTALL
 * @since 2017-09-11
 */
public class DesktopHeavyLayeredImageImpl extends LayeredIcon implements Image {
    @Nonnull
    public static Icon[] remap(Image[] icons) {
        return Arrays.stream(icons).map(TargetAWT::to).toArray(Icon[]::new);
    }

    public DesktopHeavyLayeredImageImpl(int layerCount) {
        super(layerCount);
    }

    public DesktopHeavyLayeredImageImpl(@Nonnull Image... images) {
        super(remap(images));
    }

    @Override
    public int getWidth() {
        return getIconWidth();
    }

    @Override
    public int getHeight() {
        return getIconHeight();
    }
}
