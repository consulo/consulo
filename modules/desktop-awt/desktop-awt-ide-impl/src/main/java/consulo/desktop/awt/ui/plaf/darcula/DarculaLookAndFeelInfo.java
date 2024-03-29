/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.desktop.awt.ui.plaf.darcula;

import consulo.ide.IdeBundle;
import consulo.desktop.awt.ui.plaf.LookAndFeelInfoWithClassLoader;
import consulo.desktop.awt.ui.plaf.LafWithColorScheme;
import consulo.desktop.awt.ui.plaf.LafWithIconLibrary;
import consulo.ui.image.IconLibraryManager;

import jakarta.annotation.Nonnull;

/**
 * @author Konstantin Bulenkov
 */
public class DarculaLookAndFeelInfo extends LookAndFeelInfoWithClassLoader implements LafWithColorScheme, LafWithIconLibrary {
  public DarculaLookAndFeelInfo() {
    super(IdeBundle.message("idea.dark.look.and.feel"), DarculaLaf.class.getName());
  }

  @Override
  public boolean equals(Object obj) {
    return (obj instanceof DarculaLookAndFeelInfo);
  }

  @Override
  public int hashCode() {
    return getName().hashCode();
  }

  @Nonnull
  @Override
  public String getColorSchemeName() {
    return "Darcula";
  }

  @Nonnull
  @Override
  public String getIconLibraryId() {
    return IconLibraryManager.DARK_LIBRARY_ID;
  }
}
