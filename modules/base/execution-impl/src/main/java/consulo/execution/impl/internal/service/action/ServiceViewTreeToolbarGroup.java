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

/**
 * @author VISTALL
 * @since 13.05.2024
 */
@ActionImpl(id = ServiceViewActionProvider.SERVICE_VIEW_TREE_TOOLBAR, children = {
  @ActionRef(type = ServiceViewGroupByGroup.class),
  @ActionRef(type = ServiceViewFilterGroup.class),
  @ActionRef(type = AnSeparator.class),
  @ActionRef(type = AddServiceActionGroup.class)
})
public class ServiceViewTreeToolbarGroup extends DefaultActionGroup {
  /*
 <group id="ServiceViewTreeToolbar">
      <group id="ServiceView.GroupBy" popup="true" icon="AllIcons.Actions.GroupBy"
             class="com.intellij.ide.actions.NonEmptyActionGroup">
        <action id="ServiceView.GroupByContributor" class="com.intellij.execution.services.GroupByContributorAction"/>
        <action id="ServiceView.GroupByServiceGroups" class="com.intellij.execution.services.GroupByServiceGroupsAction"/>
        <separator/>
      </group>
      <group id="ServiceView.Filter" popup="true" icon="AllIcons.General.Filter"
             class="com.intellij.ide.actions.NonEmptyActionGroup"/>
      <group id="ServiceView.OpenInNewTabGroup" popup="true" icon="AllIcons.Actions.OpenNewTab"
             class="com.intellij.execution.services.OpenInNewTabActionGroup">
        <action id="ServiceView.OpenInNewTab" class="com.intellij.execution.services.OpenInNewTabAction"/>
        <action id="ServiceView.OpenEachInNewTab" class="com.intellij.execution.services.OpenEachInNewTabAction"/>
        <action id="ServiceView.SplitByType" class="com.intellij.execution.services.SplitByTypeAction"/>
      </group>
      <separator/>
      <group id="ServiceView.AddService" popup="true" icon="AllIcons.General.Add" use-shortcut-of="NewElement"
             class="com.intellij.execution.services.AddServiceActionGroup">
      </group>
</group>
  */
}
