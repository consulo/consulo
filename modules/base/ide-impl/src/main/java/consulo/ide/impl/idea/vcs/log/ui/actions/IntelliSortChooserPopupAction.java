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
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.ide.impl.idea.vcs.log.VcsLogIcons;
import consulo.ide.impl.idea.vcs.log.data.MainVcsLogUiProperties;
import consulo.ide.impl.idea.vcs.log.data.VcsLogUiProperties;
import consulo.ide.impl.idea.vcs.log.graph.PermanentGraph;
import consulo.ide.impl.idea.vcs.log.ui.VcsLogInternalDataKeys;
import consulo.ide.impl.wm.impl.ToolWindowContentUI;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.versionControlSystem.log.VcsLogUi;
import jakarta.annotation.Nonnull;

import java.awt.*;
import java.util.function.Function;

public class IntelliSortChooserPopupAction extends DumbAwareAction {
  public IntelliSortChooserPopupAction() {
    super("IntelliSort", "Change IntelliSort Type", VcsLogIcons.IntelliSort);
  }

  @Override
  @RequiredUIAccess
  public void actionPerformed(AnActionEvent e) {
    VcsLogUi logUI = e.getRequiredData(VcsLogUi.KEY);
    VcsLogUiProperties properties = e.getRequiredData(VcsLogInternalDataKeys.LOG_UI_PROPERTIES);

    ActionGroup settingsGroup = new DefaultActionGroup(ContainerUtil.map(
      PermanentGraph.SortType.values(),
      (Function<PermanentGraph.SortType, AnAction>)sortType -> new SelectIntelliSortTypeAction(logUI, properties, sortType)
    ));

    ListPopup popup = JBPopupFactory.getInstance().createActionGroupPopup(
      null,
      settingsGroup,
      e.getDataContext(),
      JBPopupFactory.ActionSelectionAid.MNEMONICS,
      true,
      ToolWindowContentUI.POPUP_PLACE
    );
    Component component = e.getInputEvent().getComponent();
    if (component instanceof ActionButtonComponent) {
      popup.showUnderneathOf(component);
    }
    else {
      popup.showInCenterOf(component);
    }
  }

  @Override
  public void update(@Nonnull AnActionEvent e) {
    super.update(e);
    VcsLogUiProperties properties = e.getData(VcsLogInternalDataKeys.LOG_UI_PROPERTIES);
    e.getPresentation().setEnabled(properties != null);
    if (properties != null && properties.exists(MainVcsLogUiProperties.BEK_SORT_TYPE)) {
      String description = "IntelliSort: " + properties.get(MainVcsLogUiProperties.BEK_SORT_TYPE).getName();
      e.getPresentation().setDescription(description);
      e.getPresentation().setText(description);
    }
  }

  private static class SelectIntelliSortTypeAction extends ToggleAction implements DumbAware {
    private final PermanentGraph.SortType mySortType;
    private final VcsLogUi myUI;
    private final VcsLogUiProperties myProperties;

    public SelectIntelliSortTypeAction(
      @Nonnull VcsLogUi ui,
      @Nonnull VcsLogUiProperties properties,
      @Nonnull PermanentGraph.SortType sortType
    ) {
      super(sortType.getName(), sortType.getDescription() + ".", null);
      myUI = ui;
      myProperties = properties;
      mySortType = sortType;
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
      super.update(e);
      e.getPresentation().setEnabled(myUI.areGraphActionsEnabled() && myProperties.exists(MainVcsLogUiProperties.BEK_SORT_TYPE));
    }

    @Override
    public boolean isSelected(@Nonnull AnActionEvent e) {
      return myProperties.exists(MainVcsLogUiProperties.BEK_SORT_TYPE)
        && myProperties.get(MainVcsLogUiProperties.BEK_SORT_TYPE).equals(mySortType);
    }

    @Override
    public void setSelected(@Nonnull AnActionEvent e, boolean state) {
      if (state && myProperties.exists(MainVcsLogUiProperties.BEK_SORT_TYPE)) {
        myProperties.set(MainVcsLogUiProperties.BEK_SORT_TYPE, mySortType);
      }
    }
  }
}
