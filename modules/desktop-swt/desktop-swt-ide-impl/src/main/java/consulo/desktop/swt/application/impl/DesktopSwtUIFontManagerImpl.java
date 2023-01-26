/*
 * Copyright 2013-2023 consulo.io
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
package consulo.desktop.swt.application.impl;

import consulo.annotation.component.ServiceImpl;
import consulo.application.ui.impl.internal.UIFontManagerImpl;
import consulo.util.lang.Pair;
import jakarta.inject.Singleton;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Display;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 26/01/2023
 */
@Singleton
@ServiceImpl
public class DesktopSwtUIFontManagerImpl extends UIFontManagerImpl {
  @Nonnull
  @Override
  protected Pair<String, Integer> resolveSystemFontData() {
    Display current = Display.getCurrent();

    Font font = current.getSystemFont();
    FontData fontData = font.getFontData()[0];

    return Pair.create(fontData.getName(), fontData.getHeight());
  }
}
