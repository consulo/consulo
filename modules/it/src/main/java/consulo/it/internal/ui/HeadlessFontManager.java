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
import consulo.ui.font.FontManager;

import java.util.Set;

/**
 * Dummy-but-creatable headless {@link FontManager}.
 *
 * @author VISTALL
 */
public class HeadlessFontManager implements FontManager {
    @Override
    public Set<String> getAvailableFontNames() {
        return Set.of();
    }

    @Override
    public Font createFont(String fontName, int fontSize, int fontStyle) {
        return new HeadlessFont(fontName, fontSize, fontStyle);
    }
}
