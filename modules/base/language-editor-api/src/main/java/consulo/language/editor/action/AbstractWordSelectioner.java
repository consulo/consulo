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

package consulo.language.editor.action;

import consulo.annotation.access.RequiredReadAction;
import consulo.codeEditor.Editor;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;

import java.util.ArrayList;
import java.util.List;

/**
 * @author yole
 */
public abstract class AbstractWordSelectioner extends ExtendWordSelectionHandlerBase {
  @Override
  public boolean canSelect(PsiElement e) {
    return false;
  }

  @Override
  @RequiredReadAction
  public List<TextRange> select(PsiElement e, CharSequence editorText, int cursorOffset, Editor editor) {
    List<TextRange> ranges;
    if (canSelect(e)) {
      ranges = super.select(e, editorText, cursorOffset, editor);
    }
    else {
      ranges = new ArrayList<>();
    }
    SelectWordUtil.addWordSelection(editor.getSettings().isCamelWords(), editorText, cursorOffset, ranges);
    return ranges;
  }
}