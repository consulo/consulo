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
import consulo.annotation.component.ActionRef;
import consulo.application.dumb.DumbAware;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.action.AnSeparator;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.IdeActions;

/**
 * The context menu of the cells and row headers of a data grid. It is not a popup group, so a data source which shows a menu of its
 * own adds this group to it flat.
 *
 * @since 2026-10-04
 */
@ActionImpl(
    id = TableResultPopupGroup.ID,
    children = {
        @ActionRef(type = SetFirstRowIsHeaderAction.class),
        @ActionRef(type = AnSeparator.class),
        @ActionRef(id = IdeActions.ACTION_COPY),
        @ActionRef(type = ClearCellsAction.class),
        @ActionRef(type = AnSeparator.class),
        @ActionRef(type = AddRowAction.class),
        @ActionRef(type = InsertRowBeforeAction.class),
        @ActionRef(type = InsertRowAfterAction.class),
        @ActionRef(type = DeleteRowsAction.class),
        @ActionRef(type = CloneRowAction.class),
        @ActionRef(type = AnSeparator.class),
        @ActionRef(type = AddColumnAction.class),
        @ActionRef(type = InsertColumnBeforeAction.class),
        @ActionRef(type = InsertColumnAfterAction.class),
        @ActionRef(type = CloneColumnAction.class),
        @ActionRef(type = DeleteColumnsAction.class)
    }
)
public class TableResultPopupGroup extends DefaultActionGroup implements DumbAware {
    public static final String ID = "Console.TableResult.PopupGroup";

    public TableResultPopupGroup() {
        super(LocalizeValue.localizeTODO("Results"), false);
    }
}
