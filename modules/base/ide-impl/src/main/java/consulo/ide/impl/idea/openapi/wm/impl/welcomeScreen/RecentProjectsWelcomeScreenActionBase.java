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
import consulo.ui.ex.awt.speedSearch.NameFilteringListModel;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.awt.UIExAWTDataKey;

import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Konstantin Bulenkov
 */
public abstract class RecentProjectsWelcomeScreenActionBase extends DumbAwareAction {
    protected RecentProjectsWelcomeScreenActionBase() {
    }

    protected RecentProjectsWelcomeScreenActionBase(@Nonnull LocalizeValue text) {
        super(text);
    }

    protected RecentProjectsWelcomeScreenActionBase(
        @Nonnull LocalizeValue text,
        @Nonnull LocalizeValue description,
        @Nullable Image icon
    ) {
        super(text, description, icon);
    }

    @Nullable
    public static DefaultListModel getDataModel(@Nonnull AnActionEvent e) {
        JList list = getList(e);
        return list != null && list.getModel() instanceof NameFilteringListModel nameFilteringListModel
            && nameFilteringListModel.getOriginalModel() instanceof DefaultListModel defaultListModel ? defaultListModel : null;
    }

    @Nonnull
    public static List<AnAction> getSelectedElements(@Nonnull AnActionEvent e) {
        JList list = getList(e);
        List<AnAction> actions = new ArrayList<>();
        if (list != null) {
            for (Object value : list.getSelectedValues()) {
                if (value instanceof AnAction action) {
                    actions.add(action);
                }
            }
        }
        return actions;
    }

    @Nullable
    public static JList getList(@Nonnull AnActionEvent e) {
        Component component = e.getData(UIExAWTDataKey.CONTEXT_COMPONENT);
        return component instanceof JList jList ? jList : null;
    }

    public static boolean hasGroupSelected(@Nonnull AnActionEvent e) {
        for (AnAction action : getSelectedElements(e)) {
            if (action instanceof PopupProjectGroupActionGroup) {
                return true;
            }
        }
        return false;
    }

    public static void rebuildRecentProjectsList(@Nonnull AnActionEvent e) {
        DefaultListModel model = getDataModel(e);
        if (model != null) {
            rebuildRecentProjectDataModel(model);
        }
    }

    public static void rebuildRecentProjectDataModel(@Nonnull DefaultListModel model) {
        model.clear();
        for (AnAction action : RecentProjectsManager.getInstance().getRecentProjectsActions(false, true)) {
            //noinspection unchecked
            model.addElement(action);
        }
    }
}
