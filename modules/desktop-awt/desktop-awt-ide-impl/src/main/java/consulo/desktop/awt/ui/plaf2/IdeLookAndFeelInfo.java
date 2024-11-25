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
import consulo.desktop.awt.ui.plaf.LafWithIconLibrary;
import consulo.desktop.awt.ui.plaf.LookAndFeelInfoWithClassLoader;
import consulo.ui.image.IconLibraryManager;
import jakarta.annotation.Nonnull;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 2024-11-21
 */
public class IdeLookAndFeelInfo extends LookAndFeelInfoWithClassLoader implements LafWithColorScheme, LafWithIconLibrary {
    private final boolean myDark;

    public IdeLookAndFeelInfo(String name, String className, boolean dark) {
        super(name, className);
        myDark = dark;
    }

    @Override
    public boolean equals(Object obj) {
        return getClassName().equals(((UIManager.LookAndFeelInfo) obj).getClassName());
    }

    @Override
    public int hashCode() {
        return getClassName().hashCode();
    }

    @Nonnull
    @Override
    public String getColorSchemeName() {
        return myDark ? EditorColorsScheme.DARCULA_SCHEME_NAME : EditorColorsScheme.DEFAULT_SCHEME_NAME;
    }

    @Nonnull
    @Override
    public String getIconLibraryId() {
        return myDark ? IconLibraryManager.DARK_LIBRARY_ID : IconLibraryManager.LIGHT_LIBRARY_ID;
    }
}
