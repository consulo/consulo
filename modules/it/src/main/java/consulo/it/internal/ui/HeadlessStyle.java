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

import consulo.ui.color.ColorValue;
import consulo.ui.color.RGBColor;
import consulo.ui.style.Style;
import consulo.ui.style.StyleColorValue;

/**
 * Dummy-but-creatable headless {@link Style}.
 *
 * @author VISTALL
 */
public class HeadlessStyle implements Style {
    @Override
    public String getId() {
        return Style.LIGHT_ID;
    }

    @Override
    public String getName() {
        return "Headless";
    }

    @Override
    public ColorValue getColorValue(StyleColorValue colorKey) {
        return new RGBColor(0, 0, 0);
    }
}
