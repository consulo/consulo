/*
 * Copyright 2013-2025 consulo.io
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
package consulo.ide.impl.actionSystem;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.annotation.component.ActionRefAnchor;
import consulo.ui.ex.action.DefaultActionGroup;

/**
 * @author VISTALL
 * @since 21.06.2025
 */
@ActionImpl(
        id = "TouchBarEditorSearch",
        parents = {
                @ActionParentRef(value = @ActionRef(id = "TouchBar"), anchor = ActionRefAnchor.FIRST)
        },
        children = {
                @ActionRef(type = TouchBarToggleMatchCase.class)
        }
)
public class TouchBarEditorSearchActionGroup extends DefaultActionGroup {

    /* <action id="EditorSearchSession.ToggleMatchCase" class="com.intellij.find.editorHeaderActions.ToggleMatchCase"/>
      <action id="EditorSearchSession.ToggleWholeWordsOnlyAction" class="com.intellij.find.editorHeaderActions.ToggleWholeWordsOnlyAction"/>
      <action id="EditorSearchSession.ToggleRegex" class="com.intellij.find.editorHeaderActions.ToggleRegex"/>
      <!--suppress PluginXmlI18n, PluginXmlCapitalization -->
      <separator text="type.large"/>
      <action id="EditorSearchSession.PrevOccurrence" class="com.intellij.find.editorHeaderActions.PrevOccurrenceAction"/>
      <action id="EditorSearchSession.NextOccurrenceAction" class="com.intellij.find.editorHeaderActions.NextOccurrenceAction"/>
      <group id="TouchBarEditorSearch_ctrl">
        <reference ref="EditorSearchSession.ToggleMatchCase"/>
        <reference ref="EditorSearchSession.ToggleWholeWordsOnlyAction"/>
      </group>
*/
}
