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
package consulo.execution.debug;

/**
 * @author nik
 */
public interface XDebuggerActions {
  String VIEW_BREAKPOINTS = "ViewBreakpoints";

  String RESUME = "Resume";
  String PAUSE = "Pause";

  String STEP_OVER = "StepOver";
  String STEP_INTO = "StepInto";
  String SMART_STEP_INTO = "SmartStepInto";
  String FORCE_STEP_INTO = "ForceStepInto";
  String STEP_OUT = "StepOut";

  String RUN_TO_CURSOR = "RunToCursor";
  String FORCE_RUN_TO_CURSOR = "ForceRunToCursor";
  String EDIT_TYPE_SOURCE = "Debugger.EditTypeSource";

  String SHOW_EXECUTION_POINT = "ShowExecutionPoint";
  String JUMP_TO_SOURCE = "XDebugger.JumpToSource";
  String JUMP_TO_TYPE_SOURCE = "XDebugger.JumpToTypeSource";

  String EVALUATE_EXPRESSION = "EvaluateExpression";
  String QUICK_EVALUATE_EXPRESSION = "QuickEvaluateExpression";

  String TOOL_WINDOW_TOP_TOOLBAR_GROUP = "XDebugger.ToolWindow.TopToolbar";
  String TOOL_WINDOW_LEFT_TOOLBAR_GROUP = "XDebugger.ToolWindow.LeftToolbar";
  String EVALUATE_DIALOG_TREE_POPUP_GROUP = "XDebugger.Evaluation.Dialog.Tree.Popup";
  String INSPECT_TREE_POPUP_GROUP = "XDebugger.Inspect.Tree.Popup";
  String VARIABLES_TREE_TOOLBAR_GROUP = "XDebugger.Variables.Tree.Toolbar";
  String VARIABLES_TREE_POPUP_GROUP = "XDebugger.Variables.Tree.Popup";
  String WATCHES_TREE_POPUP_GROUP = "XDebugger.Watches.Tree.Popup";
  String WATCHES_TREE_TOOLBAR_GROUP = "XDebugger.Watches.Tree.Toolbar";
  String FRAMES_TREE_POPUP_GROUP = "XDebugger.Frames.Tree.Popup";

  String ADD_TO_WATCH = "Debugger.AddToWatch";

  String XNEW_WATCH = "XDebugger.NewWatch";
  String XREMOVE_WATCH = "XDebugger.RemoveWatch";
  String XEDIT_WATCH = "XDebugger.EditWatch";
  String XCOPY_WATCH = "XDebugger.CopyWatch";

  String COPY_VALUE = "XDebugger.CopyValue";
  String SET_VALUE = "XDebugger.SetValue";

  String MUTE_BREAKPOINTS = "XDebugger.MuteBreakpoints";

  String TOGGLE_SORT_VALUES = "XDebugger.ToggleSortValues";

  String INLINE_DEBUGGER = "XDebugger.Inline";

  String AUTO_TOOLTIP = "XDebugger.AutoTooltip";
  String AUTO_TOOLTIP_ON_SELECTION = "XDebugger.AutoTooltipOnSelection";

  String MARK_OBJECT = "Debugger.MarkObject";

  String FOCUS_ON_BREAKPOINT = "Debugger.FocusOnBreakpoint";
}
