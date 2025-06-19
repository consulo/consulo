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
package consulo.language.editor.util;

import consulo.application.ApplicationManager;
import consulo.codeEditor.Editor;
import consulo.codeEditor.TextComponentEditor;
import consulo.codeEditor.localize.CodeEditorLocalize;
import consulo.language.editor.hint.HintManager;
import consulo.ui.annotation.RequiredUIAccess;

/**
 * @author VISTALL
 * @since 2022-04-21
 */
public class LanguageEditorUtil {
  /**
   * @return true when not viewer
   * false otherwise, additionally information hint with warning would be shown
   */
  @RequiredUIAccess
  public static boolean checkModificationAllowed(Editor editor) {
    if (!editor.isViewer()) return true;
    if (ApplicationManager.getApplication().isHeadlessEnvironment() || editor instanceof TextComponentEditor) return false;

    HintManager.getInstance().showInformationHint(editor, CodeEditorLocalize.editingViewerHint());
    return false;
  }
}
