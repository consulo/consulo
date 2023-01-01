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
package consulo.language.editor.refactoring.internal.unwrap;

import consulo.codeEditor.EditorColors;
import consulo.codeEditor.markup.HighlighterLayer;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.TextAttributes;

/**
 * @author VISTALL
 * @since 25-Jul-22
 */
public class UnwrapHelper {
  public static final int HIGHLIGHTER_LEVEL = HighlighterLayer.SELECTION + 1;

  public static TextAttributes getTestAttributesForExtract() {
    EditorColorsManager manager = EditorColorsManager.getInstance();
    return manager.getGlobalScheme().getAttributes(EditorColors.SEARCH_RESULT_ATTRIBUTES);
  }

  public static TextAttributes getTestAttributesForRemoval() {
    EditorColorsManager manager = EditorColorsManager.getInstance();
    return manager.getGlobalScheme().getAttributes(EditorColors.DELETED_TEXT_ATTRIBUTES);
  }
}
