// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.lookup;

import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import consulo.codeEditor.Editor;
import consulo.language.Language;
import consulo.language.editor.completion.TailType;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiUtilCore;
import consulo.project.Project;

public class CommaTailType extends TailType {
  public static final TailType INSTANCE = new CommaTailType();

  @Override
  public int processTail(final Editor editor, int tailOffset) {
    CommonCodeStyleSettings styleSettings = getLocalCodeStyleSettings(editor, tailOffset);
    if (styleSettings.SPACE_BEFORE_COMMA) tailOffset = insertChar(editor, tailOffset, ' ');
    tailOffset = insertChar(editor, tailOffset, ',');
    if (styleSettings.SPACE_AFTER_COMMA) tailOffset = insertChar(editor, tailOffset, ' ');
    return tailOffset;
  }

  protected static CommonCodeStyleSettings getLocalCodeStyleSettings(Editor editor, int tailOffset) {
    final PsiFile psiFile = getFile(editor);
    Language language = PsiUtilCore.getLanguageAtOffset(psiFile, tailOffset);

    final Project project = editor.getProject();
    assert project != null;
    return CodeStyleSettingsManager.getSettings(project).getCommonSettings(language);
  }

  public String toString() {
    return "COMMA";
  }
}
