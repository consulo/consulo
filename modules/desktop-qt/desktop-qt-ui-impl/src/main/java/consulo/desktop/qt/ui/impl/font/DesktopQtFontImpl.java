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
package consulo.desktop.qt.ui.impl.font;

import consulo.ui.font.Font;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtFontImpl implements Font {
    private final String myFontName;
    private final int myFontSize;
    private final int myFontStyle;

    public DesktopQtFontImpl(String fontName, int fontSize, int fontStyle) {
        myFontName = fontName;
        myFontSize = fontSize;
        myFontStyle = fontStyle;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getFontName() {
        return null;
    }

    @Override
    public String getFamily() {
        return null;
    }

    @Override
    public int getFontStyle() {
        return 0;
    }

    @Override
    public int getFontSize() {
        return 0;
    }

    @Override
    public Font buildNewFont(int newSize) {
        return new DesktopQtFontImpl(myFontName, newSize, myFontStyle);
    }
}
