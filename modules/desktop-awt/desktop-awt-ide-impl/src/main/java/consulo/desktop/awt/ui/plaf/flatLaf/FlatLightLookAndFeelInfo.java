/*
 * Copyright 2013-2024 consulo.io
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
package consulo.desktop.awt.ui.plaf.flatLaf;

import com.formdev.flatlaf.FlatLightLaf;
import consulo.colorScheme.EditorColorsScheme;
import consulo.desktop.awt.ui.plaf.LafWithColorScheme;
import consulo.desktop.awt.ui.plaf.LookAndFeelInfoWithClassLoader;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2024-08-13
 */
public class FlatLightLookAndFeelInfo extends LookAndFeelInfoWithClassLoader implements LafWithColorScheme {
    public FlatLightLookAndFeelInfo() {
        super("Flat Light", FlatLightLaf.class.getName());
    }

    @Nonnull
    @Override
    public String getColorSchemeName() {
        return EditorColorsScheme.DEFAULT_SCHEME_NAME;
    }
}