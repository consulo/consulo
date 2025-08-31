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
package consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.actions;

import consulo.application.AllIcons;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.ToggleAction;
import consulo.application.dumb.DumbAware;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.LayoutTreeComponent;

/**
 * @author nik
 */
public class SortElementsToggleAction extends ToggleAction implements DumbAware {
  private final LayoutTreeComponent myLayoutTreeComponent;

  public SortElementsToggleAction(LayoutTreeComponent layoutTreeComponent) {
    super("Sort Elements by Names and Types", "Sort Elements by Names and Types", AllIcons.ObjectBrowser.Sorted);
    myLayoutTreeComponent = layoutTreeComponent;
  }

  @Override
  public boolean isSelected(AnActionEvent e) {
    return myLayoutTreeComponent.isSortElements();
  }

  @Override
  public void setSelected(AnActionEvent e, boolean state) {
    myLayoutTreeComponent.setSortElements(state);
  }
}
