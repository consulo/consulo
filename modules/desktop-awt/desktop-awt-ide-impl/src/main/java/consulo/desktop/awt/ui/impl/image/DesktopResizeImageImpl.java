/*
 * Copyright 2013-2019 consulo.io
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

import consulo.ide.impl.idea.ui.SizedIcon;
import consulo.ui.image.Image;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 2019-02-09
 */
public class DesktopResizeImageImpl extends SizedIcon implements Image {
    public DesktopResizeImageImpl(Icon delegate, int width, int height) {
        super(delegate, width, height);
    }

    @Override
    public int getHeight() {
        return getIconHeight();
    }

    @Override
    public int getWidth() {
        return getIconWidth();
    }
}