/*
 * Copyright 2013-2024 consulo.io
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
package consulo.execution.impl.internal.service.action;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionRef;
import consulo.execution.impl.internal.service.ServiceViewActionProvider;
import consulo.ui.ex.action.AnSeparator;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.IdeActions;

/**
 * @author VISTALL
 * @since 17.05.2024
 */
@ActionImpl(id = ServiceViewActionProvider.SERVICE_VIEW_ITEM_POPUP, children = {
  @ActionRef(type = ItemPopupActionGroup.class),
  @ActionRef(type = AnSeparator.class),
  @ActionRef(id = IdeActions.ACTION_DELETE),
  @ActionRef(type = AnSeparator.class),
  @ActionRef(id = IdeActions.ACTION_EDIT_SOURCE),
  @ActionRef(type = JumpToServicesAction.class)
})
public class ServiceViewItemPopupGroup extends DefaultActionGroup {
  /*
        <group id="ServiceViewItemPopup">
      <group class="com.intellij.execution.services.ServiceViewActionProvider$ItemPopupActionGroup" id="ServiceViewItemPopupGroup"/>
      <separator/>
      <reference id="ServiceView.OpenInNewTab"/>
      <reference id="ServiceView.OpenEachInNewTab"/>
      <reference id="ServiceView.SplitByType"/>
      <action id="ServiceView.OpenInToolWindow" class="com.intellij.execution.services.OpenInToolWindowAction"/>
      <separator/>
      <reference id="$Delete"/>
      <separator/>
      <reference id="EditSource"/>
      <action id="ServiceView.JumpToServices" class="com.intellij.execution.services.JumpToServicesAction" use-shortcut-of="ShowNavBar"/>
    </group>
   */
}
