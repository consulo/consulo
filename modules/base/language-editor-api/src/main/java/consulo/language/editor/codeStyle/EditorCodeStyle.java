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
package consulo.language.editor.codeStyle;

import consulo.codeEditor.Editor;
import consulo.language.Language;
import consulo.language.codeStyle.CodeStyle;
import consulo.language.codeStyle.CommonCodeStyleSettings;
import consulo.language.editor.util.PsiEditorUtil;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiUtilCore;

/**
 * @author VISTALL
 * @since 13/11/2022
 */
public class EditorCodeStyle {

  /**
   * Finds a language at the specified offset and common language settings for it.
   *
   * @param editor The current editor.
   * @param offset The offset to find the language at.
   */
  public static CommonCodeStyleSettings getLocalLanguageSettings(Editor editor, int offset) {
    PsiFile psiFile = PsiEditorUtil.getPsiFile(editor);
    Language language = PsiUtilCore.getLanguageAtOffset(psiFile, offset);
    return CodeStyle.getLanguageSettings(psiFile, language);
  }
}
