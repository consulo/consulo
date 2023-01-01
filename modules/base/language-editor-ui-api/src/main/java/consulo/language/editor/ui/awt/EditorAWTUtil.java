/*
 * Copyright 2013-2022 consulo.io
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
package consulo.language.editor.ui.awt;

import consulo.annotation.DeprecationInfo;
import consulo.application.ui.UISettings;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.EditorColorsScheme;

import java.awt.*;

/**
 * @author VISTALL
 * @since 29-Apr-22
 */
public class EditorAWTUtil {
  public static Font getEditorFont() {
    EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();
    int size = UISettings.getInstance().getPresentationMode() ? UISettings.getInstance().getPresentationModeFontSize() - 4 : scheme.getEditorFontSize();
    return new Font(scheme.getEditorFontName(), Font.PLAIN, size);
  }
}
