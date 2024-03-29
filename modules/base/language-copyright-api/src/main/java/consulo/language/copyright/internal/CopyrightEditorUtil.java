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
package consulo.language.copyright.internal;

import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorSettings;

/**
 * @author VISTALL
 * @since 13-Jun-22
 */
public class CopyrightEditorUtil {
  public static void setupEditor(Editor editor) {
    EditorSettings settings = editor.getSettings();
    settings.setLineNumbersShown(false);
    settings.setFoldingOutlineShown(false);
    settings.setIndentGuidesShown(false);
    settings.setLineMarkerAreaShown(false);
  }
}
