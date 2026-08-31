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
package consulo.it.internal.ui;

import consulo.ui.image.Image;

/**
 * Dummy-but-creatable headless {@link Image}.
 *
 * @author VISTALL
 */
public class HeadlessImage implements Image {
    private final int myWidth;
    private final int myHeight;

    public HeadlessImage() {
        this(Image.DEFAULT_ICON_SIZE, Image.DEFAULT_ICON_SIZE);
    }

    public HeadlessImage(int width, int height) {
        myWidth = width;
        myHeight = height;
    }

    @Override
    public int getWidth() {
        return myWidth;
    }

    @Override
    public int getHeight() {
        return myHeight;
    }
}
