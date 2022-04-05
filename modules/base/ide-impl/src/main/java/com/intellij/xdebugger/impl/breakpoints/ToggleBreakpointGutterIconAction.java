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
package com.intellij.xdebugger.impl.breakpoints;

import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.execution.debug.XDebuggerBundle;
import consulo.execution.debug.breakpoint.XBreakpoint;

/**
* @author nik
*/
class ToggleBreakpointGutterIconAction extends AnAction {
  private XBreakpoint<?> myBreakpoint;

  ToggleBreakpointGutterIconAction(XBreakpoint<?> breakpoint) {
    super(breakpoint.isEnabled() ? XDebuggerBundle.message("xdebugger.disable.breakpoint.action.text") : XDebuggerBundle.message("xdebugger.enable.breakpoint.action.text"));
    this.myBreakpoint = breakpoint;
  }

  public void actionPerformed(final AnActionEvent e) {
    myBreakpoint.setEnabled(!myBreakpoint.isEnabled());
  }
}
