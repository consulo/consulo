/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.colorScheme.internal;

import java.util.List;

public interface FontPreferences {
    String DEFAULT_FONT_NAME = "Jetbrains Mono";
    int DEFAULT_FONT_SIZE = 13;
    float DEFAULT_LINE_SPACING = 1.2f;

    List<String> getEffectiveFontFamilies();

    List<String> getRealFontFamilies();

    String getFontFamily();

    int getSize(String fontFamily);

    void copyTo(FontPreferences preferences);

    boolean useLigatures();

    boolean hasSize(String fontName);

    float getLineSpacing();

    void setLineSpacing(float lineSpacing);
}
