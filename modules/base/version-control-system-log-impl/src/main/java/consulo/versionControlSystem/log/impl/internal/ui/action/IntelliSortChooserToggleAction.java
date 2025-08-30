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
package consulo.versionControlSystem.log.impl.internal.ui.action;

import consulo.annotation.component.ActionImpl;
import consulo.application.dumb.DumbAware;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.ToggleAction;
import consulo.versionControlSystem.log.VcsLogUi;
import consulo.versionControlSystem.log.graph.PermanentGraph;
import consulo.versionControlSystem.log.impl.internal.data.MainVcsLogUiProperties;
import consulo.versionControlSystem.log.impl.internal.data.VcsLogUiProperties;
import consulo.versionControlSystem.log.internal.VcsLogActionPlaces;
import consulo.versionControlSystem.log.impl.internal.ui.VcsLogInternalDataKeys;
import consulo.versionControlSystem.log.localize.VersionControlSystemLogLocalize;
import consulo.versionControlSystem.log.util.BekUtil;
import jakarta.annotation.Nonnull;

@ActionImpl(id = VcsLogActionPlaces.VCS_LOG_INTELLI_SORT_ACTION)
public class IntelliSortChooserToggleAction extends ToggleAction implements DumbAware {
    public IntelliSortChooserToggleAction() {
        super(
            VersionControlSystemLogLocalize.actionIntelliSortChooserText(),
            VersionControlSystemLogLocalize.actionIntelliSortChooserDescription(),
            PlatformIconGroup.vcslogIntellisort()
        );
    }

    @Override
    public boolean isSelected(AnActionEvent e) {
        VcsLogUiProperties properties = e.getData(VcsLogInternalDataKeys.LOG_UI_PROPERTIES);
        return properties != null
            && properties.exists(MainVcsLogUiProperties.BEK_SORT_TYPE)
            && !properties.get(MainVcsLogUiProperties.BEK_SORT_TYPE).equals(PermanentGraph.SortType.Normal);
    }

    @Override
    @RequiredUIAccess
    public void setSelected(@Nonnull AnActionEvent e, boolean state) {
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
            PermanentGraph.SortType sortType = properties.get(MainVcsLogUiProperties.BEK_SORT_TYPE);
            int sortTypeFlag = switch (sortType) {
                case Normal -> 1;
                default -> 0;
            };
            LocalizeValue description =
                VersionControlSystemLogLocalize.actionIntelliSortChooserParametrized(sortTypeFlag, sortType.getDescription());
            e.getPresentation().setTextValue(description);
            e.getPresentation().setDescriptionValue(description);
        }
        else {
            e.getPresentation().setTextValue(VersionControlSystemLogLocalize.actionIntelliSortChooserText());
            e.getPresentation().setDescriptionValue(VersionControlSystemLogLocalize.actionIntelliSortChooserDescription());
        }
    }
}
