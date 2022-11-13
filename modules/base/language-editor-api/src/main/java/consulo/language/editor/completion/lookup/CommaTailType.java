// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.completion.lookup;

import consulo.codeEditor.Editor;
import consulo.language.codeStyle.CommonCodeStyleSettings;
import consulo.language.editor.codeStyle.EditorCodeStyle;

public class CommaTailType extends TailType {
  public static final TailType INSTANCE = new CommaTailType();

  @Override
  public int processTail(final Editor editor, int tailOffset) {
    CommonCodeStyleSettings styleSettings = EditorCodeStyle.getLocalLanguageSettings(editor, tailOffset);
    if (styleSettings.SPACE_BEFORE_COMMA) tailOffset = insertChar(editor, tailOffset, ' ');
    tailOffset = insertChar(editor, tailOffset, ',');
    if (styleSettings.SPACE_AFTER_COMMA) tailOffset = insertChar(editor, tailOffset, ' ');
    return tailOffset;
  }

  @Override
  public String toString() {
    return "COMMA";
  }
}
