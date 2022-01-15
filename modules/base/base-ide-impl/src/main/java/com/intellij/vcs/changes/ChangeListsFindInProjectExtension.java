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
package com.intellij.vcs.changes;

import com.intellij.find.FindModel;
import com.intellij.find.impl.FindInProjectExtension;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsDataKeys;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeList;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.SearchScopeProvider;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;

/**
 * @author VISTALL
 * @since 12/12/2021
 */
public class ChangeListsFindInProjectExtension implements FindInProjectExtension {
  @Override
  public boolean initModelFromContext(FindModel model, DataContext dataContext) {
    Project project = dataContext.getData(CommonDataKeys.PROJECT);
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
      ChangeListsSearchScopeProvider changeListsScopeProvider = SearchScopeProvider.EP_NAME.findExtension(ChangeListsSearchScopeProvider.class);
      if (changeListsScopeProvider != null) {
        SearchScope changeListScope = ContainerUtil.find(changeListsScopeProvider.getSearchScopes(project), scope -> scope.getDisplayName().equals(changeListName));
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
