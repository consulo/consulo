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

import consulo.ui.font.Font;

/**
 * Dummy-but-creatable headless {@link Font}.
 *
 * @author VISTALL
 */
public class HeadlessFont implements Font {
    private final String myName;
    private final int mySize;
    private final int myStyle;

    public HeadlessFont(String name, int size, int style) {
        myName = name;
        mySize = size;
        myStyle = style;
    }

    @Override
    public String getName() {
        return myName;
    }

    @Override
    public String getFontName() {
        return myName;
    }

    @Override
    public String getFamily() {
        return myName;
    }

    @Override
    public int getFontStyle() {
        return myStyle;
    }

    @Override
    public int getFontSize() {
        return mySize;
    }

    @Override
    public Font buildNewFont(int newSize) {
        return new HeadlessFont(myName, newSize, myStyle);
    }
}
