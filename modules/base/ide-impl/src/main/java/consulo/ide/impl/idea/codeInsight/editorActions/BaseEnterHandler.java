/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

package consulo.ide.impl.idea.codeInsight.editorActions;

import consulo.codeEditor.Editor;
import consulo.document.DocCommandGroupId;
import consulo.codeEditor.action.EditorWriteActionHandler;

public abstract class BaseEnterHandler extends EditorWriteActionHandler {
  private static final String GROUP_ID = "EnterHandler.GROUP_ID";

  protected BaseEnterHandler() {
    super(false);
  }

  protected BaseEnterHandler(boolean runForEachCaret) {
    super(runForEachCaret);
  }

  @Override
  public DocCommandGroupId getCommandGroupId(Editor editor) {
    return DocCommandGroupId.withGroupId(editor.getDocument(), GROUP_ID);
  }
}
