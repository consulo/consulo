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
package consulo.ide.impl.idea.ide.util;

import consulo.fileEditor.structureView.StructureViewModel;
import consulo.ide.impl.idea.ide.structureView.newStructureView.TreeActionsOwner;
import consulo.fileEditor.structureView.tree.Sorter;
import consulo.fileEditor.structureView.tree.TreeAction;

import java.util.HashSet;
import java.util.Set;

/**
* @author Konstantin Bulenkov
*/
class TreeStructureActionsOwner implements TreeActionsOwner {
  private final Set<TreeAction> myActions = new HashSet<TreeAction>();
  private final StructureViewModel myModel;

  TreeStructureActionsOwner(StructureViewModel model) {
    myModel = model;
  }

  @Override
  public void setActionActive(String name, boolean state) {
  }

  @Override
  public boolean isActionActive(String name) {
    for (Sorter sorter : myModel.getSorters()) {
      if (sorter.getName().equals(name)) {
        if (!sorter.isVisible()) return true;
      }
    }
    for(TreeAction action: myActions) {
      if (action.getName().equals(name)) return true;
    }
    return Sorter.ALPHA_SORTER_ID.equals(name);
  }

  public void setActionIncluded(TreeAction filter, boolean selected) {
    if (selected) {
      myActions.add(filter);
    }
    else {
      myActions.remove(filter);
    }
  }
}
