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
package consulo.ide.impl.idea.codeInsight.completion;

/**
 * @author peter
 */
public interface CodeCompletionFeatures {
  String EXCLAMATION_FINISH = "editing.completion.finishByExclamation";
  String SECOND_BASIC_COMPLETION = "editing.completion.second.basic";
  String EDITING_COMPLETION_SMARTTYPE_GENERAL = "editing.completion.smarttype.general";
  String EDITING_COMPLETION_BASIC = "editing.completion.basic";
  String EDITING_COMPLETION_CLASSNAME = "editing.completion.classname";
  String EDITING_COMPLETION_CAMEL_HUMPS = "editing.completion.camelHumps";
  String EDITING_COMPLETION_REPLACE = "editing.completion.replace";
  String EDITING_COMPLETION_FINISH_BY_DOT_ETC = "editing.completion.finishByDotEtc";
  String EDITING_COMPLETION_FINISH_BY_CONTROL_DOT = "editing.completion.finishByCtrlDot";
  String EDITING_COMPLETION_FINISH_BY_SMART_ENTER = "editing.completion.finishBySmartEnter";

  String EDITING_COMPLETION_CONTROL_ENTER = "editing.completion.finishByControlEnter";
  String EDITING_COMPLETION_CONTROL_ARROWS = "editing.completion.cancelByControlArrows";
  String EDITING_COMPLETION_CHANGE_SORTING = "editing.completion.changeSorting";
}
