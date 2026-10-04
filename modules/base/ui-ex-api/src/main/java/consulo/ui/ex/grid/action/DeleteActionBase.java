// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

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
package consulo.ui.ex.grid.action;

import consulo.application.Application;
import consulo.dataContext.DataContext;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.MessageBoxes;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.DeleteProvider;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.grid.DatabaseDataKeys;
import consulo.ui.ex.grid.GridUtil;
import consulo.ui.grid.DataGrid;
import consulo.ui.grid.GridColumn;
import consulo.ui.grid.ModelIndex;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * The base of the delete actions of the grid. An action is its own delete provider as well, so the delete key of the grid runs it.
 * <p/>
 * The column a column header menu was opened on is passed to the members which need it: it applies only to an action run from that
 * menu (see {@link GridUtil#getContextColumn(DataGrid, AnActionEvent)}), and never to the delete provider. While a cell editor is
 * open, the action and the provider are disabled, so their keys reach the editor.
 */
public abstract class DeleteActionBase extends DumbAwareAction implements DeleteProvider, GridEditAction, AnActionWithSyncUpdate {
    protected DeleteActionBase(LocalizeValue text, LocalizeValue description) {
        super(text, description, PlatformIconGroup.generalRemove());
    }

    @Override
    public void update(AnActionEvent event) {
        DataGrid grid = event.getData(DatabaseDataKeys.DATA_GRID_KEY);
        if (grid == null) {
            event.getPresentation().setText(getTemplatePresentation().getTextValue());
            event.getPresentation().setEnabled(false);
        }
        else {
            ModelIndex<GridColumn> contextColumn = GridUtil.getContextColumn(grid, event);
            int itemsCount = itemsCount(grid, contextColumn);
            ActionText text = text(grid);
            event.getPresentation().setText(itemsCount == 1 ? text.mySimpleDelete : text.mySimpleDeletePlural);
            event.getPresentation().setEnabled(!grid.isEditing() && isEnabled(grid, contextColumn));
        }

        event.getPresentation().setVisible(isVisible(grid));
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
        DataGrid grid = e.getData(DatabaseDataKeys.DATA_GRID_KEY);
        if (grid == null) {
            return;
        }
        deleteElement(grid, GridUtil.getContextColumn(grid, e));
    }

    @Override
    public boolean canDeleteElement(DataContext dataContext) {
        DataGrid grid = dataContext.getData(DatabaseDataKeys.DATA_GRID_KEY);
        return grid != null && isVisible(grid) && !grid.isEditing() && itemsCount(grid, ModelIndex.forColumn(grid, -1)) > 0;
    }

    @RequiredUIAccess
    @Override
    public void deleteElement(DataContext dataContext) {
        DataGrid grid = dataContext.getData(DatabaseDataKeys.DATA_GRID_KEY);
        if (grid == null) {
            return;
        }
        deleteElement(grid, ModelIndex.forColumn(grid, -1));
    }

    @RequiredUIAccess
    private void deleteElement(DataGrid grid, ModelIndex<GridColumn> column) {
        if (!grid.getDataHookup().isForSingleSource()) {
            return;
        }

        if (!Application.get().isUnitTestMode() && grid.getDataSupport().isSubmitImmediately()) {
            int itemsCount = itemsCount(grid, column);
            ActionText text = text(grid);
            String title = itemsCount == 1
                ? String.format(text.myDialogTitlePattern.get(), getItemName(grid, column))
                : text.myDialogTitlePlural.get();
            String message = itemsCount == 1
                ? String.format(text.myConfirmationPattern.get(), getItemName(grid, column))
                : String.format(text.myConfirmationPluralPattern.get(), itemsCount);

            // Consulo: the data source may show other rows or columns by the time the question is answered - a parse of its text
            // landing meanwhile - and the same indices name others then: nothing is deleted in that case
            List<?> targets = getTargets(grid, column);
            UIAccess uiAccess = UIAccess.current();
            MessageBoxes.okCancel()
                .title(LocalizeValue.of(title))
                .text(LocalizeValue.of(message))
                .showAsync(grid)
                .whenComplete((ok, error) -> {
                    if (Boolean.TRUE.equals(ok)) {
                        uiAccess.giveIfNeed(() -> {
                            if (sameInstances(targets, getTargets(grid, column))) {
                                doDelete(grid, column);
                            }
                        });
                    }
                });
            return;
        }

        doDelete(grid, column);
    }

    /**
     * Consulo: the rows or columns {@link #doDelete} would delete now, as the data model holds them. The delete asked about runs only
     * when the same instances are there once the question is answered.
     */
    protected abstract List<?> getTargets(DataGrid grid, ModelIndex<GridColumn> contextColumn);

    private static boolean sameInstances(List<?> targets1, List<?> targets2) {
        if (targets1.size() != targets2.size()) {
            return false;
        }
        for (int i = 0; i < targets1.size(); i++) {
            if (targets1.get(i) != targets2.get(i)) {
                return false;
            }
        }
        return true;
    }

    protected abstract String getItemName(DataGrid grid, ModelIndex<GridColumn> contextColumn);

    protected abstract boolean isEnabled(DataGrid grid, ModelIndex<GridColumn> contextColumn);

    @RequiredUIAccess
    protected abstract void doDelete(DataGrid grid, ModelIndex<GridColumn> contextColumn);

    protected abstract int itemsCount(DataGrid grid, ModelIndex<GridColumn> contextColumn);

    protected abstract ActionText text(DataGrid grid);

    protected abstract boolean isVisible(@Nullable DataGrid grid);

    protected static class ActionText {
        final LocalizeValue mySimpleDelete;
        final LocalizeValue mySimpleDeletePlural;
        final LocalizeValue myDialogTitlePattern;
        final LocalizeValue myDialogTitlePlural;
        final LocalizeValue myConfirmationPattern;
        final LocalizeValue myConfirmationPluralPattern;

        public ActionText(LocalizeValue simpleDelete,
                          LocalizeValue simpleDeletePlural,
                          LocalizeValue dialogTitlePattern,
                          LocalizeValue dialogTitlePlural,
                          LocalizeValue confirmationPattern,
                          LocalizeValue confirmationPluralPattern) {
            mySimpleDelete = simpleDelete;
            mySimpleDeletePlural = simpleDeletePlural;
            myDialogTitlePattern = dialogTitlePattern;
            myDialogTitlePlural = dialogTitlePlural;
            myConfirmationPattern = confirmationPattern;
            myConfirmationPluralPattern = confirmationPluralPattern;
        }
    }
}
