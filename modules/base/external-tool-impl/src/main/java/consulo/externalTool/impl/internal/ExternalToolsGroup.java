/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.externalTool.impl.internal;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.platform.base.localize.ActionLocalize;
import consulo.ui.ex.action.IdeActions;

import java.util.List;

/**
 * @author traff
 */
@ActionImpl(
    id = IdeActions.GROUP_EXTERNAL_TOOLS,
    parents = @ActionParentRef(value = @ActionRef(id = IdeActions.TOOLS_MENU))
)
public class ExternalToolsGroup extends BaseExternalToolsGroup<Tool> {
    public ExternalToolsGroup() {
        super(ActionLocalize.groupExternaltoolsgroupText());
    }

    @Override
    protected List<ToolsGroup<Tool>> getToolsGroups() {
        return ToolManager.getInstance().getGroups();
    }

    @Override
    protected List<Tool> getToolsByGroupName(String groupName) {
        return ToolManager.getInstance().getTools(groupName);
    }

    @Override
    protected ToolAction createToolAction(Tool tool) {
        return new ToolAction(tool);
    }
}
