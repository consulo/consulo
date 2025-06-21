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
package consulo.ide.impl.idea.ide.actions;

import consulo.annotation.component.ActionImpl;
import consulo.dataContext.DataContext;
import consulo.language.editor.PlatformDataKeys;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.ActionLocalize;
import consulo.ui.ex.TreeExpander;

@ActionImpl(id = "CollapseAll")
public class CollapseAllAction extends TreeCollapseAllActionBase {
    public CollapseAllAction() {
        super(ActionLocalize.actionCollapseallText(), LocalizeValue.empty(), PlatformIconGroup.actionsCollapseall());
    }

    @Override
    protected TreeExpander getExpander(DataContext dataContext) {
        return dataContext.getData(PlatformDataKeys.TREE_EXPANDER);
    }
}
