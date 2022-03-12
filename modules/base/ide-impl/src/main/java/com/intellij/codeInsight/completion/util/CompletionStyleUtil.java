// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.completion.util;

import consulo.language.codeStyle.CodeStyle;
import consulo.language.editor.completion.lookup.InsertionContext;
import consulo.language.Language;
import consulo.language.codeStyle.CommonCodeStyleSettings;
import consulo.language.psi.PsiUtilCore;

import javax.annotation.Nonnull;

public class CompletionStyleUtil {
  @Nonnull
  public static CommonCodeStyleSettings getCodeStyleSettings(@Nonnull InsertionContext context) {
    Language lang = PsiUtilCore.getLanguageAtOffset(context.getFile(), context.getTailOffset());
    return CodeStyle.getLanguageSettings(context.getFile(), lang);
  }
}
