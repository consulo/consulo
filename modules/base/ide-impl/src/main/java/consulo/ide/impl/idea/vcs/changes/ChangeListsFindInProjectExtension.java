/*
 * Copyright 2013-2021 consulo.io
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
package consulo.ide.impl.idea.vcs.changes;

import consulo.annotation.component.ExtensionImpl;
import consulo.find.FindModel;
import consulo.ide.impl.idea.find.impl.FindInProjectExtension;
import consulo.dataContext.DataContext;
import consulo.project.Project;
import consulo.versionControlSystem.VcsDataKeys;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.ChangeList;
import consulo.versionControlSystem.change.ChangeListManager;
import consulo.content.scope.SearchScope;
import consulo.content.scope.SearchScopeProvider;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;

/**
 * @author VISTALL
 * @since 12/12/2021
 */
@ExtensionImpl
public class ChangeListsFindInProjectExtension implements FindInProjectExtension {
  @Override
  public boolean initModelFromContext(FindModel model, DataContext dataContext) {
    Project project = dataContext.getData(Project.KEY);
    if (project == null) {
      return false;
    }

    ChangeList changeList = ArrayUtil.getFirstElement(dataContext.getData(VcsDataKeys.CHANGE_LISTS));
    if (changeList == null) {
      Change change = ArrayUtil.getFirstElement(dataContext.getData(VcsDataKeys.CHANGES));
      changeList = change == null ? null : ChangeListManager.getInstance(project).getChangeList(change);
    }

    if (changeList != null) {
      String changeListName = changeList.getName();
      ChangeListsSearchScopeProvider changeListsScopeProvider =
        SearchScopeProvider.EP_NAME.findExtension(ChangeListsSearchScopeProvider.class);
      if (changeListsScopeProvider != null) {
        SearchScope changeListScope = ContainerUtil.find(
          changeListsScopeProvider.getSearchScopes(project),
          scope -> scope.getDisplayName().equals(changeListName)
        );
        if (changeListScope != null) {
          model.setCustomScope(true);
          model.setCustomScopeName(changeListScope.getDisplayName());
          model.setCustomScope(changeListScope);
        }
      }

      return true;
    }

    return false;
  }
}
