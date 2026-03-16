// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.completion;

import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.language.editor.completion.CompletionInitializationContext;
import consulo.language.editor.completion.CompletionType;
import consulo.language.editor.impl.internal.completion.OffsetsInFile;
import consulo.language.psi.PsiFile;
import consulo.language.editor.util.PsiUtilBase;

/**
 * @author yole
 */
class CompletionInitializationContextImpl extends CompletionInitializationContext {
  private final OffsetsInFile myHostOffsets;

  CompletionInitializationContextImpl(Editor editor, Caret caret, PsiFile file, CompletionType completionType, int invocationCount) {
    super(editor, caret, PsiUtilBase.getLanguageInEditor(editor, file.getProject()), file, completionType, invocationCount);
    myHostOffsets = new OffsetsInFile(file, getOffsetMap()).toTopLevelFile();
  }

  
  OffsetsInFile getHostOffsets() {
    return myHostOffsets;
  }
}
