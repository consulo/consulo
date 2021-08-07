/*
 * Copyright 2013-2021 consulo.io
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
package consulo.ui.web.internal.ex;

import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.VisualPosition;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.editor.impl.view.LogicalPositionCache;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.editor.impl.CodeEditorBase;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 07/08/2021
 */
public class WebEditorView implements Disposable  {
  private final LogicalPositionCache myLogicalPositionCache;

  public WebEditorView(CodeEditorBase editor) {
    myLogicalPositionCache = new LogicalPositionCache(editor, () -> EditorUtil.getTabSize(editor));

    Disposer.register(this, myLogicalPositionCache);
  }

  @Nonnull
  public LogicalPosition offsetToLogicalPosition(int offset) {
    return myLogicalPositionCache.offsetToLogicalPosition(offset);
  }

  public int logicalPositionToOffset(@Nonnull LogicalPosition pos) {
    return myLogicalPositionCache.logicalPositionToOffset(pos);
  }

  public void reset() {
    myLogicalPositionCache.reset(true);
  }

  @Override
  public void dispose() {

  }
}
