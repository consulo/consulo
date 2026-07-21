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

import consulo.ui.impl.style.StyleManagerImpl;
import consulo.ui.style.Style;

import java.util.List;

/**
 * Dummy-but-creatable headless style manager built on the shared {@link StyleManagerImpl}.
 *
 * @author VISTALL
 */
public class HeadlessStyleManager extends StyleManagerImpl {
    private final Style myStyle = new HeadlessStyle();

    @Override
    public List<Style> getStyles() {
        return List.of(myStyle);
    }

    @Override
    public Style getCurrentStyle() {
        return myStyle;
    }

    @Override
    public void setCurrentStyle(Style newStyle) {
    }
}
