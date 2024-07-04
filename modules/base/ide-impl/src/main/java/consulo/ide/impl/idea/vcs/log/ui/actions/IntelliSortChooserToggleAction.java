/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.ide.impl.idea.vcs.log.ui.actions;

import consulo.application.dumb.DumbAware;
import consulo.ide.impl.idea.vcs.log.VcsLogIcons;
import consulo.ide.impl.idea.vcs.log.data.MainVcsLogUiProperties;
import consulo.ide.impl.idea.vcs.log.data.VcsLogUiProperties;
import consulo.ide.impl.idea.vcs.log.graph.PermanentGraph;
import consulo.ide.impl.idea.vcs.log.ui.VcsLogInternalDataKeys;
import consulo.ide.impl.idea.vcs.log.util.BekUtil;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.ToggleAction;
import consulo.versionControlSystem.log.VcsLogUi;
import jakarta.annotation.Nonnull;

public class IntelliSortChooserToggleAction extends ToggleAction implements DumbAware {
  @Nonnull
  private static final String DEFAULT_TEXT = "IntelliSort";
  @Nonnull
  private static final String DEFAULT_DESCRIPTION = "Turn IntelliSort On/Off";

  public IntelliSortChooserToggleAction() {
    super(DEFAULT_TEXT, DEFAULT_DESCRIPTION, VcsLogIcons.IntelliSort);
  }

  @Override
  public boolean isSelected(AnActionEvent e) {
    VcsLogUiProperties properties = e.getData(VcsLogInternalDataKeys.LOG_UI_PROPERTIES);
    return properties != null &&
           properties.exists(MainVcsLogUiProperties.BEK_SORT_TYPE) &&
           !properties.get(MainVcsLogUiProperties.BEK_SORT_TYPE).equals(PermanentGraph.SortType.Normal);
  }

  @Override
  public void setSelected(AnActionEvent e, boolean state) {
    VcsLogUiProperties properties = e.getData(VcsLogInternalDataKeys.LOG_UI_PROPERTIES);
    if (properties != null && properties.exists(MainVcsLogUiProperties.BEK_SORT_TYPE)) {
      PermanentGraph.SortType bekSortType = state ? PermanentGraph.SortType.Bek : PermanentGraph.SortType.Normal;
      properties.set(MainVcsLogUiProperties.BEK_SORT_TYPE, bekSortType);
    }
  }

  @Override
  public void update(@Nonnull AnActionEvent e) {
    super.update(e);

    VcsLogUi logUI = e.getData(VcsLogUi.KEY);
    VcsLogUiProperties properties = e.getData(VcsLogInternalDataKeys.LOG_UI_PROPERTIES);
    e.getPresentation().setVisible(BekUtil.isBekEnabled());
    e.getPresentation().setEnabled(BekUtil.isBekEnabled() && logUI != null);

    if (properties != null && properties.exists(MainVcsLogUiProperties.BEK_SORT_TYPE)) {
      boolean off = properties.get(MainVcsLogUiProperties.BEK_SORT_TYPE) == PermanentGraph.SortType.Normal;
      String description = "Turn IntelliSort " + (off ? "on" : "off") + ": " +
        (off ? PermanentGraph.SortType.Bek : PermanentGraph.SortType.Normal).getDescription().toLowerCase() + ".";
      e.getPresentation().setDescription(description);
      e.getPresentation().setText(description);
    }
    else {
      e.getPresentation().setText(DEFAULT_TEXT);
      e.getPresentation().setDescription(DEFAULT_DESCRIPTION);
    }
  }
}
