/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package com.intellij.codeInsight.daemon.impl;

import consulo.application.ApplicationManager;
import consulo.codeEditor.markup.TextAttributes;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.rawHighlight.HighlightInfo;
import consulo.language.editor.rawHighlight.HighlightInfoFilter;
import consulo.language.psi.PsiFile;

import javax.annotation.Nonnull;

public class HighlightInfoFilterImpl implements HighlightInfoFilter {
  private static final boolean ourTestMode = ApplicationManager.getApplication().isUnitTestMode();

  @Override
  public boolean accept(@Nonnull HighlightInfo info, PsiFile file) {
    if (ourTestMode) return true; // Tests need to verify highlighting is applied no matter what attributes are defined for this kind of highlighting

    TextAttributes attributes = info.getTextAttributes(file, null);
    // optimization
     return attributes == TextAttributes.ERASE_MARKER || attributes != null &&
           !(attributes.isEmpty() && info.getSeverity() == HighlightSeverity.INFORMATION && info.getGutterIconRenderer() == null);
  }
}
