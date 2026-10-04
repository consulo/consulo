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

import consulo.annotation.component.ActionImpl;
import consulo.application.dumb.DumbAware;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.ToggleAction;
import consulo.ui.ex.grid.GridUtil;
import consulo.ui.ex.grid.csv.CsvFormatEditor;
import consulo.ui.grid.DataGrid;
import consulo.ui.grid.ViewIndex;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

import static consulo.ui.ex.grid.DatabaseDataKeys.DATA_GRID_KEY;
import static consulo.ui.ex.grid.csv.CsvFormatEditor.CSV_FORMAT_EDITOR_KEY;

/**
 * Whether the first record is the header of the columns. The editor of a CSV format answers it; any other grid may install a
 * {@link Handler}. The action shows while the first row is selected, or on a column header.
 *
 * @author Liudmila Kornilova
 **/
@ActionImpl(id = SetFirstRowIsHeaderAction.ID)
public class SetFirstRowIsHeaderAction extends ToggleAction implements DumbAware {
    public static final String ID = "Console.TableResult.Csv.SetFirstRowIsHeader";

    public SetFirstRowIsHeaderAction() {
        super(LocalizeValue.localizeTODO("First Row Is Header"));
    }

    @Override
    public void update(AnActionEvent e) {
        DataGrid grid = e.getData(DATA_GRID_KEY);
        Handler handler = get(e);
        e.getPresentation().setEnabledAndVisible(handler != null && grid != null &&
            (grid.getSelectionModel().getSelectedRows().toView(grid).asIterable()
                .contains(ViewIndex.forRow(grid, 0)) || GridUtil.getContextColumn(grid, e).asInteger() != -1));
        super.update(e);
    }

    @Override
    public boolean isSelected(AnActionEvent e) {
        Handler handler = get(e);
        return handler != null && handler.firstRowIsHeader();
    }

    @RequiredUIAccess
    @Override
    public void setSelected(AnActionEvent e, boolean state) {
        Handler handler = get(e);
        if (handler != null) {
            handler.setFirstRowIsHeader(state);
        }
    }

    public interface Handler {
        Key<Handler> KEY = Key.create("SetFirstRowIsHeaderAction.Handler");

        boolean firstRowIsHeader();

        void setFirstRowIsHeader(boolean selected);
    }

    private static @Nullable Handler get(AnActionEvent e) {
        CsvFormatEditor csvEditor = e.getData(CSV_FORMAT_EDITOR_KEY);
        if (csvEditor != null) {
            return new Handler() {
                @Override
                public boolean firstRowIsHeader() {
                    return csvEditor.firstRowIsHeader();
                }

                @Override
                public void setFirstRowIsHeader(boolean selected) {
                    csvEditor.setFirstRowIsHeader(selected);
                }
            };
        }
        DataGrid grid = e.getData(DATA_GRID_KEY);
        return grid == null ? null : grid.getUserData(Handler.KEY);
    }
}
