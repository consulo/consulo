/*
 * Copyright 2013-2026 consulo.io
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
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnSeparator;
import consulo.ui.ex.action.ToggleAction;
import consulo.versionControlSystem.log.graph.PermanentGraph;
import consulo.versionControlSystem.log.impl.internal.data.MainVcsLogUiProperties;
import consulo.versionControlSystem.log.impl.internal.data.VcsLogUiProperties;
import consulo.versionControlSystem.log.impl.internal.ui.VcsLogInternalDataKeys;
import consulo.versionControlSystem.log.internal.VcsLogActionPlaces;
import consulo.versionControlSystem.log.localize.VersionControlSystemLogLocalize;
import consulo.versionControlSystem.log.util.BekUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-19
 */
@ActionImpl(id = VcsLogActionPlaces.GRAPH_OPTIONS_ACTION_GROUP)
public class GraphOptionsGroup extends ActionGroup implements DumbAware {
    private static class SelectSortTypeAction extends ToggleAction implements DumbAware {
        private final PermanentGraph.SortType mySortType;

        private SelectSortTypeAction(PermanentGraph.SortType sortType) {
            super(sortType.getName(), sortType.getDescription());
            mySortType = sortType;
        }

        @Override
        public void update(AnActionEvent e) {
            super.update(e);
            e.getPresentation().setEnabled(getProperties(e) != null);
        }

        @Override
        public boolean isSelected(AnActionEvent e) {
            VcsLogUiProperties properties = getProperties(e);
            return properties != null && properties.get(MainVcsLogUiProperties.BEK_SORT_TYPE) == mySortType;
        }

        @Override
        @RequiredUIAccess
        public void setSelected(AnActionEvent e, boolean state) {
            VcsLogUiProperties properties = getProperties(e);
            if (properties != null) {
                properties.set(MainVcsLogUiProperties.BEK_SORT_TYPE, mySortType);
            }
        }
    }

    public GraphOptionsGroup() {
        super(VersionControlSystemLogLocalize.groupGraphOptionsText(), VersionControlSystemLogLocalize.groupGraphOptionsDescription());
    }

    @Override
    public AnAction[] getChildren(@Nullable AnActionEvent e) {
        if (e == null) {
            return EMPTY_ARRAY;
        }

        List<AnAction> actions = new ArrayList<>();
        if (getProperties(e) != null) {
            actions.add(AnSeparator.create(VersionControlSystemLogLocalize.actionLogSortTypeSeparator()));
            for (PermanentGraph.SortType sortType : PermanentGraph.SortType.values()) {
                if (isAvailable(sortType)) {
                    actions.add(new SelectSortTypeAction(sortType));
                }
            }
        }

        ActionManager actionManager = ActionManager.getInstance();
        actions.add(AnSeparator.create(VersionControlSystemLogLocalize.actionLogBranchesSeparator()));
        actions.add(actionManager.getAction(CollapseGraphAction.class));
        actions.add(actionManager.getAction(ExpandGraphAction.class));
        return actions.toArray(EMPTY_ARRAY);
    }

    private static boolean isAvailable(PermanentGraph.SortType sortType) {
        return switch (sortType) {
            case Normal -> true;
            case Bek -> BekUtil.isBekEnabled();
            case LinearBek -> BekUtil.isLinearBekEnabled();
        };
    }

    private static @Nullable VcsLogUiProperties getProperties(AnActionEvent e) {
        VcsLogUiProperties properties = e.getData(VcsLogInternalDataKeys.LOG_UI_PROPERTIES);
        return properties != null && properties.exists(MainVcsLogUiProperties.BEK_SORT_TYPE) ? properties : null;
    }
}
