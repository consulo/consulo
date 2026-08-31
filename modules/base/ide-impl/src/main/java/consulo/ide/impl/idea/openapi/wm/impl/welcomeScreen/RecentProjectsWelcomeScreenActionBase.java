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
package consulo.ide.impl.idea.openapi.wm.impl.welcomeScreen;

import consulo.ide.impl.idea.ide.PopupProjectGroupActionGroup;
import consulo.localize.LocalizeValue;
import consulo.project.internal.RecentProjectsManager;
import consulo.ui.ListBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.LegacyDumbAwareAction;
import consulo.ui.image.Image;
import consulo.ui.model.MutableFlatDataModel;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * @author Konstantin Bulenkov
 */
public abstract class RecentProjectsWelcomeScreenActionBase extends LegacyDumbAwareAction {
    public static final Key<ListBox<AnAction>> RECENT_PROJECTS_LIST = Key.create("recent-projects-list");

    protected RecentProjectsWelcomeScreenActionBase() {
    }

    protected RecentProjectsWelcomeScreenActionBase(LocalizeValue text) {
        super(text);
    }

    protected RecentProjectsWelcomeScreenActionBase(
        LocalizeValue text,
        LocalizeValue description,
        @Nullable Image icon
    ) {
        super(text, description, icon);
    }

    public static @Nullable MutableFlatDataModel<AnAction> getDataModel(AnActionEvent e) {
        ListBox<AnAction> list = getList(e);
        return list != null && list.getDataModel() instanceof MutableFlatDataModel<AnAction> model ? model : null;
    }

    public static List<AnAction> getSelectedElements(AnActionEvent e) {
        ListBox<AnAction> list = getList(e);
        AnAction value = list == null ? null : list.getValue();
        return value == null ? List.of() : List.of(value);
    }

    public static @Nullable ListBox<AnAction> getList(AnActionEvent e) {
        return e.getData(RECENT_PROJECTS_LIST);
    }

    public static boolean hasGroupSelected(AnActionEvent e) {
        for (AnAction action : getSelectedElements(e)) {
            if (action instanceof PopupProjectGroupActionGroup) {
                return true;
            }
        }
        return false;
    }

    @RequiredUIAccess
    public static void rebuildRecentProjectsList(AnActionEvent e) {
        MutableFlatDataModel<AnAction> model = getDataModel(e);
        if (model != null) {
            rebuildRecentProjectDataModel(model);
        }
    }

    @RequiredUIAccess
    public static void rebuildRecentProjectDataModel(MutableFlatDataModel<AnAction> model) {
        model.replaceAll(List.of(RecentProjectsManager.getInstance().getRecentProjectsActions(false, true)));
    }
}
