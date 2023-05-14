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
package consulo.ide.impl.idea.openapi.editor.richcopy;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.codeEditor.RawText;
import consulo.ide.impl.idea.codeInsight.editorActions.CopyPastePostProcessor;
import consulo.language.editor.action.CopyPastePreProcessor;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import jakarta.inject.Inject;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * the following binding uses 'first' order to make sure it captures raw text before any other processor modifies it
 */
@ExtensionImpl(id = "first", order = "first")
public final class TextWithMarkupRawTextSetter implements CopyPastePreProcessor {
  private final TextWithMarkupProcessor myProcessor;

  @Inject
  TextWithMarkupRawTextSetter() {
    myProcessor = CopyPastePostProcessor.EP_NAME.findExtensionOrFail(TextWithMarkupProcessor.class);
  }

  @Nullable
  @Override
  public String preprocessOnCopy(PsiFile file, int[] startOffsets, int[] endOffsets, String text) {
    myProcessor.setRawText(text);
    return null;
  }

  @Nonnull
  @Override
  public String preprocessOnPaste(Project project, PsiFile file, Editor editor, String text, RawText rawText) {
    return text;
  }
}
