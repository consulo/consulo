/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.ide.impl.idea.codeInsight.editorActions.moveUpDown;

import consulo.codeEditor.Editor;
import consulo.language.editor.moveUpDown.StatementUpDownMover;
import consulo.language.psi.PsiFile;

import jakarta.annotation.Nullable;

/**
 * @author Dennis.Ushakov
 */
class MoveLineHandler extends BaseMoveHandler {
  public MoveLineHandler(boolean down) {
    super(down);
  }

  @Override
  @Nullable
  protected MoverWrapper getSuitableMover(Editor editor, PsiFile file) {
    StatementUpDownMover.MoveInfo info = new StatementUpDownMover.MoveInfo();
    info.indentTarget = false;
    StatementUpDownMover mover = new DefaultLineMover();
    return mover.checkAvailable(editor, file, info, isDown) ? new MoverWrapper(mover, info, isDown) : null;
  }
}
