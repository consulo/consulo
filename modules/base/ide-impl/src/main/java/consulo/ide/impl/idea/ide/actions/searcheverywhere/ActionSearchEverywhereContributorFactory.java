/*
 * Copyright 2013-2022 consulo.io
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
package consulo.ide.impl.idea.ide.actions.searcheverywhere;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.ide.impl.idea.ide.util.gotoByName.GotoActionModel;
import consulo.project.Project;
import consulo.searchEverywhere.SearchEverywhereContributor;
import consulo.searchEverywhere.SearchEverywhereContributorFactory;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.UIExAWTDataKey;

import java.awt.*;

@ExtensionImpl
public class ActionSearchEverywhereContributorFactory implements SearchEverywhereContributorFactory<GotoActionModel.MatchedValue> {
    
    @Override
    public SearchEverywhereContributor<GotoActionModel.MatchedValue> createContributor(AnActionEvent initEvent) {
        Component component = initEvent.getData(UIExAWTDataKey.CONTEXT_COMPONENT);
        DataContext dataContext = component == null ? DataManager.getInstance().createAsyncDataContext(initEvent.getDataContext()) : null;
        return new ActionSearchEverywhereContributor(
            initEvent.getData(Project.KEY),
            component,
            initEvent.getData(Editor.KEY),
            dataContext
        );
    }
}
