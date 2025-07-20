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
package consulo.usage.impl.internal.action;

import consulo.annotation.component.ActionImpl;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.usage.RuleAction;
import consulo.usage.UsageViewSettings;
import consulo.usage.localize.UsageLocalize;

@ActionImpl(id = "UsageGrouping.FileStructure")
public class GroupByFileStructureAction extends RuleAction {
    public GroupByFileStructureAction() {
        super(UsageLocalize.actionGroupByFileStructure(), PlatformIconGroup.actionsGroupbymethod());
    }

    @Override
    protected boolean getOptionValue() {
        return UsageViewSettings.getInstance().GROUP_BY_FILE_STRUCTURE;
    }

    @Override
    protected void setOptionValue(boolean value) {
        UsageViewSettings.getInstance().GROUP_BY_FILE_STRUCTURE = value;
    }
}
