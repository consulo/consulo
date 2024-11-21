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
package consulo.desktop.awt.ui.plaf2;

import consulo.colorScheme.EditorColorsScheme;
import consulo.desktop.awt.ui.plaf.LafWithColorScheme;
import consulo.desktop.awt.ui.plaf.LookAndFeelInfoWithClassLoader;
import consulo.ide.IdeBundle;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2024-11-20
 */
public class LightLookAndFeelInfo extends LookAndFeelInfoWithClassLoader implements LafWithColorScheme {
    public LightLookAndFeelInfo() {
        super(IdeBundle.message("idea.intellij.look.and.feel"), ConsuloFlatLightLaf.class.getName());
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof LightLookAndFeelInfo);
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }

    @Nonnull
    @Override
    public String getColorSchemeName() {
        return EditorColorsScheme.DEFAULT_SCHEME_NAME;
    }
}
