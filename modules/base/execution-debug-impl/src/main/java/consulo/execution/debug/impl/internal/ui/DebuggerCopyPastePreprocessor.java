/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.execution.debug.impl.internal.ui;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.codeEditor.RawText;
import consulo.language.editor.action.CopyPastePreProcessor;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.util.lang.StringUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author egor
 */
@ExtensionImpl
public class DebuggerCopyPastePreprocessor implements CopyPastePreProcessor {
  public static final Key<Boolean> REMOVE_NEWLINES_ON_PASTE = Key.create("REMOVE_NEWLINES_ON_PASTE");

  @Nullable
  @Override
  public String preprocessOnCopy(PsiFile file, int[] startOffsets, int[] endOffsets, String text) {
    return null;
  }

  @Nonnull
  @Override
  public String preprocessOnPaste(Project project, PsiFile file, Editor editor, String text, RawText rawText) {
    if (editor.getUserData(REMOVE_NEWLINES_ON_PASTE) != null) {
      return StringUtil.convertLineSeparators(text, " ");
    }
    return text;
  }
}
