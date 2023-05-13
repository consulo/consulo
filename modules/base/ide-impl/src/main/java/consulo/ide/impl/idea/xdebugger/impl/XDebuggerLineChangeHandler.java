/*
 * Copyright 2013-2023 consulo.io
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
package consulo.ide.impl.idea.xdebugger.impl;

import consulo.application.ReadAction;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorGutterComponentEx;
import consulo.execution.debug.breakpoint.XLineBreakpointType;
import consulo.ide.impl.idea.xdebugger.impl.breakpoints.XBreakpointUtil;
import consulo.util.lang.TriConsumer;

import java.util.List;

/**
 * from kotlin
 *
 * @author VISTALL
 * @since 13/05/2023
 */
public class XDebuggerLineChangeHandler {
  private final TriConsumer<EditorGutterComponentEx, XSourcePositionImpl, List<XLineBreakpointType>> handler;

  public XDebuggerLineChangeHandler(TriConsumer<EditorGutterComponentEx, XSourcePositionImpl, List<XLineBreakpointType>> handler) {
    this.handler = handler;
  }

  public void lineChanged(Editor editor, XSourcePositionImpl position) {
    List<XLineBreakpointType> types =
      ReadAction.compute(() -> XBreakpointUtil.getAvailableLineBreakpointTypes(editor.getProject(), position, editor));

    handler.accept((EditorGutterComponentEx)editor.getGutter(), position, types);
  }

  public void exitedGutter() {
    // TODO cancel run lineChanged??
  }
}
